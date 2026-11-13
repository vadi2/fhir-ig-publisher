# QA round 2 results, shard s7

Status: Phase A done (all 7 R1 builds ok). Waiting for qa/page-only-ready (Phase B).

- master.jar: HL7 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: waiting for qa/page-only-ready
- VM: 4 CPU, 15 GB. Swap file 6 GB (16 GB plus build folders would exhaust the ~30 GB disk allowance, as in round 1). Heap 7600m for every build.
- Command: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder <shared>`, no -threads, one publisher at a time.

| IG | commit | heap | R1 s | R1 qa e/w/h | I1 | I2 | W first, W1..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| IHE/ITI.DIS | 5a37d30b52 | 7600m | 123 | 0/23/119 | | | | | | | pending |
| hl7germany/medication-ig-de-r4 | 06c8d75d78 | 7600m | 213 | 1673/2112/815 | | | | | | | pending |
| HL7/davinci-pdex-formulary | 549af4d87a | 7600m | 471 | 0/0/22 | | | | | | | pending |
| ahdis/ch-ekm | ed239c8251 | 7600m | 312 | 799/8363/4466 (98 broken links) | | | | | | | pending |
| HL7/phd | 565d3336a2 | 7600m | 98 | 0/0/2 | | | | | | | pending |
| TWNHIFHIR/empd | 988d8dbbaf | 7600m | 260 | 0/0/0 | | | | | | | pending |
| hl7-eu/eps | 7392dfb20f | 7600m | 241 | 0/51/15 | | | | | | | pending |
