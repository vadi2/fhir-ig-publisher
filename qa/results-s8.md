# QA results, shard s8

Status: in progress (Phase A done; Phase C starting)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, ~14 GB, 16 GB swap file. All builds with LC_ALL=C.UTF-8 (TWCMIG has non-ASCII image names; the POSIX locale makes master fail on them). Times in seconds, whole java run (W*: edit to "Finished @").
- Watch sequence: watch stopped after W2 (for F2) and restarted on the same folder for W3..W6.
- tx notes: au-fhir-base and ch-emr R1 errors are mostly tx.fhir.org not holding the AU/CH SNOMED editions; same for every build here, so they still compare.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| hl7au/au-fhir-encounter | 71003d4 | 7600m | 49 | 0/5/165 |  |  |  |  |  |  |  |
| daviemukungi/IPTp-FHIR-implementationIG | a343cf2 | 7600m | 73 | 5/46/102 |  |  |  |  |  |  |  |
| HL7/reg-workflow-pharma | 56924f9 | 7600m | 66 | 1/0/0 |  |  |  |  |  |  |  |
| HL7/fhir-us-ndh | f56330f | 7600m | 137 | 0/11/13 |  |  |  |  |  |  |  |
| hl7au/au-fhir-base | 634d673 | 7600m | 198 | 123/465/233 |  |  |  |  |  |  |  |
| nricmig/Nricm_TWCMIG | 28d2f42 | 7600m | 267 | 6/2/490 |  |  |  |  |  |  |  |
| hl7ch/ch-emr | 327d611 | 7600m | 157 | 288/130/818 |  |  |  |  |  |  |  |
