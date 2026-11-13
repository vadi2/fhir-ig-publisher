# QA round 2 results, shard s7

Status: Phase C in progress (dis done).

- master.jar: HL7 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb ("Page rebuild: simplify")
- VM: 4 CPU, 15 GB. Swap file 6 GB (16 GB plus build folders would exhaust the ~30 GB disk allowance, as in round 1). Heap 7600m for every build.
- Command: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder <shared>`, no -threads, one publisher at a time.

| IG | commit | heap | R1 s | R1 qa e/w/h | I1 | I2 | W first, W1..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| IHE/ITI.DIS | 5a37d30b52 | 7600m | 123 | 0/23/119 | 61 | 63 | int 66, 28/27/47/45/44/33/48/45/46; master 67, 30/26/46/44/45/35/47/45/49 (W1a,W1d,W1e,W1f,W2,W3,W4,W5,W6; all edits seen on both) | 65 | 65 | 65 | 0/23/119 in every build, issue sets equal | same; pre-existing diffs: F4 THO version in ImagingStudy-study-42 NamingSystem link, F3 .index.db rows, used-by list order |
| hl7germany/medication-ig-de-r4 | 06c8d75d78 | 7600m | 213 | 1673/2112/815 | | | | | | | pending |
| HL7/davinci-pdex-formulary | 549af4d87a | 7600m | 471 | 0/0/22 | | | | | | | pending |
| ahdis/ch-ekm | ed239c8251 | 7600m | 312 | 799/8363/4466 (98 broken links) | | | | | | | pending |
| HL7/phd | 565d3336a2 | 7600m | 98 | 0/0/2 | | | | | | | pending |
| TWNHIFHIR/empd | 988d8dbbaf | 7600m | 260 | 0/0/0 | | | | | | | pending |
| hl7-eu/eps | 7392dfb20f | 7600m | 241 | 0/51/15 | | | | | | | pending |

## Notes per IG

Method: one `-rapido -watch` session per jar runs every step without a restart; output, qa files and sources are saved while it waits at "Watching for changes" after W1f, W2 and W6. F1/F2/F6 are fresh full builds by int.jar of those saved sources. W times: edit to "Finished @", seconds. Compare: qa.json counts plus qa.xml issue sets (file + severity + text); every output file after masking dates/times/UUIDs/run folder, archives entry by entry, SQLite as sorted rows; same tokens in another order counts as order-only. Skipped: qa*, fragment-usage-analysis.csv.

### dis
- Template (ihe.fhir.template base) has ant script targets onLoad/onGenerate/onCheck and no script-inputs: int logs "Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))" for every edit, then the same Differential/Complete build master runs. Times match master. As intended (step 6).
- Every comparison (R1-I1, R1-I2, I1-I2, W1f-F1, W2-F2, W6-F6, W6-I1): QA counts and issue sets equal.
- Only content diffs: ImagingStudy-study-42 NamingSystem link to THO 6.5.0 vs 7.5.0 (R1 7.5.0, I1 6.5.0, I2 7.5.0, W1f 6.5.0, F1 7.5.0: varies between runs of the same jar, F4) and package .index.db ResourceList rows (F3). With the THO version masked: R1-I1 only .index.db, I1-I2 and W1f-F1 none. W2-F2, W6-F6, W6-I1: order only (used-by lists).
