# QA results, shard s2

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, 15 GB, 16 GB swap file. Heap 7600m unless noted. Times in seconds, wall clock of the whole java run.
- R1 times of the first IGs include filling the empty package cache.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I2 | W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| HL7/aitransparency-ig | 6d0a39f | 7600m | 119 | 0/0/0 | | | | | | |
| FHIR/ig-guidance | e77c2fb | 7600m | 72 | 77/53/40 | | | | | | |
| hl7-be/medication | 6b4450d | 7600m | 108 | 4/39/93 | | | | | | |
| hl7au/au-fhir-ps | 85e161a | 7600m | 267 (invalid) | 5463/936/3771 | | | | | | R1 rerun pending: tx.fhir.org 502 during build |
| sfaassn2025-source/CRA-IG | 7dc650f | | 25 (fail) | | | | | | | skip: SUSHI 3 errors at HEAD (Claim.supportingInfo category; CodeSystem url conflict with twss base dependency) |
| sfaassn2025-source/VFS-IG | 4afd1fe | | 14 (fail) | | | | | | | skip: SUSHI 1 error at HEAD (twss-case-type CodeSystem url conflict with twss base dependency) |
| KIP-infrastructure/implementation-guide | 5b5773c | 7600m | 236 | 40/2349/3539 | | | | | | |
| uzinfocom-org/digital-health-integration | fa87466 | 7600m | 1197 | 25/363/7650 | | | | | | |
