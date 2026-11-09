package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.logging.ILoggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The start-up itself (initialize), in a watch build where SUSHI runs alongside it. The IG is for a FHIR version the
 * publisher doesn't build, so that the start-up stops (needing no packages) once SUSHI has started. SUSHI is a stand-in
 * that empties fsh-generated, as SUSHI does, and writes it again only once the start-up is waiting for it
 */
class PublisherIGLoaderStartUpTest {

  private static final String UNSUPPORTED = "Unable to support version '1.4.0'";

  @TempDir
  Path dir;

  private Path generated;
  private Path igFile;
  private WatchSession session;
  private PublisherIGLoader loader;
  private PublisherFields pf;
  private final List<String> logged = Collections.synchronizedList(new ArrayList<>());

  // the stand-in SUSHI
  private final AtomicInteger runs = new AtomicInteger();
  private volatile Thread startUp;
  private volatile String igWritten = ig("A");
  private volatile Exception failWith;
  private volatile boolean finished;

  private static String ig(String title) {
    return "{\"resourceType\":\"ImplementationGuide\",\"id\":\"x\",\"url\":\"http://example.org/ImplementationGuide/x\","
      + "\"packageId\":\"example.x\",\"version\":\"0.1.0\",\"name\":\"X\",\"title\":\"" + title + "\",\"status\":\"draft\","
      + "\"fhirVersion\":[\"1.4.0\"]}";
  }

  @BeforeEach
  void setUp() throws Exception {
    generated = dir.resolve("fsh-generated");
    igFile = generated.resolve("resources").resolve("ImplementationGuide-x.json");
    writeSushiOutput(igWritten);
    Files.createDirectories(dir.resolve("input").resolve("fsh"));
    Files.createDirectories(dir.resolve("template"));
    Files.writeString(dir.resolve("template").resolve("config.json"),
      "{\"pre-process\":[{\"folder\":\"fsh-generated/includes\",\"relativePath\":\"_includes\"}]}");
    Files.writeString(dir.resolve("ig.ini"), "[IG]\nig = fsh-generated/resources/ImplementationGuide-x.json\ntemplate = #template\n");
    Files.createDirectories(dir.resolve("packages"));

    session = new WatchSession(dir, List.of(dir.resolve("input")));
    session.startBuild();
    session.sushiRan(); // the session's first build ran SUSHI
    Files.writeString(dir.resolve("input").resolve("fsh").resolve("a.fsh"), "Profile: A"); // an edit
    session.startBuild(); // the build it started

    PublisherSettings settings = new PublisherSettings();
    settings.setConfigFile(dir.toString());
    settings.setPackageCacheFolder(dir.resolve("packages").toString());
    loader = new PublisherIGLoader(settings);
    loader.setWatchSession(session);
    newFields();
    loader.sushiRunner = folder -> logger -> {
      runs.incrementAndGet();
      finished = false;
      FileUtilities.clearDirectory(generated.toString()); // as SUSHI starts
      awaitStartUpWaiting();
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
    loader.setPf(pf);
  }

  private void writeSushiOutput(String ig) throws IOException {
    Files.createDirectories(igFile.getParent());
    Files.writeString(igFile, ig);
    Files.createDirectories(generated.resolve("includes"));
    Files.writeString(generated.resolve("includes").resolve("menu.xml"), "<ul/>");
  }

  /** SUSHI, alongside the start-up, finishes only once the start-up is waiting for it (run by it: at once) */
  private void awaitStartUpWaiting() throws InterruptedException {
    Thread t = startUp;
    if (Thread.currentThread() == t) {
      return;
    }
    long end = System.currentTimeMillis() + 30000;
    while (System.currentTimeMillis() < end) {
      if (t.getState() == Thread.State.WAITING) {
        for (StackTraceElement e : t.getStackTrace()) {
          if (e.getClassName().equals(SushiAlongside.class.getName()) && e.getMethodName().equals("join")) {
            return;
          }
        }
      }
      Thread.sleep(10);
    }
    throw new IllegalStateException("the start-up didn't wait for SUSHI");
  }

  private void initialize() throws Exception {
    startUp = Thread.currentThread();
    loader.initialize();
  }

  @Test
  void aStartUpThatFailsWaitsForSushiAlongsideIt() throws Exception {
    Error e = assertThrows(Error.class, this::initialize);
    assertEquals(UNSUPPORTED + " - must be one of 5.0.0, 4.3.0, 4.0.1, 3.0.2 or 6.0.0-snapshot1", e.getMessage());
    assertTrue(logged.contains("Sushi: running alongside the start-up"), logged.toString());
    assertEquals(1, runs.get());
    assertTrue(finished, "SUSHI had finished when the start-up failed");
    assertTrue(Files.isRegularFile(igFile));
    assertTrue(session.canSkipSushi(), "that SUSHI ran is recorded");
    assertFalse(loader.takeRestart());
  }

  @Test
  void aStartUpIsDoneAgainWhenSushiAlongsideItChangedTheIgResource() throws Exception {
    igWritten = ig("B");
    initialize(); // its failure was on the IG resource that SUSHI changed
    assertTrue(loader.takeRestart());
    assertTrue(logged.stream().anyMatch(s -> s.startsWith("(the start-up that is done again had failed: ") && s.contains(UNSUPPORTED)), logged.toString());

    // done again, on the IG resource SUSHI wrote: SUSHI doesn't run again
    newFields();
    Error e = assertThrows(Error.class, this::initialize);
    assertTrue(e.getMessage().startsWith(UNSUPPORTED), e.getMessage());
    assertEquals(1, runs.get());
    assertTrue(logged.contains("Sushi: not run again - it ran alongside the start-up that is being done again"), logged.toString());
    assertEquals("B", pf.sourceIg.getTitle());
    assertFalse(loader.takeRestart());
  }

  @Test
  void whenSushiAlongsideFailsTheNextBuildDoesntWaitForItAgain() throws Exception {
    Exception broke = new Exception("SUSHI broke");
    failWith = broke;
    Exception e = assertThrows(Exception.class, this::initialize);
    assertSame(broke, e, "SUSHI's failure is the build's");
    assertTrue(e.getSuppressed().length == 1 && e.getSuppressed()[0].getMessage().startsWith(UNSUPPORTED));
    assertFalse(session.sushiRanBefore());

    // the next build runs SUSHI first, as SUSHI failed: it fails on the IG, not on the SUSHI that failed before
    failWith = null;
    session.startBuild();
    newFields();
    Error e2 = assertThrows(Error.class, this::initialize);
    assertTrue(e2.getMessage().startsWith(UNSUPPORTED), e2.getMessage());
    assertEquals(2, runs.get());
  }
}
