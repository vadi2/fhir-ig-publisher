# Fix of the PR #42 regression on UTG

Status: done. perf3/early-publish 3a04f3a -> 69dc70f; PR #45 merged with it (f6bfcc3).

Bench: HL7/UTG d8b0804a9f on a 4 CPU / 15 GB VM with 8 GB swap.
- Command: `-Xmx10g -XX:+UseStringDeduplication -rapido -watch`.
- Each session runs the first build, then page1 (a sentence in license.xml), then sd1 (an SD description).
- Times are seconds from the save. Each rebuild swaps 13 to 20 GB, and runs made hours apart differ by about 10 %, so compare within a pair.

| session | first | page1 end | page1 page | sd1 end | sd1 page | Jekyll done to html check |
|---|---|---|---|---|---|---|
| base a4d1e90 (A) | 857 | 1009 | 999 | 1112 | 1097 | 77, 100 |
| #42 3a04f3a (A) | 846 | 1081 | 787 | 1089 | 800 | 114, 123 |
| base (B) | 751 | 887 | 881 | 945 | 932 | 77, 105 |
| fix 69dc70f (B) | 754 | 886 | 651 | 911 | 671 | 74, 89 |

Cause: between Jekyll and the html check, #42 added two things.
- The early copy: 15 s on the first build, 25 to 33 s on a rebuild.
- The full GC, moved after Jekyll: 18 to 30 s instead of 7, because the heap was swapped out while Jekyll ran.
- The end sync costs the same on base and #42 (18 to 28 s).

Fix:
- The copy runs on a background thread. The AI step, the html check, the next Jekyll run, the sync and failure recording wait for it.
- The GC is back before Jekyll.

Result: a rebuild now ends no later than on base, and its pages are up 230 to 260 s before base finishes.

Correctness:
- After the edits, the watch output equals a fresh full build by the same jar: 85602 files, after masking dates, times and UUIDs.
- The only differences are pre-existing: the order of the used-by lists in codesystem-ref-*list (same tokens) and the time column of fragment-usage-analysis.
- QA is 110/69/3 with 54 broken links on both.
- Every edit reached output.

Commits:
- 3657f77: background copy; GC back before Jekyll.
- b01605f: an Error or bug in the copy fails the build at the point that waits for it; files the build writes again are left for the sync.
- 69dc70f: simplifier pass.

Review: the pr-review-toolkit plugin install was denied in this session, so a general review agent applied the same lenses. Its findings:
- Torn package files: CONFIRMED with a stress run (40 of 40 copies torn) and FIXED. With a kept staging folder and an unknown written list, the copy now takes only files written since the build started and skips anything changed after it started. `open(O_TRUNC)` shows size 0 with the old mtime, so an mtime check alone isn't enough.
- Copy errors swallowed: CONFIRMED with a failing test and FIXED.
- Interrupts, log thread-safety, an executor per generator: not confirmed. The executor was replaced by a thread per copy anyway.

Tests: publisher.core suite, 752 tests on #42 and 789 on #45 after the merge. Failing:
- GitUtilitiesTests[4], the known failure;
- ParallelFolderZipTest x2 (a non-UTF-8 path, also fails on base);
- AIProcessorTest once on #42 (a timing flake in code #42 doesn't change; passes on reruns).

#45 merge: TailWatch.enabled is now volatile and is set by the copy thread. A page rebuild waits for the copy before its first give-way check, and a complete build waits for it before the AI step's check.

## Orchestrator review (on 69dc70f and f6bfcc3)

New commits:
- perf3/early-publish: 5509d19, 5d1aa98.
- perf3/tail-restart: merges 97a2dc5 and 4657c25, then fae64d9.

Suites, failing only GitUtilitiesTests[4] and ParallelFolderZipTest x2 (pre-existing):
- early-publish: 755 tests;
- tail-restart: 793 tests.

1. **Copy failure rethrown in recordWatchFailure ends watch mode.** CONFIRMED by reading: `awaitEarlyPublish` throws by design (test `whatFailsTheCopyOfThePagesFailsTheBuildWhenItWaitsForIt`), and `recordWatchFailure` called it inside the watch loop's catch. FIXED:
   - `finishEarlyPublish()` returns the copy's failure; `recordWatchFailure` logs it and adds it to `e` with addSuppressed, then goes on recording.
   - The normal path wraps a RuntimeException with "Unable to put the pages in <output>: ...".
   - Test: `aBuildThatHasFailedGetsWhatFailedTheCopyWithoutItBeingThrown`.
2. **Interrupt cuts the wait short.** CONFIRMED with failing test `anInterruptDoesntCutShortTheWaitForTheCopy`: with the flag set, the wait returned while the copy was still running. FIXED: the wait is uninterruptible and the flag is restored afterwards.
3. **Static files with their source's mtime skipped.** CONFIRMED with a failing test: a css with an old mtime was not copied.
   - First fix, ctime (5509d19), was REFUTED by a UTG run: renames during the build reset the ctime of last build's leftovers, and the copy listed 85578 files again.
   - FIXED in 5d1aa98 without clocks: with a kept staging folder and no Jekyll list, the copy takes only `.html` pages and files that exist in Jekyll's source folder (`temp/pages`).
   - Test `whenJekyllDidntSayWhatItWroteOnlyItsFilesAreCopiedFromAKeptStagingFolder` covers both the old-mtime css and a package rewritten during the copy.
   - UTG run fix4: the copy listed 81132 = Jekyll's files only. Page1 finished 913 s, pages up 658 s.
4. **Skipped and changed-during-copy files not in Result; give-way still enabled.** REFUTED.
   - Skipped files are never Jekyll's pages: they are last build's leftovers, or files written after the copy started.
   - Nothing writes Jekyll's files during the copy: the AI step and html checks wait for it.
   - So giving way leaves the output as #45 intends: this build's pages, and the last good build's other files.
   - UTG fix2: 29031 written + 52100 unchanged = 81131, all of Jekyll's output.
5. **Clock skew.** CONFIRMED with libfaketime: with the JVM clock 1 h behind the file system, 3 more early-publish tests failed (nothing copied). FIXED:
   - `asOf` is now the mtime of a marker file created next to the staging folder.
   - The `since` bound is gone (see 3).
   - The suites pass under faketime at -1 h and +1 h.
6. **Copy dies part way, watchPagesPublished stays false.** CONFIRMED with failing test `aCopyThatFailsPartWayCountsAsHavingPutPagesThere`. FIXED: a copy that throws marks the build's fields as having put pages there.
7. **Tail give-way returns without waiting; the copy logs through a mutable pf.** CONFIRMED with failing test `theBuildGivesWayOnlyOnceTheCopyHasFinished` (gave way while the copy's last log line was still running). FIXED:
   - `pagesPublished()` is now called last;
   - the copy logs through its own build's logger;
   - the generator's `givesWayBefore` waits for the copy when it gives way.
