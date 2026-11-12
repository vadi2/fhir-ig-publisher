package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rapido differential build (with or without watch mode): a complete build starts with an empty temp folder, a
 * differential build starts with what the last build left there. A file that the last build claimed as its output, and that this build
 * neither writes again nor claims (e.g. the pages of a resource whose id changed or that was deleted, or an output a
 * resource no longer has), would still be there when Jekyll runs, and end up in the output. This finds those files,
 * so they can be deleted before Jekyll runs.
 *
 * <p>Each build saves what it claims (the outputs it wrote or, for a reused phase, reused). A file is stale if the last
 * build claimed it, this one doesn't, and this build didn't write it: its status-change time (which a program can't
 * set) is before the marker written where a complete build would clear the temp folder. Files no build claimed
 * (e.g. Jekyll's cache) are left alone, and so are the table
 * renderer's background images (tbl_bck*.png): it writes each one once, for the first table that needs it, and only
 * that render claims it (the publisher doesn't track them either, see PublisherBase.isValidFile()).
 */
public class StaleTempFiles {

  private static final String CTIME = "unix:ctime";

  private final Path temp;
  private final Path marker;
  private final Path claims;
  private final Path claimsEncoding;
  private final Path templateMarker;
  private final Path templateWrites;
  private final Set<Path> skip = new HashSet<>();
  // how many times markTemplate() writes a file to see the status-change time move
  int probes = 1000;
  // what was in the temp folder when markTemplate() was called, and what the last build's onGenerate script wrote; null
  // if the script's writes can't be told this build (see markTemplate())
  private Set<Path> beforeTemplate;
  private Set<Path> lastTemplateWrites;
  // the lines of the record of what the last build's onGenerate script wrote that name no file in the temp folder
  private int unreadableTemplateWrites;
  // the claimed paths that name no file
  private final Set<String> unnamedClaims = new TreeSet<>();

  /**
   * @param skip folders (or files) under temp that are left alone (e.g. Rapido's own state)
   */
  public StaleTempFiles(String temp, Collection<String> skip) {
    this(Paths.get(temp), toPaths(skip));
  }

  /** a temp folder on any file system (one without status-change times isn't supported: see isSupported()) */
  StaleTempFiles(Path temp, Collection<Path> skip) {
    this.temp = temp.toAbsolutePath().normalize();
    this.marker = this.temp.resolve("_rapido").resolve("build-start");
    this.claims = this.temp.resolve("_rapido").resolve("claimed.txt");
    this.claimsEncoding = this.temp.resolve("_rapido").resolve("claimed-encoding.txt");
    this.templateMarker = this.temp.resolve("_rapido").resolve("template-start");
    this.templateWrites = this.temp.resolve("_rapido").resolve("template-written.txt");
    for (Path s : skip) {
      this.skip.add(s.toAbsolutePath().normalize());
    }
    this.skip.add(marker);
    this.skip.add(claims);
    this.skip.add(claimsEncoding);
    this.skip.add(templateMarker);
    this.skip.add(templateWrites);
  }

  private static List<Path> toPaths(Collection<String> paths) {
    List<Path> res = new ArrayList<>();
    for (String s : paths) {
      res.add(Paths.get(s));
    }
    return res;
  }

  /**
   * Whether the file system has status-change times; if not (Windows), a differential build can't tell what's stale,
   * so this isn't used
   */
  public boolean isSupported() throws IOException {
    Files.createDirectories(temp);
    try {
      return Files.getAttribute(temp, CTIME) instanceof FileTime;
    } catch (UnsupportedOperationException | IllegalArgumentException e) {
      // no 'unix' view, or no ctime in it
      return false;
    }
  }

  /** Called where a complete build would clear the temp folder: what's written from now on is this build's */
  public void mark() throws IOException {
    Files.createDirectories(marker.getParent());
    Files.write(marker, new byte[0]);
  }

  /**
   * The files in the temp folder (absolute paths) that the last build claimed, and that this build neither wrote nor
   * claims. Nothing if the last build didn't save what it claimed, or if this build didn't mark its start.
   *
   * @param claimed the paths this build claims (any case: the publisher keeps them in lower case)
   */
  public List<Path> find(Collection<String> claimed) throws IOException {
    List<Path> res = new ArrayList<>();
    if (!hasClaims() || !Files.exists(marker)) {
      return res;
    }
    Set<String> before = new HashSet<>(Files.readAllLines(claims, StandardCharsets.UTF_8));
    before.removeAll(keys(claimed));
    if (before.isEmpty()) {
      return res;
    }
    FileTime start = time(marker);
    walk(file -> {
      if (!isTableImage(file) && before.contains(key(file)) && time(file).compareTo(start) < 0) {
        res.add(file);
      }
    });
    res.sort(Comparator.naturalOrder());
    return res;
  }

  /** Saves what this build claims, for the next build's find() */
  public void save(Collection<String> claimed) throws IOException {
    Files.createDirectories(claims.getParent());
    Files.write(claims, new TreeSet<>(keys(claimed)), StandardCharsets.UTF_8);
    Files.write(claimsEncoding, fileNameEncoding().getBytes(StandardCharsets.UTF_8));
  }

  /** Whether the last build saved what it claimed, in names this build reads the same way (see whyNoClaims()) */
  public boolean hasClaims() throws IOException {
    return whyNoClaims() == null;
  }

  /**
   * Why what the last build claimed can't be compared with what this one does: it didn't save it, or it read file names
   * in another encoding (it ran under another locale), so the same file may have had another name. Null if it can be
   */
  public String whyNoClaims() throws IOException {
    if (!Files.exists(claims) || !Files.exists(claimsEncoding)) {
      return "no record of what the last build claimed in the temp folder";
    }
    String last = new String(Files.readAllBytes(claimsEncoding), StandardCharsets.UTF_8);
    String current = fileNameEncoding();
    return last.equals(current) ? null : "the last build read the temp folder's file names as " + last + ", this one reads them as " + current;
  }

  /** The claimed paths (in this build, so far) that name no file, so aren't compared or saved */
  public Set<String> unnamedClaims() {
    return Collections.unmodifiableSet(unnamedClaims);
  }

  /*
   * The template's onGenerate script runs before the build writes the resources' pages, and it can list what's in the
   * temp folder then (e.g. fhir.base.template lists _includes/*.svg for the qa page, before it writes the svgs from
   * PlantUML sources). In a stock build that's what this build wrote so far; in a differential build, what the last
   * build's onGenerate wrote is still there. So each build records what its onGenerate wrote, and a differential build
   * deletes what the last one's wrote, and this one didn't write before its onGenerate, before running it.
   *
   * What the script writes is what's new after it ran, and what the last build's script wrote that it writes again. Not
   * a file that was there and that it only changed (e.g. edited in place, or made executable): that's not the script's,
   * and if a differential build reuses it, deleting it before the script runs would lose it.
   */

  /**
   * Called just before the template's onGenerate script: what's written from now on is the script's.
   *
   * @return false if the file system's status-change times didn't move: then what the script writes isn't recorded
   *   this build (so the next one doesn't delete anything before its script runs)
   */
  public boolean markTemplate() throws IOException, InterruptedException {
    lastTemplateWrites = readTemplateWrites();
    Files.deleteIfExists(templateWrites);
    Set<Path> before = new HashSet<>();
    walk(before::add);
    Files.createDirectories(templateMarker.getParent());
    Files.write(templateMarker, new byte[0]);
    // the clock for status-change times is coarse: make sure what's written from now on is strictly after the marker,
    // so that a file the last build's script wrote is only taken as rewritten if the script rewrote it
    Path probe = templateMarker.resolveSibling("template-probe");
    FileTime start = time(templateMarker);
    try {
      for (int i = 0; i < probes; i++) {
        Files.write(probe, new byte[0]);
        if (time(probe).compareTo(start) > 0) {
          beforeTemplate = before;
          return true;
        }
        Thread.sleep(1);
      }
      beforeTemplate = null;
      return false;
    } finally {
      Files.deleteIfExists(probe);
    }
  }

  /** Called just after the template's onGenerate script: records what it wrote in the temp folder (see markTemplate()) */
  public void saveTemplateWrites() throws IOException {
    if (beforeTemplate == null) {
      return;
    }
    List<String> res = new ArrayList<>();
    FileTime start = time(templateMarker);
    walk(file -> {
      if (!beforeTemplate.contains(file) || (lastTemplateWrites.contains(file) && time(file).compareTo(start) > 0)) {
        res.add(templateWrite(file));
      }
    });
    res.sort(Comparator.naturalOrder());
    Files.createDirectories(templateWrites.getParent());
    Files.write(templateWrites, res, StandardCharsets.UTF_8);
  }

  /**
   * the files (absolute paths) in the temp folder that saveTemplateWrites() recorded; none if there's no record. Counts
   * the lines that name no file there (see unreadableTemplateWrites())
   */
  private Set<Path> readTemplateWrites() throws IOException {
    Set<Path> res = new HashSet<>();
    unreadableTemplateWrites = 0;
    if (Files.exists(templateWrites)) {
      for (String s : Files.readAllLines(templateWrites, StandardCharsets.UTF_8)) {
        Path p = templateWrite(s);
        if (p == null || !p.startsWith(temp)) {
          unreadableTemplateWrites++;
        } else if (!skip.contains(p)) {
          res.add(p);
        }
      }
    }
    return res;
  }

  /**
   * How many lines of the record of what the last build's onGenerate script wrote, read by findTemplateLeftovers(), name
   * no file in the temp folder: what they named isn't deleted
   */
  public int unreadableTemplateWrites() {
    return unreadableTemplateWrites;
  }

  /**
   * In a differential build, just before the template's onGenerate script: the files (absolute paths) the last
   * build's script wrote, that this build hasn't written (yet). Nothing if this build didn't mark its start.
   */
  public List<Path> findTemplateLeftovers() throws IOException {
    List<Path> res = new ArrayList<>();
    if (!Files.exists(marker)) {
      return res;
    }
    FileTime start = time(marker);
    for (Path p : readTemplateWrites()) {
      if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS) && time(p).compareTo(start) < 0) {
        res.add(p);
      }
    }
    res.sort(Comparator.naturalOrder());
    return res;
  }

  /**
   * How saveTemplateWrites() records a file: the path of its file URI relative to the temp folder's. That keeps the
   * name's bytes, whatever the file system encoding (a name that isn't in it, e.g. any non-ASCII name under a POSIX
   * locale, has no path java can name it by), and still names the file if the temp folder moves
   */
  private String templateWrite(Path file) {
    return tempUri().relativize(file.toUri()).getRawPath();
  }

  /** The file a line saveTemplateWrites() wrote names; null if none */
  private Path templateWrite(String s) {
    try {
      return Paths.get(URI.create(tempUri() + s).normalize());
    } catch (IllegalArgumentException e) {
      // a line that isn't a URI path; InvalidPathException is an IllegalArgumentException too
      return null;
    }
  }

  /** the temp folder's file URI, as a folder (ending in '/') */
  private URI tempUri() {
    String res = temp.toUri().toString();
    return URI.create(res.endsWith("/") ? res : res + "/");
  }

  private interface FileAction {
    void visit(Path file) throws IOException;
  }

  /**
   * visits each file in the temp folder (by its path under temp, as the publisher names it, even if temp is a link),
   * except what's skipped
   */
  private void walk(FileAction action) throws IOException {
    Path root = temp.toRealPath();
    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return skip.contains(inTemp(dir)) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path p = inTemp(file);
        if (!skip.contains(p)) {
          action.visit(p);
        }
        return FileVisitResult.CONTINUE;
      }

      private Path inTemp(Path p) {
        return temp.resolve(root.relativize(p));
      }
    });
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

  private Set<String> keys(Collection<String> paths) {
    Set<String> res = new HashSet<>();
    for (String s : paths) {
      Path p = path(s);
      if (p != null) {
        res.add(key(p));
      } else {
        unnamedClaims.add(s);
      }
    }
    return res;
  }

  /**
   * The file a path (as the publisher keeps it) names. A name that isn't a valid Path (any non-ASCII name under a POSIX
   * locale) names the file java.io writes for it: the name in the file system encoding, what can't be encoded replaced
   * (by '?'). Null if it names no file at all (e.g. it has a NUL)
   */
  static Path path(String s) {
    try {
      return Paths.get(s);
    } catch (InvalidPathException e) {
      // without the file system encoding (the property is missing, or names no charset), the default one; and if the
      // name still isn't a valid Path, the name in ASCII, which any file system encoding has
      List<Charset> charsets = new ArrayList<>();
      try {
        charsets.add(Charset.forName(System.getProperty("sun.jnu.encoding")));
      } catch (IllegalArgumentException e2) {
        // no charset by that name
      }
      charsets.add(Charset.defaultCharset());
      charsets.add(StandardCharsets.US_ASCII);
      for (Charset cs : charsets) {
        try {
          return Paths.get(new String(s.getBytes(cs), cs));
        } catch (InvalidPathException e2) {
          // try the next one
        }
      }
      return null;
    }
  }

  /** the encoding of the file system's names (the locale's), as it was when the JVM started */
  private static String fileNameEncoding() {
    String res = System.getProperty("sun.jnu.encoding");
    return res == null ? Charset.defaultCharset().name() : res;
  }

  private static boolean isTableImage(Path p) {
    return p.getFileName().toString().startsWith("tbl_bck");
  }

  /**
   * When the file was last written: its status-change time, which a program can't set (it can set the modification
   * time back, as a copy that keeps the source's time does)
   */
  private FileTime time(Path p) throws IOException {
    return (FileTime) Files.getAttribute(p, CTIME, LinkOption.NOFOLLOW_LINKS);
  }

  /** a claimed path as kept in the claims file: absolute, normalised, in lower case (as the publisher keeps it) */
  private static String key(Path p) {
    return p.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
  }
}
