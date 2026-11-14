# QA round 2 results, shard s7

Status: Phase C in progress (dis, phd done).

- master.jar: HL7 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb ("Page rebuild: simplify")
- VM: 4 CPU, 15 GB. Swap file 6 GB (16 GB plus build folders would exhaust the ~30 GB disk allowance, as in round 1). Heap 7600m for every build.
- Command: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder <shared>`, no -threads, one publisher at a time.

| IG | commit | heap | R1 s | R1 qa e/w/h | I1 | I2 | W first, W1..W6 | F1 | F2 | F6 | qa | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| IHE/ITI.DIS | 5a37d30b52 | 7600m | 123 | 0/23/119 | 61 | 63 | int 66, 28/27/47/45/44/33/48/45/46; master 67, 30/26/46/44/45/35/47/45/49 (W1a,W1d,W1e,W1f,W2,W3,W4,W5,W6; all edits seen on both) | 65 | 65 | 65 | 0/23/119 in every build, issue sets equal | same; pre-existing diffs: F4 THO version in ImagingStudy-study-42 NamingSystem link, F3 .index.db rows, used-by list order |
| hl7germany/medication-ig-de-r4 | 06c8d75d78 | 7600m | 213 | 1673/2112/815 | 149 | 146 | int 144, 90/90/89/132/135/134/110/138/138/144; master 158, 89/89/91/140/147/144/121/142/141/141 (W1a,W1c,W1d,W1e,W1f,W2,W3,W4,W5,W6; all edits seen on both) | 151 | 152 | 153 | 1673/2112/815 in every build, issue sets equal (4580) | same; pre-existing diffs: F3 .index.db rows (R1 vs int) |
| HL7/davinci-pdex-formulary | 549af4d87a | 7600m | 471 | 0/0/22 | 142 | 111 | int unseeded: every watch build fails, as master (see notes); int seeded rerun queued; master seeded 166, 46/67/66/67/54/10/52/80 (W1a,W1e,W1f,W2,W3,W4,W5,W6; W4 SUSHI error in my test instance) | 131 | 126 | 131 | 0/0/22 in R1/I1/I2, issue sets equal (22) | pending (seeded watch rerun); full builds same: stack-trace line numbers in comparison error pages, F3 .index.db rows |
| ahdis/ch-ekm | ed239c8251 | 7600m | 312 | 799/8363/4466 (98 broken links) | 289 | 229 | int 265, 188/185/207/208/214/221/209/207/210; master 259, 180/186/200/199/198/198/196/197/191 (W1a,W1d,W1e,W1f,W2,W3,W4,W5,W6; all edits seen) | 250 | 265 | 265 | 799/8363/4466, 98 broken links in every build; issue sets equal (13614) | same; pre-existing diffs: F4 NamingSystem/ch-term version links, mapping column order (varies I1 vs I2), F3 .index.db rows |
| HL7/phd | 565d3336a2 | 7600m | 98 | 0/0/2 | 104 | 89 | int 105, 41/37/36/66/64/55/48/67/68/57; master 106, 42/38/38/71/72/61/51/72/67/58 (W1a,W1c,W1d,W1e,W1f,W2,W3,W4,W5,W6; all edits seen on both) | 104 | 99 | 104 | 0/0/2 in every build, issue sets equal | same; pre-existing diffs: "Child profiles" list order of PhdBaseObservation, F3 .index.db rows, used-by list order |
| TWNHIFHIR/empd | 988d8dbbaf | 7600m | 260 | 0/0/0 | 226 | 217 | int 227, 131/127/127/211/215/218/181/36/155/212; master 238, 134/132/128/215/205/207/190/40/170/213 (W1a,W1b,W1d,W1e,W1f,W2,W3,W4,W5,W6; W1b and W4 not seen on either jar, see notes) | 224 | 216 | 220 | 0/0/0 in every build, no issues | same; pre-existing diffs: F3 .index.db rows (R1 vs int), used-by list order |
| hl7-eu/eps | 7392dfb20f | 7600m | 241 | 0/51/15 | 205 | 165 | int 187, 77/73/70/69/128/127/122/100/126/124/125; master 197, 81/89/76/71/135/126/124/103/129/127/126 (W1a,W1b,W1c,W1d,W1e,W1f,W2,W3,W4,W5,W6; all edits seen) | 178 | 179 | 186 | 0/51/15 in every build, issue sets equal (66) | same; pre-existing diffs: Java stack-trace line numbers in comparison error pages, F3 .index.db rows (R1 vs int) |

## Notes per IG

Method: one `-rapido -watch` session per jar runs every step without a restart; output, qa files and sources are saved while it waits at "Watching for changes" after W1f, W2 and W6. F1/F2/F6 are fresh full builds by int.jar of those saved sources. W times: edit to "Finished @", seconds. Compare: qa.json counts plus qa.xml issue sets (file + severity + text); every output file after masking dates/times/UUIDs/run folder, archives entry by entry, SQLite as sorted rows; same tokens in another order counts as order-only. Skipped: qa*, fragment-usage-analysis.csv.

### dis
- Template (ihe.fhir.template base) has ant script targets onLoad/onGenerate/onCheck and no script-inputs: int logs "Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))" for every edit, then the same Differential/Complete build master runs. Times match master. As intended (step 6).
- Every comparison (R1-I1, R1-I2, I1-I2, W1f-F1, W2-F2, W6-F6, W6-I1): QA counts and issue sets equal.
- Only content diffs: ImagingStudy-study-42 NamingSystem link to THO 6.5.0 vs 7.5.0 (R1 7.5.0, I1 6.5.0, I2 7.5.0, W1f 6.5.0, F1 7.5.0: varies between runs of the same jar, F4) and package .index.db ResourceList rows (F3). With the THO version masked: R1-I1 only .index.db, I1-I2 and W1f-F1 none. W2-F2, W6-F6, W6-I1: order only (used-by lists).

### phd
- Template hl7.fhir.template: ant targets, no script-inputs, so int does the same build as master for every edit (logged as above). Times match master.
- QA counts and issue sets equal in every comparison.
- Content diffs only in StructureDefinition-PhdBaseObservation.profile.{json,xml,ttl}.html (and full-ig.zip copies): the escaped narrative lists the child profiles in a different order. The order differs between every pair of builds, int vs int too (R1, I1, I2, F1, W1f, W6 all differ), as round 1 found on master: pre-existing. Plus F3 .index.db rows vs R1. I1-I2: order only.

### empd
- Template: TW Core template, no script-inputs, so int does a Complete Build for every edit, as master does. Times match master.
- QA: 0/0/0 and no issues in every build.
- Content: R1-I1 and R1-I2 differ only in .index.db ResourceList rows (F3, empty in R1). Every int-int comparison (I1-I2, W1f-F1, W2-F2, W6-F6, W6-I1) has no content diff, only order-only files.
- W1b (changes.xml) marker not in output on either jar: input/pagecontent/changes.xml is not a page in sushi-config. Test artifact.
- W4 (new QaNew.fsh patient) failed SUSHI on both jars: my test instance lacks the required age extension. Test artifact. Both jars recovered at W5. int logged "Complete Build (the last build didn't finish)" there, which is correct.

### medde
- Template: local custom-template on fhir.base.template, no script-inputs, so int does a Complete Build for every edit, as master does. Times match master.
- QA counts and all 4580 issues equal in every comparison.
- Content: R1 vs I1/I2 differ only in .index.db ResourceList rows (F3). Every int-int comparison: no content diff, order only.

### eps
- Template: local ig-template on hl7.fhir.template, no script-inputs, so int does a Complete Build for every edit.
- QA counts and all 66 issues equal in every comparison.
- Content: 8 comparison pages (comparison-v1.0.0-ballot/xx-12-*, ips-comparison-v2.0.1/xx-57/63/66/83/101/107/110-*, plus full-ig.zip copies) hold a DefinitionException stack trace ("Found a slice at 'Bundle.entry' but there was no definition for the slicing"), present in R1 on master too. Only the Publisher frame line numbers differ: the stack goes through a different call path in watch vs full and in master vs int. Masked with `MASKX='\tat [^\n]*\([A-Za-z]+\.java:\d+\)'`: R1-I1 only F3 .index.db, W6-I1 and W1f-F1 no content diff. I1-I2 no content diff even unmasked.

### chekm
- Template ch.fhir.ig.template (on fhir2.base), no script-inputs, so int does a Complete Build for every edit.
- QA counts and all 13614 issues equal in every comparison.
- Raw content diffs 498 to 674 files per pair, I1-I2 included (498): NamingSystem link versions in narratives (THO 7.0.1/7.1.0..., ch-term), the F4 pattern round 1 found on master too. Masked with `MASKX='terminology\.hl7\.org/\d+\.\d+\.\d+||fhir\.ch/ig/ch-term/\d+\.\d+\.\d+||ch-term/\d+\.\d+\.\d+'`: R1-I1 6, I1-I2 2, W1f-F1 4, W2-F2 2, W6-F6 4, W6-I1 4 files, all en/StructureDefinition-ch-ekm-{patient,immunization}-mappings.html (mapping columns in a different order; also between I1 and I2, round 1 showed it on master) and .index.db (F3, vs R1).

### formulary
- Full builds: R1, I1, I2 QA 0/0/22, issue sets equal. Raw diffs: 2 comparison-v2.1.0 error pages (stack-trace line numbers, as eps) and .index.db (F3). Masked with the eps stack mask: R1-I1 none, I1-I2 and R1-I2 only .index.db.
- int (5111080) `-rapido -watch`, unseeded: every build fails with "Publishing Content Failed: Warning: Could not find file .../output/en/index.html to copy" (multilingual IG, fresh output folder). The watch keeps running and rebuilds on each edit, but each build fails the same way, so no edit reaches output (W1a/W1e/W2/W3/W4 checks false) and W1f/W2/W6 snapshots are empty; W-F comparisons void. Round 1 found the same on master, while round-1 int (qa/integration-ready) did not fail: page-only-ready is based on HL7 master 2ccca179 and does not carry that fix. Master (2ccca17) watch unseeded fails the same way (4x the same warning, no build finished; kept as keep/formulary/MW-unseeded): pre-existing, not a regression.
- Rerun queued: int watch seeded with the R1 output (`--seed-output`), then F1/F2/F6 again and the compares.
- Master watch seeded (`--seed-output` with the R1 output): every edit seen except W4, where SUSHI rejects my test InsurancePlan (meta.lastUpdated min 1); int unseeded hit the same SUSHI error at W4. Test artifact.

## Phase D

int = intD.jar from qa/page-only-ready e6a0c6a2b71d3c562675d8af3707351428c3a28a (PR vadi2#56; the ref moved before Phase D started). master.jar as above. Same declared template copy and same edits on both jars, heap 7600m.

Declarations (qa2/script-inputs.md lists; on the template the IG names):

| IG | template in ig.ini | how declared | script-inputs list |
|---|---|---|---|
| phd | hl7.fhir.template#current | copy in /qa/tpl + ig.ini override | hl7.base |
| dis | #dis-template (local, base ihe.fhir.template) | config.json added to the local template | fhir.base |
| empd | fhir.base.template#1.0.0 | copy + override (1.0.0 ant.xml reads the same paths as current) | fhir.base |
| medde | #custom-template (local, base fhir.base.template) | config.json in the local template | fhir.base |
| eps | #ig-template (local, base hl7.fhir.template) | config.json in the local template | hl7.base |
| chekm | ch.fhir.ig.template#current (base fhir2.base, no scripts or config of its own; not in the dev table) | copy + override | fhir2.base |
| formulary | hl7.davinci.template#current | copy + override | davinci |

Steps: first build, the IG's page edits from Phase C (W1a, W1b, W1c where the IG has them), W1d, W1e (add page), W1f (remove it), W2 (non-page edit), W1a2 (the W1a page again: marker qaw1a replaced by qaw1z, checks qaw1z present and qaw1a gone). F1 = int full build of the sources after W1f, F2 after W1a2. Times in seconds, first build then each step.

| IG | int: first, per step (mode) | master: first, per step | edits seen | W1f-F1 | W1a2-F2 | verdict |
|---|---|---|---|---|---|---|
| phd | 89; W1a 14 P, W1c 15 P, W1d 32 C (menu.xml read by the scripts), W1e 58 C, W1f 58 C, W2 46 C (not a page) then D, W1a2 13 P | 82; 33/32/29/55/53/45/31 (D, D, D, C, C, D, D) | all, both jars | QA equal (0/0/2, 2 issues); content: child-profiles order only | same | same; page rebuild 13-15 s vs master 31-33 s |
| dis | 133; W1a 17 P, W1d 10 P (include fragment), W1e 40 C, W1f 33 C, W2 35 C (not a page), W1a2 10 P | 49; 22/22/35/33/38/21 (D, D, C, C, C, D) | all, both jars | QA equal (0/23/119, 142 issues); content: F4 THO version in ImagingStudy-study-42 only (none with THO masked) | same | same; page rebuild 10-17 s vs master 21-22 s; int first build 133 vs 49 is a network wait (see notes) |
| empd | 188; W1a 44 P, W1b 96 C (changes.xml is transformed) then D, W1d 47 P (menu.xml), W1e 153 C, W1f 153 C, W2 153 C (not a page), W1a2 45 P | 176; 95/92/96/149/153/151/91 (D, D, D, C, C, C, D) | all but W1b, both jars (test artifact, as Phase C) | QA equal (0/0/0); no content diff, order only | same | same; page rebuild 44-47 s vs master 91-96 s |
| medde | 128; W1a 76 C then D (page has a resource fragment), W1c 20 P, W1d 20 P, W1e 119 C, W1f 112 C, W2 113 C (not a page), W1a2 80 C then D | 125; 76/80/79/115/117/117/79 (D, D, D, C, C, C, D) | all, both jars | QA equal (1673/2112/815, 4580 issues); no content diff, order only | same | same; page rebuild 20 s vs master 79-80 s; W1a falls back safely |
| eps | 159; W1a 20 P, W1b 65 C (modelmap.xml is transformed) then D, W1c 60 C (intro .xml is transformed) then D, W1d 22 P (include), W1e 110 C, W1f 108 C, W2 107 C (not a page), W1a2 22 P | 153; 65/61/58/55/107/101/105/56 (D, D, D, D, C, C, C, D) | all, both jars | QA equal (0/51/15, 66 issues); raw 8 comparison pages + zip copies (stack-trace lines, known); masked: none | same | same; page rebuild 20-22 s vs master 55-65 s |
| chekm | 234; W1a 151 C (examples.md has resource content, then tx failures), W1d 32 P (include logo.html), W1e 170 C, W1f 169 C, W2 182 C (not a page), W1a2 152 C | 239; 156/153/166/168/164/146 (all C: tx calls failed in the last build) | all, both jars | QA equal (799/8363/4466/98, 13614 issues); raw 670 files (F4); masked: only ch-ekm patient mappings page (mapping column order, known) | raw 500 (F4); masked: patient + immunization mappings pages only | same; page rebuild 32 s vs master 153 s |
| formulary | seeded (unseeded first build fails, as Phase C): 99; W1a 14 P, W1e 71 C, W1f 66 C, W2 63 C (not a page), W1a2 13 P | seeded: 96; 49/68/68/63/42 (D, C, C, C, D) | all, both jars | QA equal (0/0/22, 22 issues); raw 2 comparison pages + zip copies (stack-trace lines, known); masked: none | QA equal; masked: package.db only (one THO vs R4 link, varies between full builds too, see notes) | same; page rebuild 13-14 s vs master 42-49 s |

Modes: P = "Page Rebuild (1 page changed)", C = Complete Build (reason), D = Differential Build.

### Phase D notes
- phd: template loaded from the copy ("Load Template hl7.fhir.template#current from local folder /home/user/qa/tpl/hl7.fhir.template (override in ig.ini)"). Page rebuild: Jekyll into temp/pages/.watch-output, link check over all 2849 pages, QA, then "Updated output: 11 written, 0 deleted, 2854 unchanged". W1a2 (page edit right after W2's differential build) is a page rebuild with the old marker gone. W1d edits input/includes/menu.xml, which is in the declared list: int logs "Complete Build (.../input/includes/menu.xml is read by the template's scripts)" and the menu edit shows in output: this is the plan's step 6 (script-inputs edit gives a full build). The only content diffs vs F1/F2 are the "Child profiles" list order of PhdBaseObservation in its .profile.{json,xml,ttl}.html narratives, which varies between every pair of builds (Phase C).
- dis: local #dis-template with config.json, no override needed. W1d edits input/includes/fragment-header.html (not declared: none of the fhir.base scripts read includes) and int logs "Page Rebuild (1 page changed)": Jekyll renders every page anyway, and the marker is in all 699 pages that carry the header, the same count as the full build F1. W1f-F1 and W1a2-F2: no content diff once the THO version in NamingSystem links is masked (`MASKX='terminology\.hl7\.org/\d+\.\d+\.\d+'`, F4, as Phase C). First build 133 s on int vs 49 s on master: 42 s of it in "Sending Usage Stats to Server" (network wait) and a slower generate step (tx), not the jar; Phase C first builds were 66/67.
- empd: template from the copy ("Load Template fhir.base.template#1.0.0 from local folder /home/user/qa/tpl/fhir.base.template (override in ig.ini)"). W1b edits changes.xml, which is not a page: Complete Build ("is transformed") then Differential; the marker is not visible on either jar, the same as Phase C (test artifact). W1d edits input/includes/menu.xml (not in fhir.base): Page Rebuild, and 1052 of 1053 html files changed vs W1a, so the menu reached the pages. Both comparisons have no content diff.
- medde: local #custom-template with config.json ("Load Template from #custom-template"). W1a edits schema-freitext.md, which has `{% fragment MedicationRequest/... %}`, so int logs "Complete Build (... had content that depends on the resources)" and then runs a Differential Build, the same 111 of 830 files and the same time as master. This is a correct fallback, not a page rebuild. W1c/W1d are plain pages: Page Rebuild 20 s. Both comparisons have no content diff; all 4580 issues are equal.
- eps: local #ig-template with config.json. W1b (pagecontent/modelmap.xml) and W1c (pagecontent/StructureDefinition-...-intro.xml) are .xml page sources that the publisher transforms, so int logs "Complete Build (... is transformed)" and then runs the same Differential Build as master (46 of 110 files), at the same time. This is a safe fallback, not a page rebuild. W1a/W1a2 (.md) and W1d (input/includes/composition-eu-eps-intro.xml) are Page Rebuilds. The raw diffs are the 8 known comparison pages (+ full-ig.zip copies) with Publisher stack-trace line numbers; with the Phase C stack mask, both comparisons show 0 content diffs.
- chekm: template from the copy ("Load Template ch.fhir.ig.template#current from local folder /home/user/qa/tpl/ch.fhir.ig.template (override in ig.ini)"). In Phase C, W1d without the declaration logged "Complete Build (the template doesn't declare what its scripts read ...)" at 185 s; with the declaration it is a Page Rebuild at 32 s. W1a/W1a2 edit examples.md, which depends on the resources (Complete Build). After that, every build after a full one is complete on both jars because "terminology server calls failed in the last build (463)" (tx.fhir.org calls fail from this container; master does the same). With the round-1 F4 mask (THO, ch-term versions) the only content diffs are the en/StructureDefinition-ch-ekm-{patient,immunization}-mappings.html column order, which also varies I1 vs I2 (Phase C).
- formulary: template from the copy ("Load Template hl7.davinci.template#current from local folder /home/user/qa/tpl/hl7.davinci.template (override in ig.ini)"). The unseeded first build failed on both jars (output/en/index.html, as in Phase C) and was kept as DW-unseeded / DMW-unseeded; both reran seeded with the R1 output. Only W1a/W1e/W1f/W2/W1a2 apply (there is no W1b/W1c/W1d target). W1a2-F2: package.db ResourceList row 49 (ValueSet insuranceplan-type) links to hl7.org/fhir/R4/valueset-insuranceplan-type.html in the watch build and to terminology.hl7.org/7.5.0/... in F2. This row is not stable in full builds either: R1, I1, I2 and F1 have the R4 link, while F2, D-F1, D-F2 and master watch W1f/W1a2 have the THO link. It is the same canonical in core and THO, so this is the F4/F3 family (link source chosen nondeterministically), not watch-related.

