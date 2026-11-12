# QA results, shard s2

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, 15 GB, 16 GB swap file. Heap 7600m unless noted. Times in seconds, wall clock of the whole java run.
- R1 times of the first IGs include filling the empty package cache.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I2 | W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| HL7/aitransparency-ig | 6d0a39f | 7600m | 119 | 0/0/0 | 63 | 46 | 1.2/26.6/22.3/36.6/36.4/27.0 (all edits shown) | 57 | 56 | REGRESSION: spec.internals targets drop 43 *.ai.html pages (PR #48 perf/watch-fsh-tail, commit 349fc124, also in base); pre-existing diffs: en.tgz package/.index.db 0 vs 16 rows (master M1 vs R1 differs too), warm tx cache narrative of Bundle-b3c1f2d4 identifier type "MedicareHIC" vs "United States Medicare Number" + qa-txservers use count (master 2nd build same folder shows it too) |
| FHIR/ig-guidance | e77c2fb | 7600m | 72 | 77/53/40 | | | | | | |
| hl7-be/medication | 6b4450d | 7600m | 108 | 4/39/93 | | | | | | |
| hl7au/au-fhir-ps | 85e161a | 7600m | 267 (invalid) | 5463/936/3771 | | | | | | R1 rerun pending: tx.fhir.org 502 during build |
| sfaassn2025-source/CRA-IG | 7dc650f | | 25 (fail) | | | | | | | skip: SUSHI 3 errors at HEAD (Claim.supportingInfo category; CodeSystem url conflict with twss base dependency) |
| sfaassn2025-source/VFS-IG | 4afd1fe | | 14 (fail) | | | | | | | skip: SUSHI 1 error at HEAD (twss-case-type CodeSystem url conflict with twss base dependency) |
| KIP-infrastructure/implementation-guide | 5b5773c | 7600m | 236 | 40/2349/3539 | | | | | | |
| uzinfocom-org/digital-health-integration | fa87466 | 7600m | 1197 | 25/363/7650 | | | | | | |

## Notes

### aitransparency
- Masked/ignored besides the plan list: java stack frame line numbers in comparison-*/ error pages (the error is the same, frames move with code), fragment-usage-analysis.csv (has a Time (ms) column), comma-separated used-by lists in *-ref-all-list.csv/json.
- REGRESSION repro: full build of HL7/aitransparency-ig 6d0a39f with qa/integration or a jar from PR #48 head 9614fa3. definitions.json.zip!spec.internals (also definitions.xml/ttl.zip, package*.tgz!package/other/spec.internals, validator*.pack) has 248 targets; master has 291. The 43 missing are the *.ai.html pages, which are not in output/ of either build (the AI step deletes them), so the change looks intended (commit "spec.internals doesn't list the ai pages the AI step removes"), but it changes published output.
- Watch: W1..W6 all showed in output. F2 vs W2, F6 vs W6 and I1 vs W6 differ only by the pre-existing warm tx cache narrative (8 Bundle-b3c1f2d4 pages + qa-txservers.html). No stale output.
