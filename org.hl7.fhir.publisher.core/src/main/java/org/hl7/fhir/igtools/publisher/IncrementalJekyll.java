package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

/**
 * Watch mode: runs Jekyll with its documented {@code --incremental} option, so that a rebuild that changed only
 * include fragments renders only the pages that use them. Jekyll stays an external process. This decides when its
 * incremental build gives what a complete build gives, and keeps what that needs between builds.
 *
 * <p>Jekyll's incremental build renders a page again when the page's file, one of its layouts, or one of the files
 * it included has a different modification time than in Jekyll's last run (it records them in
 * {@code .jekyll-metadata}), or when the page's output file is missing. It doesn't follow {@code _data},
 * {@code _config.yml}, which pages and files exist ({@code site.pages} and the like) or plugins. So a build is
 * incremental only when Jekyll's last run in this session succeeded, everything that has been added, changed or
 * deleted in Jekyll's source folder since then is in {@code _includes}, and the site has no plugins. Any other build
 * deletes {@code .jekyll-metadata}, and Jekyll renders every page, as it does without the option.
 *
 * <p>The publisher writes most of the source folder again in every build, mostly with the same content. A file whose
 * content is the same as in Jekyll's last run (same size and SHA-256) gets that run's modification time back, so
 * neither this nor Jekyll takes it for a change.
 *
 * <p>Jekyll writes to a folder of its own in the temp folder ({@link #SITE}; Jekyll doesn't read names that start
 * with a dot), and the build gets a copy of it in the staging folder: the html check fills in the publish box of the
 * pages there in place, and the pages that Jekyll doesn't render again have to be as Jekyll wrote them. Between
 * runs, .jekyll-metadata is kept in {@link #STATE}. Both folders change in every run, so a page rebuild doesn't
 * compare them with what they were (see PageRebuild).
 */
public class IncrementalJekyll {

  /** Jekyll's destination, in the temp folder */
  public static final String SITE = ".watch-jekyll";
  /** what Jekyll's last incremental run recorded, in its source folder (the temp folder) */
  public static final String METADATA = ".jekyll-metadata";
  /**
   * where .jekyll-metadata is kept between runs, in the temp folder: the build deletes the files at the top of the
   * temp folder that it didn't write (PublisherGenerator.cleanOutput)
   */
  public static final String STATE = ".watch-jekyll-state";
  private static final String CACHE = ".jekyll-cache";

  /** the folders that Jekyll reads although their names start with an underscore */
  private static final Set<String> JEKYLL_FOLDERS = Set.of("_includes", "_layouts", "_data", "_plugins", "_sass", "_posts", "_drafts");
  /**
   * settings in _config.yml that load plugins, or make Jekyll read other folders that start with an underscore (a
   * folder that it doesn't read, such as _rapido, isn't followed here)
   */
  private static final Pattern UNSAFE_CONFIG = Pattern.compile("^(plugins|gems|plugins_dir|whitelist|collections|include|includes_dir|layouts_dir|data_dir|source)\\s*:", Pattern.MULTILINE);

  static final class Source {
    final long size;
    final FileTime modified;
    final byte[] sha256;

    Source(long size, FileTime modified, byte[] sha256) {
      this.size = size;
      this.modified = modified;
      this.sha256 = sha256;
    }
  }

  static final class Stamp {
    final long size;
    final FileTime modified;

    Stamp(BasicFileAttributes a) {
      size = a.size();
      modified = a.lastModifiedTime();
    }

    boolean same(BasicFileAttributes a) {
      return a.size() == size && a.lastModifiedTime().equals(modified);
    }
  }

  /** the source folder as Jekyll's last successful run saw it, or null */
  private Map<String, Source> sources;
  /** the source folder as the run that is about to start sees it */
  private Map<String, Source> pending;
  /** for each file copied from SITE to the staging folder: its stamp in SITE and that of the copy */
  private Map<String, Stamp[]> copied = new HashMap<>();
  private int changed;

  /**
   * Before Jekyll runs: works out whether this run can be incremental, and if not, deletes .jekyll-metadata. Files
   * with the same content as in the last run get the modification time they had then.
   *
   * @return null if the run can be incremental, or why it can't
   */
  public String prepare(Path source) throws IOException {
    Map<String, Source> now = new HashMap<>();
    String why = listSources(source, now);
    pending = now;
    if (why == null) {
      why = whyNotIncremental(source, now);
    }
    if (why != null) {
      Files.deleteIfExists(source.resolve(METADATA));
      Files.deleteIfExists(source.resolve(STATE).resolve(METADATA));
    }
    return why;
  }

  /**
   * Just before Jekyll runs: puts .jekyll-metadata where Jekyll reads it, if {@link #prepare} found that this run can
   * be incremental (without it, Jekyll renders every page)
   */
  public void beforeRun(Path source) throws IOException {
    Path kept = source.resolve(STATE).resolve(METADATA);
    if (pending == null) {
      Files.deleteIfExists(kept);
      Files.deleteIfExists(source.resolve(METADATA));
    } else if (Files.exists(kept)) {
      Files.move(kept, source.resolve(METADATA), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** how many files the last {@link #prepare} found changed (all in _includes if it was incremental) */
  public int getChanged() {
    return changed;
  }

  /** Jekyll succeeded: the next run compares with what this one saw, and gets what Jekyll recorded */
  public void succeeded(Path source) throws IOException {
    // (without prepare(), what the next run compares with isn't known)
    sources = pending;
    pending = null;
    Path metadata = source.resolve(METADATA);
    if (Files.exists(metadata)) {
      Files.createDirectories(source.resolve(STATE));
      Files.move(metadata, source.resolve(STATE).resolve(METADATA), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** Jekyll failed: the next run renders every page */
  public void failed(Path source) throws IOException {
    sources = null;
    pending = null;
    Files.deleteIfExists(source.resolve(METADATA));
    Files.deleteIfExists(source.resolve(STATE).resolve(METADATA));
  }

  private String listSources(Path source, Map<String, Source> now) throws IOException {
    String[] why = {null};
    MessageDigest md = sha256();
    byte[] buf = new byte[1 << 16];
    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (dir.getParent() != null && dir.getParent().equals(source)) {
          String name = dir.getFileName().toString();
          if (isOwnFolder(name) || (name.startsWith("_") && !JEKYLL_FOLDERS.contains(name))) {
            return FileVisitResult.SKIP_SUBTREE;
          }
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.getParent().equals(source) && isOwnFile(file.getFileName().toString())) {
          return FileVisitResult.CONTINUE;
        }
        if (!attrs.isRegularFile()) {
          why[0] = "Jekyll's source folder has something that isn't a file or a folder: " + file;
          return FileVisitResult.TERMINATE;
        }
        String rel = relative(source, file);
        Source last = sources == null ? null : sources.get(rel);
        if (last != null && last.size == attrs.size() && last.modified.equals(attrs.lastModifiedTime())) {
          now.put(rel, last);
          return FileVisitResult.CONTINUE;
        }
        byte[] hash = hash(file, md, buf);
        if (last != null && last.size == attrs.size() && Arrays.equals(last.sha256, hash) && setModified(file, last.modified)) {
          // written again with the same content: for Jekyll, as if it hadn't been written
          now.put(rel, last);
        } else {
          now.put(rel, new Source(attrs.size(), attrs.lastModifiedTime(), hash));
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return why[0];
  }

  /** false if the time can't be set (the file then counts as changed) */
  private static boolean setModified(Path file, FileTime time) {
    try {
      Files.setLastModifiedTime(file, time);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  private String whyNotIncremental(Path source, Map<String, Source> now) throws IOException {
    changed = 0;
    if (sources == null) {
      return "no earlier Jekyll run in this session";
    }
    if (!Files.isRegularFile(source.resolve(STATE).resolve(METADATA))) {
      return "no " + METADATA;
    }
    if (!Files.isDirectory(source.resolve(SITE))) {
      return "no " + SITE;
    }
    if (Files.exists(source.resolve("_plugins")) || Files.exists(source.resolve("Gemfile")) || System.getenv("BUNDLE_GEMFILE") != null) {
      return "the site can have plugins";
    }
    Path config = source.resolve("_config.yml");
    if (Files.isRegularFile(config) && UNSAFE_CONFIG.matcher(Files.readString(config)).find()) {
      return "_config.yml has settings that Jekyll's incremental build doesn't follow";
    }
    for (Map.Entry<String, Source> e : now.entrySet()) {
      if (e.getValue() != sources.get(e.getKey())) {
        if (!e.getKey().startsWith("_includes/")) {
          return (sources.containsKey(e.getKey()) ? "changed: " : "added: ") + e.getKey();
        }
        changed++;
      }
    }
    for (String rel : sources.keySet()) {
      if (!now.containsKey(rel)) {
        if (!rel.startsWith("_includes/")) {
          return "deleted: " + rel;
        }
        changed++;
      }
    }
    return null;
  }

  /** the folders in the temp folder that the build keeps for this, or that aren't Jekyll's source */
  private static boolean isOwnFolder(String name) {
    return name.equals(SITE) || name.equals(STATE) || name.equals(CACHE) || name.equals(PublisherIGLoader.WATCH_OUTPUT_STAGING);
  }

  private static boolean isOwnFile(String name) {
    return name.equals(METADATA);
  }

  /** what clearing the temp folder in watch mode leaves */
  public static boolean kept(String name) {
    return name.equals(SITE) || name.equals(STATE) || name.equals(PublisherIGLoader.WATCH_OUTPUT_STAGING);
  }

  /**
   * After Jekyll: makes the staging folder a copy of what Jekyll wrote, as it would be if Jekyll had written there
   * (Jekyll deletes everything else in its destination, but .git and .svn). A file is copied again unless neither its
   * copy nor the file in SITE has changed since it was last copied.
   *
   * @return how many files were copied
   */
  public int copySite(Path site, Path staging) throws IOException {
    Map<String, BasicFileAttributes> siteFiles = new HashMap<>();
    list(site, siteFiles, null);
    Map<String, BasicFileAttributes> stagedFiles = new HashMap<>();
    Map<String, BasicFileAttributes> stagedFolders = new HashMap<>();
    Files.createDirectories(staging);
    list(staging, stagedFiles, stagedFolders);
    for (String rel : stagedFiles.keySet()) {
      if (!siteFiles.containsKey(rel) && !keptByJekyll(rel)) {
        Files.delete(staging.resolve(rel));
      }
    }
    Map<String, Stamp[]> nowCopied = new HashMap<>();
    int count = 0;
    for (Map.Entry<String, BasicFileAttributes> e : siteFiles.entrySet()) {
      String rel = e.getKey();
      Stamp[] last = copied.get(rel);
      BasicFileAttributes staged = stagedFiles.get(rel);
      if (last != null && staged != null && last[0].same(e.getValue()) && last[1].same(staged)) {
        nowCopied.put(rel, last);
        continue;
      }
      Path to = staging.resolve(rel);
      Files.createDirectories(to.getParent());
      Files.copy(site.resolve(rel), to, StandardCopyOption.REPLACE_EXISTING);
      nowCopied.put(rel, new Stamp[] {new Stamp(e.getValue()), new Stamp(Files.readAttributes(to, BasicFileAttributes.class))});
      count++;
    }
    copied = nowCopied;
    // folders that Jekyll didn't write (deepest first)
    stagedFolders.keySet().stream().filter(rel -> !keptByJekyll(rel) && !Files.isDirectory(site.resolve(rel)))
        .sorted((a, b) -> b.length() - a.length()).forEach(rel -> FileUtils.deleteQuietly(staging.resolve(rel).toFile()));
    return count;
  }

  /** Jekyll's keep_files (.git and .svn by default): what it leaves in its destination */
  private static boolean keptByJekyll(String rel) {
    return rel.startsWith(".git") || rel.startsWith(".svn");
  }

  private static void list(Path root, Map<String, BasicFileAttributes> files, Map<String, BasicFileAttributes> folders) throws IOException {
    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (folders != null && !dir.equals(root)) {
          folders.put(relative(root, dir), attrs);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        files.put(relative(root, file), attrs);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  static String relative(Path root, Path p) {
    return root.relativize(p).toString().replace('\\', '/');
  }

  private static byte[] hash(Path file, MessageDigest md, byte[] buf) throws IOException {
    md.reset();
    try (InputStream in = Files.newInputStream(file)) {
      int n;
      while ((n = in.read(buf)) > 0) {
        md.update(buf, 0, n);
      }
    }
    return md.digest();
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /** the arguments for jekyll build */
  public static List<String> buildArguments(String destination) {
    return List.of("build", "--incremental", "--destination", destination);
  }
}
