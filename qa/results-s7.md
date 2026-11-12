# QA results, shard s7

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, 15 GB. Swap file 6 GB, not 16 GB: the 16 GB swap plus build temp folders filled the disk allowance (empd R1 failed with rc=1, redone). Heap 7600m for every build.
- Every build: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder <shared>`, no -threads, one publisher at a time.
- Times in seconds: whole java run for R1/I1/I2/F2/F6; watch: first build, then each edit to the next "Finished @".
- Watch: one `-rapido -watch` session runs W1..W6 without a restart; output and sources are snapshotted after W2 and W6. F2/F6 are fresh full builds of those snapshotted sources, run after the watch session.
- Compare: qa.json counts, qa.txt lines and qa.xml issues (run folder and the watch staging folder masked); every output file after masking dates, times, UUIDs, run folder; archives (zip, xlsx, tgz, pack, nested) entry by entry, SQLite files as sorted rows; a file whose tokens are the same in another order counts as order-only. Skipped: qa*, fragment-usage-analysis.csv (timing column).
- Run incidents: formulary R1 redone (tx.fhir.org restarted during the first run: 65 "cache lost" errors); empd and eps R1 redone (first runs overlapped a stray publisher).

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I2 | W first, W1..W6 | F2 | F6 | qa (all int builds) | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| IHE/ITI.DIS | 5a37d30b52 | 7600m | 103 | 0/23/119 | 74 | 66 | 66, 10/37/32/43/39/30 (all edits seen) | 60 | 60 | 0/23/119 | REGRESSION (order only, looks intended): llms.txt/llms.md page order, HL7 #1461 watch-page-rebuild (in base). pre-existing diffs: THO version in a NamingSystem narrative link, package .index.db rows, used-by list order |
| hl7germany/medication-ig-de-r4 | 06c8d75d78 | 7600m | 174 | 1673/2112/815 | 150 | 168 | 143, 12/107/98/131/130/106 (all edits seen) | 144 | 144 | 1673/2112/815 | REGRESSION (order only, looks intended): llms.txt/llms.md page order, HL7 #1461 (as dis). pre-existing diffs: package .index.db rows, tx.fhir.org bcp:47 expansion display in package-combined.tgz, used-by list order |
| HL7/davinci-pdex-formulary | 549af4d87a | 7600m | 116 | 0/0/22 | 117 | 95 | 114, 5/61/59/76/74/63 (all edits seen, see notes) | 115 | 114 | 0/0/22 | pending master watch: W2-only link difference. REGRESSION (looks intended): spec.internals drops .ai.html targets, PR #48 |
| ahdis/ch-ekm | cb48773900 | 7600m | 315 | 794/8352/4461 | 292 | 210 | 284, 8/218/222/230/232/229 (all edits seen) | 270 | 258 | 794/8352/4461 | REGRESSION (looks intended): spec.internals drops .ai.html targets, PR #48. pre-existing diffs: mapping column order (QA line numbers), NamingSystem version links in narratives, package .index.db rows |
| HL7/phd | 565d3336a2 | 7600m | 100 | 0/0/2 | 100 | 88 | 108, 1/42/36/72/75/40 (all edits seen) | 101 | 95 | 0/0/2 | REGRESSION (looks intended): spec.internals drops .ai.html targets, PR #48. pre-existing diffs: child-profile and used-by list order, package .index.db rows |
| TWNHIFHIR/empd | 988d8dbbaf | 7600m | 209 | 0/0/0 | 228 | 216 | 212, 9/148/146/187/183/137 (all edits seen) | 208 | 198 | 0/0/0 | REGRESSION (order only, looks intended): llms.txt/llms.md page order, HL7 #1461 (as dis). pre-existing: used-by list order |
| hl7-eu/eps | 3314660fc5 | 7600m | 188 | 0/51/15 | 183 | 147 | 181, 2/98/76/124/131/93 (all edits seen) | 176 | 197 | 0/51/15 | pending master watch: comparison error pages in watch builds have 2 extra stack frames (as formulary). REGRESSION (looks intended): spec.internals drops .ai.html targets, PR #48; llms.txt/llms.md page order, HL7 #1461. pre-existing: used-by list order |

## Notes per IG

### dis
- QA counts and messages equal in every build (I1, I2, W2, W6, F2, F6 vs R1 and each other).
- I1 vs R1: package.tgz `.index.db` ResourceList has 56 rows with int, 0 with master R1. Order-only: llms.txt/llms.md page order, used-by lists in *-ref-*list.json/csv.
- I2, W2, W6 vs fresh builds (R1, I1, F2, F6): ImagingStudy-study-42 narrative links the DICOM NamingSystem to THO 6.5.0 instead of 7.5.0 (both packages are loaded; 6.5.0 comes in as a dependency).
- Master reruns, same heap: M1 fresh 67 s, M2 second build in the M1 folder 71 s, MW watch first 71, W1..W6 28/43/33/43/45/57 (W1 is a Differential Build on master, a Page Rebuild on int). QA equal to R1 in all.
- pre-existing: M1 and M2 link the NamingSystem to THO 6.5.0 and have 56 .index.db rows, R1 had 7.5.0 and 0 rows; MW W2/W6 differ from int W2/W6 in the same NamingSystem link. Used-by order in *-ref-*list.json/csv differs between R1 and M1 (order only).
- REGRESSION (order only): llms.txt and llms.md (also in ai.zip and full-ig.zip) list the pages in name order with int and in file system order with master (R1 = M1 = M2). `AIProcessor`: `Arrays.sort(files)` with the comment "in name order: the lists of pages and the zip don't depend on the order the file system lists them in", from merge 46cb7b7 (perf2/ws2-jekyll), which is in HL7 #1461 watch-page-rebuild e70bb8a and in the base a4d1e90. Repro: full build of IHE/ITI.DIS 5a37d30b52 with int.jar vs master.jar, diff output/llms.txt. Same finding as s3/s4/s5.

### medde
- QA counts and messages equal in every build. Every int build equals every other int build (I1 = I2, W2 = F2, W6 = F6, W6 = I1).
- vs R1: package.tgz `.index.db` rows (28 vs 0), and package-combined.tgz ValueSet-all-languages (tx.fhir.org bcp:47 expansion) display "Interlingua (International Auxiliary Language" in R1 vs "... Association)" in every int build. Master rerun M1 (fresh, 155 s) has the int values for both, so both are pre-existing (they vary between master runs). int vs M1: order only (llms.txt page order, as dis; used-by lists).

### chekm
- QA counts equal in every build (794/8352/4461, 98 broken links); message sets equal once Line/col is masked.
- Line numbers of about 60 broken-link errors in en/StructureDefinition-ch-ekm-patient-mappings.html and -immunization-mappings.html move between builds: the mapping columns (e.g. "Immunization Form to CH EKM Immunization") come in a different order. This also varies between two int builds (I1 vs I2) and between W2/F2, so it is run-to-run variation. Master rerun M1 (fresh, 258 s): 84 QA line numbers in ch-ekm-patient-mappings.html differ from master R1, so pre-existing.
- Every comparison (I1, I2 vs R1; I1 vs I2; W2 vs F2; W6 vs F6; W6 vs I1) differs in NamingSystem link versions in narratives (THO 7.0.1/5.3.0/6.2.0, ch-term 3.3.0/3.4.0; 460 to 612 files plus their escaped .json.html/.xml.html), the known pattern from dis. Master varies too: Composition-ChEkmCompositionExample-HepatitisC.json links THO 7.0.1 in R1 and 7.3.0 in M1 (int I1: 5.3.0), so pre-existing.
- vs R1 also spec.internals (#48) and .index.db rows.

### phd
- QA SAME (0/0/2) in every build.
- W2 = F2, W6 = F6, W6 = I1: no content difference, order only.
- Order only, in every pair including int vs int: used-by lists in *-ref-*list.csv/json and package.db Resources, and the "Child profiles" list of the abstract PhdBaseObservation (narrative and pages). Master varies too: R1 lists PhdNumericObservation first, master rerun M1 (fresh, 91 s) PhdBitsEnumerationObservation, and the used-by row in valueset-ref-list.csv differs between R1 and M1, so pre-existing.
- I1, I2 vs R1: spec.internals (#48, .ai.html targets) and package .index.db ResourceList rows.

### empd
- QA SAME (0/0/0) in every build. No content difference in any comparison (differ=0 everywhere).
- I1, I2 vs R1: order only, llms.txt/llms.md page order (#1461) and used-by lists in *-ref-*list.csv/json.
- W2 = F2, W6 = F6, W6 = I1: order only, used-by lists and the order of a list in package-combined.tgz StructureDefinition-Observation-vitalSigns-twcore-0.3.2.json (same kind of run-to-run list order as phd).

### eps
- QA SAME (0/51/15) in every build.
- I1, I2 vs R1: spec.internals only (547 targets with master, 482 with int; every missing one is a `*.ai.html` page, none added), PR #48. Order only: llms.txt/llms.md (#1461), used-by lists.
- W2 vs F2, W6 vs F6, W6 vs I1: 16 comparison error pages (comparison-v1.0.0-ballot, ips-comparison-v2.0.1, and their full-ig.zip copies) print a stack trace, and in watch builds it has two extra frames, `Publisher.completeBuild` and `Publisher.lambda$execute$0`. Nothing else. Same as formulary; checked against the formulary master watch.

### formulary
- QA counts and messages equal in every build.
- I1, I2 vs R1: only spec.internals (definitions.*.zip, package*.tgz other/, validator*.pack): int lists 554 targets, master 641; the 87 missing are `*.ai.html` pages, which are in neither output (the AI step deletes them). Commit 349fc12 "spec.internals doesn't list the ai pages the AI step removes", PR #48 perf/watch-fsh-tail, also in the base. Same as s2/s4. Repro: full build with int.jar vs master.jar, diff `targets` in output/definitions.json.zip!spec.internals.
- Watch: this IG renders pages into output/en/ (output/*.html are redirect stubs), so the spec's html checks reported MISSING for W1/W2/W4. Against output/en/: every edit reached output (manifest hashes after each step, snapshots): W1 changed en/background.html and W6 restored its W0 hash, W4 created en/Patient-qa-new-patient.html, W5 removed it. Specs of chekm/phd/eps fixed to check output/en/ before their runs.
- W6 = F6 = I1 except comparison-v2.1.0 error pages (two profiles fail to compare on both jars): the stack trace in the page has extra frames (`Publisher.completeBuild`) when built in watch mode. Line numbers masked.
- W2 vs F2: in the W2 snapshot only, package-combined.tgz StructureDefinition-us-core-location-7.0.0.json Location.address.type comment links `location.html#` instead of `http://hl7.org/fhir/R4/location.html#`, and one package.db CodeSystemListRefs row for insuranceplan-type links R4 instead of THO 7.5.0. Master watch rerun pending.
