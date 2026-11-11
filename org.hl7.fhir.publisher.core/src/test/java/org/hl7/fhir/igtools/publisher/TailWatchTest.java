package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Watch mode: once its pages are in the output folder, a build stops between two steps of what's left when the watched
 * files changed, and otherwise does every step
 */
class TailWatchTest {

  @TempDir
  Path dir;

  private final List<String> log = new ArrayList<>();

  /** A source of changes that says what it's told to, and counts the calls */
  private static class Source implements TailWatch.ChangeSource {
    SourceSnapshot.Changes changes;
    IOException failure;
    int starts;
    int asked;
    int stops;

    @Override
    public void start() {
      starts++;
    }

    @Override
    public SourceSnapshot.Changes settledChanges() throws IOException {
      asked++;
      if (failure != null) {
        throw failure;
      }
      return changes;
    }

    @Override
    public void stop() {
      stops++;
    }
  }

  /** The changes between a folder with a file in it and the same folder after the file was written again */
  private SourceSnapshot.Changes aChange() throws IOException {
    Path f = dir.resolve("input").resolve("a.fsh");
    Files.createDirectories(f.getParent());
    Files.write(f, "one".getBytes(StandardCharsets.UTF_8));
    SourceSnapshot before = SourceSnapshot.take(List.of(dir.resolve("input")), null, new HashSet<>());
    Files.write(f, "another".getBytes(StandardCharsets.UTF_8));
    SourceSnapshot.Changes res = SourceSnapshot.take(List.of(dir.resolve("input")), before, new HashSet<>()).changesSince(before);
    assertFalse(res.isEmpty());
    return res;
  }

  @Test
  void withoutAChangeEveryStepRuns() throws Exception {
    Source source = new Source();
    TailWatch tail = new TailWatch(source, false, null, log::add);
    tail.pagesPublished();
    for (TailWatch.Step s : TailWatch.Step.values()) {
      assertFalse(tail.givesWayBefore(s), s.name());
    }
    // and nothing that changed is nothing too
    source.changes = SourceSnapshot.take(Collections.emptyList(), null, new HashSet<>()).changesSince(SourceSnapshot.take(Collections.emptyList(), null, new HashSet<>()));
    assertFalse(tail.givesWayBefore(TailWatch.Step.QA));
    assertFalse(tail.isAbandoned());
    assertNull(tail.getAbandonedBefore());
    assertTrue(tail.notDone().isEmpty());
    assertEquals(1, source.starts);
    assertEquals(TailWatch.Step.values().length + 1, source.asked);
    assertEquals(List.of(), log);
    tail.close();
    assertEquals(1, source.stops);
  }

  @Test
  void aBuildDoesNotGiveWayBeforeItsPagesAreInTheOutputFolder() throws Exception {
    Source source = new Source();
    source.changes = aChange();
    TailWatch tail = new TailWatch(source, false, null, log::add);
    assertFalse(tail.givesWayBefore(TailWatch.Step.PACKAGES));
    assertEquals(0, source.starts);
    assertEquals(0, source.asked);
    tail.close();
    assertEquals(0, source.stops, "it never started looking");

    tail = new TailWatch(source, false, null, log::add);
    tail.pagesPublished();
    assertTrue(tail.givesWayBefore(TailWatch.Step.PACKAGES));
  }

  @Test
  void aChangeStopsTheBuildBeforeTheNextStepAndEveryStepAfterIt() throws Exception {
    Source source = new Source();
    TailWatch tail = new TailWatch(source, false, null, log::add);
    tail.pagesPublished();
    assertFalse(tail.givesWayBefore(TailWatch.Step.PACKAGES));
    source.changes = aChange();
    assertTrue(tail.givesWayBefore(TailWatch.Step.AI));
    assertTrue(tail.isAbandoned());
    assertEquals(TailWatch.Step.AI, tail.getAbandonedBefore());
    // the steps after it aren't done either, and the files aren't looked at again
    assertTrue(tail.givesWayBefore(TailWatch.Step.FULL_ZIP));
    assertTrue(tail.givesWayBefore(TailWatch.Step.QA));
    assertEquals(TailWatch.Step.AI, tail.getAbandonedBefore());
    assertEquals(2, source.asked);
    assertEquals(List.of("the AI step's files", "the html checks", "full-ig.zip", "the qa pages",
        "bringing the output folder up to date with all of that"), tail.notDone());
    assertEquals(3, log.size());
    assertEquals("=== File changes detected while the build finishes: giving up the rest of this build, and building again ===", log.get(0));
    assertTrue(log.get(1).contains("a.fsh"), log.get(1));
    assertTrue(log.get(2).contains("Not done by this build: the AI step's files, the html checks, full-ig.zip, the qa pages, "), log.get(2));
  }

  @Test
  void whatAPageRebuildDidNotDoHasNoPackageStep() throws Exception {
    Source source = new Source();
    source.changes = aChange();
    TailWatch tail = new TailWatch(source, true, null, log::add);
    tail.pagesPublished();
    assertTrue(tail.givesWayBefore(TailWatch.Step.PACKAGES));
    assertEquals("the AI step's files", tail.notDone().get(0));

    tail = new TailWatch(source, false, null, log::add);
    tail.pagesPublished();
    assertTrue(tail.givesWayBefore(TailWatch.Step.PACKAGES));
    assertEquals("the packages and zips (not all of them, if some were written already)", tail.notDone().get(0));
  }

  @Test
  void aBuildGoesOnWhenFinishingItIsTheQuickerWay() throws Exception {
    Source source = new Source();
    source.changes = aChange();
    String[] why = {"only the content of pages changed"};
    TailWatch tail = new TailWatch(source, false, c -> why[0], log::add);
    tail.pagesPublished();
    assertFalse(tail.givesWayBefore(TailWatch.Step.PACKAGES));
    assertFalse(tail.givesWayBefore(TailWatch.Step.AI));
    assertFalse(tail.isAbandoned());
    // said once
    assertEquals(List.of("=== File changes detected while the build finishes: it goes on to its end (only the content of pages changed) ==="), log);
    // then something else changes too
    why[0] = null;
    assertTrue(tail.givesWayBefore(TailWatch.Step.FULL_ZIP));
    assertEquals(TailWatch.Step.FULL_ZIP, tail.getAbandonedBefore());
  }

  @Test
  void whenTheFilesCantBeLookedAtTheBuildGoesOnToItsEnd() throws Exception {
    Source source = new Source();
    source.failure = new IOException("input can't be read");
    TailWatch tail = new TailWatch(source, false, null, log::add);
    tail.pagesPublished();
    assertFalse(tail.givesWayBefore(TailWatch.Step.PACKAGES));
    source.failure = null;
    source.changes = aChange();
    assertFalse(tail.givesWayBefore(TailWatch.Step.AI), "the changes are found after the build");
    assertFalse(tail.isAbandoned());
    assertEquals(1, source.asked);
    assertEquals(1, source.stops, "the files aren't watched for the rest of the build");
    assertEquals(List.of("Unable to look for changes while the build finishes (it goes on to its end): java.io.IOException: input can't be read"), log);
  }

  @Test
  void onceItIsClosedTheBuildRunsToItsEnd() throws Exception {
    Source source = new Source();
    TailWatch tail = new TailWatch(source, false, null, log::add);
    tail.pagesPublished();
    assertFalse(tail.givesWayBefore(TailWatch.Step.QA));
    tail.close();
    assertEquals(1, source.stops);
    source.changes = aChange();
    assertFalse(tail.givesWayBefore(TailWatch.Step.SYNC));
    assertEquals(1, source.asked);
    tail.close();
    assertEquals(1, source.stops, "stopped once");
  }
}
