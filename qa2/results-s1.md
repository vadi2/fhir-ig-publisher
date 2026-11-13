# QA round 2 results, shard s1

Status: Phase A done; waiting for `qa/page-only-ready` (Phase B). Master `-rapido -watch` runs (MW) for timing and classification in progress.

- master.jar: HL7 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: not yet
- VM: 4 CPU, 15 GB, swap file 8 GB (16 GB does not fit the ~30 GB disk allowance). All builds LC_ALL=C.UTF-8, IG_GATE_OFF=1, MALLOC_ARENA_MAX=2, -XX:+UseStringDeduplication, no -threads. Times in seconds (R*/I*/F*: whole java run; W*: edit to "Finished @").
- R1 outputs kept as output.tar.xz + sha256 manifest; compare masks per plan rule 4 (dates, run path, UUIDs, lastUpdated, qa files, order-only lists).

## Phase A (master R1)

| IG | commit | heap | R1 s | R1 qa e/w/h | output files |
|---|---|---|---|---|---|
| ChitSR/sample-ig | 0701fa3 | 7600m | 64 | 1/6/3 | 1252 |
| HL7/demo-collect | ca65805 | 7600m | 95 | 0/0/0 | 2315 |
| costateixeira/spenser | 21a4640 | 7600m | 65 | 58/20/139 | 4689 |
| HL7/fhir-consent-management | aad3563 | 7600m | 156 | 2/0/0 | 3161 |
| HL7/emedicinal-product-info | 7ab31d3 | 7600m | 169 | 5/0/21 | 5564 |
| hyweb-cdc/niis | b343858 | 7600m | 128 | 0/0/0 | 1340 |
| uzinfocom-org/digital-health-ig | f0ae8ac | 7600m | 794 | 5/268/9543 | 23017 |
| HL7/US-Core | d85ba3f | 10g | 697 | 7/17/1 | 17145 |

No IG in s1 is skipped (TW Core is s6; the two SUSHI failures are s2).
