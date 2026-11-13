package org.hl7.fhir.igtools.publisher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Brings a folder up to date with a freshly built copy of it, touching only what differs. In watch mode the build
 * writes to a staging folder and this moves what is new or changed into the output folder and deletes what the
 * build no longer produced, so the output stays complete while a rebuild runs.
 *
 * <p>Each folder is listed once (on Windows the listing carries the sizes, so there is no call per file to find
 * them). A file whose size and content are the same is left alone, so its modification time doesn't change. A new
 * or changed file is moved from the staging folder, replacing the old one in one rename, so no one sees it half
 * written (across file systems, a copy next to it is renamed over it), and qa.json is moved last. Stale files are
 * deleted before anything is moved in, so a name that only changed case (one file on Windows and macOS) ends up
 * with the new case. A read-only file is made writable and tried again.
 *
 * <p>Files that are moved are no longer in the staging folder afterwards, unless the sync is asked to keep them (then
 * they are copied).
 */
public class OutputFolderSync {

  public static class Result {
    private int written;
    private int deleted;
    private int unchanged;
    private final List<String> failures = new ArrayList<>();

    /** new or changed files */
    public int getWritten() {
      return written;
    }

    /** stale files and folders */
    public int getDeleted() {
      return deleted;
    }

    public int getUnchanged() {
      return unchanged;
    }

    /** a message for each file or folder that couldn't be updated or deleted */
    public List<String> getFailures() {
      return Collections.unmodifiableList(failures);
    }
  }

  private static final int BUFFER = 64 * 1024;
  private static final String QA_JSON = "qa.json";

  private OutputFolderSync() {
  }

  public static Result sync(Path staged, Path target) throws IOException {
    return sync(staged, target, false);
  }

  /**
   * As sync(staged, target), but if keep is true the new and changed files are copied rather than moved, so that the
   * staging folder still has the whole output afterwards (a Jekyll kept running between builds only writes what
   * changed into it: see JekyllDriver)
   */
  public static Result sync(Path staged, Path target, boolean keep) throws IOException {
    Result result = new Result();
    // a folder that is a link (output linked into a web server's folder, say) is synced where it points
    staged = staged.toRealPath();
    boolean exists = Files.isDirectory(target);
    if (!exists) {
      Files.createDirectories(target);
    }
    target = target.toRealPath();
    if (staged.startsWith(target) || target.startsWith(staged)) {
      // everything in the target that isn't staged, the staging folder itself included, would be deleted
      throw new IOException("The folder " + target + " holds, or is inside, the staging folder " + staged);
    }
    Map<String, BasicFileAttributes> stagedFiles = new HashMap<>();
    Set<String> stagedDirs = new HashSet<>();
    // what can't be listed here would look stale, and be deleted from the target
    list(staged, stagedFiles, stagedDirs, null);

    Map<String, BasicFileAttributes> targetFiles = new HashMap<>();
    Set<String> targetDirs = new HashSet<>();
    if (exists) {
      // what can't be listed here is left alone
      list(target, targetFiles, targetDirs, result);
    }

    for (String rel : targetFiles.keySet()) {
      if (!stagedFiles.containsKey(rel)) {
        delete(target.resolve(rel), rel, result);
      }
    }
    // children sort after their parents, so in reverse order a folder is emptied before it is deleted
    TreeSet<String> staleDirs = new TreeSet<>(Collections.reverseOrder());
    for (String rel : targetDirs) {
      if (!stagedDirs.contains(rel)) {
        staleDirs.add(rel);
      }
    }
    for (String rel : staleDirs) {
      if (delete(target.resolve(rel), rel, result)) {
        targetDirs.remove(rel);
      }
    }

    for (String rel : stagedDirs) {
      if (!targetDirs.contains(rel)) {
        try {
          // an empty folder, or the parent of the files below
          Files.createDirectories(target.resolve(rel));
          targetDirs.add(rel);
        } catch (IOException ex) {
          result.failures.add("Unable to create " + rel + ": " + describe(ex));
        }
      }
    }
    byte[] ba = new byte[BUFFER];
    byte[] bb = new byte[BUFFER];
    List<Map.Entry<String, BasicFileAttributes>> files = new ArrayList<>(stagedFiles.size());
    Map.Entry<String, BasicFileAttributes> qa = null;
    for (Map.Entry<String, BasicFileAttributes> e : stagedFiles.entrySet()) {
      if (e.getKey().equals(QA_JSON)) {
        qa = e;
      } else {
        files.add(e);
      }
    }
    if (qa != null) {
      // last, so that what waits for it to change finds the pages in place
      files.add(qa);
    }
    for (Map.Entry<String, BasicFileAttributes> e : files) {
      String rel = e.getKey();
      Path from = staged.resolve(rel);
      Path to = target.resolve(rel);
      BasicFileAttributes old = targetFiles.get(rel);
      try {
        if (old != null && old.isRegularFile() && old.size() == e.getValue().size() && sameContent(from, to, ba, bb)) {
          result.unchanged++;
        } else {
          if (keep) {
            copy(from, to);
          } else {
            move(from, to);
          }
          result.written++;
        }
      } catch (IOException ex) {
        result.failures.add("Unable to update " + rel + ": " + describe(ex));
      }
    }
    return result;
  }

  /**
   * Copies into the target the files of the staging folder that are new or changed there, before the build that writes
   * to the staging folder has finished (in watch mode: the pages, once Jekyll has rendered them). Nothing is deleted,
   * qa.json is left for the sync that follows when the build has finished (what waits for it to change then finds the
   * whole output in place), and the files stay in the staging folder. As in a sync, a file is replaced in one rename,
   * and one whose content is the same is left alone.
   *
   * @param files the files to copy, in the staging folder (those that aren't there, or aren't files, are skipped), or
   *              null for every file in it
   * @param leftOut tells, by its path relative to the staging folder, a file that isn't copied (null: none)
   * @param offered what the calls before this one copied to the target, by relative path (a digest of the content),
   *              kept up to date here; or null. A file that is as it was then, and still in the target, isn't
   *              looked at again: the target has it, or what the build made of it since (the html checks change
   *              the pages Jekyll wrote), which a copy would put back to how it was before
   */
  public static Result publish(Path staged, Path target, Collection<Path> files, Predicate<String> leftOut,
      Map<String, String> offered) throws IOException {
    return publish(staged, target, files, leftOut, offered, null);
  }

  /**
   * As publish(staged, target, files, leftOut, offered), while the build goes on writing to the staging folder: a file
   * last modified after asOf (null: no bound), or that changes while it is copied, is left for the sync, so that what is
   * only partly written doesn't get into the target
   */
  public static Result publish(Path staged, Path target, Collection<Path> files, Predicate<String> leftOut,
      Map<String, String> offered, FileTime asOf) throws IOException {
    Result result = new Result();
    // the files are named under the staging folder as it was given, which may be reached through a link
    Path given = staged.toAbsolutePath().normalize();
    staged = staged.toRealPath();
    // (a folder that is a link is one already: creating it fails before Java 20)
    if (!Files.isDirectory(target)) {
      Files.createDirectories(target);
    }
    target = target.toRealPath();
    if (staged.startsWith(target) || target.startsWith(staged)) {
      throw new IOException("The folder " + target + " holds, or is inside, the staging folder " + staged);
    }
    Set<String> rels = new TreeSet<>();
    if (files == null) {
      Map<String, BasicFileAttributes> all = new HashMap<>();
      list(staged, all, new HashSet<>(), null);
      rels.addAll(all.keySet());
    } else {
      for (Path f : files) {
        Path p = given.resolve(f).normalize();
        if (p.startsWith(given) && Files.isRegularFile(p)) {
          rels.add(given.relativize(p).toString());
        }
      }
    }
    rels.remove(QA_JSON);
    if (leftOut != null) {
      rels.removeIf(leftOut);
    }
    byte[] ba = new byte[BUFFER];
    byte[] bb = new byte[BUFFER];
    Set<Path> dirs = new HashSet<>();
    for (String rel : rels) {
      Path from = staged.resolve(rel);
      Path to = target.resolve(rel);
      try {
        BasicFileAttributes was = Files.readAttributes(from, BasicFileAttributes.class);
        if (asOf != null && was.lastModifiedTime().compareTo(asOf) > 0) {
          continue;
        }
        String digest = offered == null ? null : digest(from, ba);
        boolean there = Files.isRegularFile(to);
        if (there && digest != null && digest.equals(offered.get(rel))) {
          result.unchanged++;
          continue;
        }
        if (dirs.add(to.getParent())) {
          Files.createDirectories(to.getParent());
        }
        if (there && Files.size(to) == Files.size(from) && sameContent(from, to, ba, bb)) {
          // (not noted as offered: it may be what the build made of the file, put back in the staging folder)
          result.unchanged++;
        } else if (copy(from, to, asOf == null ? null : was)) {
          result.written++;
          if (digest != null) {
            offered.put(rel, digest);
          }
        } else if (offered != null) {
          offered.remove(rel);
        }
      } catch (IOException ex) {
        if (offered != null) {
          offered.remove(rel);
        }
        result.failures.add("Unable to update " + rel + ": " + describe(ex));
      }
    }
    return result;
  }

  /**
   * the files and folders under root, by their path relative to it. If result is given, an entry that can't be read is
   * reported there and left out, else it fails the listing
   */
  private static void list(Path root, Map<String, BasicFileAttributes> files, Set<String> dirs, Result result) throws IOException {
    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
        if (result == null || file.equals(root)) {
          throw e;
        }
        result.failures.add("Unable to read " + root.relativize(file) + ": " + describe(e));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (!dir.equals(root)) {
          dirs.add(root.relativize(dir).toString());
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        files.put(root.relativize(file).toString(), attrs);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  private static String digest(Path file, byte[] buffer) throws IOException {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
    try (InputStream in = Files.newInputStream(file)) {
      int n;
      while ((n = in.read(buffer)) > 0) {
        md.update(buffer, 0, n);
      }
    }
    return new BigInteger(1, md.digest()).toString(16);
  }

  private static boolean sameContent(Path a, Path b, byte[] ba, byte[] bb) throws IOException {
    try (InputStream ia = Files.newInputStream(a); InputStream ib = Files.newInputStream(b)) {
      while (true) {
        int na = ia.readNBytes(ba, 0, BUFFER);
        int nb = ib.readNBytes(bb, 0, BUFFER);
        if (na != nb || !Arrays.equals(ba, 0, na, bb, 0, nb)) {
          return false;
        }
        if (na < BUFFER) {
          return true;
        }
      }
    }
  }

  /** replaces a file with another, in one rename where the file system allows it */
  static void move(Path from, Path to) throws IOException {
    try {
      rename(from, to);
    } catch (AccessDeniedException e) {
      // a read-only file can't be replaced on Windows
      if (!makeWritable(to)) {
        throw e;
      }
      rename(from, to);
    }
  }

  /** replaces a file with a copy of another, in one rename: the copy is made next to it first */
  static void copy(Path from, Path to) throws IOException {
    copy(from, to, null);
  }

  /**
   * As copy(from, to), unless from no longer has the attributes was (null: no check) once the copy is made: then the
   * copy is dropped, and false returned
   */
  private static boolean copy(Path from, Path to, BasicFileAttributes was) throws IOException {
    try {
      return copyOver(from, to, was);
    } catch (AccessDeniedException e) {
      // a read-only file can't be replaced on Windows
      if (!makeWritable(to)) {
        throw e;
      }
      return copyOver(from, to, was);
    }
  }

  private static boolean copyOver(Path from, Path to, BasicFileAttributes was) throws IOException {
    // (a copy left by a sync that stopped is stale, and deleted by the next)
    Path copy = to.resolveSibling(".sync-" + to.getFileName());
    Files.copy(from, copy, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    try {
      if (was != null && !isAsItWas(from, was)) {
        Files.delete(copy);
        return false;
      }
      Files.move(copy, to, StandardCopyOption.ATOMIC_MOVE);
      return true;
    } catch (IOException ex) {
      Files.deleteIfExists(copy);
      throw ex;
    }
  }

  /** whether a file has not been written since it had these attributes */
  private static boolean isAsItWas(Path file, BasicFileAttributes was) throws IOException {
    BasicFileAttributes now = Files.readAttributes(file, BasicFileAttributes.class);
    return now.lastModifiedTime().equals(was.lastModifiedTime()) && now.size() == was.size();
  }

  private static void rename(Path from, Path to) throws IOException {
    try {
      // replaces the file in one call: a move that isn't atomic deletes it first
      Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      // the staging folder is on another file system, where a move deletes the file and then copies: copy next to
      // it and rename that over it instead (a copy left by a sync that stopped is stale, and deleted by the next)
      Path copy = to.resolveSibling(".sync-" + to.getFileName());
      Files.copy(from, copy, StandardCopyOption.REPLACE_EXISTING);
      try {
        Files.move(copy, to, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException ex) {
        Files.deleteIfExists(copy);
        throw ex;
      }
      try {
        Files.delete(from);
      } catch (IOException ex) {
        // the file is in place; the staging folder is emptied at the start of the next build
      }
    }
  }

  private static boolean delete(Path p, String rel, Result result) {
    try {
      try {
        Files.delete(p);
      } catch (AccessDeniedException e) {
        // a read-only file can't be deleted on Windows
        if (!makeWritable(p)) {
          throw e;
        }
        Files.delete(p);
      }
      result.deleted++;
      return true;
    } catch (IOException e) {
      result.failures.add("Unable to delete " + rel + ": " + describe(e));
      return false;
    }
  }

  private static boolean makeWritable(Path p) {
    File f = p.toFile();
    return f.exists() && !f.canWrite() && f.setWritable(true);
  }

  private static String describe(IOException e) {
    return e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " " + e.getMessage());
  }
}
