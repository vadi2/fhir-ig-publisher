# Dev: page-only watch rebuilds without the Jekyll driver (rework of HL7 #1461)

Branch `perf4/watch-page-only` from HL7 master 2ccca179. PR: pending (after bench). Ready ref `qa/page-only-ready` = 5111080.

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
Final sessions on 5111080: change, base, base, change. Running.

## Review rounds
- Round 1 (code-reviewer, silent-failure-hunter, pr-test-analyzer, comment-analyzer + type-design-analyzer). Fixed (0925ade): Jekyll error exit accepted in a page rebuild; spec.internals stale after a front-matter rename (now: same output file set or complete build); searchform.html placeholders; context kept loaded after a build that kept nothing; comments; tests for unload paths, copyForRecheck, custom-content marking, prepareTemp twice, fragments restore, OOM rethrow, add/remove reason. Rejected as design nits: PageRebuild state enum, package-private hooks, pf in constructor, merging Template's two methods, isWatched rename; rebuildPages success-path unit test (needs finishIg refactor; the bench shows consecutive Page Rebuilds).
- Round 2 (two agents, all roles). Only a comment fix (270fcfa). Rejected: "pages replayed by Rapido miss the custom-content flag": pages are never replayed (RapidoTracker.alwaysDirty is true for anything not PROCESS_RESOURCE, so canReplay is false).
- code-simplifier: 5111080 (unused overload, duplicated loop and check, constant placement).
