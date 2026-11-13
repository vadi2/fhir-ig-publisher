# QA round 2 results, shard s4

**Status: Phase C 6/7 IGs done; base rerunning its watch sequence after a container restart at ~21:10 (I1/I2 done). Phase D queued after it.**

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951 (sha256 56b2e4adb4af80ef5b495b3013b726df254243b5437eb500b2c7c6a5ae5b238f)
- VM: 4 CPU, 15 GB, 8 GB swap. Every build: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder /home/user/qa/pcf`, fresh `cp -a` of the clone, no -threads.
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb (sha256 5427eec163be3afa8c60609060cb24fd4a9f37956e97cb67dcc58f6da0f9b84a)
- Times in seconds: whole java run for R1/I1/I2/F1/F2; watch: first build, then each edit to the next "Watching for changes". IW = int.jar watch, MW = master.jar watch, same edits, same heap.
- QA = err/warn/hints from qa.txt; messages compared by file + text (run folder masked).
- F6 = I1 (fresh full build of the clean sources by int.jar), so W6 is compared to I1 and to R1. Output snapshots taken while the watch was idle after the step (not stopped).
- THO-normalised (where stated): `terminology.hl7.org/<version>/NamingSystem-*` links and their display text (DICOM Unique Id/DUI, NPI/United States National Provider Identifier, URI/Uniform Resource Identifier (URI)), the known pre-existing F4; raw counts are given too.
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
| hl7-eu/readi | 58 | 165/58/39 | 55 | 165/58/39 | 57 / 57 | 165/58/39 each | 55; 24 32 37 35 36 34 41 40 40 (no W1b, W1c) | 60; 24 35 39 40 40 43 44 40 42 | same (pre-existing diffs only) |
| HL7/imaging-service-request-ig | 78 | 195/225/244 | 74 | 195/225/244 | 78 / 78 | 195/225/244 each | 80; 37 65 64 60 60 44 61 60 58 (no W1b, W1d) | 86; 40 70 66 64 64 46 66 67 65 | same (pre-existing diffs only) |
| RIVO-Noord/zorgviewer-ig | 118 | 669/137/128 | 98 | 669/137/128 | 112 / 109 | 669/137/128 each | 118; 80 80 73 74 72 70 68 72 68 68 (no W1b) | 121; 78 71 71 70 69 72 69 (W4-W6 not run) | same (pre-existing diffs only) |
| HL7/davinci-ra | 91 | 0/0/0 | 83 | 0/0/0 | 98 / 97 | n/a (watch build fails) | 86; 57 57 58 64 62 63 63 64 65 63 (every build failed) | 87; 62 56 60 65 64 64 68 (every build failed) | same for full builds; watch not comparable (pre-existing failure, both jars) |
| hl7au/au-fhir-core | 153 | 62/220/47 | 110 | 62/220/47 | 128 / 128 | 62/220/47 each | 134; 101 98 97 98 102 101 103 107 104 104 (no W1b) | 140; 99 104 105 103 104 104 112 107 110 106 | same (pre-existing diffs only) |

### HL7Chile/MHD-CL
- Template: local hl7chile-ig-template on hl7.fhir.template (ant/onCheck scripts, no script-inputs): every int.jar watch step logged "Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))" and then ran master's Rapido path (Differential for W1a/W1c/W3, Complete for the rest), same modes and times as master. Page-only rebuild never used here, as intended.
- Edits: W1a cambios.md, W1b n/a (no html/xml page), W1c ReferenciaDocumental-intro.md, W1d template includes/fragment-footer.html, W1e/W1f qa-nueva.md + sushi-config pages, W2 docRef.fsh Description, W3 ReferDocEjemplo masterIdentifier (also inside the MHD Bundle), W4/W5 new DocumentReference instance, W6 revert. All markers found/absent as expected.
- R1 vs I1: QA identical (33 messages); output identical except package .index.db ResourceList (F3, pre-existing).
- I1 vs I2, F1 vs W1f, F2 vs W2, I1 vs W6: output identical (0 files). QA: the reused-folder builds (I2, watch) lack "The IG is not for the international realm, and it uses SNOMED CT, so it should fix the SCT edition" (28 to 27 warnings); master does the same (master watch W6: 27, round 1 R2m: 27). Pre-existing.
- R1 vs W6: the two above only.

### hl7-eu/readi
- Template: local ig-template on hl7.fhir.template, no script-inputs: every int.jar watch step fell back to master's Rapido path ("the template doesn't declare what its scripts read"), same modes and times as master. Pages are in output/en/ (root html are redirects).
- Edits: W1a knownIssues.md, W1b/W1c n/a (no html/xml page, no intro/notes file), W1d template includes/fragment-footer.html, W1e/W1f qa-new.md + sushi-config pages, W2 location-readi.fsh Description, W3 study title in Bundle-readi-sample-1-r4.json, W4/W5 copy of Demo_NCT01931527.json with a new id, W6 revert. All markers found/absent as expected (int.jar run; the master run's checks looked only at the root redirect files).
- QA identical everywhere (165/58/39, 265 messages, also for I2 and the watch: no SCT-edition-style drop here).
- R1 vs I1, R1 vs I2, R1 vs W6: only package .index.db ResourceList (F3, pre-existing). I1 vs I2, F2 vs W2, I1 vs W6: 0 files.
- F1 vs W1f: 36 files (Bundle-readi-sample-1-r4 json/xml/ttl/html/md, examples.*.zip, ai.zip, packages, package.db) differ only in the narrative link `terminology.hl7.org/7.1.0` (F1) vs `7.5.0` (W1f) for NamingSystem-uri: F4, pre-existing (round 1 showed master flipping between these for this bundle). 0 files with that version masked.

### HL7/imaging-service-request-ig
- Template hl7.fhir.template#current (no script-inputs): every int.jar watch step fell back to master's Rapido path, same modes and times as master.
- Edits: W1a architecture.md, W1b n/a, W1c new pagecontent/StructureDefinition-imaging-practitioner-intro.md (Complete Build "files added or removed", as master), W1d n/a (no includes/menu file; menu in sushi-config), W1e/W1f qa-new.md + sushi-config pages, W2 Practitioner profile Description, W3 practitioner family name (also in the create-order Bundle), W4/W5 new Practitioner instance (.fsh), W6 revert. All markers found/absent as expected.
- QA identical everywhere (195/225/244, 669 messages).
- Raw: R1 vs I1 2 files (F3 .index.db), R1 vs I2 76, I1 vs I2 74, F1 vs W1f 70, F2 vs W2 4, I1 vs W6 0, R1 vs W6 2 (F3).
- The 74/70 files are Bundle-example-order-bundle and ImagingStudy-example-requested-procedure-imaging-study (json/ttl/xml, en/*.html, .md, ai.zip, examples.*.zip, packages, package.db): NamingSystem-dui narrative link `terminology.hl7.org/7.5.0` vs `5.0.0` and display `DICOM Unique Id` vs `DUI` (F4, pre-existing, same as round 1). The 4 in F2 vs W2 are codesystem-ref-all-list.csv "used by" order with a quoted field (masked once quotes are ignored).
- THO-normalised: R1 vs I2 2 (F3), I1 vs I2 0, F1 vs W1f 0, F2 vs W2 0.

### RIVO-Noord/zorgviewer-ig
- Template: local rivonoord-template on fhir.base.template (no script-inputs): int.jar fell back every step, then master's Rapido gave a Complete Build every step ("terminology server calls failed in the last build (5)", STU3 IG, as round 1), master the same. Times equal to master.
- Edits: W1a copyrights.md, W1b n/a, W1c StructureDefinition-Appointment-notes.md, W1d input/includes/menu.xml, W1e/W1f qa-new.md + page entry in input/zorgviewer-ig.json, W2 Appointment profile description (json), W3 Patient-Nedap name (referenced by the Nedap Bundles), W4/W5 copy of Patient-Nedap with a new id, W6 revert. All markers found/absent as expected.
- Output: 0 differing files in every pair (R1/I1, R1/I2, I1/I2, F1/W1f, F2/W2, I1/W6, R1/W6), with the csv/json "used by" masks.
- QA counts identical (669/137/128, 977 messages). In the watch builds 11 html-check messages name `temp/pages/.watch-output/<page>.html` instead of `output/<page>.html` (same text otherwise): F2, pre-existing (round 1: master watch the same).
- The master watch run stopped at W4 because of an edit-script error on my side (fixed), so master W4-W6 times are missing.

### HL7/davinci-ra
- **pre-existing (both jars): `-rapido -watch` fails on every build, including the first.** The template's onCheck (hl7.davinci.template, `scripts/ant-davinci.xml:45`, target onCheck.davinci.findHomeLinks) copies `${dir.output}/index.html` = `output/en/index.html`, but under -watch Jekyll writes to `temp/pages/.watch-output`, so the file isn't there: "Publishing Content Failed: Warning: Could not find file .../output/en/index.html to copy", and "The build failed, so the pages in output are still those of the last build that succeeded" (none). master.jar (MW) and int.jar (IW) give the same error at the same line for every step; output/ stays empty, so W1f/W2/W6 can't be compared and the marker checks fail on both jars. int.jar logs the fallback ("doesn't declare what its scripts read") then "Complete Build (after failure)", as master. Repro: HL7/davinci-ra@e3d2d69, `-rapido -watch` with master 2ccca179. (Round 1 had watch output for this IG, so this came with a newer master or template.)
- Edits (applied, but no output): W1a credits.md, W1c ra-patient-group-intro.md, W1d includes/link-list.md, W1e/W1f qa-new.md + sushi-config pages, W2 ra-patient-group description (xml), W3 Patient-ra-patient01 family, W4/W5 Patient copy, W6 revert.
- Full builds: QA identical (0/0/0). Raw: R1 vs I1 904 files, R1 vs I2 98, I1 vs I2 898: all NamingSystem narrative links/display (THO 5.0.0/7.2.0/7.5.0, NPI vs United States National Provider Identifier, URI vs Uniform Resource Identifier (URI)): F4, pre-existing, as round 1. THO-normalised: I1 vs I2 0; R1 vs I1 and R1 vs I2 6 = .index.db (F3) + comparison-v2.0.0/xx-79-... and comparison-v2.1.0/xx-128-... (full-ig.zip copies too), which show a Java stack trace (master too) whose line numbers differ with the code. Noise.

### hl7au/au-fhir-core
- Template hl7.au.sparked.template (no script-inputs): int.jar fell back every step, then master's Rapido gave Complete Builds ("terminology server calls failed in the last build (64)", AU terminology not resolvable via tx.fhir.org, as round 1), master the same; times equal.
- Edits: W1a support.md, W1b n/a (background.xml isn't in the IG), W1c StructureDefinition-au-core-patient-notes.md, W1d includes/nonnormative-example-boilerplate.md (examples.md includes it), W1e/W1f qa-new.md + page in input/ig.xml, W2 au-core-patient description (xml), W3 Patient wang-li family, W4/W5 Patient copy, W6 revert. All markers found/absent as expected.
- QA counts identical (62/220/47, 329 messages; environmental, CI 2/1/3). Watch builds: the duplicate-anchor warning of StructureDefinition-au-core-bloodpressure-definitions.html names temp/pages/.watch-output/en/ instead of output/en/ (F2, pre-existing, as round 1).
- Raw: R1 vs I1 / I2 84 files, F1 vs W1f, F2 vs W2, I1 vs W6 82, I1 vs I2 0, R1 vs W6 6. The 82 are Composition-aups-minimal and DocumentReference-aups-basicsummary-data (and their html/zip/package copies): NamingSystem-uri link THO version (F4, pre-existing).
- THO-normalised: R1 vs I1 6 = .index.db (F3) + comparison-v2.0.0/xx-3-Identifier-Identifier.html and xx-5-Address-Address.html (+ full-ig.zip copies), which show a Java stack trace (master too): line numbers differ with the code, and a watch build's frames differ from a full build's (Publisher.execute line 233 vs 243). Watch vs fresh build: only those 4 stack-trace files.
