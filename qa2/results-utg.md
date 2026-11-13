# QA round 2 results, shard utg

Status: Phase C: I1, I2 and the int watch sequence done, all equal; F1 = W1f; F2, F6 running (21:35). Phase D next.

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: vadi2 qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb
- UTG: HL7/UTG master d8b0804a9f3dda4fc208eead147bae6d9b80b69b (same commit as round 1)
- VM: 4 CPU, 15 GB, 9 GB swap file (disk allowance about 30 GB; a UTG work folder is 8.6 GB). Heap 10g for every build (as round 1; peak "Max Memory Used = 9Gb"). Times in seconds, wall clock of the whole java run. R1 ran on an empty package cache.
- R2 = a second master full build on a warm cache. Round 1's R1 differed from all later builds (cold cache), so R2 is the reference for int builds; this time R1 and R2 match apart from known items.
- Compare: qa.json counts and qa.txt message set (run folder masked); every output file after masking dates, times, UUIDs and the run folder; archives (zip, tgz, pack, xlsx, nested) entry by entry; package.db/.index.db as sorted rows; same tokens in another order = order-only. Skipped: qa*, fragment-usage-analysis.csv.

| IG | commit | heap | R1 | R2 | R1/R2 qa e/w/h | I1 | I2 | W first, W1a..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/UTG | d8b0804a9f | 10g | 880 | 788 | 110/69/3 (BL 54) | 819 | 847 | 821, W1b 878, W1d 940, W1e 889, W1f 874, W2 883, W3 923, W4 934, W5 888, W6 896 (all edits seen) | 758 | | | |

## Notes
- R1 vs R2 (both master): QA counts and the 4463 qa.txt lines equal. 85601 files each; all equal after masking except: used-by order in codesystem-/valueset-ref-(all-)list.csv/json (and csvs.zip, full-ig.zip) (order only, known); hl7.terminology.en.tgz `package/.index.db` (and its copy in full-ig.zip): R2 has ResourceList rows for the Bundle-hxutg1-* files that R1 lacks (F3, known pre-existing).
- I1 vs R2 and I2 vs R2: QA counts and all 4463 qa.txt lines equal; 85601 files each, all equal after masking except used-by order in codesystem-/valueset-ref lists (order only, known). Round 1's spec.internals and llms.txt differences are gone (both are in master now).
- Watch (int, one `-rapido -watch` session, edits applied cumulatively, output snapshotted after each step; F builds run afterwards from the same sources): W1a (markdown page) and W1c (intro/notes) don't apply, since UTG has no markdown pages and no intro/notes files. W1b = a sentence in pagecontent/license.xml; W1d = a menu item in includes/menu.xml; W1e = new page pagecontent/qa2-newpage.xml + its page entry in utg.xml; W1f = remove both; W2 = description of StructureDefinition-ext-namingsystem-title; W3 = display in CodeSystem-FDI-surface; W4 = a copy of it with a new id/url; W5 = delete the copy; W6 = restore all.
- Every edit reached output: W1b en/license.html; W1d the menu item in 25526 html pages; W1e en/qa2-newpage.html; W1f no page files or marker left; W2 4 SD pages; W3 5 CodeSystem pages; W4 the copy's pages (20 files); W5 none left; W6 no marker left in any html.
- QA in the watch: counts 110/69/3 in every step except W4 (113/69/3). The 3 extra W4 errors are expected from the test copy: its all-system value set ValueSet/surface has the original's system, and its OID is reported twice as a duplicate of FDI-surface. Round 1 had 1 extra; the duplicate OID check is new on master. Apart from that, the qa.txt message sets differ from R2 only in line numbers (W1d to W5: the extra menu line shifts every html line by 1).
- W1f vs F1 (int full build of the same sources): QA counts and messages equal; files equal after masking apart from used-by order in the ref lists (order only, known).
- Every int rebuild is a Complete Build. The log gives two reasons: "the template doesn't declare what its scripts read (script-inputs in its config.json)", then "terminology server calls failed in the last build (6)". The 6 calls fail on every build: tx.fhir.org "Unable to provide support for code system" (nubc-UB92, naics, epsg-crs, epsg-ca, mesh, hsloc, SCDHEC, EPSG). So declaring script-inputs (Phase D) can't reach a Page Rebuild on UTG at this commit.
