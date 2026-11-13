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
