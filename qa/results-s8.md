# QA results, shard s8

Status: in progress (Phase C: 4 of 7 IGs run)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, ~14 GB, 16 GB swap file. All builds with LC_ALL=C.UTF-8 (TWCMIG has non-ASCII image names; under the POSIX locale master fails on them). Times in seconds, whole java run; W*: edit to "Finished @". A "!" after a W time means a check failed.
- Watch sequence: watch stopped after W2 (for F2) and restarted on the same folder (restart) for W3..W6. au-fhir-encounter has no examples, so no W3.
- Compare: QA counts and the multiset of (file, severity, text) from qa.xml; output by masked per-file hashes (archives per entry). "ORDER" = same lines in another order.
- tx notes: au-fhir-base and ch-emr R1 errors are mostly tx.fhir.org not holding the AU/CH SNOMED editions; same for every build here, so they still compare.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | watch | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| hl7au/au-fhir-encounter | 71003d4 | 7600m | 49 | 0/5/165 | 46 | 0/5/165 | 60 | first:46 W1:2 W2:25 restart:41 W4:33 W5:29 W6:26 | 54 | 57 | QA equal; W2=F2, W6=F6=I1 apart from ref-list order. R1 vs I1: package/pack/full-ig.zip and llms.txt order (likely F1/F2; drill-down pending) |
| daviemukungi/IPTp-FHIR-implementationIG | a343cf2 | 7600m | 73 | 5/46/102 | 72 | 5/46/102 | 70 | first:69 W1:8 W2:40 restart:56 W3:43 W4:51 W5:52 W6:39 | 67 | 66 | same (QA messages equal; only *-ref-*list.{json,csv} row order, which varies between runs of the same jar, incl. W vs F and I1 vs W6) |
| HL7/reg-workflow-pharma | 56924f9 | 7600m | 66 | 1/0/0 | 67 | 1/0/0 | 63 |  | 86 | 71 | R1 vs I1/I2: F1, F2 (see notes); pre-existing: Binary md array identity. W2=F2 same. Watch W4 to be rerun (my W4 example was invalid FSH) |
| HL7/fhir-us-ndh | f56330f | 7600m | 137 | 0/11/13 | 160 | 0/11/13 | 130 | first:144 W1:24 W2:89 restart:107 W3:88 W4:127 W5:126 W6:84 | 144 | 135 | R1 vs I1: pre-existing only (P1 search parameter source, P2 THO version in links); I2 vs R1: P3 warm-cache identifier type text (master check pending). W2=F2, W6=F6, W6=I1 apart from P1/P2 |
| hl7au/au-fhir-base | 634d673 | 7600m | 198 | 123/465/233 |  |  |  |  |  |  |  |
| nricmig/Nricm_TWCMIG | 28d2f42 | 7600m | 267 | 6/2/490 |  |  |  |  |  |  |  |
| hl7ch/ch-emr | 327d611 | 7600m | 157 | 288/130/818 |  |  |  |  |  |  |  |

## Notes

- `*-ref-*list.{json,csv}` (and csvs.zip, full-ig.zip copies): row order differs between any two runs, including two runs of the same jar (I1 vs W6, F2 vs W2). Not counted as a finding.
- full-ig.zip also contains fragment-usage-analysis.csv (timing data), ignored.
- **F1, REGRESSION (intended), #48 perf/watch-fsh-tail (9614fa3), commit 349fc124** (also in the base): `spec.internals` (definitions.{json,xml,ttl}.zip, package*.tgz!package/other, validator*.pack, full-ig.zip) no longer lists the `*.ai.html` targets. Neither build has those pages in output/ (the AI step deletes them), so master lists targets that don't exist. reg-workflow-pharma: 176 targets dropped. Repro: full build with each jar, `tar xzOf output/package.tgz package/other/spec.internals | grep -c ai.html`.
- **F2, REGRESSION (order only, intended), HL7 #1461 watch-page-rebuild (e70bb8a, commit 709525b, in the base)**: `llms.txt`, `en/llms.txt` and `ai.zip` list the pages in file name order (`AIProcessor.loadPages`: `Arrays.sort(files)`); master uses `File.listFiles()` order. Repro: full build with each jar, `sed -n 5,15p output/llms.txt`. Same as found in s4, s5, s6.
- pre-existing: `en/Binary-*.md` contains a Java array identity (`[B@5fb2088b`) that differs on every run (master too).
- **P1, pre-existing (fhir-us-ndh)**: the search parameter list on the NDH profile pages (`StructureDefinition-ndh-*.html/md`) resolves some parameters to davinci-pdex-plan-net SearchParameters in some builds and to core ones in others. Seen in both directions between master runs (R1 vs the earlier POSIX-locale master run) and between int runs (F2 vs W2).
- **P2, pre-existing (fhir-us-ndh)**: the THO version in the `NamingSystem-npi` narrative link of `Organization-Social-Towson-Food` varies between builds (5.5.0 and 6.3.0 from master runs, 7.1.0 and 7.5.0 from int runs).
- **P3 (fhir-us-ndh, I2 only)**: in the second build in the same folder, narratives render the NPI identifier type as "NPI" instead of "United States National Provider Identifier" (323 files). Same pattern as the warm tx cache narrative difference other shards reported as pre-existing; master check (two builds in one folder) pending.
