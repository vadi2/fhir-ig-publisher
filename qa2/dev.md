# Dev: page-only watch rebuilds without the Jekyll driver (rework of HL7 #1461)

Branch `perf4/watch-page-only` from HL7 master 2ccca179. PR: vadi2/fhir-ig-publisher#56. Ready ref `qa/page-only-ready` = 3957c419.

## State
- Ported from e70bb8af (diff 32046bc..e70bb8af) only what the page-only rebuild needs: WatchSession.changesSincePreviousBuild,
  PageRebuild (trimmed), Publisher.rebuildPages + finishIg (code moved, not changed), PublisherGenerator regeneratePages /
  checkPageRebuild / afterJekyll / finishGenerate(replay), PublisherFields.pagesWithCustomContent + translationSourcesFor,
  SuppressedMessageInformation use counts, HTMLInspector.copyForRecheck.
- Dropped: JekyllDriver + jekyll-driver.rb, FullIgZip, HtmlCheckRecords, AIProcessor cache, ConformanceStatementHandler,
  OutputFolderSync and PreviousVersionComparator changes, Template ant-script analysis (regex + reflection).
- Suite at 5111080: 598 run, failures only the known GitUtilitiesTests.testGetGitSource[4] and ParallelFolderZipTest (2, locale).
- Mutation checks: 24 targeted mutations of the new code, all killed by the new tests.

## Decisions
- Template declaration (proposal): config.json `"script-inputs": ["input/includes/menu.xml", "input/data", ...]`, paths
  from the IG folder that the template's scripts read. A template with a script and no declaration: complete build. A
  template with an onJekyll target: complete build (it runs on the temp folder, which holds the pages). A template without
  scripts needs nothing. A page edit in or under a declared path: complete build.
- onCheck: always run again in a page rebuild, and its messages must equal the build's (else complete build). So no
  "onCheck reads output" flag is needed. The output is listed for the package-step stash after onCheck, so what onCheck
  writes is written fresh each time.
- Nothing is captured once the build knows a page rebuild can't follow (e.g. undeclared template): no overhead then.
- Context unload is deferred only in watch mode when a page rebuild may follow (core unload() also shuts down the tx
  client, which the html checks of the page rebuild need). Builds without -watch: unchanged.
- Jekyll failing, or returning an error after it said it succeeded, in a page rebuild: complete build.
- Jekyll must write the same set of output files as in the build (front matter can rename pages, and the package's spec.internals lists them): else complete build.
- A regenerated searchform.html gets its placeholders filled as the build does.
- pr-review-toolkit: `claude plugin install` was denied by the session's permission classifier; the six roles were run as
  general-purpose subagents with briefs written from the role names.

## Bench (uz.dhp.core 2c39cd0a, bench-recipe.md flags, -Xmx7600m, no -threads)
Template: local copy of fhir2.base.template#current with `script-inputs` added (ig.ini override, same seed for both jars).
Smoke session (change jar at e9c6e69, before review fixes): page edits were Page Rebuilds, 250.8 s and 247.3 s to Finished (Jekyll 77, AI step 21, html check 91, full-ig.zip 49, qa 6), FSH edits 479 to 510 s; QA counts equal to the build before.
Final sessions on 5111080: change, base, base, change. Medians (s, edit to Finished):

| edit | n per jar | master | this PR | saved |
|---|---|---|---|---|
| page | 4 | 391.9 | 236.7 | 155.2 (40%) |
| profile | 6 | 479.8 | 466.5 | 13.3 (noise) |
| instance | 6 | 446.7 | 434.9 | 11.8 (noise) |
| first build | 2 | 655.8 | 632.3 | 23.5 (noise) |

All edits reached output; QA counts identical between jars at every step.

## Review rounds
- Round 1 (code-reviewer, silent-failure-hunter, pr-test-analyzer, comment-analyzer + type-design-analyzer). Fixed (0925ade): Jekyll error exit accepted in a page rebuild; spec.internals stale after a front-matter rename (now: same output file set or complete build); searchform.html placeholders; context kept loaded after a build that kept nothing; comments; tests for unload paths, copyForRecheck, custom-content marking, prepareTemp twice, fragments restore, OOM rethrow, add/remove reason. Rejected as design nits: PageRebuild state enum, package-private hooks, pf in constructor, merging Template's two methods, isWatched rename; rebuildPages success-path unit test (needs finishIg refactor; the bench shows consecutive Page Rebuilds).
- Round 2 (two agents, all roles). Only a comment fix (270fcfa). Rejected: "pages replayed by Rapido miss the custom-content flag": pages are never replayed (RapidoTracker.alwaysDirty is true for anything not PROCESS_RESOURCE, so canReplay is false).
- code-simplifier: 5111080 (unused overload, duplicated loop and check, constant placement).
- Template inheritance (6a89124e, 2a11c14e, 4a82199a, e6a0c6a2): merged configs concatenate script-inputs, so a child with its own scripts inherited its base's declaration. Now a template with active content (script, targets, otherScripts, overwritten ant script, non-allowlisted files) that doesn't declare drops the declarations before it. Nothing changes unless some template declares; the master active-content scan is untouched (a Windows separator miss is covered in the new check only). Review rounds 3-5 on it; round 5 confirmed nothing. Suite at 4a82199a: 608 run, known failures only. The `|| overwritesAntScript` call can't be mutation-killed on Linux (the scan catches the same case); isAntScript is unit tested.
- qa2/script-inputs.md: per-template lists and the QA recipe (orchestrator request).
- Phase D (orchestrator): (1) tx failures. Master's Rapido does a complete build after a build with failed tx calls; the page rebuild ignored that, so a transient failure's messages stayed until a non-page build. Now honoured (d6c691d1, 3957c419): PageRebuild.checkTxFailures() with Rapido's own count (RapidoTracker.txFailures(pf)), at checkPageRebuild (nothing kept), at the end of a complete build (finished()) and after a page rebuild. IGs with persistent tx failures (vris, eeqre, au-fhir-base) get master's behaviour, a complete build each time. Review rounds 6-7: round 6 found the count too early (failures in onCheck, combined package, html checks, qa missed) and a page rebuild's own failures uncounted, fixed; round 7 confirmed nothing. Not unit tested: the call after a page rebuild in rebuildPages (no success-path unit test). Suite at 3957c419: 610 run, known failures only. Bench unaffected: no bench build had a failed tx call. (2) xml pages (process mode not PROCESS_NONE) refuse with "is transformed": deliberate, now said in the PR.
