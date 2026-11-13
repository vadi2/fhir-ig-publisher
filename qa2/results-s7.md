# QA round 2 results, shard s7

Status: Phase A (master reference builds) in progress.

- master.jar: HL7 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: waiting for qa/page-only-ready
- VM: 4 CPU, 15 GB. Swap file 6 GB (16 GB plus build folders would exhaust the ~30 GB disk allowance, as in round 1). Heap 7600m for every build.
- Command: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder <shared>`, no -threads, one publisher at a time.

| IG | commit | heap | R1 s | R1 qa e/w/h | I1 | I2 | W first, W1..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
