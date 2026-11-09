package org.hl7.fhir.igtools.publisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.utilities.logging.ILoggingService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Watch mode: if the changes can't be checked for a page rebuild, or the page rebuild fails, a complete build follows */
class RebuildPagesFallbackTest {

  @TempDir
  Path dir;

  @Test
  void whenTheChangesCantBeChecked() throws Exception {
    boolean[] discarded = {false};
    PageRebuild state = new PageRebuild() {
      @Override
      public List<FetchedFile> pagesFor(SourceSnapshot.Changes changes, List<String> reason) throws IOException {
        throw new IOException("a path that can't be read");
      }

      @Override
      public void discard() {
        discarded[0] = true;
      }
    };
    Publisher p = new Publisher();
    List<String> log = new ArrayList<>();
    p.setPf(new PublisherFields());
    p.getPf().logger = new ILoggingService() {
      public void logMessage(String message) {
        log.add(message);
      }

      public void logDebugMessage(LogCategory category, String message) {
      }
    };
    Field f = Publisher.class.getDeclaredField("pageRebuild");
    f.setAccessible(true);
    f.set(p, state);

    assertFalse(p.rebuildPages(new WatchSession(dir, Collections.emptyList())));
    assertTrue(discarded[0]);
    assertNull(f.get(p));
    assertEquals(List.of("Rapido Mode: Complete Build (the changes couldn't be checked: java.io.IOException: a path that can't be read)"), log);
  }

  @Test
  void whenThePageRebuildFails() throws Exception {
    boolean[] discarded = {false};
    List<String> log = new ArrayList<>();
    PublisherFields pf = new PublisherFields();
    pf.vsCache = dir.toString();
    pf.logger = new ILoggingService() {
      public void logMessage(String message) {
        log.add(message);
      }

      public void logDebugMessage(LogCategory category, String message) {
      }
    };
    PageRebuild state = new PageRebuild() {
      @Override
      public List<FetchedFile> pagesFor(SourceSnapshot.Changes changes, List<String> reason) {
        return new ArrayList<>();
      }

      @Override
      public PublisherFields getPf() {
        return pf;
      }

      @Override
      public void restore() {
        // as checkMakeFile() does for a file written twice
        throw new Error("Output file x is being generated more than once");
      }

      @Override
      public void discard() {
        discarded[0] = true;
      }
    };
    Publisher p = new Publisher();
    p.setPf(pf);
    Field f = Publisher.class.getDeclaredField("pageRebuild");
    f.setAccessible(true);
    f.set(p, state);

    assertFalse(p.rebuildPages(new WatchSession(dir, Collections.emptyList())));
    assertTrue(discarded[0]);
    assertNull(f.get(p));
    assertEquals("Rapido Mode: the page rebuild failed (java.lang.Error: Output file x is being generated more than once), so doing a complete build",
        log.get(log.size() - 1));
  }

  @Test
  void whenACompleteBuildCantGoOnFromWhatTheLastBuildLeft() throws Exception {
    List<String> log = new ArrayList<>();
    Publisher p = new Publisher();
    p.setPf(new PublisherFields());
    p.getPf().logger = new ILoggingService() {
      public void logMessage(String message) {
        log.add(message);
      }

      public void logDebugMessage(LogCategory category, String message) {
      }
    };
    Field f = Publisher.class.getDeclaredField("pageRebuild");
    f.setAccessible(true);
    PageRebuild carried = new PageRebuild();
    List<Object> builtWith = new ArrayList<>();
    int[] resets = {0};
    p.completeBuild(carried, () -> {
      builtWith.add(f.get(p));
      if (builtWith.size() == 1) {
        throw new Exception("Publishing failed", new HTMLInspector.OutputRewriteFailure("Jekyll can't write en/a.md again"));
      }
    }, () -> resets[0]++);
    assertEquals(2, builtWith.size());
    assertSame(carried, builtWith.get(0));
    assertNotSame(carried, builtWith.get(1));
    assertEquals(1, resets[0]);
    assertEquals(List.of("Rapido Mode: Complete Build again, without what the last build left (Publishing failed)"), log);

    // any other failure, or one without anything from the last build, is the build's
    builtWith.clear();
    assertThrows(IOException.class, () -> p.completeBuild(new PageRebuild(), () -> {
      builtWith.add(f.get(p));
      throw new IOException("a FSH error");
    }, () -> resets[0]++));
    assertThrows(HTMLInspector.OutputRewriteFailure.class, () -> p.completeBuild(null, () -> {
      builtWith.add(f.get(p));
      throw new HTMLInspector.OutputRewriteFailure("Jekyll can't write en/a.md again");
    }, () -> resets[0]++));
    assertEquals(2, builtWith.size());
    assertEquals(1, resets[0]);
  }

  @Test
  void onlyACompleteBuildThatFinishedPassesWhatItFoundOn() throws Exception {
    Publisher p = new Publisher();
    p.setPf(new PublisherFields());
    p.getPf().logger = new ILoggingService() {
      public void logMessage(String message) {
      }

      public void logDebugMessage(LogCategory category, String message) {
      }
    };
    Field f = Publisher.class.getDeclaredField("pageRebuild");
    f.setAccessible(true);
    HtmlCheckRecords found = new HtmlCheckRecords((String) null);
    p.completeBuild(null, () -> ((PageRebuild) f.get(p)).setHtmlRecords(found), () -> {});
    assertSame(found, ((PageRebuild) f.get(p)).carryOver().getHtmlRecords());

    // a build that fails late still holds the last build's records: they mustn't go on
    PageRebuild carried = ((PageRebuild) f.get(p)).carryOver();
    assertThrows(IOException.class, () -> p.completeBuild(carried, () -> {
      throw new IOException("a fatal html check message");
    }, () -> {}));
    assertEquals("the last build didn't finish", carried.getNotReusable());
    assertNull(carried.carryOver().getHtmlRecords());

    // a build done again: what it used finished, the state it dropped didn't
    PageRebuild again = new PageRebuild();
    again.setHtmlRecords(found);
    again.finished();
    PageRebuild dropped = again.carryOver();
    p.completeBuild(dropped, () -> {
      if (f.get(p) == dropped) {
        throw new HTMLInspector.OutputRewriteFailure("Jekyll can't write en/a.md again");
      }
      ((PageRebuild) f.get(p)).setHtmlRecords(found);
    }, () -> {});
    assertSame(found, ((PageRebuild) f.get(p)).carryOver().getHtmlRecords());
    assertNull(dropped.carryOver().getHtmlRecords());
  }
}
