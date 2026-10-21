package org.hl7.fhir.igtools.publisher;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Watch mode, Rapido differential build: a complete build starts with an empty temp folder, a differential build
 * starts with what the last build left there. A file the last build wrote that this build doesn't write or reuse (e.g.
 * the pages of a resource whose id changed, or an output a resource no longer has) would still be there when Jekyll
 * runs, and end up in the output. This finds those files, so they can be deleted before Jekyll runs.
 *
 * <p>A file is kept if this build wrote it (its status-change time, which can't be set by a program, is not before the
 * marker this writes where a complete build would clear the temp folder), or if the build claims it without writing it
 * again: the publisher doesn't rewrite a file whose content is unchanged, and a reused (replayed) phase claims the
 * outputs it wrote in an earlier build. Everything else is what a complete build wouldn't have.
 */
public class StaleTempFiles {

  private static final String CTIME = "unix:ctime";

  private final Path temp;
  private final Path marker;
  private final Set<Path> skip = new HashSet<>();

  /**
   * @param skip folders (or files) under temp that are left alone (e.g. Rapido's own state)
   */
  public StaleTempFiles(String temp, Collection<String> skip) {
    this.temp = Paths.get(temp).toAbsolutePath().normalize();
    this.marker = this.temp.resolve("_rapido").resolve("build-start");
    for (String s : skip) {
      this.skip.add(Paths.get(s).toAbsolutePath().normalize());
    }
    this.skip.add(marker);
  }

  /** Whether the file system says when a file was last written to (or its attributes set); else this can't be used */
  public static boolean isSupported(String folder) {
    try {
      Path p = Paths.get(folder);
      Files.createDirectories(p);
      return Files.getAttribute(p, CTIME) instanceof FileTime;
    } catch (Exception e) {
      return false;
    }
  }

  /** Called where a complete build would clear the temp folder: what's written from now on is this build's */
  public void mark() throws IOException {
    Files.createDirectories(marker.getParent());
    Files.write(marker, new byte[0]);
  }

  /**
   * The files in the temp folder (absolute paths) that this build neither wrote nor claims.
   *
   * @param claimed the paths the build claims (any case: the publisher keeps them in lower case)
   */
  public List<Path> find(Collection<String> claimed) throws IOException {
    FileTime start = ctime(marker);
    Set<String> keep = new HashSet<>();
    for (String s : claimed) {
      keep.add(key(Paths.get(s)));
    }
    List<Path> res = new ArrayList<>();
    Files.walkFileTree(temp, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return skip.contains(dir) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (!skip.contains(file) && ctime(file).compareTo(start) < 0 && !keep.contains(key(file))) {
          res.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    res.sort(Comparator.naturalOrder());
    return res;
  }

  /**
   * Deletes the files. Folders are left, even if empty: Jekyll doesn't copy an empty folder to the output, and the
   * build may still write into it
   */
  public void delete(List<Path> files) throws IOException {
    for (Path p : files) {
      Files.deleteIfExists(p);
    }
  }

  /** The temp folder's path to the file, for the log */
  public String relative(Path p) {
    return temp.relativize(p).toString();
  }

  private static FileTime ctime(Path p) throws IOException {
    return (FileTime) Files.getAttribute(p, CTIME, java.nio.file.LinkOption.NOFOLLOW_LINKS);
  }

  private static String key(Path p) {
    return p.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
  }

  static String key(String s) {
    return key(new File(s).toPath());
  }
}
