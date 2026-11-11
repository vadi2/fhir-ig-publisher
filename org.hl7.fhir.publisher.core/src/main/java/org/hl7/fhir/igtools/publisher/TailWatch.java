package org.hl7.fhir.igtools.publisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Watch mode: lets the rest of a build give way to the next one. Once Jekyll has rendered the pages and they are in the
 * output folder (see PublisherGenerator.publishPagesEarly()), what is left of the build (its tail: the packages and
 * zips, the AI step, the html checks, full-ig.zip, the qa pages, the update of the output folder) is most of its time,
 * and the author has what they were waiting for. A file saved meanwhile would wait for all of it, and then for a whole
 * build. So between the steps of the tail the build asks here whether the watched files changed since it started, and
 * if they did, it stops there: the next build starts at once, and the last build of a row of saves does the whole tail.
 *
 * <p>The build only stops between steps, never inside one, so no file is left half written. What it leaves for the
 * next build is what it had completed: Rapido's state (which only holds what was validated and generated before
 * Jekyll ran, so the build saves it when it stops), and what the last html checks and AI step that ran to their end
 * found, with the output files written since (see PageRebuild.withUnprocessed()). The steps it didn't do are done by
 * the next build that finishes, for everything.
 */
class TailWatch {

  /**
   * The steps of the tail, in the order they run, for saying what a build that stopped didn't do. A build stops only
   * before PACKAGES (or between two of them), AI, FULL_ZIP or QA: never between the AI step and the html checks (see
   * PageRebuild.withUnprocessed()), nor once it has started on qa.html (qa-tx.html and qa-ipreview.html may be in the
   * staging folder already when it stops before QA)
   */
  enum Step {
    PACKAGES("the packages and zips (not all of them, if some were written already)", false),
    AI("the AI step's files", true),
    HTML_CHECK("the html checks", true),
    FULL_ZIP("full-ig.zip", true),
    QA("the qa pages", true),
    SYNC("bringing the output folder up to date with all of that", true);

    private final String description;
    private final boolean inPageRebuild;

    Step(String description, boolean inPageRebuild) {
      this.description = description;
      this.inPageRebuild = inPageRebuild;
    }
  }

  /** Tells whether the watched files changed since the build started */
  interface ChangeSource {
    /** Starts looking for changes: called once, before the first call of settledChanges() */
    void start() throws IOException;

    /**
     * What changed in the watched files since the build started, once they have been quiet for a moment; null or
     * empty if nothing did. Cheap when nothing was saved since the last call
     */
    SourceSnapshot.Changes settledChanges() throws IOException, InterruptedException;

    /** Stops looking for changes (only called after start()) */
    void stop();
  }

  private final ChangeSource source;
  private final boolean pageRebuild;
  private final Function<SourceSnapshot.Changes, String> whyFinish;
  private final Consumer<String> log;
  private boolean enabled;
  private boolean started;
  private boolean closed;
  private Step abandonedBefore;
  private String finishingFor;

  /**
   * @param source tells whether the watched files changed
   * @param pageRebuild whether this is the tail of a page rebuild, which has no package step
   * @param whyFinish why the build is to do its whole tail in spite of these changes (null: it gives way to them)
   * @param log where to say what was decided
   */
  TailWatch(ChangeSource source, boolean pageRebuild, Function<SourceSnapshot.Changes, String> whyFinish, Consumer<String> log) {
    this.source = source;
    this.pageRebuild = pageRebuild;
    this.whyFinish = whyFinish;
    this.log = log;
  }

  /** The pages of this build are in the output folder: from now on it can give way to a change */
  void pagesPublished() {
    enabled = true;
  }

  /**
   * Asked between two steps of the tail: whether the build stops here, before this step, because the watched files
   * changed since it started. Once it has said so it says so for every step after
   */
  boolean givesWayBefore(Step next) {
    if (abandonedBefore != null) {
      return true;
    }
    if (!enabled || closed) {
      return false;
    }
    SourceSnapshot.Changes changes;
    try {
      if (!started) {
        source.start();
        started = true;
      }
      changes = source.settledChanges();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (IOException | RuntimeException e) {
      // the changes are found when the build has finished, as they were before
      log.accept("Unable to look for changes while the build finishes (it goes on to its end): " + e);
      close();
      return false;
    }
    if (changes == null || changes.isEmpty()) {
      return false;
    }
    String why = whyFinish == null ? null : whyFinish.apply(changes);
    if (why != null) {
      if (!why.equals(finishingFor)) {
        finishingFor = why;
        log.accept("=== File changes detected while the build finishes: it goes on to its end (" + why + ") ===");
      }
      return false;
    }
    abandonedBefore = next;
    log.accept("=== File changes detected while the build finishes: giving up the rest of this build, and building again ===");
    log.accept(changes.toString().trim());
    log.accept("This build's pages are in the output folder as Jekyll rendered them. Not done by this build: " + String.join(", ", notDone())
        + ". The next build that finishes does them");
    return true;
  }

  /** What the build that stopped didn't do */
  List<String> notDone() {
    List<String> res = new ArrayList<>();
    if (abandonedBefore != null) {
      for (Step s : Step.values()) {
        if (s.ordinal() >= abandonedBefore.ordinal() && (s.inPageRebuild || !pageRebuild)) {
          res.add(s.description);
        }
      }
    }
    return res;
  }

  /** The step the build stopped before, or null if it didn't */
  Step getAbandonedBefore() {
    return abandonedBefore;
  }

  /**
   * The build has finished, or is past the last point where it can stop (it is about to write qa.html, save its
   * state and bring the output folder up to date, which go together): the changes are looked for after it, as
   * without this
   */
  void close() {
    closed = true;
    if (started) {
      started = false;
      source.stop();
    }
  }
}
