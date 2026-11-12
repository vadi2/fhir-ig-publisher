# QA results, shard s1

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: 6c1abf463d8ae87d30871898c6abdaa63c7df673 (qa/integration-ready)
- VM: 4 CPU, 15 GB, 16 GB swap file. Times in seconds, wall clock of the whole java run (W*: edit to "Finished @").
- Watch sequence: watch stopped after W2 (for F2) and restarted on the same folder for W3..W6.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| ChitSR/sample-ig | 0701fa3 | 7600m | 78 | 1/6/3 |  |  |  |  |  |  |  |
| HL7/demo-collect | 0cd5dc5 | 7600m | 107 | 0/0/0 |  |  |  |  |  |  |  |
| costateixeira/spenser | 63c79e0 | 7600m | 63 | 58/20/139 |  |  |  |  |  |  |  |
| HL7/fhir-consent-management | aad3563 |  |  |  |  |  |  |  |  |  |  |
| HL7/emedicinal-product-info | 66968b7 | 7600m | 135 | 7/0/0 |  |  |  |  |  |  |  |
| hyweb-cdc/niis | b343858 |  |  |  |  |  |  |  |  |  |  |
| uzinfocom-org/digital-health-ig | 31e406f | 7600m | 748 | 5/268/9446 |  |  |  |  |  |  |  |
| HL7/US-Core | 3a65397 |  |  |  |  |  |  |  |  |  |  |

