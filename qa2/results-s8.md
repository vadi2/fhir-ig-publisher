# QA round 2 results, shard s8

Status: Phase C running (2 of 7 IGs done).

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951 (jar sha256 4cfad5d75546efb3...)
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb (jar sha256 166cb202a37b87b5...)
- VM: 4 CPU, ~14 GB, 16 GB swap file. Heap 7600m for every build. LC_ALL=C.UTF-8 (TWCMIG has non-ASCII file names). Times in seconds, whole java run.
- Watch edits planned per IG (scripts kept in /home/user/qa/scripts on the QA VM): W1a markdown page append; W1b n/a for all 7 (no IG has an html/xml page that renders: au-fhir-base downloads.xml is shadowed by downloads.md, encounter background.xml is not in the IG); W1c intro-notes for au-fhir-base, ndh, rwp (none for the others); W1d menu.xml (encounter, au-fhir-base, TWCMIG), sushi-config menu (IPTp, rwp), includes/markdown-link-references.md (ndh), input/data/changelog yml (ch-emr); W1e/W1f new page in ig.xml / sushi-config pages (IPTp: pagecontent only, no pages list).

| IG | commit | heap | R1 | R1 qa e/w/h/links | I1 | I2 | watch | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| hl7au/au-fhir-encounter | 71003d4 | 7600m | 116 | 0/5/165/4 | 46 | 43 | int first:47 W1a:24 W1d:22 W1e:31 W1f:26 restart:36 W2:27 restart:35 W4:32 W5:28 W6:26; master first:53 W1a:24 W1d:25 W1e:30 W1f:27 restart:37 W2:30 restart:38 W4:34 W5:35 W6:26 | 46 | 52 | 49 | QA equal in every build and step. All edits reached output. W1f=F1, W2=F2, W6=F6=I1. R1 vs I1/I2: only P5 (.index.db rows in en.tgz: R1 0, int 2). |
| daviemukungi/IPTp-FHIR-implementationIG | a343cf2 | 7600m | 75 | 5/46/102/1 | 74 | 72 | int first:72 W1a:35 W1d:51 W1e:50 W1f:51 restart:56 W2:55 restart:58 W3:43 W4:52 W5:49 W6:47; master first:72 W1a:35 W1d:50 W1e:50 W1f:49 restart:55 W2:55 restart:54 W3:43 W4:52 W5:50 W6:57 | 63 | 65 | 64 | same: QA equal in every build and step, all edits reached output, 0 differing files in R1 vs I1/I2, W1f vs F1, W2 vs F2, W6 vs F6 and I1 |
| HL7/reg-workflow-pharma | ff82d32 | 7600m | 79 | 1/0/0/0 | | | | | | | pending |
| HL7/fhir-us-ndh | f56330f | 7600m | 315 | 0/11/13/0 | | | | | | | pending |
| hl7au/au-fhir-base | 2855355 | 7600m | 196 | 122/465/233/0 | | | | | | | pending |
| nricmig/Nricm_TWCMIG | 28d2f42 | 7600m | 291 | 6/2/490/0 | | | | | | | pending |
| hl7ch/ch-emr | 2c23945 | 7600m | 164 | 288/130/818/1 | | | | | | | pending |

## Notes

- Compare: qa.json counts and the multiset of (file, severity, text) from qa.xml; every output file after masking dates, UUIDs, run folder and `[B@` ids; archives (zip, tgz, pack, xlsx) entry by entry; sqlite tables dumped as sorted rows with blobs as text; `*-ref-*list.{json,csv}` and llms.txt compared as sorted lines with sorted comma tokens ("used by" order).
- au-fhir-encounter and IPTp (fhir.base.template): the template declares no script-inputs, so every int watch rebuild is a Complete Build ("the template doesn't declare what its scripts read"); master does a Differential Build for page edits. Times are similar.
- Round 1's F1 (spec.internals ai.html targets) and F2 (llms.txt order) no longer show: both are in master 2ccca17 now.
- P5 (pre-existing in round 1, intermittent): package `.index.db` ResourceList has 0 rows in some builds. au-fhir-encounter R1 0 rows, I1/I2 2 rows. Master rerun pending.

- R1 QA counts match round 1 except au-fhir-base 122 errors (123 in round 1). au-fhir-base, rwp and ch-emr are at newer commits than in round 1.
