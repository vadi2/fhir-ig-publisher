# QA round 2 results, shard s2

Status: Phase A (master reference builds) in progress; int.jar not yet available (qa/page-only-ready not pushed).

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: (pending)
- VM: 4 CPU, 15 GB, 16 GB swap file. Heap 7600m for all IGs. Times in seconds, wall clock of the whole java run. R1 of the first IG includes filling the empty package cache.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I2 | W1a..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/aitransparency-ig | 6d0a39f | 7600m | 134 | 0/0/0 | | | | | | | |
| FHIR/ig-guidance | a9d8a94 | 7600m | 81 | 77/53/40 | | | | | | | |
| hl7-be/medication | 6b4450d | 7600m | 114 | 4/39/93 | | | | | | | |
| hl7au/au-fhir-ps | 4d0b69d | 7600m | | | | | | | | | |
| KIP-infrastructure/implementation-guide | 5b5773c | 7600m | | | | | | | | | |
| uzinfocom-org/digital-health-integration | 4ff5446 | 7600m | | | | | | | | | |
| sfaassn2025-source/VFS-IG | f5e0831 | | | | | | | | | | skip: SUSHI errors at HEAD in round 1 (4afd1fe); not re-run |
| sfaassn2025-source/CRA-IG | 90942c7 | | | | | | | | | | skip: SUSHI errors at HEAD in round 1 (7dc650f); not re-run |

## Notes
- Building master with mvn here: Maven Central downloads through the proxy fail at random ("Could not find artifact" with an empty error, cached in *.lastUpdated). Worked with `-Daether.connector.basic.threads=1` after deleting ~/.m2/repository/**/*.lastUpdated.
