# qa/integration

All of Vadim's unsubmitted performance work on vadi2/fhir-ig-publisher, merged into one branch for QA on real IGs
(see `qa/QA-PLAN.md` on `claude/ig-publisher-perf-41aejs-notes`).

Base: `origin/claude/ig-publisher-perf-41aejs` at `a4d1e90f7964d858fd6eb25305801db89c22e5ac`
(master 32046bc + HL7 #1460 rapido-reused-signposts + #1461 watch-page-rebuild + older versions of
perf/watch-fsh-tail, perf/weak-edges, perf/native-outputs-threads). Core: 7.0.5 (unchanged).

## Merged (in this order)

| PR | branch | head merged | merge commit | conflicts |
|---|---|---|---|---|
| HL7 #1461 | watch-page-rebuild | e70bb8a | (already in base) | |
| HL7 #1460 | rapido-reused-signposts | 2c0ef0d | (already in base) | |
| HL7 #1466 | rapido-ig-entry-text | 929b703 | d6af077 | RapidoTracker, PublisherRapidoWatchTest |
| #50 | perf/validate-sd-extension-count | 3d0196c | 1f45e3a | none |
| #51 | perf/native-outputs-threads | f41ac1e | 4da7e48 | none |
| #48 | perf/watch-fsh-tail | 9614fa3 | ba3fe11 | none |
| #49 | perf/weak-edges | 15bf459 | (already in base) | |
| #39 | perf3/rapido-state | faa64d3 | cb76192 | PublisherRapidoWatchTest |
| #40 | perf3/qa-prep-reuse | 8e8b586 | 8f7801d | none |
| #41 | perf3/archives-reuse | 498c93f | 93d8dc8 | RapidoTracker, PublisherRapidoWatchTest |
| #42 | perf3/early-publish | 3a04f3a | 4f388f9 | test imports |
| #45 | perf3/tail-restart | 14c0cdb | 1d6df52 | PublisherGenerator, RapidoTracker imports |
| #43 | perf3/translations-dirty | da06c02 | fafb05f | RapidoTracker imports |
| #44 | perf3/summaries-reuse | bceb111 | a8de38d | PublisherGenerator imports, RapidoTracker.save() |
| #46 | perf3/native-skip | 1686b40 | 07637af | PublisherGenerator imports |
| #22 | perf2/ws6-watch-deps | 9e064e4 | 11f8b74 | Publisher, RapidoTracker, RapidoDependencyAnalyserTest, PublisherRapidoWatchTest |
| #25 | perf2/ws10-nonascii-names | 3ea44bf | 32f9658 | Publisher |

After the merges: 2def8fa (comment placement only, see Fixes).

## Left out

- **#47 fix/rapido-scrub-tx-cache (1c5a8df)**: calls `TerminologyCache.scrub()` / `TerminologyCache.ScrubResult`, which come
  from an unreleased hapifhir/org.hl7.fhir.core change; core 7.0.5 doesn't have them, so it doesn't compile.
- **#38 perf2/ws7-input-cache (352adff)**: #38 left out: needs core fork change. Its head does contain upstream/master
  now, but it imports `org.hl7.fhir.services.context.IResourceReadListener` (vadi2/org.hl7.fhir.core
  `perf2/ws7-read-hook`), absent from core 7.0.5, so it doesn't compile. Not merged into qa/integration either.

## Conflicts and how they were resolved

- **#1466 into the base (weak edges + #1460 signposts)**, `RapidoTracker`:
  - fields of both sides kept;
  - `scan()` takes both the weak-edge `parent`/`grandparent` (#49) and #1466's `skip` set (the IG entries' name and
    description, hashed into a throwaway hasher but still scanned for references);
  - `igEntryText()`/`children()` (#1466) added next to the weak-edge helpers;
  - `recordValidation`: #1466's early record (key + found profiles, kept even when the rest can't be), then the
    status check, messages, #1460's signposts, and the reuse marker written last so a record without signposts is
    never reused.
  - Test: `validateB(pf, null)` became ambiguous; cast to `String`.
- **#39 rapido-state**, test only: two `state()` helpers (`File` from #1466, `Path` from #39); kept the `Path` one, the
  #1466 callers use `.toFile()`. Kept both #39's mocked context and #1466's `igMode`.
- **#41 archives-reuse vs #39 rapido-state** (same thing done two ways: journaled expansions).
  - #39 stores each expansion once in `_rapido/expansions/<hash>.json` and keeps the parsed ValueSets in memory
    between watch builds.
  - #41 keeps them as unparsed JSON (`SavedExpansions`, parsed only if expansions.* must be written again) and records
    an `expansionsHash` for the archive keys.
  - **Kept both**: #39's hash-named files and in-process cache (cache and kept state now hold the hash-verified JSON
    strings instead of parsed ValueSets), and #41's lazy parsing; replay hands the strings plus `expansionsHash` to
    `SavedExpansions`.
  - `save()` writes #41's archive records and #39's `keep(null)` before writing.
  - The test `theNextBuildOfTheProcessStartsFromTheStateItSaved` now gets JSON strings back and parses them to check.
  - Behaviour note: an expansion file whose hash matches but won't parse now fails when expansions.* are written (as
    in #41), instead of making `canReplay` false (as in #39). It can only happen if the parser stops reading what
    `saveExpansion` wrote; any jar change forces a complete build anyway.
- **#45 tail-restart vs #41 archives-reuse**: `archives().finish()` (#41) runs before #45's safe point ahead of the AI
  step. **Integration fix** in the merge: #45 can stop a build before or between package steps, after #41 moved the
  last build's archives aside for Jekyll; they would have stayed in `_rapido/archives` and been missing from the
  staging output. `PublisherGenerator` now overrides `givesWayBefore()` so a stop calls `archives().finish(false)`,
  which puts them back (nothing is recorded for them, so the next build writes them again). A no-op once the package
  step finished.
- **#43, #42, #46**: import lists only.
- **#44 summaries-reuse**: `RapidoTracker.save()` writes `steps` (#44), `archives` (#41), then `keep(null)` (#39).
- **#22 ws6-watch-deps** (validation build state):
  - #22 replaces `statusWarnings`/`statusBefore` by `buildState()` (statusWarnings, the context's
    unsupportedCodeSystems and codeSystemsUsed), saved under `"state"`.
  - `recordValidation` order: #1466's early found-profiles record, #22's state check (`stateChange`/`isReplayable`),
    messages, #1460's signposts, `"state"` last (the marker `savedValidation` requires).
  - `savedValidation`: #22's state check plus #1460's signpost restore.
  - `Publisher`: #22's log text "Rapido Mode: Not a Page Rebuild (...)" kept together with #45's
    `carriedOver = state.carryOver()`.
  - Tests: both sides' new `RapidoDependencyAnalyserTest` tests kept. In `PublisherRapidoWatchTest` the #1460/#1466 tests'
    helpers (`validator()`, `withValidator()`) now give the mocked context the two sets `buildState()` reads, and
    check the `"state"` key instead of `"statusBefore"`.
  - Checked that files #44/#46 reuse are still claimed for #22's `StaleTempFiles` (else they'd be deleted as stale),
    and that the new Rapido files (`expansions`, `archives`, native records) are under the skipped `_rapido`.
- **#25 ws10-nonascii-names**: watch loop has #45's `tailChanges(...)` and #25's `failure = null`; condition
  `takeTailGaveWay() || waitForChanges(..., failure)`.

## Fixes made on this branch

- In the #45 merge: `givesWayBefore()` override putting archives back (above).
- In the #1466/#39/#41/#22 merges: test adaptations listed above (no test was disabled or weakened; each still
  checks its PR's behaviour).
- 2def8fa: moved the `givesWayBefore` override above the `archives()` javadoc it had split (comment placement only).

## Tests

`org.hl7.fhir.publisher.core`, full suite, `LC_ALL=C.UTF-8`, at 32f9658: **997 run, 1 failure, 0 errors,
10 skipped**. The failure is the known `GitUtilitiesTests.testGetGitSource[4]` (fails on master too).

## Review of the merge resolutions

pr-review-toolkit code-reviewer and silent-failure-hunter, run over `git show --cc` of each merge commit.
- code-reviewer: no bugs at its threshold; one cosmetic point (split javadoc), fixed in 2def8fa.
- silent-failure-hunter: nothing that gives wrong output. Not acted on (each only makes a later build slower or a
  log less clear, checked against the code):
  - a build that stops its tail and then fails to save its state doesn't print the "Build Failed" banner before
    building again (the exception and stack dump are still logged; the next build is a complete one);
  - `RapidoArchives.finish()` swallows an IOException moving an archive back (#41's code; the archive is just written
    again);
  - the parse-failure behaviour change noted under #41.

## Smoke test

CLI jar from 32f9658 (code identical to this head apart from comments), HL7/aitransparency-ig @ 6d0a39f, 16 GB swap,
`IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar int.jar -ig . -tx https://tx.fhir.org`.

- Full build without -rapido: finished in 110 s, 930 files in output/, QA 0 errors, 0 warnings, 0 hints.
- `-rapido -watch`, fresh copy:
  - first build (complete): 54 s, QA 0/0/0;
  - page edit (a sentence appended to input/pagecontent/about.md): Page Rebuild, finished 2 s after the save, the
    sentence is in output/en/about.html;
  - resource edit (AIModelCard profile Description in input/fsh/AIDevice.fsh): Differential Build, 32 s; reused 8 of 43
    validations, native outputs of 20 of 60 files, 152 of 154 resource lists, 2 of 11 archives; the new text is in
    output/en/StructureDefinition-AI-ModelCard.html and StructureDefinition-AI-ModelCard.json; QA 0/0/0.

This only shows the combined jar works end to end; output equality with fresh builds is what the QA sessions check.
