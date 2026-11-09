package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    assertEquals(Arrays.asList("a.html"), g.changedSources(temp, out));
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
    pf.stagedOutputKept = true;
    // (the last build didn't finish, or its checks can't be used again)
    assertEquals(null, generator(new PageRebuild()).changedSources(temp, out));
    assertEquals(null, generator(null).changedSources(temp, out));
    assertEquals(Arrays.asList("a.html"), g.changedSources(temp, out));
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
}
