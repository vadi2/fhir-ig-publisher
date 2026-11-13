# QA round 2 results, shard s8

Status: Phase A done (R1 with master.jar for all 7 IGs); waiting for qa/page-only-ready (Phase B).

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951 (jar sha256 4cfad5d75546efb3...)
- int.jar: not built yet
- VM: 4 CPU, ~14 GB, 16 GB swap file. Heap 7600m for every build. LC_ALL=C.UTF-8 (TWCMIG has non-ASCII file names). Times in seconds, whole java run.
- Watch edits planned per IG (scripts kept in /home/user/qa/scripts on the QA VM): W1a markdown page append; W1b n/a for all 7 (no IG has an html/xml page that renders: au-fhir-base downloads.xml is shadowed by downloads.md, encounter background.xml is not in the IG); W1c intro-notes for au-fhir-base, ndh, rwp (none for the others); W1d menu.xml (encounter, au-fhir-base, TWCMIG), sushi-config menu (IPTp, rwp), includes/markdown-link-references.md (ndh), input/data/changelog yml (ch-emr); W1e/W1f new page in ig.xml / sushi-config pages (IPTp: pagecontent only, no pages list).

| IG | commit | heap | R1 | R1 qa e/w/h/links | I1 | I2 | watch | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| hl7au/au-fhir-encounter | 71003d4 | 7600m | 116 | 0/5/165/4 | | | | | | | pending |
| daviemukungi/IPTp-FHIR-implementationIG | a343cf2 | 7600m | 75 | 5/46/102/1 | | | | | | | pending |
| HL7/reg-workflow-pharma | ff82d32 | 7600m | 79 | 1/0/0/0 | | | | | | | pending |
| HL7/fhir-us-ndh | f56330f | 7600m | 315 | 0/11/13/0 | | | | | | | pending |
| hl7au/au-fhir-base | 2855355 | 7600m | 196 | 122/465/233/0 | | | | | | | pending |
| nricmig/Nricm_TWCMIG | 28d2f42 | 7600m | 291 | 6/2/490/0 | | | | | | | pending |
| hl7ch/ch-emr | 2c23945 | 7600m | 164 | 288/130/818/1 | | | | | | | pending |

## Notes

- R1 QA counts match round 1 except au-fhir-base 122 errors (123 in round 1). au-fhir-base, rwp and ch-emr are at newer commits than in round 1.
