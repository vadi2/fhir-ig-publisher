# QA results, shard s5

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, 15 GB, swap file 10 GB (shrunk from 16 GB to free disk, swap stayed unused). Times in seconds, wall clock of the whole java run (W*: edit to "Finished @"; W0 = first watch build).
- Watch sequence: one watch session runs W1..W6 without a restart; its output is snapshotted after each step. F2 and F6 are separate fresh full builds run afterwards with the W2 and W6 edits applied.
- Compare: file set + content after masking dates/times, run path, UUIDs; qa counts + message set from qa.xml.
- eu-lab: SUSHI couldn't fetch hl7.fhir.eu.extensions.r4#current, so it was seeded from build.fhir.org (1.4.0-ci, 2026-10-29) before R1; same package for every build.
- tx-eco and cds-hooks have no example referenced from another resource, so they have no W3. tx-eco has no FSH; its W2/W4 edit a FeatureDefinition, rendered as Binary-FeatureDefinition-*.json.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | W0, W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/fhir-tx-ecosystem-ig | 29546b35 | 7600m | 92 | 0/6/15 | 40 | 0/6/15 | 36 | 37, 2.4/26/-/33/33/24 | 42 | 40 | same |
| HL7/cds-hooks-library | 8afbe0fe | 7600m | 91 | 7/0/0 | 43 | 7/0/0 | 34 | 39, 1.0/21/-/27/25/20 | 43 | 39 | same |
| HL7/uv-pocd | 8e377d42 | 7600m | 77 | 0/0/4 | 74 | 0/0/4 | 62 | 78, 1.2/28/24/63/58/29 | 71 | 71 | same |
| hl7-eu/laboratory | 4571ab84 | 7600m | 168 | 5/11/62 |  |  |  |  |  |  |  |
| HL7/fhir-bfdr | 1f981246 | 7600m | 406 | 291/365/791 |  |  |  |  |  |  |  |
| HL7/davinci-pdex-plan-net | 8d9037d0 | 9g | 275 | 11/865/8 |  |  |  |  |  |  |  |
| aehrc/smart-forms-ig | 95651ff7 | 9g | 309 | 146/294/119 |  |  |  |  |  |  |  |
| nw-gmsa/nw-gmsa.github.com | 7ec7380b | 9g |  |  |  |  |  |  |  |  |  |

## Notes per IG

### tx-eco
- I1, I2, F6 vs R1: same qa counts and messages (21). 31 files differ: build timestamps inside binaries (validator.pack, package.tgz, .index.db, package.db, xlsx entry times), timings in fragment-usage-analysis.csv, and llms.txt / ai.zip page list order.
- llms.txt order: int lists the pages in name order in every build; master in file system order. Intended: AIProcessor sorts the files ("in name order: the lists of pages and the zip don't depend on the order the file system lists them in"), from the ws2-jekyll merge inside HL7 #1461 watch-page-rebuild. Not a regression.
- W2 vs F2, W6 vs F6, W6 vs I1: same qa and messages; 22 files differ, all build timestamps and timings as above.
- Watch: W1 page rebuild in 2.4 s, the edit is in en/ecosystem.html (multilingual IG; root ecosystem.html is the language redirect). W2 differential build, W4/W5 complete builds, new file appears and is gone again; W6 removes both edits.

### cds-hooks
- I1, I2, F6 vs R1: same qa counts and messages (7). 55 files differ: build timestamps inside binaries (xlsx, packages, spec.internals, packs, package.db), fragment-usage-analysis.csv timings, and llms.txt / en/llms.txt / ai.zip page order (same entries, name order as in tx-eco).
- W2 vs F2, W6 vs F6, W6 vs I1: same qa and messages; 46 files differ, timestamps and timings only.
- Watch: W1 page rebuild 1.0 s (edit in en/patient-view.html), W2 differential build (28 of 35 files), W4/W5 complete builds add and remove Basic-QaNewW4; W6 removes both edits.

### pocd
- I1, I2, F6 vs R1: same qa counts and messages (4). 143 files differ: timestamps inside binaries (xlsx, packages, packs, spec.internals), fragment-usage-analysis.csv timings, llms.txt page order, and the "used by" order in valueset-ref-list / valueset-ref-all-list (csv, json) and codesystem-ref-all-list.json (same entries, checked with an order-insensitive compare).
- W2 vs F2, W6 vs F6, W6 vs I1: same qa and messages; the same classes of diffs (used-by order also varies between two int builds).
- Watch: W1 page rebuild 1.2 s (en/guidance.html), W2 differential (47 of 103 files), W3 differential (70 of 103), W4/W5 complete builds add and remove Patient-QaNewW4; W6 removes all edits.
