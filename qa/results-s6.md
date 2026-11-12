# QA results, shard s6

(in progress)

Setup: master 32046bc (master.jar), integration `qa/integration-ready` 6c1abf4 (int.jar). 16 GB swap, Jekyll 4.4.1,
SUSHI 3.21.0, JDK 21. Every build: `LC_ALL=C.UTF-8` (the container's default locale is ASCII; omni/lars has
non-ASCII image file names and fails without it, on both jars), `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m
-XX:+UseStringDeduplication`, `-tx https://tx.fhir.org`, shared package cache. Compare masks per QA-PLAN step 4, plus:
archives (zip, xlsx, pack, tgz, nested) compared entry by entry, package.db compared as sorted rows, the
`temp/pages/.watch-output` staging path (qa.txt of a watch build), the Time and Size columns of
fragment-usage-analysis.csv, key order of `*-ref-list.json`/`*-ref-all-list.json` ("used by" lists).
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
