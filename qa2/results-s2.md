# QA round 2 results, shard s2

Status: Phase C in progress (1 of 6 IGs done).

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb ("Page rebuild: simplify", on top of master 2ccca17)
- VM: 4 CPU, 15 GB, 16 GB swap file. Heap 7600m for all IGs. Times in seconds, wall clock of the whole java run. R1 of the first IG includes filling the empty package cache.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I2 | W1a..W6 int / master (s) | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/aitransparency-ig | 6d0a39f | 7600m | 134 | 0/0/0 | 61 | 51 | W1a 25/22, W1c 45/43, W1e 42/39, W1f 37/40, W2 42/39, W3 31/32, W4 39/43, W5 41/39, W6 40/37 (all edits shown) | 56 | 55 | = I1 | same: no page rebuild here (template has scripts without script-inputs, so every watch build is Complete/Differential as on master); only known pre-existing diffs |
| FHIR/ig-guidance | a9d8a94 | 7600m | 81 | 77/53/40 | | | | | | | |
| hl7-be/medication | 6b4450d | 7600m | 114 | 4/39/93 | | | | | | | |
| hl7au/au-fhir-ps | 4d0b69d | 7600m | 348 | 1928/346/2244 | | | | | | | |
| KIP-infrastructure/implementation-guide | 5b5773c | 7600m | 262 | 40/2349/3539 | | | | | | | |
| uzinfocom-org/digital-health-integration | 4ff5446 | 7600m | 1262 | 25/363/7650 | | | | | | | |
| sfaassn2025-source/VFS-IG | f5e0831 | | | | | | | | | | skip: SUSHI errors at HEAD in round 1 (4afd1fe); not re-run |
| sfaassn2025-source/CRA-IG | 90942c7 | | | | | | | | | | skip: SUSHI errors at HEAD in round 1 (7dc650f); not re-run |

## Notes
- Building master with mvn here: Maven Central downloads through the proxy fail at random ("Could not find artifact" with an empty error, cached in *.lastUpdated). Worked with `-Daether.connector.basic.threads=1` after deleting ~/.m2/repository/**/*.lastUpdated.
- au-ps R1: first attempt failed at start (tx.fhir.org capability statement timeout, 19 s), as did one retry; the next clean run passed. A third attempt was invalid (my own cleanup deleted its work folder) and is not counted.

### aitransparency
- W1b/W1d n/a (all pages are markdown; menu comes from sushi-config). W1c creates input/pagecontent/StructureDefinition-AI-ModelCard-intro.md (no intro files exist).
- Every watch rebuild starts with "Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))" (hl7.fhir.template), then the usual Rapido decision (W1a/W3 Differential, others Complete). So int never tries a page rebuild here and behaves as master; timings equal within noise.
- R1 vs I1: only package/.index.db in the en.tgz (ResourceList 0 vs 16 rows, known F3). R1 vs I2 (same folder): also the warm tx cache narrative of Bundle-b3c1f2d4 ("MedicareHIC" vs "United States Medicare Number", known from round 1, master too) and the qa-txservers.html use count.
- F1 vs W1f: same Bundle-b3c1f2d4 narrative + qa-txservers count; F2 vs W2: qa-txservers count only; I1 vs W6 (W6 sources = original, so I1 is its fresh build): the Bundle narrative + qa-txservers. No stale output.
