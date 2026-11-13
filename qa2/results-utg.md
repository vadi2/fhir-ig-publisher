# QA round 2 results, shard utg

Status: Phase C and D done; a master watch timing run (W1b, W1d) is still running (23:50).

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: vadi2 qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb
- UTG: HL7/UTG master d8b0804a9f3dda4fc208eead147bae6d9b80b69b (same commit as round 1)
- VM: 4 CPU, 15 GB, 9 GB swap file (disk allowance about 30 GB; a UTG work folder is 8.6 GB). Heap 10g for every build (as round 1; peak "Max Memory Used = 9Gb"). Times in seconds, wall clock of the whole java run. R1 ran on an empty package cache.
- R2 = a second master full build on a warm cache. Round 1's R1 differed from all later builds (cold cache), so R2 is the reference for int builds; this time R1 and R2 match apart from known items.
- Compare: qa.json counts and qa.txt message set (run folder masked); every output file after masking dates, times, UUIDs and the run folder; archives (zip, tgz, pack, xlsx, nested) entry by entry; package.db/.index.db as sorted rows; same tokens in another order = order-only. Skipped: qa*, fragment-usage-analysis.csv.

| IG | commit | heap | R1 | R2 | R1/R2 qa e/w/h | I1 | I2 | W first, W1a..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/UTG | d8b0804a9f | 10g | 880 | 788 | 110/69/3 (BL 54) | 819 | 847 | 821, W1b 878, W1d 940, W1e 889, W1f 874, W2 883, W3 923, W4 934, W5 888, W6 896 (all edits seen) | 758 | 718 | 716 | same (known order-only used-by lists; W4 +3 errors expected from the test copy) |

## Notes
- R1 vs R2 (both master): QA counts and the 4463 qa.txt lines equal. 85601 files each; all equal after masking except: used-by order in codesystem-/valueset-ref-(all-)list.csv/json (and csvs.zip, full-ig.zip) (order only, known); hl7.terminology.en.tgz `package/.index.db` (and its copy in full-ig.zip): R2 has ResourceList rows for the Bundle-hxutg1-* files that R1 lacks (F3, known pre-existing).
- I1 vs R2 and I2 vs R2: QA counts and all 4463 qa.txt lines equal; 85601 files each, all equal after masking except used-by order in codesystem-/valueset-ref lists (order only, known). Round 1's spec.internals and llms.txt differences are gone (both are in master now).
- Watch (int, one `-rapido -watch` session, edits applied cumulatively, output snapshotted after each step; F builds run afterwards from the same sources): W1a (markdown page) and W1c (intro/notes) don't apply, since UTG has no markdown pages and no intro/notes files. W1b = a sentence in pagecontent/license.xml; W1d = a menu item in includes/menu.xml; W1e = new page pagecontent/qa2-newpage.xml + its page entry in utg.xml; W1f = remove both; W2 = description of StructureDefinition-ext-namingsystem-title; W3 = display in CodeSystem-FDI-surface; W4 = a copy of it with a new id/url; W5 = delete the copy; W6 = restore all.
- Every edit reached output: W1b en/license.html; W1d the menu item in 25526 html pages; W1e en/qa2-newpage.html; W1f no page files or marker left; W2 4 SD pages; W3 5 CodeSystem pages; W4 the copy's pages (20 files); W5 none left; W6 no marker left in any html.
- QA in the watch: counts 110/69/3 in every step except W4 (113/69/3). The 3 extra W4 errors are expected from the test copy: its all-system value set ValueSet/surface has the original's system, and its OID is reported twice as a duplicate of FDI-surface. Round 1 had 1 extra; the duplicate OID check is new on master. Apart from that, the qa.txt message sets differ from R2 only in line numbers (W1d to W5: the extra menu line shifts every html line by 1).
- W1f vs F1 (int full build of the same sources): QA counts and messages equal; files equal after masking apart from used-by order in the ref lists (order only, known).
- Every int rebuild is a Complete Build. The log gives two reasons: "the template doesn't declare what its scripts read (script-inputs in its config.json)", then "terminology server calls failed in the last build (6)". The 6 calls fail on every build: tx.fhir.org "Unable to provide support for code system" (nubc-UB92, naics, epsg-crs, epsg-ca, mesh, hsloc, SCDHEC, EPSG). So declaring script-inputs (Phase D) can't reach a Page Rebuild on UTG at this commit.
- W2 vs F2, W6 vs F6, F6 vs R2, W6 vs R2: QA counts and messages equal; files equal after masking apart from the used-by order (order only, known).
- No FINDING in Phase C, so no master reruns were needed.

## Phase D

- int.jar for Phase D: qa/page-only-ready e6a0c6a2b71d3c562675d8af3707351428c3a28a (PR vadi2#56), built after the ref moved. Phase C used 5111080.
- Template: hl7.utg.template#current (base hl7.base.template). Its own script ant-utg.xml only overrides targets with "...skipped" echoes (the MIF part is commented out) and reads nothing itself, so it can be declared with the hl7.base list from qa2/script-inputs.md. The copy is in /home/user/qa/tpl-utg (config.json keeps `script` ant-utg.xml and adds `script-inputs`), and ig.ini has `hl7.utg.template#current = /home/user/qa/tpl-utg`. The log shows "Load Template hl7.utg.template#current from local folder ... (override in ig.ini)" and no "doesn't declare" fallback.

| IG | commit | heap | int first | W1b (page) | W1d (menu.xml) | mode | verdict |
|---|---|---|---|---|---|---|---|
| HL7/UTG | d8b0804a9f | 10g | 802 | 853, Complete Build | 909, Complete Build | page rebuild not reachable | skip (after W1d): UTG can't reach a page rebuild |

- W1b (a sentence in pagecontent/license.xml): `Rapido Mode: Complete Build (.../input/pagecontent/license.xml is transformed)`, then for the build itself `Complete Build (terminology server calls failed in the last build (6))`. The edit is in output (en/license.html), QA 110/69/3.
- W1d (menu.xml, a declared script input; this is the plan's step 6 check): `Complete Build (.../input/includes/menu.xml is read by the template's scripts)`, as expected. The menu item is in 25526 pages and QA is 110/69/3. The output has the same file set as the Phase C W1d snapshot; 29765 files are byte-identical and the rest differ by build time only, the same as Phase C W1d vs F builds.
- Why the page path can't be reached on UTG:
  - every UTG page is an xml page in input/pagecontent (no markdown pages, no intro/notes files), and int treats an edit to a transformed xml page as needing a Complete Build;
  - the 6 tx calls (tx.fhir.org can't support nubc-UB92, naics, epsg, mesh, hsloc, SCDHEC) fail on every build, which forces a Complete Build anyway.
  As the orchestrator suggested, I stopped after W1d. W1e/W1f/W2/W1a and the master Phase D run were not done: every step would be a Complete Build on both jars, already covered by Phase C (W1e/W1f/W2 there, all equal to F builds).
