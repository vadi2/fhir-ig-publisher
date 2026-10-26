package org.hl7.fhir.igtools.publisher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.hl7.fhir.utilities.validation.ValidationMessage;

/**
 * Watch mode: what a build keeps so that, when only the content of some pages changed since, the next build can reuse
 * everything else the build worked out, and only copy the changed pages to the temp folder again, run Jekyll, and redo
 * what comes after Jekyll (the html checks, qa, the final zip).
 *
 * <p>The build keeps (see PublisherGenerator.finishGenerate()):
 * <ul>
 *   <li>just before Jekyll runs, a list of the files in the temp folder, so that what comes after leaves there (e.g.
 *   full-ig.zip) can be taken out again, and Jekyll sees exactly what it saw the first time</li>
 *   <li>after the package step, a copy of the files it wrote to the output folder (package.tgz, the zips...), which
 *   Jekyll removes from the output folder when it runs again: a page-only change can't change them, because nothing in
 *   them depends on page content</li>
 *   <li>and then, before the html checks, the messages (the build's, and each file's and resource's), which the html
 *   checks and qa add to, and how many of them the suppressed messages had matched; a fresh copy of the html
 *   inspector, which collects what it found as it checks; and which fragments the inspector marked as used</li>
 * </ul>
 *
 * <p>A rebuild that reuses this puts all of that back, and runs the same code as the build for the rest. The template's
 * check script, which runs between Jekyll and the package step, runs again, and must report the same as in the build.
 */
public class PageRebuild {

  /**
   * Text in a page that makes the publisher do more than copy it (see PublisherGenerator.processCustomLiquid()): these
   * pages are checked against, or depend on, the resources, so a change to one is not a page-only change
   */
  private static final String[] CUSTOM_CONTENT_MARKERS = {"{% sql", "{% fragment", "{% json", "{% class-diagram", "{% uml",
      "{% multi-map", "{% lang-fragment", "{% dataset", "{%!", "[[[", "validationRule"};

  private static class Stamp {
    private final long size;
    private final FileTime modified;

    private Stamp(long size, FileTime modified) {
      this.size = size;
      this.modified = modified;
    }

    private boolean sameAs(Stamp other) {
      return other != null && other.size == size && other.modified.equals(modified);
    }
  }

  /**
   * The copies of the package outputs that haven't been discarded: watch mode usually ends with Ctrl-C, which doesn't
   * get to discard(), so they're deleted when the JVM exits
   */
  private static final Set<Path> STASHES = ConcurrentHashMap.newKeySet();
  private static boolean shutdownHookAdded;

  private PublisherFields pf;
  private String notReusable;

  private final List<ValidationMessage> errors = new ArrayList<>();
  private final Map<FetchedFile, List<ValidationMessage>> fileErrors = new IdentityHashMap<>();
  private final Map<FetchedFile, List<ValidationMessage>> fileFilteredMessages = new IdentityHashMap<>();
  private final Map<FetchedResource, List<ValidationMessage>> resourceErrors = new IdentityHashMap<>();
  private final Map<String, Boolean> fragmentsUsed = new HashMap<>();
  // which messages the suppressed messages had matched (and the comments, which matching sets), and how often each had
  // been used: qa and the html checks add to these
  private final Set<ValidationMessage> matched = Collections.newSetFromMap(new IdentityHashMap<>());
  private final Map<ValidationMessage, String> comments = new IdentityHashMap<>();
  private List<int[]> suppressedUseCounts;
  private HTMLInspector inspector;
  private Map<String, String> uncsList;
  private String onCheckMessages;

  private Map<Path, Stamp> tempFiles;
  private Set<Path> tempFolders;
  private Map<Path, Stamp> outputAfterJekyll;
  private Path stashFolder;
  private final List<Path> stashed = new ArrayList<>();
  private boolean captured;
  private boolean complete;
  // what the html checks found, and what the AI step worked out, in the last build or rebuild (for the next rebuild)
  private HtmlCheckRecords htmlRecords;
  private final AIProcessor.Cache aiCache = new AIProcessor.Cache();

  /** The fields of the build that this keeps the state of */
  public void setPf(PublisherFields pf) {
    this.pf = pf;
  }

  public PublisherFields getPf() {
    return pf;
  }

  HtmlCheckRecords getHtmlRecords() {
    return htmlRecords;
  }

  void setHtmlRecords(HtmlCheckRecords htmlRecords) {
    this.htmlRecords = htmlRecords;
  }

  public AIProcessor.Cache getAiCache() {
    return aiCache;
  }

  /** Why the build's state can't be reused for a page-only rebuild, if it can't */
  public void notReusable(String reason) {
    if (notReusable == null) {
      notReusable = reason;
    }
  }

  public String getNotReusable() {
    if (notReusable != null) {
      return notReusable;
    }
    if (!complete) {
      return "the last build didn't finish";
    }
    return captured ? null : "the last build didn't get to the package step";
  }

  public void setUncsList(Map<String, String> uncsList) {
    this.uncsList = uncsList;
  }

  public Map<String, String> getUncsList() {
    return uncsList;
  }

  /** What the template's check script reported in the build (see checkOnCheckMessages()) */
  public void setOnCheckMessages(Map<String, List<ValidationMessage>> messages) {
    onCheckMessages = describe(messages);
  }

  /**
   * Throws an exception if the template's check script reported something different after the page rebuild ran Jekyll
   * than in the build: what it reported in the build is among the messages put back
   */
  public void checkOnCheckMessages(Map<String, List<ValidationMessage>> messages) throws IOException {
    String now = describe(messages);
    if (!Objects.equals(now, onCheckMessages)) {
      throw new IOException("The template's check script reported something different");
    }
  }

  private static String describe(Map<String, List<ValidationMessage>> messages) {
    if (messages == null) {
      return null;
    }
    StringBuilder b = new StringBuilder();
    for (Map.Entry<String, List<ValidationMessage>> e : new TreeMap<>(messages).entrySet()) {
      b.append(e.getKey()).append('\n');
      for (ValidationMessage m : e.getValue()) {
        b.append(' ').append(m.getLevel()).append('|').append(m.getType()).append('|').append(m.getLocation()).append('|')
            .append(m.getLine()).append(':').append(m.getCol()).append('|').append(m.getMessage()).append('\n');
      }
    }
    return b.toString();
  }

  /** Is content one that a page-only rebuild can handle (see CUSTOM_CONTENT_MARKERS)? */
  public static boolean isPlainContent(byte[] content) {
    return isPlainContent(new String(content, StandardCharsets.UTF_8));
  }

  public static boolean isPlainContent(String s) {
    for (String m : CUSTOM_CONTENT_MARKERS) {
      if (s.contains(m)) {
        return false;
      }
    }
    return true;
  }

  // --- the build -----------------------------------------------------------------------------------------------------

  /** Captures the state that the html checks, qa and what comes after them start from (see class doc) */
  private void capture() throws IOException {
    errors.addAll(pf.errors);
    for (FetchedFile f : pf.fileList) {
      fileErrors.put(f, new ArrayList<>(f.getErrors()));
      fileFilteredMessages.put(f, f.getFilteredMessages() == null ? null : new ArrayList<>(f.getFilteredMessages()));
      for (FetchedResource r : f.getResources()) {
        resourceErrors.put(r, new ArrayList<>(r.getErrors()));
      }
    }
    for (Map.Entry<String, PublisherBase.FragmentUseRecord> e : pf.fragmentUses.entrySet()) {
      fragmentsUsed.put(e.getKey(), e.getValue().used);
    }
    for (List<ValidationMessage> list : messageLists()) {
      for (ValidationMessage m : list) {
        if (m.isMatched()) {
          matched.add(m);
        }
        comments.put(m, m.getComment());
      }
    }
    suppressedUseCounts = pf.suppressedMessages.getUseCounts();
    inspector = pf.inspector == null ? null : pf.inspector.copyForRecheck();
    captured = true;
  }

  /** Called just before Jekyll runs */
  public void beforeJekyll() throws IOException {
    tempFolders = new HashSet<>();
    tempFiles = list(Paths.get(pf.tempDir), tempFolders);
  }

  /** Called when Jekyll has run, before the package step writes its files to the output folder */
  public void afterJekyll() throws IOException {
    outputAfterJekyll = list(Paths.get(pf.outputDir), null);
  }

  /**
   * Called when the package step has run, before the html checks: captures the state (see class doc), and keeps a copy
   * of what the package step wrote to the output folder
   */
  public void afterPackage() throws IOException {
    capture();
    Path out = Paths.get(pf.outputDir);
    Map<Path, Stamp> now = list(out, null);
    for (Path p : outputAfterJekyll.keySet()) {
      if (!now.containsKey(p)) {
        notReusable("the package step deleted " + p);
      }
    }
    stashFolder = Files.createTempDirectory("ig-watch-");
    keepUntilExit(stashFolder);
    for (Map.Entry<Path, Stamp> e : now.entrySet()) {
      if (!e.getValue().sameAs(outputAfterJekyll.get(e.getKey()))) {
        Path dst = stashFolder.resolve(e.getKey());
        Files.createDirectories(dst.getParent());
        Files.copy(out.resolve(e.getKey()), dst, StandardCopyOption.COPY_ATTRIBUTES);
        stashed.add(e.getKey());
      }
    }
    outputAfterJekyll = null;
  }

  /** Called when the build (including qa) has finished without an error */
  public void finished() {
    complete = true;
  }

  // --- the rebuild ---------------------------------------------------------------------------------------------------

  /** Whether the two times are on the same day, in the time zone that the build writes dates in */
  static boolean sameDay(Calendar a, Calendar b) {
    Calendar c = (Calendar) b.clone();
    c.setTimeZone(a.getTimeZone());
    return a.get(Calendar.YEAR) == c.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
  }

  /**
   * Which files to copy to the temp folder again, if the changes are all changes to the content of pages that this state
   * can rebuild; otherwise null, and why not in reason (the first entry)
   */
  public List<FetchedFile> pagesFor(SourceSnapshot.Changes changes, List<String> reason) throws IOException {
    String nr = getNotReusable();
    if (nr != null) {
      reason.add(nr);
      return null;
    }
    if (!sameDay(pf.getExecTime(), Calendar.getInstance())) {
      // the pages say what day they were built on (and other files too), and a page rebuild keeps the last build's date
      reason.add("the day changed since the last build");
      return null;
    }
    if (changes == null || changes.isEmpty()) {
      reason.add("nothing changed");
      return null;
    }
    if (!changes.getAdded().isEmpty() || !changes.getRemoved().isEmpty()) {
      reason.add("files were added or removed");
      return null;
    }
    List<FetchedFile> res = new ArrayList<>();
    for (Path p : changes.getModified()) {
      String path = new File(p.toString()).getCanonicalPath();
      FetchedFile f = pf.altMap.get("page/" + path);
      if (f == null) {
        f = pf.altMap.get("pre-page/" + path);
      }
      if (f == null || f.isFolder() || !pf.fileList.contains(f)) {
        reason.add(p + " is not a page");
        return null;
      }
      for (FetchedFile.Output o : f.getOutputs()) {
        if (o.getProcessMode() != FetchedFile.PROCESS_NONE) {
          reason.add(p + " is transformed");
          return null;
        }
      }
      if (pf.resourceFileNames.contains(path) || isInFolder(path, pf.resourceDirs) || isInFolder(path, pf.resourceFactoryDirs)
          || isInFolder(path, pf.testDirs) || isInFolder(path, pf.dataDirs) || isInFolder(path, pf.otherDirs) || isInFolder(path, pf.binaryPaths)) {
        reason.add(p + " is also read as something other than a page");
        return null;
      }
      if (pf.pagesWithCustomContent.contains(f)) {
        reason.add(p + " had content that depends on the resources");
        return null;
      }
      if (!isPlainContent(Files.readAllBytes(p))) {
        reason.add(p + " has content that depends on the resources");
        return null;
      }
      if (!res.contains(f)) {
        res.add(f);
      }
    }
    return res;
  }

  private static boolean isInFolder(String path, Collection<String> folders) throws IOException {
    for (String folder : folders) {
      String c = new File(folder).getCanonicalPath();
      if (path.startsWith(c + File.separator)) {
        return true;
      }
    }
    return false;
  }

  /** Puts back the state as it was captured (see class doc) */
  public void restore() throws IOException {
    pf.errors.clear();
    pf.errors.addAll(errors);
    for (Map.Entry<FetchedFile, List<ValidationMessage>> e : fileErrors.entrySet()) {
      e.getKey().getErrors().clear();
      e.getKey().getErrors().addAll(e.getValue());
    }
    for (Map.Entry<FetchedFile, List<ValidationMessage>> e : fileFilteredMessages.entrySet()) {
      // qa caches what it shows of a file's messages
      e.getKey().setFilteredMessages(e.getValue() == null ? null : new ArrayList<>(e.getValue()));
    }
    for (Map.Entry<FetchedResource, List<ValidationMessage>> e : resourceErrors.entrySet()) {
      e.getKey().getErrors().clear();
      e.getKey().getErrors().addAll(e.getValue());
    }
    for (Map.Entry<String, PublisherBase.FragmentUseRecord> e : pf.fragmentUses.entrySet()) {
      e.getValue().used = Boolean.TRUE.equals(fragmentsUsed.get(e.getKey()));
    }
    for (List<ValidationMessage> list : messageLists()) {
      for (ValidationMessage m : list) {
        m.setMatched(matched.contains(m));
        m.setComment(comments.get(m));
      }
    }
    pf.suppressedMessages.setUseCounts(suppressedUseCounts);
    pf.inspector = inspector == null ? null : inspector.copyForRecheck();
  }

  /** The captured message lists */
  private List<List<ValidationMessage>> messageLists() {
    List<List<ValidationMessage>> res = new ArrayList<>();
    res.add(errors);
    res.addAll(fileErrors.values());
    res.addAll(resourceErrors.values());
    for (List<ValidationMessage> list : fileFilteredMessages.values()) {
      if (list != null) {
        res.add(list);
      }
    }
    return res;
  }

  /**
   * Makes the temp folder what it was when Jekyll ran in the build, except for the files in changed (the paths of the
   * files just written again, which are recorded as they are now): removes what was added to it since. Throws an
   * exception if something else in it changed
   */
  public void prepareTemp(Collection<String> changed) throws IOException {
    Path temp = Paths.get(pf.tempDir);
    for (String s : changed) {
      Path rel = temp.relativize(Paths.get(s));
      Stamp st = stamp(temp.resolve(rel));
      if (!tempFiles.containsKey(rel) || st == null) {
        throw new IOException("Page output " + s + " was not in the temp folder before");
      }
      tempFiles.put(rel, st);
    }
    Set<Path> folders = new HashSet<>();
    Map<Path, Stamp> now = list(temp, folders);
    for (Map.Entry<Path, Stamp> e : tempFiles.entrySet()) {
      if (!e.getValue().sameAs(now.get(e.getKey()))) {
        throw new IOException("The temp file " + e.getKey() + " was changed or removed since the last build");
      }
    }
    for (Path p : now.keySet()) {
      if (!tempFiles.containsKey(p)) {
        Files.delete(temp.resolve(p));
      }
    }
    List<Path> extraFolders = new ArrayList<>();
    for (Path p : folders) {
      if (!tempFolders.contains(p)) {
        extraFolders.add(p);
      }
    }
    // deepest first, so that a folder is empty when it's deleted
    extraFolders.sort(Comparator.comparingInt(Path::getNameCount).reversed());
    for (Path p : extraFolders) {
      Files.delete(temp.resolve(p));
    }
  }

  /** Puts back the files that the package step wrote to the output folder (Jekyll has removed them) */
  public void restorePackageOutputs() throws IOException {
    Path out = Paths.get(pf.outputDir);
    for (Path p : stashed) {
      Path dst = out.resolve(p);
      Files.createDirectories(dst.getParent());
      Files.copy(stashFolder.resolve(p), dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }
  }

  /** The files in the output folder that restorePackageOutputs() puts back */
  public List<String> getPackageOutputs() {
    List<String> res = new ArrayList<>();
    Path out = Paths.get(pf.outputDir);
    for (Path p : stashed) {
      res.add(out.resolve(p).toAbsolutePath().toString());
    }
    return res;
  }

  /**
   * Puts back this file in the output folder (an absolute path), if the package step wrote it; returns whether it
   * did
   */
  public boolean restorePackageOutput(String file) throws IOException {
    Path out = Paths.get(pf.outputDir).toAbsolutePath();
    Path p = Paths.get(file).toAbsolutePath();
    if (!p.startsWith(out) || !stashed.contains(out.relativize(p))) {
      return false;
    }
    Files.copy(stashFolder.resolve(out.relativize(p)), p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    return true;
  }

  /** Deletes the copies of the package files */
  public void discard() {
    if (stashFolder != null) {
      delete(stashFolder);
      STASHES.remove(stashFolder);
      stashFolder = null;
    }
  }

  private static synchronized void keepUntilExit(Path folder) {
    STASHES.add(folder);
    if (!shutdownHookAdded) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> STASHES.forEach(PageRebuild::delete)));
      shutdownHookAdded = true;
    }
  }

  private static void delete(Path folder) {
    try (Stream<Path> s = Files.walk(folder)) {
      s.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    } catch (IOException e) {
      // nothing: it's in the system temp folder
    }
  }

  // --- utilities -----------------------------------------------------------------------------------------------------

  private static Stamp stamp(Path p) throws IOException {
    if (!Files.isRegularFile(p)) {
      return null;
    }
    BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
    return new Stamp(a.size(), a.lastModifiedTime());
  }

  private static final String RAPIDO_STATE = "_rapido";

  /** The files under root (relative to it) with their size and modified time; and the folders, if folders isn't null */
  private static Map<Path, Stamp> list(Path root, Set<Path> folders) throws IOException {
    Map<Path, Stamp> res = new HashMap<>();
    if (!Files.isDirectory(root)) {
      return res;
    }
    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (dir.equals(root.resolve(RAPIDO_STATE))) {
          // Rapido's state (see RapidoTracker, StaleTempFiles), which Jekyll doesn't read: each build saves it after
          // Jekyll has run, a page rebuild too
          return FileVisitResult.SKIP_SUBTREE;
        }
        if (folders != null && !dir.equals(root)) {
          folders.add(root.relativize(dir));
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (attrs.isRegularFile()) {
          res.put(root.relativize(file), new Stamp(attrs.size(), attrs.lastModifiedTime()));
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return res;
  }

  /** for tests */
  Path getStashFolder() {
    return stashFolder;
  }

  /** for tests */
  List<Path> getStashed() {
    return Collections.unmodifiableList(stashed);
  }
}
