package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.utilities.IniFile;
import org.hl7.fhir.utilities.TimeTracker;
import org.hl7.fhir.utilities.logging.ILoggingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Watch mode: a build that gives up the rest of its tail for a change (see TailWatch) leaves the next build what it
 * had completed, and nothing of what it hadn't
 */
class WatchTailAbandonTest {

  @TempDir
  Path dir;

  private final List<String> log = new ArrayList<>();

  private Publisher publisher(PublisherFields pf) {
    Publisher p = new Publisher();
    pf.logger = new ILoggingService() {
      public void logMessage(String message) {
        log.add(message);
      }

      public void logDebugMessage(LogCategory category, String message) {
      }
    };
    p.setPf(pf);
    return p;
  }

  private static Field stateField() throws Exception {
    Field f = Publisher.class.getDeclaredField("pageRebuild");
    f.setAccessible(true);
    return f;
  }

  /** A tail that has stopped before this step */
  private TailWatch stopped(TailWatch.Step before, boolean pageRebuild) throws IOException {
    Path f = dir.resolve("watched").resolve("a.fsh");
    Files.createDirectories(f.getParent());
    Files.write(f, "one".getBytes(StandardCharsets.UTF_8));
    SourceSnapshot start = SourceSnapshot.take(List.of(f.getParent()), null);
    Files.write(f, "another".getBytes(StandardCharsets.UTF_8));
    SourceSnapshot.Changes changes = SourceSnapshot.take(List.of(f.getParent()), start).changesSince(start);
    TailWatch tail = new TailWatch(new TailWatch.ChangeSource() {
      public void start() {
      }

      public SourceSnapshot.Changes settledChanges() {
        return changes;
      }

      public void stop() {
      }
    }, pageRebuild, null, s -> { });
    tail.pagesPublished();
    assertTrue(tail.givesWayBefore(before));
    return tail;
  }

  private static Set<String> set(String... s) {
    return new HashSet<>(Arrays.asList(s));
  }

  // --- what the next build is told about the output files -------------------------------------------------------------

  @Test
  void theFilesABuildWroteAndDidNotCheckAreNewForTheNextBuild() throws Exception {
    Publisher p = publisher(new PublisherFields());
    Field f = stateField();
    HtmlCheckRecords lastChecks = new HtmlCheckRecords((String) null);
    PageRebuild last = new PageRebuild();
    last.setHtmlRecords(lastChecks);
    last.finished();

    // the build's Jekyll writes a page, and then the build stops before its AI step and html checks
    PageRebuild state = last.carryOver();
    p.completeBuild(state, () -> {
      assertEquals(set("/out/a.html"), state.withUnprocessed(set("/out/a.html")));
      state.tailAbandoned();
    }, () -> { });
    assertSame(state, f.get(p));
    assertEquals("the last build gave up its packages, html checks and qa for a change", state.getNotReusable());

    // the next build goes on from the checks before it, and has that page to check as well as its own
    PageRebuild next = state.carryOver();
    assertSame(lastChecks, next.getHtmlRecords());
    assertEquals(1, next.countUnprocessed());
    assertEquals(set("/out/a.html", "/out/b.html"), next.withUnprocessed(set("/out/b.html")));

    // and if it stops too, the one after it has both
    next.tailAbandoned();
    PageRebuild third = next.carryOver();
    assertEquals(set("/out/a.html", "/out/b.html", "/out/c.html"), third.withUnprocessed(set("/out/c.html")));

    // until the html checks have run to their end (the AI step runs before them)
    HtmlCheckRecords checks = new HtmlCheckRecords((String) null);
    third.setHtmlRecords(checks);
    assertEquals(0, third.countUnprocessed());
    third.finished();
    PageRebuild fourth = third.carryOver();
    assertSame(checks, fourth.getHtmlRecords());
    assertEquals(set("/out/d.html"), fourth.withUnprocessed(set("/out/d.html")));
  }

  @Test
  void whenABuildThatStoppedHadEveryFileToCheckSoHasTheNext() {
    PageRebuild state = new PageRebuild();
    state.setHtmlRecords(new HtmlCheckRecords((String) null));
    // Jekyll wrote the whole site
    assertNull(state.withUnprocessed(null));
    assertEquals(-1, state.countUnprocessed());
    state.tailAbandoned();

    PageRebuild next = state.carryOver();
    assertEquals(-1, next.countUnprocessed());
    assertNull(next.withUnprocessed(set("/out/b.html")), "all the files are new");
    next.setHtmlRecords(new HtmlCheckRecords((String) null));
    assertEquals(set("/out/c.html"), next.withUnprocessed(set("/out/c.html")));
  }

  @Test
  void aBuildThatStoppedAfterItsHtmlChecksPassesOnWhatTheyFound() {
    PageRebuild state = new PageRebuild();
    state.withUnprocessed(set("/out/a.html"));
    HtmlCheckRecords checks = new HtmlCheckRecords((String) null);
    state.setHtmlRecords(checks);
    // it stops before full-ig.zip, or before the qa pages
    state.tailAbandoned();

    PageRebuild next = state.carryOver();
    assertSame(checks, next.getHtmlRecords());
    assertEquals(0, next.countUnprocessed());
  }

  @Test
  void aBuildThatFailedOrDroppedItsStateStillPassesNothingOn() {
    // a failure isn't between two steps: what the output has isn't known
    PageRebuild failed = new PageRebuild();
    failed.setHtmlRecords(new HtmlCheckRecords((String) null));
    failed.withUnprocessed(set("/out/a.html"));
    PageRebuild next = failed.carryOver();
    assertNull(next.getHtmlRecords());
    assertEquals(0, next.countUnprocessed());

    PageRebuild dropped = new PageRebuild();
    dropped.setHtmlRecords(new HtmlCheckRecords((String) null));
    dropped.dropped();
    dropped.tailAbandoned();
    assertNull(dropped.carryOver().getHtmlRecords());
  }

  @Test
  void aPageRebuildThatStoppedLeavesItsFilesForTheNextPageRebuild() {
    // the state is the last complete build's: the page rebuild after the one that stopped uses it again
    PageRebuild state = new PageRebuild();
    HtmlCheckRecords checks = new HtmlCheckRecords((String) null);
    state.setHtmlRecords(checks);
    state.finished();
    assertEquals(set("/out/a.html"), state.withUnprocessed(set("/out/a.html")));
    // (the rebuild stops here: the state isn't touched again)
    assertFalse(state.isTailAbandoned());
    assertSame(checks, state.getHtmlRecords());
    assertEquals(set("/out/a.html", "/out/b.html"), state.withUnprocessed(set("/out/b.html")));
    // and a complete build after it has them too
    assertEquals(2, state.carryOver().countUnprocessed());
  }

  // --- how the build ends ---------------------------------------------------------------------------------------------

  @Test
  void aCompleteBuildThatStopsSavesItsStateAndCountsAsComplete() throws Exception {
    PublisherFields pf = new PublisherFields();
    pf.rapido = mock(RapidoTracker.class);
    pf.watchTail = stopped(TailWatch.Step.PACKAGES, false);
    Publisher p = publisher(pf);
    PageRebuild state = new PageRebuild();
    stateField().set(p, state);
    IniFile tracker = new IniFile(dir.resolve(".build-tracker.ini").toString());
    tracker.setBooleanProperty("status", "started", true, null);
    tracker.setBooleanProperty("status", "complete", false, null);
    tracker.save();

    p.tailAbandoned(tracker, false);

    verify(pf.rapido).save();
    assertTrue(new IniFile(dir.resolve(".build-tracker.ini").toString()).getBooleanProperty("status", "complete"),
        "or the next build would be a complete one that reuses nothing");
    assertTrue(state.isTailAbandoned());
    assertFalse(pf.watchOutputSynced);
    assertEquals(1, log.size());
    assertTrue(log.get(0).startsWith("Stopped @ "), log.get(0));
    assertTrue(log.get(0).contains(", before the packages and zips (not all of them, if some were written already): building again for the changes."), log.get(0));
  }

  @Test
  void aPageRebuildThatStopsLeavesRapidosStateAndItsOwn() throws Exception {
    PublisherFields pf = new PublisherFields();
    pf.rapido = mock(RapidoTracker.class);
    pf.watchTail = stopped(TailWatch.Step.AI, true);
    Publisher p = publisher(pf);
    PageRebuild state = new PageRebuild();
    state.finished();
    stateField().set(p, state);

    p.tailAbandoned(new IniFile(dir.resolve(".build-tracker.ini").toString()), true);

    verify(pf.rapido, never()).save();
    assertFalse(state.isTailAbandoned(), "the next page rebuild goes on from it");
    assertTrue(log.get(0).contains(", before the AI step's files: building again"), log.get(0));
  }

  private static void finishIg(Publisher p, IniFile tracker, boolean pageRebuilt) throws Throwable {
    Method m = Publisher.class.getDeclaredMethod("finishIg", Map.class, IniFile.class, TimeTracker.Session.class, boolean.class);
    m.setAccessible(true);
    try {
      m.invoke(p, null, tracker, new TimeTracker().start("generate"), pageRebuilt);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  @Test
  void aBuildThatStoppedDoesNotWriteQaNorSyncTheOutputFolder() throws Throwable {
    PublisherFields pf = new PublisherFields();
    pf.rapido = mock(RapidoTracker.class);
    pf.outputDir = dir.resolve("staging").toString();
    pf.watchOutputDir = dir.resolve("output").toString();
    Files.createDirectories(dir.resolve("staging"));
    Files.write(dir.resolve("staging").resolve("a.html"), "a".getBytes(StandardCharsets.UTF_8));
    pf.watchTail = stopped(TailWatch.Step.AI, false);
    Publisher p = publisher(pf);
    stateField().set(p, new PageRebuild());
    IniFile tracker = new IniFile(dir.resolve(".build-tracker.ini").toString());

    finishIg(p, tracker, false);

    verify(pf.rapido).save();
    assertFalse(Files.exists(dir.resolve("output")), "the output folder is left as it is");
    assertFalse(Files.exists(dir.resolve("staging").resolve("qa.html")));
    assertFalse(pf.watchOutputSynced);
    assertTrue(log.stream().noneMatch(s -> s.startsWith("Finished @")), log.toString());
    assertTrue(log.get(log.size() - 1).startsWith("Stopped @ "), log.toString());

    // one that didn't stop goes on to the qa pages (which need a loaded IG: this one has none)
    PublisherFields whole = new PublisherFields();
    whole.rapido = mock(RapidoTracker.class);
    Publisher q = publisher(whole);
    assertNotNull(assertThrows(NullPointerException.class, () -> finishIg(q, tracker, false)));
    verify(whole.rapido, never()).save();
  }

  // --- looking for changes ---------------------------------------------------------------------------------------------

  @Test
  void theRestOfABuildSeesWhatChangedInTheWatchedFilesSinceItStarted() throws Exception {
    Path input = dir.resolve("ig").resolve("input");
    Files.createDirectories(input);
    Path fsh = input.resolve("a.fsh");
    Files.write(fsh, "one".getBytes(StandardCharsets.UTF_8));
    Path root = dir.resolve("ig");
    Publisher p = publisher(new PublisherFields());
    WatchSession session = new WatchSession(root, List.of(input));
    try (FileChangeMonitor monitor = new FileChangeMonitor(List.of(FileChangeMonitor.MonitoredPath.recursive(input)))) {
      session.startBuild();
      TailWatch.ChangeSource source = p.tailChanges(monitor, session, root, new HashSet<>());
      // the files aren't watched while the build runs
      assertFalse(monitor.isWatching());
      source.start();
      assertTrue(monitor.isWatching());
      assertNull(source.settledChanges(), "nothing was saved");

      // a file saved with what it had isn't a change
      Files.write(fsh, "one".getBytes(StandardCharsets.UTF_8));
      SourceSnapshot.Changes changes = waitForEvents(monitor, source);
      assertTrue(changes.isEmpty(), changes.toString());

      Files.write(fsh, "another".getBytes(StandardCharsets.UTF_8));
      changes = waitForEvents(monitor, source);
      assertEquals(Set.of(fsh), changes.getModified());

      source.stop();
      assertFalse(monitor.isWatching(), "as it is left for the wait between builds");
    }
  }

  /** What the source says once the monitor has reported the write */
  private static SourceSnapshot.Changes waitForEvents(FileChangeMonitor monitor, TailWatch.ChangeSource source) throws Exception {
    long giveUp = System.currentTimeMillis() + 20_000;
    while (!monitor.hasChanges()) {
      assertTrue(System.currentTimeMillis() < giveUp, "the write wasn't reported");
      Thread.sleep(20);
    }
    SourceSnapshot.Changes res = source.settledChanges();
    assertNotNull(res);
    assertFalse(monitor.hasChanges(), "the files were quiet when they were compared");
    return res;
  }
}
