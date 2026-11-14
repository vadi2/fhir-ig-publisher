# uz-integ W1a deleted output file: code read

Finding: qa2/results-s2.md, uz-integ "one-off" note. The first int Page Rebuild logged "14 written, 1 deleted". The repro gave "13 written, 0 deleted".

Conclusion: **no path found**. Code at 3957c419 (same as e6a0c6a2 for this path). No code changed.

## What decides "deleted"
- `Publisher.syncWatchOutput()` calls `OutputFolderSync.sync(staging = pf.outputDir, target = output/)`.
- It deletes every file and folder in the target that is not in the staging folder at that moment. The deleted count includes folders.
- The full build's sync moves its files out of staging, so after a first build into an empty output, staging holds only empty folders.
- A page rebuild's staging folder at sync time is built up in this order:
  1. Jekyll writes its output, then the template's onCheck runs.
  2. `checkJekyllOutputs` requires that file set to equal the build's set at the same point (`afterJekyll`). A difference forces a Complete Build.
  3. `restorePackageOutputs` copies back every file the package step added or changed (stashed in `afterPackage`). If the package step deleted a file, `notReusable` is set.
  4. The steps after the package step run again as in the build: AIProcessor, html check, fragment-usage-analysis.csv, full-ig.zip, then finishIg (qa-dep.html, qa-tx.html, qa-ipreview.html, ValidationPresenter qa.* and qa-txservers.html, qa.json, qa-time-report.*).

So a deletion needs a file in the build's staging at sync time that none of these steps produces again.

## Ruled out
- **Jekyll or onCheck output:** the set is checked equal. A mismatch gives a Complete Build, not a sync.
- **Package step output** (package.tgz, zips, sub-package-list.json, combined package): stashed when it differs by size or mtime from after Jekyll. A new file always differs, so it is always stashed and restored.
  - A failed stash or restore throws, and the next build is a Complete Build.
  - Size plus mtime can miss a same-size rewrite within the same mtime tick (ns on Linux). That would at most give a "written", never a deletion.
- **Steps after the package step:** they run again. I found no conditional file among them that could be missing in one run and present in another:
  - AIProcessor's .md files depend only on the html set (checked equal) and on package.tgz (restored).
  - qa-tx.html depends on the tx log temp file existing, which createTempFile ensures.
  - The qa-* files are written every time.
- **Writers in background threads:**
  - DependentIGFinder writes qa-dep.html in `finish()`, which waits for its thread.
  - ExtensionTracker only posts to a server.
  - ParallelFolderZip and ProfileSpreadsheetPrefetcher write to temp.
  - I found no writer to output/ or staging that can finish after a listing or after the sync.
- **Writes straight to output/** (bypassing staging): none found. qa-tx.html and PublicationChecker go through `stagedPath`.
- **Leftover files in staging** from the last sync: Jekyll's clean plus the file set check would turn any difference into a Complete Build.

## What the numbers suggest
- The original run wrote one file more than the repro and deleted one, so it differed in two files, not one. Two explanations fit:
  - Something outside the publisher changed output/ or staging between the first build and W1a. This was the run that had container restarts and a disk-full stop.
  - An empty folder was counted: the deleted count includes folders. If the 30541 figure counted entries, not files, a deleted empty folder would show as -1.
- Neither is shown, because the file wasn't named.

## If it shows up again
Log the deleted paths in `syncWatchOutput`. This is a small diagnostic, not done here. Also record `find output -type d -empty` before and after the rebuild.
