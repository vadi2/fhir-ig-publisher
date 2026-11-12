package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.hl7.fhir.igtools.templates.Template;
import org.hl7.fhir.utilities.logging.ILoggingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Watch mode: a complete build lets the Jekyll that's kept running render just what depends on the temp files whose
 * content changed, when the output it goes on from is still there, and so is what the last html checks found
 */
class WatchCompleteBuildJekyllTest {

  @TempDir
  Path dir;

  private JekyllDriver driver;
  private PublisherSettings settings;
  private PublisherFields pf;
  private String temp;
  private String out;

  @BeforeEach
  void setUp() throws Exception {
    Path script = dir.resolve("fake-jekyll.sh");
    Files.write(script, "while read l; do printf '@@ig-jekyll-driver@@ {\"ok\":true,\"full\":true}\\n'; done\n".getBytes(StandardCharsets.UTF_8));
    driver = new JekyllDriver(Arrays.asList("sh", script.toString().replace(File.separatorChar, '/')), System.getenv());
    settings = new PublisherSettings();
    settings.setWatchMode(true);
    pf = new PublisherFields();
    pf.logger = mock(ILoggingService.class);
    pf.tempDir = dir.resolve("temp").toString();
    pf.outputDir = dir.resolve("output").toString();
    write("temp/a.html", "a");
    write("temp/_rapido/state.json", "{}");
    PublisherIGLoader loader = loader();
    loader.setUpWatchOutput();
    temp = new File(pf.tempDir).getAbsolutePath();
    out = new File(pf.outputDir).getAbsolutePath();
  }

  @AfterEach
  void stop() {
    driver.stop();
  }

  private PublisherIGLoader loader() {
    PublisherIGLoader loader = new PublisherIGLoader(settings);
    loader.setPf(pf);
    loader.setJekyllDriver(driver);
    return loader;
  }

  private void write(String rel, String content) throws IOException {
    Path f = dir.resolve(rel);
    Files.createDirectories(f.getParent());
    Files.write(f, content.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void theStagedOutputIsKeptWhileJekyllIsRunningForIt() throws Exception {
    PublisherIGLoader loader = loader();
    assertFalse(loader.keepsStagedOutput());
    assertTrue(driver.build(temp, out, 20_000, s -> {}).isOk());
    assertTrue(loader.keepsStagedOutput());

    settings.setGenerationOff(true);
    assertFalse(loader.keepsStagedOutput());
    settings.setGenerationOff(false);
    loader.setJekyllDriver(null);
    assertFalse(loader.keepsStagedOutput());
    loader.setJekyllDriver(driver);
    String staging = pf.watchOutputDir;
    pf.watchOutputDir = null;
    assertFalse(loader.keepsStagedOutput());
    pf.watchOutputDir = staging;
    assertTrue(loader.keepsStagedOutput());
    driver.stop();
    assertFalse(loader.keepsStagedOutput());
  }

  @Test
  void aBuildEmptiesTheStagingUnlessJekyllGoesOnFromIt() throws Exception {
    PublisherIGLoader loader = loader();
    assertTrue(driver.build(temp, out, 20_000, s -> {}).isOk());
    Files.write(Paths.get(out, "a.html"), "a".getBytes(StandardCharsets.UTF_8));
    Files.write(Paths.get(out, "qa.json"), "{}".getBytes(StandardCharsets.UTF_8));
    Files.write(Paths.get(out, "package.tgz"), "tgz".getBytes(StandardCharsets.UTF_8));
    loader.clearOutputUnlessKept();
    assertTrue(pf.stagedOutputKept);
    assertTrue(new File(out, "a.html").exists());
    // (a build that fails before it writes its qa.json mustn't seem to have written that one: see recordWatchFailure)
    assertFalse(new File(out, "qa.json").exists());
    // (nor is the combined package made from the last build's package)
    assertFalse(new File(out, "package.tgz").exists());

    driver.stop();
    loader.clearOutputUnlessKept();
    assertFalse(pf.stagedOutputKept);
    assertFalse(new File(out, "a.html").exists());
  }

  @Test
  void whyJekyllCouldntWriteAPageAgainIsKept() throws Exception {
    JekyllDriver failing = mock(JekyllDriver.class);
    IOException why = new IOException("the pipe broke");
    when(failing.isRunningFor(any(), any())).thenReturn(true);
    when(failing.rewrite(any(), any(), any(), anyLong(), any())).thenThrow(why);
    PublisherGenerator g = new PublisherGenerator(settings);
    g.setPf(pf);
    g.setWatchTools(failing, null);
    HTMLInspector.OutputRewriteFailure e = assertThrows(HTMLInspector.OutputRewriteFailure.class,
        () -> g.rewriteOutputs(null, Collections.singletonList(Paths.get(out, "a.html").toString())));
    assertSame(why, e.getCause());
  }

  private PublisherGenerator generator(PageRebuild carried) {
    PublisherGenerator g = new PublisherGenerator(settings);
    g.setPf(pf);
    g.setWatchTools(driver, null);
    g.setPageRebuild(carried);
    return g;
  }

  private static PageRebuild carried() {
    PageRebuild res = new PageRebuild();
    res.setHtmlRecords(new HtmlCheckRecords((String) null));
    return res;
  }

  @Test
  void aBuildThatStoppedKeepingItsStateLeavesNothingForTheNext() {
    // what it has is the last build's: this build's html checks and AI step didn't go into it
    PageRebuild state = carried();
    generator(state).keepForPageRebuild(() -> {
      throw new IOException("disk full");
    });
    state.finished();
    assertNull(state.carryOver().getHtmlRecords());

    PageRebuild kept = carried();
    generator(kept).keepForPageRebuild(() -> {});
    kept.finished();
    assertNotNull(kept.carryOver().getHtmlRecords());
  }

  @Test
  void aCompleteBuildGoesOnFromTheLastBuildsOutputAndChecks() throws Exception {
    pf.stagedOutputKept = true;
    PublisherGenerator g = generator(carried());
    // the first build: nothing to go on from
    assertEquals(null, g.changedSources(temp, out));
    assertTrue(driver.build(temp, out, 20_000, s -> {}).isOk());
    assertEquals(Collections.emptyList(), g.changedSources(temp, out));
    write("temp/a.html", "a2");
    write("temp/_rapido/state.json", "{\"a\": 1}");
    write("temp/full-ig.zip", "zip");
    write("temp/.ai-outputs/0", "the AI step's file, kept while Jekyll runs");
    assertEquals(Arrays.asList("a.html"), g.changedSources(temp, out));
  }

  @Test
  void theAiStepsFilesAreKeptWhileJekyllRunsUnlessTheTemplatesCheckMaySeeThem() throws Exception {
    PageRebuild state = carried();
    assertNotNull(generator(null).keepAiOutputs(state, out));
    assertNull(generator(null).keepAiOutputs(null, out));
    // the check runs before the AI step, and must see what `jekyll build` leaves
    pf.template = mock(Template.class);
    when(pf.template.whyOnCheckMaySeeOutput(out)).thenReturn("it has no script");
    assertNull(generator(null).keepAiOutputs(state, out));

    // what fails as it keeps them makes the AI step write them again, not the build fail
    pf.template = null;
    PageRebuild failing = mock(PageRebuild.class);
    AIProcessor.Cache cache = mock(AIProcessor.Cache.class);
    when(failing.getAiCache()).thenReturn(cache);
    doThrow(new java.io.UncheckedIOException(new IOException("disk full"))).when(cache).keepOutputs(any());
    assertNull(generator(null).keepAiOutputs(failing, out));
    doThrow(new IOException("disk full")).when(cache).keepOutputs(any());
    assertNull(generator(null).keepAiOutputs(failing, out));
  }

  @Test
  void withoutTheLastBuildsOutputOrChecksJekyllBuildsEverything() throws Exception {
    pf.stagedOutputKept = true;
    PublisherGenerator g = generator(carried());
    g.changedSources(temp, out);
    assertTrue(driver.build(temp, out, 20_000, s -> {}).isOk());
    write("temp/a.html", "a2");

    pf.stagedOutputKept = false;
    assertEquals(null, g.changedSources(temp, out));
    verify(pf.logger).logMessage("Rapido Mode: Jekyll renders the whole site (the last build's staged output isn't kept)");
    pf.stagedOutputKept = true;
    // (the last build didn't finish, or its checks can't be used again)
    assertEquals(null, generator(new PageRebuild()).changedSources(temp, out));
    verify(pf.logger).logMessage("Rapido Mode: Jekyll renders the whole site (the last build's html checks can't be reused)");
    assertEquals(null, generator(null).changedSources(temp, out));
    verify(pf.logger).logMessage("Rapido Mode: Jekyll renders the whole site (there's no state kept from the last build)");
    assertEquals(Arrays.asList("a.html"), g.changedSources(temp, out));
    // what Jekyll read isn't known before it has built the site (the first call), and once it's stopped
    driver.stop();
    assertEquals(null, g.changedSources(temp, out));
    verify(pf.logger, times(2)).logMessage(startsWith("Rapido Mode: Jekyll renders the whole site (what it last read isn't known"));
  }

  @Test
  void theHtmlChecksGoOnFromTheLastBuildsOnlyIfJekyllLeftTheOtherOutputFilesAsThatBuildLeftThem() {
    HtmlCheckRecords last = new HtmlCheckRecords((String) null);
    // a complete build: Jekyll wrote these, and left the others
    assertEquals(last, PublisherGenerator.recordsForCheck(null, Collections.singleton("a.html"), last));
    // it wrote everything
    assertNull(PublisherGenerator.recordsForCheck(null, null, last));
    // a page rebuild: its own
    PageRebuild replay = carried();
    assertEquals(replay.getHtmlRecords(), PublisherGenerator.recordsForCheck(replay, null, last));
  }

  @Test
  void notWhenTheTemplatesCheckMayLookAtTheOutput() throws Exception {
    pf.stagedOutputKept = true;
    pf.template = mock(Template.class);
    when(pf.template.whyOnCheckMaySeeOutput(out)).thenReturn("it has no script");
    PublisherGenerator g = generator(carried());
    assertEquals(null, g.changedSources(temp, out));
    assertTrue(driver.build(temp, out, 20_000, s -> {}).isOk());
    write("temp/a.html", "a2");
    assertEquals(null, g.changedSources(temp, out));
    // nor are the temp files kept track of
    assertEquals(null, driver.changedSources(temp, out));

    when(pf.template.whyOnCheckMaySeeOutput(out)).thenReturn(null);
    assertEquals(null, g.changedSources(temp, out));
    assertTrue(driver.build(temp, out, 20_000, s -> {}).isOk());
    write("temp/a.html", "a3");
    assertEquals(Arrays.asList("a.html"), g.changedSources(temp, out));
  }

  @Test
  void memoryIsReclaimedAfterJekyllOnlyIfTheJekyllThatIsKeptRunningRendersTheSite() throws Exception {
    PublisherGenerator g = generator(null);
    // the build starts Jekyll: the memory is freed for it first
    assertFalse(g.reclaimsMemoryAfterJekyll());
    assertTrue(driver.build(temp, out, 20_000, s -> {}).isOk());
    assertTrue(g.reclaimsMemoryAfterJekyll());
    driver.stop();
    assertFalse(g.reclaimsMemoryAfterJekyll());
    // not watch mode
    g.setWatchTools(null, null);
    assertFalse(g.reclaimsMemoryAfterJekyll());
  }

  private String output(String rel) throws IOException {
    return new String(Files.readAllBytes(Paths.get(pf.watchOutputDir, rel)), StandardCharsets.UTF_8);
  }

  @Test
  void thePagesJekyllWroteAreInTheOutputFolderBeforeTheBuildHasFinished() throws Exception {
    write("temp/.watch-output/en/a.html", "a, rendered again");
    write("temp/.watch-output/en/b.html", "b, as the last html check left it");
    write("output/en/a.html", "a");
    write("output/en/b.html", "b");
    write("output/package.tgz", "the last build's");
    write("output/qa.json", "{}");
    pf.watchBuildStarted = System.nanoTime();

    generator(null).publishPagesEarly(Collections.singleton(Paths.get(out, "en", "a.html").toString()));

    assertEquals("a, rendered again", output("en/a.html"));
    assertEquals("b", output("en/b.html"));
    assertEquals("the last build's", output("package.tgz"));
    assertEquals("{}", output("qa.json"));
    assertTrue(pf.watchPagesPublished);
    assertFalse(pf.watchOutputSynced);
    // the staging folder still has the page, for the html checks and the sync at the end
    assertTrue(new File(out, "en/a.html").exists());
    verify(pf.logger).logMessage(startsWith("The pages are up in " + pf.watchOutputDir + ", "));
  }

  @Test
  void whenJekyllWroteTheWholeSiteAllOfItIsPutInTheOutputFolder() throws Exception {
    write("temp/.watch-output/index.html", "home");
    write("temp/.watch-output/en/a.html", "a");
    write("temp/.watch-output/en/a.ai.html", "what the AI step makes a.md from, and deletes");
    write("output/gone.html", "a page the build no longer makes");

    generator(null).publishPagesEarly(null);

    assertEquals("home", output("index.html"));
    assertEquals("a", output("en/a.html"));
    assertFalse(new File(pf.watchOutputDir, "en/a.ai.html").exists());
    // (deleted by the sync at the end of the build)
    assertEquals("a page the build no longer makes", output("gone.html"));
    assertTrue(pf.watchPagesPublished);
  }

  @Test
  void pagesThatJekyllWroteAsTheyWereLeaveTheOutputFolderTheLastBuilds() throws Exception {
    write("temp/.watch-output/a.html", "a");
    write("output/a.html", "a");

    generator(null).publishPagesEarly(Collections.singleton(Paths.get(out, "a.html").toString()));

    // so a failure after this still says that the pages are those of the last build that succeeded
    assertFalse(pf.watchPagesPublished);
  }

  @Test
  void nothingIsPutInTheOutputFolderEarlyIfJekyllDidntRenderTheSite() throws Exception {
    write("temp/.watch-output/a.html", "a2");
    write("output/a.html", "a");

    settings.setGenerationOff(true);
    generator(null).publishPagesEarly(null);
    settings.setGenerationOff(false);
    settings.setSimplifierMode(true);
    generator(null).publishPagesEarly(null);
    settings.setSimplifierMode(false);
    // not watch mode: the build writes to the output folder itself
    String output = pf.watchOutputDir;
    pf.watchOutputDir = null;
    generator(null).publishPagesEarly(null);
    pf.watchOutputDir = output;

    assertEquals("a", output("a.html"));
    assertFalse(pf.watchPagesPublished);
  }

  @Test
  void nothingIsPutInTheOutputFolderEarlyIfJekyllReturnedAnError() throws Exception {
    write("temp/.watch-output/a.html", "a2, or half of it");
    write("output/a.html", "a");
    PublisherGenerator g = generator(null);
    g.jekyllReturnedAnError();

    g.publishPagesEarly(null);

    assertEquals("a", output("a.html"));
    assertFalse(pf.watchPagesPublished);
  }

  @Test
  void aPageJekyllWroteAgainAsItWasStaysAsTheLastBuildsHtmlChecksLeftIt() throws Exception {
    write("temp/.watch-output/a.html", "a, as Jekyll wrote it");
    PublisherGenerator g = generator(null);
    g.publishPagesEarly(null);
    // the rest of that build, and its sync
    write("output/a.html", "a, html checked");
    pf.watchPagesPublished = false;

    // the next build: Jekyll renders the whole site again
    g.publishPagesEarly(null);

    assertEquals("a, html checked", output("a.html"));
    assertFalse(pf.watchPagesPublished);
  }

  @Test
  void anOutputFolderThatCantBeWrittenToEarlyDoesntFailTheBuild() throws Exception {
    write("temp/.watch-output/a.html", "a2");
    write("blocked", "a file where the output folder's parent would be");
    pf.watchOutputDir = dir.resolve("blocked").resolve("output").toString();

    generator(null).publishPagesEarly(null);

    assertFalse(pf.watchPagesPublished);
    verify(pf.logger).logMessage(startsWith("Unable to put the pages in " + pf.watchOutputDir + " yet"));
  }

  @Test
  void whatCantBePutInTheOutputFolderEarlyDoesntFailTheBuild() throws Exception {
    write("temp/.watch-output/a/page.html", "a is a folder now");
    write("temp/.watch-output/b.html", "b2");
    write("output/a", "a file");
    write("output/b.html", "b");

    generator(null).publishPagesEarly(null);

    assertEquals("b2", output("b.html"));
    verify(pf.logger).logMessage(startsWith("  Unable to update a"));
    verify(pf.logger).logMessage(contains("1 written, 0 unchanged, 1 failed"));
  }
}
