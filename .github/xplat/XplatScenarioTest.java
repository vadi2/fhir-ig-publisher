package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.json.model.JsonProperty;
import org.hl7.fhir.utilities.json.parser.JsonParser;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Cross-platform QA scenarios (qa/xplat branch only, not for merge). It builds the rapido-qa-ig fixture in separate
 * JVMs, as the command line does, and records each check in target/xplat/checks.txt:
 * <ul>
 *   <li>a page whose file name is non-ASCII (Latin-1, and outside Latin-1), across -rapido builds</li>
 *   <li>a case-only rename of a resource file between two -rapido builds</li>
 *   <li>a -rapido -watch session with output, temp and input files held open (and, on Windows, an input file locked
 *   exclusively, as Excel does) during a page rebuild and a resource rebuild, then a case-only rename and a
 *   non-ASCII page edit</li>
 * </ul>
 * Checks marked hard fail the test on both code sides; those about what the changed Rapido code promises (differential
 * builds, page rebuilds, a -rapido build reporting what a complete build does) are hard only with -Dxplat.side=all2.
 *
 * <p>mvn test -Dtest=XplatScenarioTest -Dxplat.qa=true -Dxplat.side=all2|base -Dxplat.fixture=.../rapido-qa-ig
 */
@EnabledIfSystemProperty(named = "xplat.qa", matches = "true")
class XplatScenarioTest {

  private static final boolean ALL2 = "all2".equals(System.getProperty("xplat.side"));
  private static final Path ROOT = Paths.get(System.getProperty("xplat.out", "target/xplat")).toAbsolutePath();
  private static final Path FIXTURE = Paths.get(System.getProperty("xplat.fixture", "src/test/resources/rapido-qa-ig")).toAbsolutePath();

  /** in Latin-1 (and Cp1252, the default file encoding of Java 17 on Windows), with characters NFC and NFD differ on */
  static final String LATIN = "größe-ñ";
  /** outside Latin-1 */
  static final String WIDE = "страница-日本";

  private static final Pattern LOG_TOTALS = Pattern.compile("Errors: \\d+, Warnings: \\d+, Info: \\d+, Broken Links: \\d+");
  private static final Pattern SUPPRESSED = Pattern.compile("<li>(.*?) <span style=\"color: (?:navy|maroon)\">\\((\\d+) uses\\)</span></li>");
  private static final String WATCHING = "Watching for changes (Cascais:Rapido mode)";
  private static final String CHANGES = "=== File Changes Detected ===";
  private static final String FAILED = "Publishing Content Failed";

  private String scenario;
  private final List<String> hardFailures = new ArrayList<>();

  // --- scenarios ------------------------------------------------------------------------------------------------------

  @Test
  void nonAsciiPageLatin1() throws Exception {
    nonAsciiPage("A1-nonascii-latin1", LATIN);
  }

  @Test
  void nonAsciiPageWide() throws Exception {
    nonAsciiPage("A2-nonascii-wide", WIDE);
  }

  private void nonAsciiPage(String name, String page) throws Exception {
    scenario = name;
    Path base = fresh(name);
    Path w = base.resolve("w");
    copy(FIXTURE, w);
    addPage(w, page, "XPLAT-NA-0");

    Build b0 = build(w, true, base, "a0");
    hard("rapido build 1 (complete) succeeds", b0.ok(), b0.tail());
    hard("rapido build 1: output/" + page + ".html has the page", contains(w, "output/" + page + ".html", "XPLAT-NA-0"), listOutput(w, page));

    append(w, "input/pagecontent/" + page + ".md", "\nXPLAT-NA-1\n");
    Build b1 = build(w, true, base, "a1");
    hard("rapido build 2 (page edited) succeeds", b1.ok(), b1.tail());
    hard("rapido build 2: output/" + page + ".html has the edit", contains(w, "output/" + page + ".html", "XPLAT-NA-1"), listOutput(w, page));
    all2("rapido build 2 is differential", b1.log.contains("Rapido Mode: Differential Build"), kind(b1.log));

    Path r = base.resolve("r1");
    copy(w, r);
    Build c1 = build(r, false, base, "a1-complete");
    hard("complete build of a fresh copy succeeds", c1.ok(), c1.tail());
    hard("complete build: output/" + page + ".html has the edit", contains(r, "output/" + page + ".html", "XPLAT-NA-1"), listOutput(r, page));
    parity("rapido build 2 reports what the complete build does", w, b1, r, c1);
  }

  @Test
  void caseOnlyRenameOfAResourceFile() throws Exception {
    scenario = "B-case-rename";
    Path base = fresh(scenario);
    Path w = base.resolve("w");
    copy(FIXTURE, w);

    Build b0 = build(w, true, base, "b0");
    hard("rapido build 1 (complete) succeeds", b0.ok(), b0.tail());

    Path examples = w.resolve("input/examples");
    caseRename(examples, "Patient-qa-plain.json", "patient-QA-plain.json");
    List<String> names = list(examples);
    hard("the case-only rename took effect", names.contains("patient-QA-plain.json") && !names.contains("Patient-qa-plain.json"), names.toString());

    Build b1 = build(w, true, base, "b1");
    hard("rapido build 2 (after the rename) succeeds", b1.ok(), b1.tail());
    hard("rapido build 2: output/Patient-qa-plain.html is there", Files.exists(w.resolve("output/Patient-qa-plain.html")), listOutput(w, "qa-plain"));
    info("rapido build 2 kind", true, kind(b1.log));

    Path r = base.resolve("r1");
    copy(w, r);
    Build c1 = build(r, false, base, "b1-complete");
    hard("complete build of a fresh copy succeeds", c1.ok(), c1.tail());
    parity("rapido build 2 reports what the complete build does", w, b1, r, c1);

    Build b2 = build(w, true, base, "b2");
    hard("rapido build 3 (no change) succeeds", b2.ok(), b2.tail());
    hard("rapido build 3: output/Patient-qa-plain.html is there", Files.exists(w.resolve("output/Patient-qa-plain.html")), listOutput(w, "qa-plain"));
    all2("rapido build 3 (no change) is differential", b2.log.contains("Rapido Mode: Differential Build"), kind(b2.log));
    parity("rapido build 3 reports what the complete build does", w, b2, r, c1);
  }

  @Test
  void watchWithFilesHeldOpen() throws Exception {
    scenario = "C-watch";
    Path base = fresh(scenario);
    Path w = base.resolve("w");
    copy(FIXTURE, w);
    addPage(w, LATIN, "XPLAT-W-NA-0");
    // stands for a spreadsheet the author has open in Excel, which locks it
    Files.write(w.resolve("input/held-open.xlsx"), "not really a spreadsheet".getBytes(StandardCharsets.UTF_8));

    Path log = base.resolve("watch.log");
    Process p = start(w, log);
    List<Closeable> held = new ArrayList<>();
    try {
      Watch s = new Watch(p, log);
      hard("watch: first build finishes", s.await(1) && !s.failed(), s.tail());

      // files held open while the session rebuilds: pages a browser or preview has open, the temp copy of the page,
      // and the input spreadsheet (locked exclusively; that's only enforced on Windows)
      for (String f : new String[] {"output/other.html", "output/index.html", "output/qa.html"}) {
        hold(held, w.resolve(f));
      }
      try (Stream<Path> t = Files.walk(w.resolve("temp"))) {
        for (Path f : t.filter(x -> Files.isRegularFile(x) && x.getFileName().toString().startsWith("other.")).limit(3).collect(Collectors.toList())) {
          hold(held, f);
        }
      }
      RandomAccessFile raf = new RandomAccessFile(w.resolve("input/held-open.xlsx").toFile(), "rw");
      held.add(raf);
      raf.getChannel().lock();
      info("watch: files held open", true, held.size() + " handles");

      // 1. a page-only change
      append(w, "input/pagecontent/other.md", "\nXPLAT-W-1\n");
      boolean done = s.await(2);
      hard("watch: page rebuild with files held open finishes", done && !s.failed(), s.tail());
      hard("watch: output/other.html has the edit", contains(w, "output/other.html", "XPLAT-W-1"), s.tail());
      all2("watch: the page edit is a page rebuild", s.section().contains("Rapido Mode: Page Rebuild"), s.kind());
      hard("watch: no rebuild nobody asked for", s.quiet(), s.tail());
      info("watch: lock or access errors in the page rebuild", !lockErrors(s.section()), lockExcerpt(s.section()));

      // 2. a resource change
      sub(w, "input/examples/Patient-qa-plain.json", "\"Plain QA\"", "\"Plain QA XPLAT-W-2\"");
      done = s.await(3);
      hard("watch: resource rebuild with files held open finishes", done && !s.failed(), s.tail());
      hard("watch: output/Patient-qa-plain.html has the edit", contains(w, "output/Patient-qa-plain.html", "XPLAT-W-2"), s.tail());
      info("watch: resource rebuild kind", true, s.kind());
      hard("watch: no rebuild nobody asked for", s.quiet(), s.tail());
      info("watch: lock or access errors in the resource rebuild", !lockErrors(s.section()), lockExcerpt(s.section()));

      for (Closeable c : held) {
        c.close();
      }
      held.clear();

      // 3. a case-only rename of a resource file
      caseRename(w.resolve("input/examples"), "Patient-qa-plain.json", "patient-QA-plain.json");
      done = s.await(4);
      hard("watch: rebuild after a case-only rename finishes", done && !s.failed(), s.tail());
      hard("watch: output/Patient-qa-plain.html is still there", contains(w, "output/Patient-qa-plain.html", "XPLAT-W-2"), s.tail());
      info("watch: rebuild kind after the case-only rename", true, s.kind());
      hard("watch: no rebuild nobody asked for", s.quiet(), s.tail());

      // 4. a non-ASCII page edited
      append(w, "input/pagecontent/" + LATIN + ".md", "\nXPLAT-W-NA-1\n");
      done = s.await(5);
      hard("watch: rebuild after a non-ASCII page edit finishes", done && !s.failed(), s.tail());
      hard("watch: output/" + LATIN + ".html has the edit", contains(w, "output/" + LATIN + ".html", "XPLAT-W-NA-1"), listOutput(w, "gr"));
      all2("watch: the non-ASCII page edit is a page rebuild", s.section().contains("Rapido Mode: Page Rebuild"), s.kind());
      hard("watch: no rebuild nobody asked for", s.quiet(), s.tail());
    } finally {
      for (Closeable c : held) {
        try {
          c.close();
        } catch (IOException e) {
          // ignore
        }
      }
      stop(p);
    }
  }

  @AfterEach
  void finish() {
    assertTrue(hardFailures.isEmpty(), "failed checks:\n" + String.join("\n", hardFailures));
  }

  // --- checks ---------------------------------------------------------------------------------------------------------

  private void hard(String name, boolean ok, String detail) throws IOException {
    record(name, ok, true, detail);
  }

  /** hard only on the code under test: what the changed Rapido code promises */
  private void all2(String name, boolean ok, String detail) throws IOException {
    record(name, ok, ALL2, detail);
  }

  private void info(String name, boolean ok, String detail) throws IOException {
    record(name, ok, false, detail);
  }

  private synchronized void record(String name, boolean ok, boolean hard, String detail) throws IOException {
    String status = ok ? "PASS" : hard ? "FAIL" : "INFO";
    String d = detail == null ? "" : detail.replace("\r", "").replace("\n", " ⏎ ");
    if (d.length() > 3000) {
      d = "..." + d.substring(d.length() - 3000);
    }
    String line = status + " | " + scenario + " | " + name + (d.isEmpty() || (ok && !name.contains("kind") && !name.contains("held")) ? "" : " | " + d);
    System.out.println("XPLAT-CHECK " + line);
    Files.createDirectories(ROOT);
    Files.write(ROOT.resolve("checks.txt"), (line + "\n").getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    if (!ok && hard) {
      hardFailures.add(line);
    }
  }

  private void parity(String name, Path w, Build rapido, Path r, Build complete) throws IOException {
    if (!rapido.ok() || !complete.ok()) {
      all2(name, false, "a build failed");
      return;
    }
    try {
      Map<String, Integer> expected = messages(r, complete.log);
      Map<String, Integer> actual = messages(w, rapido.log);
      String diff = describe(expected, actual);
      all2(name, diff.isEmpty() && expected.keySet().stream().anyMatch(k -> k.startsWith("qa.compare.txt|")), diff.isEmpty() ? "no messages" : diff);
    } catch (Exception e) {
      all2(name, false, "can't read the QA outputs: " + e);
    }
  }

  private static String kind(String log) {
    List<String> res = new ArrayList<>();
    for (String l : log.split("\r?\n")) {
      if (l.contains("Rapido Mode: ") && (l.contains("Build") || l.contains("Rebuild"))) {
        res.add(l.trim());
      }
    }
    return res.isEmpty() ? "(no Rapido Mode line)" : String.join(" / ", res);
  }

  private static boolean lockErrors(String log) {
    return !lockExcerpt(log).isEmpty();
  }

  private static String lockExcerpt(String log) {
    List<String> res = new ArrayList<>();
    for (String l : log.split("\r?\n")) {
      String s = l.toLowerCase();
      if (s.contains("accessdenied") || s.contains("being used by another process") || s.contains("locked a portion") || s.contains("unable to read")
          || s.contains("filesystemexception") || s.contains("permission denied")) {
        res.add(l.trim());
      }
    }
    return res.stream().distinct().limit(20).collect(Collectors.joining(" ⏎ "));
  }

  // --- builds ---------------------------------------------------------------------------------------------------------

  private static class Build {
    int exit;
    String log;

    boolean ok() {
      return exit == 0 && LOG_TOTALS.matcher(log).find();
    }

    String tail() {
      return "exit " + exit + ": " + lastLines(log, 40);
    }
  }

  private static List<String> command(Path ig, boolean rapido, boolean watch) {
    List<String> cmd = new ArrayList<>();
    cmd.add(ProcessHandle.current().info().command().orElse("java"));
    cmd.add("-Xmx3g");
    cmd.add("-cp");
    cmd.add(System.getProperty("java.class.path"));
    cmd.add(Publisher.class.getName());
    cmd.add("-ig");
    cmd.add(ig.toString());
    cmd.add("-no-sushi");
    if (rapido) {
      cmd.add("-rapido");
    }
    if (watch) {
      cmd.add("-watch");
    }
    return cmd;
  }

  private Build build(Path ig, boolean rapido, Path logDir, String name) throws IOException, InterruptedException {
    File log = logDir.resolve(name + ".log").toFile();
    long t = System.currentTimeMillis();
    Process p = new ProcessBuilder(command(ig, rapido, false)).redirectErrorStream(true).redirectOutput(log).start();
    Build b = new Build();
    if (!p.waitFor(30, TimeUnit.MINUTES)) {
      stop(p);
      b.exit = -1;
    } else {
      b.exit = p.exitValue();
    }
    b.log = new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);
    System.out.println("XPLAT-BUILD " + scenario + " " + name + " exit " + b.exit + " in " + (System.currentTimeMillis() - t) / 1000 + "s: " + kind(b.log));
    return b;
  }

  private static Process start(Path ig, Path log) throws IOException {
    return new ProcessBuilder(command(ig, true, true)).redirectErrorStream(true).redirectOutput(log.toFile()).start();
  }

  private static void stop(Process p) throws InterruptedException {
    p.descendants().forEach(ProcessHandle::destroyForcibly);
    p.destroyForcibly();
    p.waitFor(60, TimeUnit.SECONDS);
  }

  /** a watch session's log */
  private static class Watch {
    private final Process p;
    private final Path log;
    private int n;

    Watch(Process p, Path log) {
      this.p = p;
      this.log = log;
    }

    String text() {
      try {
        return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
      } catch (IOException e) {
        return "";
      }
    }

    /** waits until the session has said it is watching n times; false if it stopped or took too long */
    boolean await(int n) throws InterruptedException {
      this.n = n;
      long giveUp = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30);
      while (System.currentTimeMillis() < giveUp) {
        if (count(text(), WATCHING) >= n) {
          return true;
        }
        if (!p.isAlive()) {
          return false;
        }
        Thread.sleep(2000);
      }
      return false;
    }

    /** nothing started another build, for a while after the last one */
    boolean quiet() throws InterruptedException {
      Thread.sleep(15000);
      String s = text();
      int i = nth(s, WATCHING, n);
      String after = i < 0 ? "" : s.substring(i + WATCHING.length());
      return p.isAlive() && count(s, WATCHING) == n && !after.contains(CHANGES) && !after.contains("Load IG") && !after.contains("Rapido Mode:");
    }

    /** the log of the last build: from the end of the one before */
    String section() {
      String s = text();
      int i = n <= 1 ? 0 : nth(s, WATCHING, n - 1);
      return i < 0 ? s : s.substring(i);
    }

    boolean failed() {
      String s = section();
      return s.contains(FAILED) || s.contains("=== Build Failed");
    }

    String kind() {
      return XplatScenarioTest.kind(section());
    }

    String tail() {
      return (p.isAlive() ? "running" : "exited " + p.exitValue()) + ": " + lastLines(section(), 40);
    }
  }

  private static int count(String s, String what) {
    int n = 0;
    for (int i = s.indexOf(what); i >= 0; i = s.indexOf(what, i + 1)) {
      n++;
    }
    return n;
  }

  private static int nth(String s, String what, int n) {
    int i = -1;
    for (int k = 0; k < n; k++) {
      i = s.indexOf(what, i + 1);
      if (i < 0) {
        return -1;
      }
    }
    return i;
  }

  private static String lastLines(String s, int n) {
    String[] lines = s.split("\r?\n");
    return String.join("\n", Arrays.asList(lines).subList(Math.max(0, lines.length - n), lines.length));
  }

  // --- files ----------------------------------------------------------------------------------------------------------

  private static Path fresh(String name) throws IOException {
    Path p = ROOT.resolve(name);
    if (Files.exists(p)) {
      deleteAll(p);
    }
    Files.createDirectories(p);
    return p;
  }

  /** a markdown page, in the IG's page list and linked from the home page */
  private static void addPage(Path w, String page, String text) throws IOException {
    Files.write(w.resolve("input/pagecontent/" + page + ".md"), ("A page whose file name is not ASCII. " + text + "\n").getBytes(StandardCharsets.UTF_8));
    sub(w, "input/ImplementationGuide-rapido-qa.json", "\"sourceUrl\": \"other.html\"",
        "\"sourceUrl\": \"" + page + ".html\", \"name\": \"" + page + ".html\", \"title\": \"Non-ASCII " + page + "\", \"generation\": \"markdown\"}, {\"sourceUrl\": \"other.html\"");
    append(w, "input/pagecontent/index.md", "\nSee [the non-ASCII page](" + page + ".html).\n");
  }

  /** renames in two steps: a move to a name that differs only in case is a no-op on a case-insensitive file system */
  private static void caseRename(Path dir, String from, String to) throws IOException {
    Path tmp = dir.resolve("xplat-rename.tmp");
    Files.move(dir.resolve(from), tmp);
    Files.move(tmp, dir.resolve(to));
  }

  private static List<String> list(Path dir) throws IOException {
    try (Stream<Path> s = Files.list(dir)) {
      return s.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
    }
  }

  private static String listOutput(Path w, String part) {
    try {
      return "output has: " + list(w.resolve("output")).stream().filter(n -> n.contains(part) || !n.matches("[\\x00-\\x7F]*")).collect(Collectors.toList());
    } catch (IOException e) {
      return "no output folder: " + e;
    }
  }

  private static boolean contains(Path w, String path, String text) {
    try {
      return new String(Files.readAllBytes(w.resolve(path)), StandardCharsets.UTF_8).contains(text);
    } catch (Exception e) {
      return false;
    }
  }

  private static void hold(List<Closeable> held, Path f) {
    try {
      held.add(new FileInputStream(f.toFile()));
    } catch (IOException e) {
      System.out.println("XPLAT can't hold " + f + ": " + e);
    }
  }

  private static void sub(Path w, String path, String from, String to) throws IOException {
    Path p = w.resolve(path);
    String s = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    if (!s.contains(from)) {
      throw new IOException(from + " not in " + path);
    }
    Files.write(p, s.replace(from, to).getBytes(StandardCharsets.UTF_8));
  }

  private static void append(Path w, String path, String text) throws IOException {
    Path p = w.resolve(path);
    Files.write(p, (new String(Files.readAllBytes(p), StandardCharsets.UTF_8) + text).getBytes(StandardCharsets.UTF_8));
  }

  /** copies the IG's source files (not what a build writes) */
  private static void copy(Path from, Path to) throws IOException {
    try (Stream<Path> s = Files.walk(from)) {
      for (Path p : (Iterable<Path>) s::iterator) {
        Path rel = from.relativize(p);
        String first = rel.getNameCount() == 0 ? "" : rel.getName(0).toString();
        if (first.equals("output") || first.equals("temp") || first.equals("template") || first.startsWith("qa")) {
          continue;
        }
        Path t = to.resolve(rel.toString());
        if (Files.isDirectory(p)) {
          Files.createDirectories(t);
        } else {
          Files.copy(p, t, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  private static void deleteAll(Path p) throws IOException {
    try (Stream<Path> s = Files.walk(p)) {
      s.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(x -> x.toFile().delete());
    }
  }

  // --- the messages (as RapidoQaMessagesTest on perf2/all2 reads them) -------------------------------------------------

  static Map<String, Integer> messages(Path ig, String log) throws Exception {
    Map<String, Integer> res = new TreeMap<>();
    String root = ig.toFile().getCanonicalPath();
    Path out = ig.resolve("output");

    String totals = null;
    Matcher m = LOG_TOTALS.matcher(log);
    while (m.find()) {
      totals = m.group();
    }
    add(res, "log|" + totals);

    JsonObject qa = JsonParser.parseObject(out.resolve("qa.json").toFile());
    for (String n : new String[] {"errs", "warnings", "hints", "broken-links", "suppressed-hints", "suppressed-warnings"}) {
      add(res, "qa.json|" + n + "=" + qa.asString(n));
    }

    String section = "";
    for (String line : Files.readAllLines(out.resolve("qa.compare.txt"), StandardCharsets.UTF_8)) {
      line = norm(line, root, ig);
      if (line.startsWith("== ")) {
        section = line;
      } else if (!line.trim().isEmpty() && !line.startsWith("Generated ") && !line.startsWith("$--")) {
        add(res, "qa.compare.txt|" + section + "|" + line.trim());
      }
    }

    String html = new String(Files.readAllBytes(out.resolve("qa.html")), StandardCharsets.UTF_8);
    m = SUPPRESSED.matcher(html);
    while (m.find()) {
      add(res, "qa.html suppressed|" + m.group(1) + "|" + m.group(2));
    }

    Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(out.resolve("qa.xml").toFile());
    NodeList oos = xml.getElementsByTagName("OperationOutcome");
    for (int i = 0; i < oos.getLength(); i++) {
      Element oo = (Element) oos.item(i);
      String file = "";
      for (Element e : children(oo, "extension")) {
        file = value(e, "valueString");
      }
      for (Element issue : children(oo, "issue")) {
        StringBuilder b = new StringBuilder("qa.xml|" + norm(file, root, ig));
        b.append("|").append(value(issue, "severity")).append("|").append(value(issue, "code"));
        for (Element e : children(issue, "location")) {
          b.append("|@").append(e.getAttribute("value"));
        }
        for (Element e : children(issue, "expression")) {
          b.append("|").append(e.getAttribute("value"));
        }
        for (Element d : children(issue, "details")) {
          b.append("|").append(value(d, "text"));
        }
        add(res, norm(b.toString(), root, ig));
      }
    }

    NpmPackage npm;
    try (InputStream s = new FileInputStream(out.resolve("package.tgz").toFile())) {
      npm = NpmPackage.fromPackage(s);
    }
    JsonObject summary = JsonParser.parseObject(npm.load("other", "validation-summary.json"));
    for (JsonProperty p : summary.getProperties()) {
      JsonObject o = (JsonObject) p.getValue();
      add(res, "validation-summary.json|" + p.getName() + "|" + o.asString("errors") + "|" + o.asString("warnings"));
    }
    JsonObject bundle = JsonParser.parseObject(npm.load("other", "validation-oo.json"));
    for (JsonObject entry : bundle.forceArray("entry").asJsonObjects()) {
      JsonObject oo = entry.getJsonObject("resource");
      add(res, "validation-oo.json|" + oo.asString("id"));
      for (JsonObject issue : oo.forceArray("issue").asJsonObjects()) {
        JsonObject details = issue.getJsonObject("details");
        add(res, norm("validation-oo.json|" + oo.asString("id") + "|" + issue.asString("severity") + "|" + issue.asString("code")
            + "|" + issue.forceArray("expression").asStrings() + "|" + (details == null ? "" : details.asString("text")), root, ig));
      }
    }
    return res;
  }

  private static String norm(String s, String root, Path ig) {
    return s == null ? "" : s.replace(root, "<ig>").replace(ig.toString(), "<ig>");
  }

  private static void add(Map<String, Integer> set, String key) {
    set.merge(key, 1, Integer::sum);
  }

  private static List<Element> children(Element e, String name) {
    List<Element> res = new ArrayList<>();
    for (int i = 0; i < e.getChildNodes().getLength(); i++) {
      if (e.getChildNodes().item(i) instanceof Element && name.equals(((Element) e.getChildNodes().item(i)).getTagName())) {
        res.add((Element) e.getChildNodes().item(i));
      }
    }
    return res;
  }

  private static String value(Element e, String name) {
    List<Element> c = children(e, name);
    return c.isEmpty() ? "" : c.get(0).getAttribute("value");
  }

  static String describe(Map<String, Integer> expected, Map<String, Integer> actual) {
    StringBuilder b = new StringBuilder();
    for (Map.Entry<String, Integer> e : expected.entrySet()) {
      int n = actual.getOrDefault(e.getKey(), 0);
      if (n != e.getValue()) {
        b.append("complete ").append(e.getValue()).append(" x, -rapido ").append(n).append(" x: ").append(e.getKey()).append("\n");
      }
    }
    for (Map.Entry<String, Integer> e : actual.entrySet()) {
      if (!expected.containsKey(e.getKey())) {
        b.append("complete 0 x, -rapido ").append(e.getValue()).append(" x: ").append(e.getKey()).append("\n");
      }
    }
    return b.toString();
  }
}
