# QA results, shard utg

Summary: UTG built on both jars (R1, R2 master; I1, I2, W, F2, F6 int), every watch edit reached output, QA counts and messages equal to R2 in every int build (W4 has the one expected error from the test copy). W6 = F6 = I1 and W2 = F2 apart from the build time. Intended changes: spec.internals without the 4220 .ai.html targets (PR #48), llms.txt page order (HL7 #1461). Perf: every watch rebuild is a Complete Build on both jars (6 failed tx calls); int watch builds take about 110 s more than master (early page copy, PR #42) and are slower than a fresh full build. Everything else pre-existing.

Status: done

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- UTG: HL7/UTG master d8b0804a9f3dda4fc208eead147bae6d9b80b69b
- VM: 4 CPU, 15 GB. Swap file 10 GB, not 16 GB (the session disk allowance is about 30 GB; a UTG work folder is 8.6 GB: temp 5.8, output 2.6). Heap 10g for every build (peak "Max Memory Used = 9Gb", about 2.8 GB swapped at peak).
- Every build: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx10g -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder <shared>`, no -threads, one publisher at a time. Times in seconds: whole java run for R1/R2/I1/I2/F2/F6; watch: first build, then each edit to the next "Finished @". One `-rapido -watch` session runs W1..W6 without a restart; output is snapshotted after W2 and W6; F2/F6 are fresh full builds of those sources, run afterwards.
- Compare: qa.json counts and the qa.txt message set (file + text, run folder masked); every output file after masking dates, times, UUIDs and the run folder; archives (zip, tgz, pack, nested) entry by entry; package.db as sorted rows; a file whose tokens are the same in another order counts as order-only. Skipped: qa*, the time column of fragment-usage-analysis.csv.
- Reference: R1 was the first build on an empty package cache and differs from every later build, master included (see notes), so R2 (master again, warm cache) is the reference for the int builds.

| IG | commit | heap | R1 | R2 | R1/R2 qa e/w/h | I1 | I2 | W first, W1..W6 | F2 | F6 | qa (all int builds) | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/UTG | d8b0804a9f | 10g | 843 | 804 | 110/69/3 | 834 | 814 | 1042, 1342/1022/1026/970/936/1001 (all edits seen) | 768 | 758 | 110/69/3 (W4: 111, see notes) | REGRESSION (looks intended): spec.internals drops 4220 .ai.html targets, PR #48. REGRESSION (order only, looks intended): llms.txt page order, HL7 #1461. Perf (looks intended): watch builds about 110 s slower than master, early page copy, PR #42 |

## Notes

- QA counts and messages: I1, I2 equal to R2 (110/69/3, 54 broken links, 236 messages).
- R1 vs R2 (both master): R1 renders the jurisdiction as text "Global (Whole world)" on 25.5k pages where R2 (and every later build) shows the flag image, and R1 has no assets/images/usa.svg, so R2 has 9 more broken-link errors about it (54 vs 45). Cold package cache on the first run. pre-existing, not a QA finding against int.
- Used-by order in codesystem-/valueset-ref-*list.json/csv (and csvs.zip) differs between master runs: order only, pre-existing.
- I1, I2 vs R2: only these.
  - REGRESSION (looks intended): spec.internals `targets` (package.tgz and hl7.terminology.r3/r4/r5.tgz `package/other/spec.internals`, validator.pack, validator-hl7.terminology.pack, and their copies in full-ig.zip) has 25527 entries with int, 29747 with master (R1 = R2). The 4220 missing are all `*.ai.html`, which are in neither output (the AI step deletes them). Commit 349fc12 "spec.internals doesn't list the ai pages the AI step removes", PR #48 perf/watch-fsh-tail (also in the base a4d1e90). Confirmed with a jar from the #48 head 9614fa3 alone: full build P48 (815 s) has the same 4220 missing, QA equal. Repro: full build of UTG with int.jar or #48 jar vs master.jar, diff `targets` in output/validator.pack!spec.internals.
  - REGRESSION (order only, looks intended): llms.txt and en/llms.txt (also in en/ai.zip, full-ig.zip) list pages in name order with int, file system order with master (R1 = R2). Same as the s3/s4/s5/s7 finding: `AIProcessor` `Arrays.sort(files)`, HL7 #1461 watch-page-rebuild (in the base).
  - package.db: equal as sorted rows (raw bytes differ).
- I2 (second full build in the I1 folder) equals I1 in everything above.

### Watch (int)
- Edits: W1 a sentence in pagecontent/license.xml; W2 the description of StructureDefinition-ext-namingsystem-title; W3 a display in CodeSystem-FDI-surface (used by ValueSet-surface); W4 a copy of that CodeSystem with a new id/url; W5 deletes the copy; W6 restores every file.
- Every edit reached output: W1 en/license.html, W2 the three SD pages, W3 the CodeSystem pages, W4 the new CodeSystem pages, W5 no copy files left, W6 no marker tokens left in any html (one hit in full-ig.zip bytes is compression noise: no entry contains it).
- QA: W1, W2, W3, W5, W6 equal to R2. W4 has one more error, expected from the test edit: the copy keeps `valueSet` = ValueSet/surface, whose system is the original CodeSystem ("has an 'all system' value set ... doesn't have a matching system").
- W2 vs F2 and W6 vs F6: QA equal; files equal apart from used-by list order (order only, as between master runs) and the build time in spec.internals `date-time` and validator*.pack version.info: watch keeps the time of its first build. F6 vs I1/I2: the same, build time only.
- Every rebuild was a Complete Build: "terminology server calls failed in the last build (6)"; UTG has 6 tx calls that fail every build. Each also logs "Not a Page Rebuild (the template's script scripts/ant-utg.xml uses ${dir.scripts}/filesToBundle.xslt, which isn't read)" and "the html checks can't be reused (CodeSystem-consentpolicycodes.html has conformance clauses)", so on UTG a page edit could not be a Page Rebuild even without the tx failures.
- Master watch rerun MW (first build and W1 only, same edit): first 881 s, W1 986 s, also a Complete Build for the same tx reason, W1 edit seen. pre-existing.
- Perf (looks intended): int first watch build 1042 s vs master 881 s, and int rebuilds 936 to 1342 s, slower than a fresh int full build (758 to 834 s). The extra time is two output writes master doesn't do: "The pages are up in output, 12:36 after the build started" (76 s, 81132 files) and a later "Updated output" (34 s), plus a slower html check (126 s vs 112 s). Commit a039ec8 "Watch: put the pages in the output folder when Jekyll has rendered them", PR #42 perf3/early-publish: pages are visible at 756 s instead of 881 s on master, the build ends later. Attributed from the code and log line, not run with a #42-only jar. W1 (1342 s) is slower than the other int rebuilds (936 to 1026 s); one sample each, tx.fhir.org timing varies.
- pre-existing, seen in passing: spec.internals `date-time` and version.info `date` use `yyyyMMddhhmmssZ` (12 hour clock): F2 started 14:09Z and shows 020952. Same format string on master (SpecMapManager, PublisherGenerator).
