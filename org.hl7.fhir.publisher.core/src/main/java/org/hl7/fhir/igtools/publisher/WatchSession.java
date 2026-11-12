package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hl7.fhir.igtools.templates.Template;

/**
 * What watch mode (-watch) keeps from one build to the next:
 * <ul>
 *   <li>the content of the watched source files when the current and the previous build started, to decide whether
 *   anything changed since, and what</li>
 *   <li>the source files as the last SUSHI run saw them, so SUSHI is not run again when only page content changed</li>
 * </ul>
 *
 * What a rebuild reuses is decided by -rapido, as for any build: the validation and generated outputs of the files
 * that neither changed nor depend on (or are used by) one that did. When only the content of plain pages changed, the
 * pages are rendered again on top of what the last build left instead (see PageRebuild).
 */
public class WatchSession {

  /**
   * The folders under input/ whose content SUSHI doesn't read: it only lists the pages, and checks whether
   * input/includes/menu.xml exists. A change to the content of a file in one of them doesn't change what SUSHI
   * generates (adding, renaming or deleting one does)
   */
  private static final String[] PAGE_FOLDERS = {"pagecontent", "pages", "intro-notes", "includes", "images"};

  private final Path fshGenerated;
  private final List<Path> pageFolders = new ArrayList<>();
  private final List<Path> watched;
  /** the local template folders watched, which SUSHI doesn't read */
  private final List<Path> templateFolders = new ArrayList<>();
  /** what isn't read in the template folders: the template is loaded without them (see TemplateManager) */
  private final Set<Path> excluded = new HashSet<>();
  /** folders watched since the file monitor was made */
  private final List<Path> notMonitored = new ArrayList<>();

  private SourceSnapshot buildStart;
  private SourceSnapshot previousBuildStart;
  /** the watched files when the last build failed (null if it didn't) */
  private SourceSnapshot afterFailure;
  private SourceSnapshot sushiInputs;
  /** while the template writes to its folder, the folder as it was before */
  private OutputWindow output;
  /*
   * The loop guard, by window (a script target, or "translations"; see outputWritten()):
   */
  /** files in the template folder that changed in a window of this build, but aren't named as its output */
  private Map<String, Set<Path>> writtenThisBuild = new HashMap<>();
  /**
   * files that changed in a window, and alone started the next build (with others like them), for that many builds
   * in a row up to this one
   */
  private Map<String, Map<Path, Integer>> startedBuilds = new HashMap<>();
  /** files that a window writes on every build, with different content: taken as its output */
  private final Map<String, Set<Path>> alwaysWritten = new HashMap<>();
  private Consumer<String> log = s -> { };
  /** when the first build of the current day started (see buildTime) */
  private Calendar buildTime;

  private static class OutputWindow {
    final Path folder;
    final String name;
    final Predicate<String> managed;
    final SourceSnapshot before;

    OutputWindow(Path folder, String name, Predicate<String> managed, SourceSnapshot before) {
      this.folder = folder;
      this.name = name;
      this.managed = managed;
      this.before = before;
    }
  }


  /**
   * @param rootDir the folder that contains ig.ini
   * @param watched the files and folders watched for changes (all under rootDir; local template folders are added
   *                as builds load them, see watchTemplateFolders())
   */
  public WatchSession(Path rootDir, List<Path> watched) {
    Path root = rootDir.toAbsolutePath().normalize();
    this.fshGenerated = root.resolve("fsh-generated");
    for (String s : PAGE_FOLDERS) {
      pageFolders.add(root.resolve("input").resolve(s));
    }
    this.watched = new ArrayList<>();
    for (Path p : watched) {
      this.watched.add(p.toAbsolutePath().normalize());
    }
  }

  private SourceSnapshot take(Collection<Path> roots, SourceSnapshot previous) throws IOException {
    return SourceSnapshot.take(roots, previous, excluded);
  }

  /**
   * The time a build that starts now says it was built at: the time the first build of the day started, so that builds
   * of the same content write the same outputs. A build on another day gets the time it started (the pages show the
   * day, and a page rebuild needs the day to be today, see PageRebuild.pagesFor)
   */
  public Calendar buildTime(Calendar now) {
    if (buildTime == null || !PageRebuild.sameDay(buildTime, now)) {
      buildTime = (Calendar) now.clone();
    }
    return (Calendar) buildTime.clone();
  }

  /** Records the content of the watched files as the build that is starting will see them */
  public void startBuild() throws IOException {
    previousBuildStart = buildStart;
    afterFailure = null;
    buildStart = take(watched, buildStart);
    countWhatStartedThisBuild(previousBuildStart);
  }

  /**
   * If this build was started only by files that the last build's windows changed without naming them as output, it
   * counts for each of them (and its window) one more build in a row that it started; any other start breaks the row
   */
  private void countWhatStartedThisBuild(SourceSnapshot last) {
    Map<String, Set<Path>> written = writtenThisBuild;
    Map<String, Map<Path, Integer>> before = startedBuilds;
    writtenThisBuild = new HashMap<>();
    startedBuilds = new HashMap<>();
    if (last == null || written.isEmpty()) {
      return;
    }
    Set<Path> changed = buildStart.changesSince(last).all();
    Set<Path> all = new HashSet<>();
    for (Set<Path> files : written.values()) {
      all.addAll(files);
    }
    if (changed.isEmpty() || !all.containsAll(changed)) {
      return;
    }
    for (Map.Entry<String, Set<Path>> e : written.entrySet()) {
      Map<Path, Integer> was = before.getOrDefault(e.getKey(), Collections.emptyMap());
      for (Path f : e.getValue()) {
        if (changed.contains(f)) {
          startedBuilds.computeIfAbsent(e.getKey(), k -> new HashMap<>()).put(f, was.getOrDefault(f, 0) + 1);
        }
      }
    }
  }

  /**
   * What changed in the watched files between the start of the previous build (with what SUSHI generated in it) and
   * the start of the current one; null if this is the first build
   */
  public SourceSnapshot.Changes changesSincePreviousBuild() {
    return previousBuildStart == null ? null : buildStart.changesSince(previousBuildStart);
  }

  /**
   * The build has loaded its template from these local folders (see TemplateManager.getLocalFolders()): they are
   * watched from now on, as they are now. Folders already watched are left as they are
   */
  public void watchTemplateFolders(List<String> folders) throws IOException {
    for (String f : folders) {
      Path p = Paths.get(f).toAbsolutePath().normalize();
      if (isWatched(p)) {
        continue;
      }
      watched.add(p);
      templateFolders.add(p);
      excluded.add(p.resolve(".git"));
      excluded.add(p.resolve("output"));
      notMonitored.add(p);
      if (buildStart != null) {
        buildStart = buildStart.withTree(p, take(Collections.singletonList(p), null));
      }
    }
  }

  private boolean isWatched(Path p) {
    for (Path w : watched) {
      if (p.startsWith(w)) {
        return true;
      }
    }
    return false;
  }

  /**
   * What the template tells about writing to its own folder (see Template.OutputListener): a template that is used in
   * place (template = #template) is a watched folder, and what the build writes there is its own output, not a change
   * that needs another build. Only the files the template names as its output (and those found to change on every
   * build, see outputWritten()) are taken as such; any other change made meanwhile stays a change. The folders a
   * template is loaded from (#folder, ig.ini overrides) are copied to the template folder first, so nothing is ever
   * taken from them
   */
  public Template.OutputListener templateOutputs() {
    return new Template.OutputListener() {
      @Override
      public void writing(String folder, String window, Predicate<String> managed) throws IOException {
        outputWriting(Paths.get(folder), window, managed);
      }

      @Override
      public void written() throws IOException {
        outputWritten();
      }
    };
  }

  void outputWriting(Path folder, String window, Predicate<String> managed) throws IOException {
    Path p = folder.toAbsolutePath().normalize();
    output = null;
    if (buildStart != null && isWatched(p)) {
      output = new OutputWindow(p, window, managed, take(Collections.singletonList(p), buildStart));
    }
  }

  /**
   * What changed in the template folder while the window ran: the files it names as its output are the build's;
   * anything else stays a change (it may be the user's), except for the loop guard. A file that a window writes on
   * every build with different content (say, with the time in it) would start a build after every build. So a file
   * that alone started two builds in a row, each time after the same window changed it, and that the window changes
   * a third time, is taken as that window's output from then on, until a run of the window leaves it as it was. That
   * costs at most two extra builds; a user who saves a file while a window runs in each of three builds in a row, to
   * other content each time, would see that third save go unbuilt
   */
  void outputWritten() throws IOException {
    OutputWindow w = output;
    output = null;
    if (w == null || buildStart == null) {
      return;
    }
    SourceSnapshot after = take(Collections.singletonList(w.folder), w.before);
    Set<Path> changed = after.changesSince(w.before).all();
    Set<Path> always = alwaysWritten.computeIfAbsent(w.name, k -> new HashSet<>());
    Map<Path, Integer> started = startedBuilds.getOrDefault(w.name, Collections.emptyMap());
    Set<Path> absorbed = new TreeSet<>();
    Set<Path> learnt = new TreeSet<>();
    Set<Path> unlearnt = new TreeSet<>();
    for (Path f : always) {
      if (!changed.contains(f)) {
        unlearnt.add(f);
      }
    }
    always.removeAll(unlearnt);
    for (Path f : changed) {
      if (w.managed.test(w.folder.relativize(f).toString().replace('\\', '/')) || always.contains(f)) {
        absorbed.add(f);
      } else if (started.getOrDefault(f, 0) >= 2) {
        learnt.add(f);
      } else {
        writtenThisBuild.computeIfAbsent(w.name, k -> new HashSet<>()).add(f);
      }
    }
    if (!unlearnt.isEmpty()) {
      log.accept("Watch mode: " + describe(w.folder, unlearnt) + " not changed by " + w.name
          + " this time: changes to it count again");
    }
    if (!learnt.isEmpty()) {
      always.addAll(learnt);
      absorbed.addAll(learnt);
      log.accept("Watch mode: " + describe(w.folder, learnt) + " changed by " + w.name + " in every build: "
          + "taken as its output from now on, not as changes");
    }
    if (!absorbed.isEmpty()) {
      buildStart = buildStart.withFiles(absorbed, after);
      log.accept("Watch mode: " + describe(w.folder, absorbed) + " written by the build to the template folder: not a change");
    }
  }

  private static String describe(Path folder, Collection<Path> files) {
    if (files.size() > 3) {
      return files.size() + " files";
    }
    List<String> names = new ArrayList<>();
    for (Path f : files) {
      names.add(folder.relativize(f).toString().replace('\\', '/'));
    }
    return String.join(", ", names);
  }

  /** Where watch mode reports what it decides (the publisher's log) */
  public void setLog(Consumer<String> log) {
    this.log = log;
  }

  /** The folders watched since the file monitor was last made (and forgets them): it has to be made again */
  public List<Path> takeNotMonitored() {
    List<Path> res = new ArrayList<>(notMonitored);
    notMonitored.clear();
    return res;
  }

  /**
   * The build wrote these files (e.g. a module's pre-processing, in input): as they are now, they are part of the
   * build's sources, not a change that needs another build
   */
  public void buildWrote(Collection<Path> files) throws IOException {
    if (buildStart != null && !files.isEmpty()) {
      buildStart = buildStart.withFiles(files, take(files, buildStart));
    }
  }

  /**
   * The files in the watched folders that a step of the build (e.g. the test data factories) is about to write: see
   * stepWriting()
   */
  public static final class StepWrites {
    private final String step;
    private final List<Path> trees;
    private final List<Pattern> files;
    private final List<Path> roots;
    private final SourceSnapshot before;

    private StepWrites(String step, List<Path> trees, List<Pattern> files, List<Path> roots, SourceSnapshot before) {
      this.step = step;
      this.trees = trees;
      this.files = files;
      this.roots = roots;
      this.before = before;
    }

    private boolean covers(Path p) {
      for (Path t : trees) {
        if (p.startsWith(t)) {
          return true;
        }
      }
      for (Pattern f : files) {
        if (f.matcher(p.toString()).matches()) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * A step of the build is about to write these: any file below trees, and files (a factory's output files may sit
   * next to the user's). A part of a file's path may be named by a value, as $name$ (input/$type$/x.json): then it is
   * any file whose path fits it. Those of them that aren't watched are left out. Null if none is watched, or before the
   * first build
   */
  public StepWrites stepWriting(String step, Collection<Path> trees, Collection<Path> files) throws IOException {
    if (buildStart == null) {
      return null;
    }
    List<Path> t = watchedOnly(trees);
    List<Path> roots = new ArrayList<>(t);
    List<Pattern> f = new ArrayList<>();
    for (Path file : files) {
      Path n = file.toAbsolutePath().normalize();
      // what is read before and after the step: the file, or the folder above the first part named by a value
      Path root = aboveValues(n);
      if (isWatched(root) && !excluded(root)) {
        f.add(filePattern(n));
        if (!roots.contains(root)) {
          roots.add(root);
        }
      }
    }
    if (roots.isEmpty()) {
      return null;
    }
    return new StepWrites(step, t, f, roots, take(roots, buildStart));
  }

  private static final Pattern VALUE = Pattern.compile("\\$[^$]*\\$");

  // the folder above the first part of the (absolute) path named by a value (the path itself if none is)
  private static Path aboveValues(Path file) {
    Path res = file.getRoot();
    for (Path name : file) {
      if (VALUE.matcher(name.toString()).find()) {
        return res;
      }
      res = res.resolve(name);
    }
    return file;
  }

  // any path that fits it, whatever the values are (a value may name folders too)
  private static Pattern filePattern(Path file) {
    String s = file.toString();
    StringBuilder b = new StringBuilder();
    Matcher m = VALUE.matcher(s);
    int last = 0;
    while (m.find()) {
      b.append(Pattern.quote(s.substring(last, m.start()))).append(".*");
      last = m.end();
    }
    return Pattern.compile(b.append(Pattern.quote(s.substring(last))).toString());
  }

  private boolean excluded(Path p) {
    for (Path e : excluded) {
      if (p.startsWith(e)) {
        return true;
      }
    }
    return false;
  }

  private List<Path> watchedOnly(Collection<Path> paths) {
    List<Path> res = new ArrayList<>();
    for (Path p : paths) {
      Path n = p.toAbsolutePath().normalize();
      if (isWatched(n) && !res.contains(n)) {
        res.add(n);
      }
    }
    return res;
  }

  /**
   * The step has written what it writes (see stepWriting()): what changed in its folders since then is the build's
   * own, part of its sources as it is now, not a change that needs another build. Without this, a step that writes
   * other content on every build (the factories' random values) starts a build after every build, and stops each one
   * as soon as it can (see TailWatch). An edit the user saves in those folders in the seconds the step runs is taken
   * for the build's, as with a module's writes. Returns those files
   */
  public Set<Path> stepWrote(StepWrites writes) throws IOException {
    if (writes == null || buildStart == null) {
      return Collections.emptySet();
    }
    SourceSnapshot after = take(writes.roots, writes.before);
    Set<Path> written = new TreeSet<>();
    for (Path p : after.changesSince(writes.before).all()) {
      if (writes.covers(p)) {
        written.add(p);
      }
    }
    if (!written.isEmpty()) {
      buildStart = buildStart.withFiles(written, after);
      if (sushiInputs != null) {
        // SUSHI ran on the sources before the step wrote: they are not a change for SUSHI either
        sushiInputs = sushiInputs.withFiles(written, after);
      }
      log.accept("Watch mode: " + (written.size() > 3 ? written.size() + " files" : written.toString()) + " written by "
          + writes.step + ": not a change");
    }
    return written;
  }

  /**
   * The build failed. It may have failed for a reason outside the sources (a server that couldn't be reached, a
   * timeout), so saving a file, even without changing it, builds again: see savedSinceFailure()
   */
  public void buildFailed() throws IOException {
    afterFailure = take(watched, buildStart);
  }

  /** Whether a watched file was saved since the last build failed (false if it didn't fail) */
  public boolean savedSinceFailure() throws IOException {
    return afterFailure != null && take(watched, afterFailure).savedSince(afterFailure);
  }

  /** What changed in the watched files since the current (or last) build started */
  public SourceSnapshot.Changes changesSinceBuildStart() throws IOException {
    SourceSnapshot now = take(watched, buildStart);
    return now.changesSince(buildStart);
  }

  // --- SUSHI -------------------------------------------------------------------------------------------------------

  /**
   * SUSHI doesn't need to run if it ran before in this session and nothing changed since then except the content of
   * pages and the local template folders (SUSHI's output, in fsh-generated, must not have changed either)
   */
  public boolean canSkipSushi() {
    if (sushiInputs == null || buildStart == null) {
      return false;
    }
    SourceSnapshot.Changes changes = buildStart.changesSince(sushiInputs);
    for (Path p : changes.getAdded()) {
      if (!isTemplate(p)) {
        return false;
      }
    }
    for (Path p : changes.getRemoved()) {
      if (!isTemplate(p)) {
        return false;
      }
    }
    for (Path p : changes.getModified()) {
      if (!isPageContent(p) && !isTemplate(p)) {
        return false;
      }
    }
    return true;
  }

  private boolean isTemplate(Path p) {
    for (Path f : templateFolders) {
      if (p.startsWith(f)) {
        return true;
      }
    }
    return false;
  }

  private boolean isPageContent(Path p) {
    for (Path f : pageFolders) {
      if (p.startsWith(f)) {
        return true;
      }
    }
    return false;
  }

  /**
   * SUSHI ran successfully at the start of this build. What it generated is part of the build's sources from now on,
   * not a change that needs another build
   */
  public void sushiRan() throws IOException {
    if (buildStart == null) {
      return;
    }
    takeSushiOutput();
    // the inputs are taken from before SUSHI ran: if one changed while it was running, it's seen as changed next time
    sushiInputs = buildStart;
  }

  /**
   * SUSHI was run, but didn't finish properly: it must run next time. What it wrote (it writes its output even when
   * it reports errors) is not a change that needs another build either: the same sources would fail the same way
   */
  public void sushiFailed() throws IOException {
    sushiInputs = null;
    if (buildStart != null) {
      takeSushiOutput();
    }
  }

  private void takeSushiOutput() throws IOException {
    SourceSnapshot generated = take(Collections.singletonList(fshGenerated), buildStart);
    buildStart = buildStart.withTree(fshGenerated, generated);
  }
}
