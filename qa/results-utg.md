# QA results, shard utg

Status: in progress (Phase C: watch sequence)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- UTG: HL7/UTG master d8b0804a9f3dda4fc208eead147bae6d9b80b69b
- VM: 4 CPU, 15 GB. Swap file 10 GB, not 16 GB (the session disk allowance is about 30 GB; a UTG work folder is 8.6 GB: temp 5.8, output 2.6). Heap 10g for every build (peak "Max Memory Used = 9Gb", about 2.8 GB swapped at peak).
- Every build: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx10g -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder <shared>`, no -threads, one publisher at a time. Times are the whole java run in seconds.
- Compare: qa.json counts and the qa.txt message set (file + text, run folder masked); every output file after masking dates, times, UUIDs and the run folder; archives (zip, tgz, pack, nested) entry by entry; package.db as sorted rows; a file whose tokens are the same in another order counts as order-only. Skipped: qa*, the time column of fragment-usage-analysis.csv.
- Reference: R1 was the first build on an empty package cache and differs from every later build, master included (see notes), so R2 (master again, warm cache) is the reference for the int builds.

| IG | commit | heap | R1 | R2 | R1/R2 qa e/w/h | I1 | I2 | W first, W1..W6 | F2 | F6 | qa (all int builds) | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/UTG | d8b0804a9f | 10g | 843 | 804 | 110/69/3 | 834 | 814 | (running) | | | 110/69/3 | REGRESSION (looks intended): spec.internals drops 4220 .ai.html targets, PR #48. REGRESSION (order only, looks intended): llms.txt page order, HL7 #1461. watch: pending |

## Notes

- QA counts and messages: I1, I2 equal to R2 (110/69/3, 54 broken links, 236 messages).
- R1 vs R2 (both master): R1 renders the jurisdiction as text "Global (Whole world)" on 25.5k pages where R2 (and every later build) shows the flag image, and R1 has no assets/images/usa.svg, so R2 has 9 more broken-link errors about it (54 vs 45). Cold package cache on the first run. pre-existing, not a QA finding against int.
- Used-by order in codesystem-/valueset-ref-*list.json/csv (and csvs.zip) differs between master runs: order only, pre-existing.
- I1, I2 vs R2: only these.
  - REGRESSION (looks intended): spec.internals `targets` (package.tgz and hl7.terminology.r3/r4/r5.tgz `package/other/spec.internals`, validator.pack, validator-hl7.terminology.pack, and their copies in full-ig.zip) has 25527 entries with int, 29747 with master (R1 = R2). The 4220 missing are all `*.ai.html`, which are in neither output (the AI step deletes them). Commit 349fc12 "spec.internals doesn't list the ai pages the AI step removes", PR #48 perf/watch-fsh-tail (also in the base a4d1e90). Confirmed with a jar from the #48 head 9614fa3 alone: full build P48 (815 s) has the same 4220 missing, QA equal. Repro: full build of UTG with int.jar or #48 jar vs master.jar, diff `targets` in output/validator.pack!spec.internals.
  - REGRESSION (order only, looks intended): llms.txt and en/llms.txt (also in en/ai.zip, full-ig.zip) list pages in name order with int, file system order with master (R1 = R2). Same as the s3/s4/s5/s7 finding: `AIProcessor` `Arrays.sort(files)`, HL7 #1461 watch-page-rebuild (in the base).
  - package.db: equal as sorted rows (raw bytes differ).
- I2 (second full build in the I1 folder) equals I1 in everything above.
