# QA results, shard s8

Status: in progress (Phase C: 2 of 7 IGs run)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, ~14 GB, 16 GB swap file. All builds with LC_ALL=C.UTF-8 (TWCMIG has non-ASCII image names; under the POSIX locale master fails on them). Times in seconds, whole java run; W*: edit to "Finished @". A "!" after a W time means a check failed.
- Watch sequence: watch stopped after W2 (for F2) and restarted on the same folder (restart) for W3..W6. au-fhir-encounter has no examples, so no W3.
- Compare: QA counts and the multiset of (file, severity, text) from qa.xml; output by masked per-file hashes (archives per entry). "ORDER" = same lines in another order.
- tx notes: au-fhir-base and ch-emr R1 errors are mostly tx.fhir.org not holding the AU/CH SNOMED editions; same for every build here, so they still compare.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | watch | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| hl7au/au-fhir-encounter | 71003d4 | 7600m | 49 | 0/5/165 | 46 | 0/5/165 | 60 | first:46 W1:2 W2:25 restart:41 W4:33 W5:29 W6:26 | 54 | 57 |  |
| daviemukungi/IPTp-FHIR-implementationIG | a343cf2 | 7600m | 73 | 5/46/102 | 72 | 5/46/102 | 70 | first:69 W1:8 W2:40 restart:56 W3:43 W4:51 W5:52 W6:39 | 67 | 66 | same (QA messages equal; only *-ref-*list.{json,csv} row order, which varies between runs of the same jar, incl. W vs F and I1 vs W6) |
| HL7/reg-workflow-pharma | 56924f9 | 7600m | 66 | 1/0/0 |  |  |  |  |  |  |  |
| HL7/fhir-us-ndh | f56330f | 7600m | 137 | 0/11/13 |  |  |  |  |  |  |  |
| hl7au/au-fhir-base | 634d673 | 7600m | 198 | 123/465/233 |  |  |  |  |  |  |  |
| nricmig/Nricm_TWCMIG | 28d2f42 | 7600m | 267 | 6/2/490 |  |  |  |  |  |  |  |
| hl7ch/ch-emr | 327d611 | 7600m | 157 | 288/130/818 |  |  |  |  |  |  |  |

## Notes

- `*-ref-*list.{json,csv}` (and csvs.zip, full-ig.zip copies): row order differs between any two runs, including two runs of the same jar (I1 vs W6, F2 vs W2). Not counted as a finding.
- full-ig.zip also contains fragment-usage-analysis.csv (timing data), ignored.
