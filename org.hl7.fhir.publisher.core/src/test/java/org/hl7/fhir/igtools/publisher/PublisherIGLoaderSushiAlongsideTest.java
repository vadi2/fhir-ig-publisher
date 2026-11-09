package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hl7.fhir.model.core.Coding;
import org.hl7.fhir.model.core.ImplementationGuide;
import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.json.model.JsonObject;
import org.hl7.fhir.utilities.logging.ILoggingService;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueType;
import org.hl7.fhir.utilities.validation.ValidationMessage.Source;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * In a watch session, once SUSHI has run, a rebuild's start-up goes on while SUSHI runs: it must not read what SUSHI
 * writes (fsh-generated) until SUSHI has finished. Here SUSHI is a stand-in that empties fsh-generated, as SUSHI does,
 * and writes it again only when the test lets it
 */
class PublisherIGLoaderSushiAlongsideTest {

  private static final String IG = "{\"resourceType\":\"ImplementationGuide\",\"id\":\"x\"}";

  @TempDir
  Path dir;

  private Path generated;
  private Path igFile;
  private File fsh;
  private WatchSession session;
  private PublisherIGLoader loader;
  private PublisherFields pf;
  private final List<String> logged = Collections.synchronizedList(new ArrayList<>());

  // the stand-in SUSHI
  private final AtomicInteger runs = new AtomicInteger();
  private final CountDownLatch emptied = new CountDownLatch(1);
  private final CountDownLatch release = new CountDownLatch(1);
  private volatile String igWritten = IG;
  private volatile Exception failWith;
  private volatile boolean finished;

  @BeforeEach
  void setUp() throws Exception {
    generated = dir.resolve("fsh-generated");
    igFile = generated.resolve("resources").resolve("ImplementationGuide-x.json");
    writeSushiOutput(IG);
    Files.createDirectories(dir.resolve("input").resolve("fsh"));
    Files.createDirectories(dir.resolve("input").resolve("pagecontent"));
    Files.writeString(dir.resolve("ig.ini"), "[IG]\nig = fsh-generated/resources/ImplementationGuide-x.json\ntemplate = t\n");
    fsh = dir.resolve("fsh").toFile(); // as initialize() passes it

    session = new WatchSession(dir, List.of(dir.resolve("input")));
    session.startBuild();
    session.sushiRan(); // the session's first build ran SUSHI
    Files.writeString(dir.resolve("input").resolve("fsh").resolve("a.fsh"), "Profile: A"); // an edit
    session.startBuild(); // the build it started

    PublisherSettings settings = new PublisherSettings();
    settings.setConfigFile(dir.toString());
    loader = new PublisherIGLoader(settings);
    loader.setWatchSession(session);
    newFields();
    loader.sushiRunner = folder -> logger -> {
      runs.incrementAndGet();
      FileUtilities.clearDirectory(generated.toString()); // as SUSHI starts
      emptied.countDown();
      assertTrue(release.await(30, TimeUnit.SECONDS));
      logger.logMessage("Sushi: stand-in ran on " + folder.getName());
      writeSushiOutput(igWritten);
      if (failWith != null) {
        throw failWith;
      }
      finished = true;
    };
  }

  /** as a build starts */
  private void newFields() {
    pf = new PublisherFields();
    pf.logger = new ILoggingService() {
      @Override
      public void logMessage(String message) {
        logged.add(message);
      }

      @Override
      public void logDebugMessage(LogCategory category, String message) {
      }
    };
    pf.rootDir = dir.toString();
    pf.igName = igFile.toString();
    pf.sourceIg = new ImplementationGuide();
    pf.resourceDirs.add(generated.resolve("resources").toString());
    pf.pagesDirs.add(dir.resolve("input").resolve("pagecontent").toString());
    loader.setPf(pf);
  }

  private static JsonObject includes() {
    return new JsonObject().add("folder", "fsh-generated/includes").add("relativePath", "_includes");
  }

  private void writeSushiOutput(String ig) throws IOException {
    Files.createDirectories(igFile.getParent());
    Files.writeString(igFile, ig);
    Files.createDirectories(generated.resolve("includes"));
    Files.writeString(generated.resolve("includes").resolve("menu.xml"), "<ul/>");
  }

  /** as initializeFromIg goes, up to where SUSHI has started */
  private void startUpToSushi() throws Exception {
    loader.startSushi(fsh);
    loader.readIgBeforeSushi();
    loader.preProcess(includes());
    loader.startPendingSushi();
    assertTrue(emptied.await(30, TimeUnit.SECONDS));
  }

  @Test
  void theStartUpDoesntSeeFshGeneratedUntilSushiHasFinished() throws Exception {
    startUpToSushi();

    // SUSHI has emptied fsh-generated: had the folders been checked now, its folders would have been dropped
    loader.checkFoldersNowOrAfterSushi();
    assertTrue(pf.resourceDirs.contains(generated.resolve("resources").toString()));
    assertTrue(pf.prePagesDirs.isEmpty());
    assertFalse(finished);

    release.countDown();
    loader.finishSushi();

    assertTrue(finished);
    assertEquals(List.of(generated.resolve("resources").toString()), pf.resourceDirs);
    assertEquals(List.of(dir.resolve("input").resolve("pagecontent").toString()), pf.pagesDirs);
    assertEquals(List.of(generated.resolve("includes").toString()), pf.prePagesDirs);
    assertTrue(pf.preProcessInfo.containsKey(generated.resolve("includes").toString()));
    assertTrue(logged.contains("Sushi: stand-in ran on " + dir.getFileName()), logged.toString());
    assertFalse(loader.takeRestart());
    assertTrue(session.canSkipSushi(), "SUSHI ran for this build");
  }

  @Test
  void whatTheFolderCheckReportsGoesWhereItWouldHave() throws Exception {
    pf.pagesDirs.add(generated.resolve("resources").toString()); // a page folder that is a resource folder as well
    startUpToSushi();
    loader.checkFoldersNowOrAfterSushi();
    ValidationMessage later = new ValidationMessage(Source.Publisher, IssueType.INFORMATIONAL, "later", "reported after the folder check", IssueSeverity.INFORMATION);
    pf.errors.add(later);

    release.countDown();
    loader.finishSushi();

    assertEquals(2, pf.errors.size());
    assertTrue(pf.errors.get(0).getMessage().contains("is a page folder"), pf.errors.get(0).getMessage());
    assertSame(later, pf.errors.get(1));
  }

  @Test
  void whenSushiChangesTheIgResourceTheStartUpIsDoneAgainWithoutSushi() throws Exception {
    igWritten = IG.replace("\"x\"", "\"y\"");
    startUpToSushi();
    loader.checkFoldersNowOrAfterSushi();

    release.countDown();
    loader.finishSushi();

    assertTrue(pf.prePagesDirs.isEmpty(), "what is left is done by the start-up that is done again");
    assertTrue(loader.takeRestart());
    assertFalse(loader.takeRestart());

    // the start-up done again: SUSHI ran for this build
    newFields();
    loader.startSushi(fsh);
    loader.readIgBeforeSushi();
    loader.preProcess(includes());
    loader.startPendingSushi();
    loader.checkFoldersNowOrAfterSushi();
    loader.finishSushi();
    assertEquals(1, runs.get());
    assertEquals(List.of(generated.resolve("includes").toString()), pf.prePagesDirs);
    assertFalse(loader.takeRestart());

    // the next build has nothing left from that one
    Files.writeString(dir.resolve("input").resolve("fsh").resolve("a.fsh"), "Profile: B");
    session.startBuild();
    newFields();
    startUpToSushi();
    loader.checkFoldersNowOrAfterSushi();
    loader.finishSushi();
    assertEquals(2, runs.get());
    assertEquals(List.of(generated.resolve("includes").toString()), pf.prePagesDirs);
    assertFalse(loader.takeRestart());
  }

  @Test
  void whenSushiFailsTheBuildFailsWithItsFailure() throws Exception {
    failWith = new IOException("Sushi failed with errors");
    startUpToSushi();
    loader.checkFoldersNowOrAfterSushi();

    release.countDown();
    IOException e = assertThrows(IOException.class, () -> loader.finishSushi());

    assertSame(failWith, e);
    assertTrue(logged.contains("Sushi: stand-in ran on " + dir.getFileName()), logged.toString());
    assertFalse(session.sushiRanBefore(), "it runs again next time");
    assertFalse(loader.takeRestart());
  }

  @Test
  void aStartUpThatFailedWhileSushiRanFailsAsItDidIfTheIgResourceIsTheSame() throws Exception {
    startUpToSushi();
    release.countDown();

    loader.sushiAfterFailure(new Exception("the start-up failed"));

    assertTrue(finished);
    assertFalse(loader.takeRestart());
    assertTrue(pf.prePagesDirs.isEmpty(), "the build failed");
    loader.finishSushi();
    assertTrue(pf.prePagesDirs.isEmpty());
  }

  /** what reading the IG resource again fails with isn't why the start-up failed */
  @Test
  void aStartUpFailureIsntHiddenByWhatFailsAfterSushi() throws Exception {
    startUpToSushi();
    Exception startUp = new Exception("the start-up failed");
    release.countDown();
    // SUSHI writes the IG resource again; it then can't be read
    while (!finished) {
      Thread.sleep(10);
    }
    assertTrue(igFile.toFile().setReadable(false));

    loader.sushiAfterFailure(startUp);

    assertFalse(loader.takeRestart());
    assertEquals(1, startUp.getSuppressed().length);
    assertTrue(startUp.getSuppressed()[0] instanceof IOException, startUp.getSuppressed()[0].toString());
  }

  @Test
  void aStartUpThatFailedBeforeSushiStartedFailsAsItDidIfTheIgResourceIsTheSame() throws Exception {
    release.countDown();
    loader.startSushi(fsh);
    loader.readIgBeforeSushi();

    loader.sushiAfterFailure(new Exception("the template couldn't be loaded"));

    assertTrue(finished);
    assertFalse(loader.takeRestart());
  }

  @Test
  void aStartUpThatFailedBeforeSushiStartedFailsWithSushisFailureIfItFails() throws Exception {
    failWith = new IOException("Sushi failed with errors");
    release.countDown();
    loader.startSushi(fsh);
    loader.readIgBeforeSushi();
    Exception startUp = new Exception("the template couldn't be loaded");

    IOException e = assertThrows(IOException.class, () -> loader.sushiAfterFailure(startUp));

    assertSame(failWith, e);
    assertSame(startUp, e.getSuppressed()[0]);
    assertFalse(session.sushiRanBefore(), "it runs again next time");
  }

  /** the JVM is in no state to go on: SUSHI isn't waited for */
  @Test
  void aFatalErrorDoesntWaitForSushi() throws Exception {
    startUpToSushi();
    try {
      assertTimeoutPreemptively(Duration.ofSeconds(5), () -> loader.sushiAfterFailure(new OutOfMemoryError()));
      assertFalse(loader.takeRestart());
      assertFalse(finished);
    } finally {
      release.countDown();
    }
  }

  @Test
  void aFailureWithoutSushiAlongsideIsLeftAsItIs() throws Exception {
    loader.setWatchSession(null);

    loader.sushiAfterFailure(new Exception("the start-up failed"));

    assertEquals(0, runs.get());
    assertFalse(loader.takeRestart());
  }

  @Test
  void aStartUpThatFailedWhileSushiRanIsDoneAgainIfSushiChangedTheIgResource() throws Exception {
    igWritten = IG.replace("\"x\"", "\"y\"");
    startUpToSushi();
    release.countDown();

    loader.sushiAfterFailure(new Exception("the start-up failed"));

    assertTrue(loader.takeRestart());
  }

  @Test
  void aStartUpThatFailedWhileSushiFailedFailsWithSushisFailure() throws Exception {
    failWith = new IOException("Sushi failed with errors");
    startUpToSushi();
    release.countDown();
    Exception startUp = new Exception("the start-up failed");

    IOException e = assertThrows(IOException.class, () -> loader.sushiAfterFailure(startUp));

    assertSame(failWith, e);
    assertSame(startUp, e.getSuppressed()[0]);
  }

  @Test
  void aStartUpThatFailedBeforeSushiStartedRunsSushiFirst() throws Exception {
    igWritten = IG.replace("\"x\"", "\"y\"");
    release.countDown();
    loader.startSushi(fsh);
    loader.readIgBeforeSushi();

    loader.sushiAfterFailure(new Exception("the template couldn't be loaded"));

    assertTrue(finished);
    assertTrue(loader.takeRestart());
  }

  @Test
  void aStartUpThatReadsWhatSushiWritesWaitsForIt() throws Exception {
    release.countDown();
    param(pf.sourceIg.getDefinition(), "path-suppressed-warnings", "fsh-generated/ignoreWarnings.txt");
    loader.startSushi(fsh);
    loader.readIgBeforeSushi();

    loader.startPendingSushi();

    assertTrue(finished);
    loader.checkFoldersNowOrAfterSushi();
    assertEquals(List.of(generated.resolve("resources").toString()), pf.resourceDirs);
  }

  @Test
  void aStartUpThatWaitedForSushiIsDoneAgainIfItChangedTheIgResource() throws Exception {
    igWritten = IG.replace("\"x\"", "\"y\"");
    release.countDown();
    param(pf.sourceIg.getDefinition(), "path-suppressed-warnings", "fsh-generated/ignoreWarnings.txt");
    loader.startSushi(fsh);
    loader.readIgBeforeSushi();
    loader.preProcess(includes());

    loader.startPendingSushi();

    assertTrue(loader.takeRestart());
    assertTrue(pf.prePagesDirs.isEmpty());
  }

  @Test
  void whatTheStartUpReadsFromTheParameters() throws Exception {
    loader.startSushi(fsh);
    ImplementationGuide.ImplementationGuideDefinitionComponent def = pf.sourceIg.getDefinition();
    param(def, "path-resource", "fsh-generated/resources");
    param(def, "path-pages", "fsh-generated/includes");
    param(def, "path-resource", "fsh-generated/*");
    param(def, "path-suppressed-warnings", "input/ignoreWarnings.txt");
    param(def, "path-expansion-params", "../../input/exp-params.json");
    param(def, "generate", "example-narratives");
    param(def, "special-url", "http://example.org/x");
    assertFalse(loader.startupReadsSushiOutput());

    // found from the IG resource's folder
    param(def, "path-expansion-params", "../exp-params.json");
    assertTrue(loader.startupReadsSushiOutput());
  }

  @Test
  void anyOtherParameterNamingAFileInFshGenerated() throws Exception {
    loader.startSushi(fsh);
    param(pf.sourceIg.getDefinition(), "path-liquid", "fsh-generated/liquid");
    assertTrue(loader.startupReadsSushiOutput());
  }

  @Test
  void oidsAreKeptNextToAnIgResourceInFshGenerated() throws Exception {
    loader.startSushi(fsh);
    param(pf.sourceIg.getDefinition(), "auto-oid-root", "1.2.3");
    assertTrue(loader.startupReadsSushiOutput());
    pf.igName = dir.resolve("input").resolve("ig.json").toString();
    assertFalse(loader.startupReadsSushiOutput());
  }

  @Test
  void sushiRunsFirstWithoutWatch() throws Exception {
    loader.setWatchSession(null);
    release.countDown();
    loader.startSushi(fsh);
    assertTrue(finished);
  }

  @Test
  void sushiRunsFirstUntilItHasRunInTheSession() throws Exception {
    loader.setWatchSession(new WatchSession(dir, List.of(dir.resolve("input"))));
    assertFalse(loader.canRunSushiAlongside());
  }

  @Test
  void sushiRunsFirstAfterItFailed() throws Exception {
    session.sushiFailed();
    assertFalse(loader.canRunSushiAlongside());
  }

  @Test
  void sushiRunsFirstWhenAModulePreProcessesTheIg() throws Exception {
    assertTrue(loader.canRunSushiAlongside());
    Files.writeString(dir.resolve("ig.ini"), "[IG]\nig = fsh-generated/resources/ImplementationGuide-x.json\ntemplate = t\nmodule = m\n");
    assertFalse(loader.canRunSushiAlongside());
  }

  @Test
  void sushiRunsFirstWithoutAnIgResourceToReadBeforeIt() throws Exception {
    Files.delete(igFile);
    assertFalse(loader.canRunSushiAlongside());
  }

  private static void param(ImplementationGuide.ImplementationGuideDefinitionComponent def, String code, String value) {
    def.addParameter().setCode(new Coding().setCode(code)).setValue(value);
  }
}
