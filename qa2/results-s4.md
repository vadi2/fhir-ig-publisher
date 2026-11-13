# QA round 2 results, shard s4

**Status: Phase C in progress (1/7 IGs done).**

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951 (sha256 56b2e4adb4af80ef5b495b3013b726df254243b5437eb500b2c7c6a5ae5b238f)
- VM: 4 CPU, 15 GB, 8 GB swap. Every build: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder /home/user/qa/pcf`, fresh `cp -a` of the clone, no -threads.
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb (sha256 5427eec163be3afa8c60609060cb24fd4a9f37956e97cb67dcc58f6da0f9b84a)
- Times in seconds: whole java run for R1/I1/I2/F1/F2; watch: first build, then each edit to the next "Watching for changes". IW = int.jar watch, MW = master.jar watch, same edits, same heap.
- QA = err/warn/hints from qa.txt; messages compared by file + text (run folder masked).
- F6 = I1 (fresh full build of the clean sources by int.jar), so W6 is compared to I1 and to R1. Output snapshots taken while the watch was idle after the step (not stopped).
- Masked: dates/times/timestamps, UUIDs, run folder, "used by" entries in *-ref-*list.json/csv, fragment-usage-analysis.csv, qa* reports; archives (zip/tgz/pack/xlsx) compared entry by entry, sqlite dumped sorted.

| IG | commit | heap | R1 | R1 qa | verdict |
|---|---|---|---|---|---|
| HL7Chile/MHD-CL | 360872e | 7600m | 157 | 0/28/5 (0 broken links) | pending int.jar |
| hl7-eu/readi | 1928b88 | 7600m | 86 | 165/58/39 (3 broken links) | pending int.jar |
| RIVO-Noord/zorgviewer-ig | 43e4dab | 7600m | 185 | 669/137/128 (43 broken links) | pending int.jar |
| HL7/davinci-ra | e3d2d69 | 7600m | 413 | 0/0/0 (0 broken links) | pending int.jar |
| HL7/imaging-service-request-ig | def90dc | 7600m | 129 | 195/225/244 (5 broken links) | pending int.jar |
| hl7au/au-fhir-core | 3de0d01 | 7600m | 185 | 62/220/47 (0 broken links) | pending int.jar |
| sfaassn2025-source/base | e11fe94 | 7600m | 362 | 0/3/2 (0 broken links) | pending int.jar |

## Phase C

| IG | I1 | I1 qa | I2 | I2 qa | F1 / F2 | W1f / W2 / W6 qa | IW first, W1a..W6 | MW first, W1a..W6 | verdict |
|---|---|---|---|---|---|---|---|---|---|
| HL7Chile/MHD-CL | 73 | 0/28/5 | 54 | 0/27/5 | 55 / 57 | 0/27/5 each | 58; 27 26 30 38 37 40 32 37 33 35 (no W1b) | 59; 27 27 30 36 37 36 34 36 38 35 | same (pre-existing diffs only) |

### HL7Chile/MHD-CL
- Template: local hl7chile-ig-template on hl7.fhir.template (ant/onCheck scripts, no script-inputs): every int.jar watch step logged "Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))" and then ran master's Rapido path (Differential for W1a/W1c/W3, Complete for the rest), same modes and times as master. Page-only rebuild never used here, as intended.
- Edits: W1a cambios.md, W1b n/a (no html/xml page), W1c ReferenciaDocumental-intro.md, W1d template includes/fragment-footer.html, W1e/W1f qa-nueva.md + sushi-config pages, W2 docRef.fsh Description, W3 ReferDocEjemplo masterIdentifier (also inside the MHD Bundle), W4/W5 new DocumentReference instance, W6 revert. All markers found/absent as expected.
- R1 vs I1: QA identical (33 messages); output identical except package .index.db ResourceList (F3, pre-existing).
- I1 vs I2, F1 vs W1f, F2 vs W2, I1 vs W6: output identical (0 files). QA: the reused-folder builds (I2, watch) lack "The IG is not for the international realm, and it uses SNOMED CT, so it should fix the SCT edition" (28 to 27 warnings); master does the same (master watch W6: 27, round 1 R2m: 27). Pre-existing.
- R1 vs W6: the two above only.
