# QA results, shard s6

(in progress)

Setup: master 32046bc (master.jar), integration `qa/integration-ready` 6c1abf4 (int.jar). 16 GB swap (4 GB from 14:00, disk), Jekyll 4.4.1,
SUSHI 3.21.0, JDK 21. Every build: `LC_ALL=C.UTF-8` (the container's default locale is ASCII; omni/lars has
non-ASCII image file names and fails without it, on both jars), `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m
-XX:+UseStringDeduplication`, `-tx https://tx.fhir.org`, shared package cache. Compare masks per QA-PLAN step 4, plus:
archives (zip, xlsx, pack, tgz, nested) compared entry by entry, package.db compared as sorted rows, the
`temp/pages/.watch-output` staging path (qa.txt of a watch build), the Time and Size columns of
fragment-usage-analysis.csv, key order of `*-ref-list.json`/`*-ref-all-list.json` ("used by" lists).
Known by-design differences, normalized by the compare from ucum-test on (reported once under the IG that showed
them first): the Pages list order in llms.txt / llms.md (HL7 #1461), and `*.ai.html` entries in the package's
`other/spec.internals` targets (349fc12). "Used by" order is also masked in the `*-ref-*list.csv` files.
From crmi on the compare also masks UUIDs glued to a prefix (`hc<uuid>`, `cb<uuid>`) and Java stack-trace
frames (`\tat ...(X.java:N)`: comparison pages embed exception traces whose line numbers differ by jar and call path).
F2/F6 are built from a pristine copy of the IG with the same edits applied (the watch session runs on).

## TU9IVy1UV0NvcmVJRw/VFcgQ29yZSBJRyBJVFJJ (tw.gov.mohw.twcore)
skip: source not reachable. The qas.json repo path is base64 (MOHW-TWCoreIG / "TW Core IG ITRI"), not a GitHub repo.

## TEHIK-EE/ig-ee-qre (ee.fhir.qre) @ 9f4140f
- heap 7600m. QA 40 / 103 / 1045 (errs / warnings / hints) in every build.
- times (s): R1 73, R1b (master again) 65, I1 69, I2 64; watch first 71, W1 1.6 (Page Rebuild), W2 52.9, W3 50.7,
  W4 51.7, W5 53.5, W6 50.9; F2 68, F6 69. W2 to W6 are Complete Builds on both jars by design: the IG's
  CodeSystems aren't on tx.fhir.org (227 failed tx calls), and Rapido rebuilds everything after tx failures.
- every edit reached output/; F2 = W2, F6 = W6, W6 = I1 (no differences after masking).
- R1 = R1b (master is deterministic here).
- verdict: **diff by design, not a bug**: I1, I2, W6 differ from R1 only in the order of the Pages list in
  `llms.txt` (and the same file in `ai.zip` and `full-ig.zip`): master lists `download.md` before `artifacts.md`
  (the order `File.listFiles()` returns), int sorts by name. Source: `AIProcessor` "in name order: the lists of
  pages and the zip don't depend on the order the file system lists them in", from HL7 #1461 watch-page-rebuild
  (e70bb8a, already in the base). Reproduce: full build of ig-ee-qre with each jar, `sed -n 5,8p output/llms.txt`.

## hl7-be/ucum-test (hl7.fhir.be.ucum-test) @ af477f6
- heap 7600m. QA 0 / 33 / 12 in every build.
- times (s): R1 50, I1 48, I2 43; watch first 51, W1 2.2 (Page Rebuild), W2 21.6 (Differential, 14 of 34 to render),
  W3 30.7, W4 28.6, W5 27.5, W6 29.1 (Complete Builds: "changed: ig", SUSHI's IG resource changes with an example's
  title or the example list); F2 46, F6 48.
- every edit reached output/ (the pages are under output/en/; W3 checked by the page's hash: changed at W3, back
  to the original at W6). F2 = W2, F6 = W6, W6 = I1, I2 = I1.
- verdict: **diff by design, not a bug**: I1, I2, W6 differ from R1 only in (a) llms.txt page order (as ee-qre) and
  (b) `package/other/spec.internals` "targets" (in package.tgz, package.r4.tgz, package.r4b.tgz, definitions.*.zip,
  validator*.pack, full-ig.zip): master lists the 23 `*.ai.html` pages, int doesn't. Master's list is wrong: the AI
  step deletes those pages, they are not in master's output/ either. Source: 349fc12 "spec.internals doesn't list the
  ai pages the AI step removes" (older #48 perf/watch-fsh-tail, in the base). Reproduce: full build with each jar,
  `tar xzOf output/package.tgz package/other/spec.internals | grep -c ai.html`: master 23, int 0.

## HL7/crmi-ig (hl7.fhir.uv.crmi) @ 89c464e
- heap 7600m. QA 17 / 25 / 235 in every build.
- times (s): R1 191, I1 162, I2 155; watch first 180, W1 36.7 (Page Rebuild; html checks not reused: artifact-signing
  page has conformance clauses), W2 98 (Differential, 81 of 264 to render), W3 160, W4 154, W5 154, W6 165 (Complete
  Builds, "changed: ig"); F2 164, F6 163.
- every edit reached output/en/. F2 = W2, F6 = W6, W6 = I1 (no differences after masking).
- verdict: **FINDING F-IDX (package .index.db), see below**: I1, I2, W6 differ from R1 only in
  `hl7.fhir.uv.crmi.en.tgz` (and its copy in full-ig.zip): `package/.index.db` table ResourceList is empty in master,
  filled in int (rows match `.index.json`). With `.index.db` ignored, I1 = R1 (0 differing files). The main
  package.tgz has an empty ResourceList on both jars (129 entries in `.index.json`).

## HL7/fhir-cqm (hl7.fhir.uv.cqm) @ 5b61912
- heap 7600m. QA 19 / 224 / 381 in every build.
- times (s): R1 228, I1 142, I2 159; watch first 157, W1 4.5 (Page Rebuild), W2 91 (Differential, 272 of 415),
  W3 78 (Differential), W4 152 (Complete: input/oids.ini changed), W5 142 (Complete: files added/removed),
  W6 84 (Differential); F2 137, F6 129. The first watch attempt was lost to a full disk at 13:43; the numbers are
  from the rerun.
- every edit reached output/en/.
- compare masks used here in addition (env NORMTX, NORMANCHOR): identifier-type displays
  ("United States National Provider Identifier" vs "NPI", "Uniform Resource Identifier (URI)" vs "URI") and
  CLD anchor pairs `<a name="X"> </a><a name="hcX"> </a>`.
- I1 vs R1: F-IDX in `hl7.fhir.uv.cqm.en.tgz` (as crmi). package.db and package.tgz/.r4.tgz/.r4b.tgz differ;
  F2 vs W2, F6 vs W6, W6 vs I1: only package.db (and its copy in full-ig.zip).
- explained, not jar related:
  - validation column numbers in qa.html / validation-oo.json (master c174834, int c174843): cqm narratives link
    to crmi profiles through the shared cache's crmi#dev, which holds `file://` paths of whichever crmi build dir
    wrote it last (R1.ig vs W/W6.src). QA setup artifact; the paths are masked but shift columns.
  - identifier-type displays and package.db CodeSystemListRefs (R4 vs THO 7.5.0 links for measure-* value sets)
    vary between all builds, int full vs int full included (F2 vs F6, I1 vs I2). Terminology/resolution
    nondeterminism.
- watch-only, pre-existing (master watch, all 6 steps, run the same way: W1 Differential 93s, W2 105, W3 106,
  W4 217 Complete, W5 128 Complete, W6 107): the CodeSystemListRefs row `subject-type` resolves to R4
  "Subject type" (int W2 and W6, master W6), while every full build has the local "Subject Type Value Set".
  ValueSet-measure-stratifier-type.html gets the extra CLD anchor pair in Differential Builds on both jars
  (int W2 and W6, master W2 and W6), and in no full build.
- verdict: **F-IDX only**. The other differences are pre-existing: watch Differential Build artifacts that master
  shows too, terminology nondeterminism, and the crmi#dev path artifact.

## HL7/cqf-recommendations (hl7.fhir.uv.cpg) @ e2fb413
- heap 7600m. QA 51 / 179 / 440 in every build, same message set.
- times (s): R1 301, I1 274, I2 293; watch first 290, W1 66 (Page Rebuild; html checks not reused: a page has
  conformance clauses), W2 219 (Differential, 492 of 986), W3 176 (Differential, 379 of 986), W4 287 and W5 280
  (Complete, "changed: ig"), W6 186 (Differential, 489 of 986); F2 256, F6 258. The first I1 attempt failed when
  the disk filled at 13:43; these numbers are from the fresh rerun.
- every edit reached output/en/. F2 = W2, F6 = W6, W6 = I1 (no differences after masking).
- verdict: **F-IDX only**. I1, I2 and W6 differ from R1 only in `hl7.fhir.uv.cpg.en.tgz` (and its copy in
  full-ig.zip): `package/.index.db` ResourceList is empty on master and filled on int.

## omnifhir/lars (lars) @ aa9243c
- heap 7600m. QA 13 / 21 / 40 in every build, same message set.
- times (s): R1 829, I1 728, I2 717; watch first about 780, W1 41 (Page Rebuild), W2 355 (Differential, 43 of 81),
  W3 513 (Differential), W4 731 and W5 675 (Complete, files added/removed), W6 368 (Differential); F2 749, F6 699.
  Master watch (W1, W2 only): first 737, W1 304 (Differential), W2 691 (Complete, "changed: ig").
- every edit reached output/.
- W6 = I1. I1, I2, W6 vs R1: F-IDX only (package.tgz, ResourceList 0 rows on master, 43 on int = .index.json).
- **FINDING L2 (regression, watch only, HL7 #1466 d6af077)**: W2 (FSH profile description edit). On master, SUSHI's IG
  change forces a Complete Build. With #1466 an IG entry's description alone no longer does, so int runs a
  Differential Build. Its output differs from F2 (full build of the same source) and from master's W2:
  - a. package-combined.tgz `StructureDefinition-DiagnosticReport-twcore-0.3.2.json` (a dependency SD): both
    markdown links read `[檢驗檢查](observation.html)`. In F2, every full build and master W2, one reads
    `(http://hl7.org/fhir/R4/observation.html)`. genCombinedPackage serializes dependency SDs from the live context
    (PublisherGenerator ~1167-1190), so the SD looks rewritten in place by an earlier render and reused.
  - b. ValueSet-lars-specimen-type-values.html: the CLD gains `<a name="lars-specimen-type-values"> </a><a
    name="hclars-specimen-type-values"> </a>`. Not in F2 or master W2. Master's own Differential Builds of cqm add
    the same anchor pair, so b is a pre-existing Differential Build defect that #1466 exposes for this edit.
    Master never ran a Differential Build for lars, so a is not shown to be pre-existing.
  W3 and W6 (also Differential) are clean, and W6 = I1. QA counts and messages are equal.
- L3 (pre-existing): the column order in organization-lars-mappings.html (LabMonthly vs LabDaily) varies by build
  (R1, I1, W6 Monthly first; F2, F6, W2 and master W2 Daily first). This explains F6 vs W6.
- verdict: **REGRESSION L2a (watch only; #1466 exposes the differential path for this edit) + F-IDX**. L2b is
  pre-existing.
