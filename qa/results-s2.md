# QA results, shard s2

Status: in progress (Phase A: reference builds with master.jar)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: (waiting for qa/integration-ready)
- VM: 4 CPU, 15 GB, 16 GB swap file. Heap 7600m unless noted. Times in seconds, wall clock of the whole java run.
- R1 times of the first IGs include filling the empty package cache.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I2 | W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| HL7/aitransparency-ig | 6d0a39f | 7600m | 119 | 0/0/0 | | | | | | |
| FHIR/ig-guidance | e77c2fb | 7600m | 72 | 77/53/40 | | | | | | |
| hl7-be/medication | 6b4450d | 7600m | 108 | 4/39/93 | | | | | | |
