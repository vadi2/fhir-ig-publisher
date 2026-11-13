# QA round 2 results, shard utg

Status: Phase A done (R1, plus R2 as in round 1). Waiting for qa/page-only-ready (Phase B).

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: (pending)
- UTG: HL7/UTG master d8b0804a9f3dda4fc208eead147bae6d9b80b69b (same commit as round 1)
- VM: 4 CPU, 15 GB, 9 GB swap file (disk allowance about 30 GB; a UTG work folder is 8.6 GB). Heap 10g for every build (as round 1; peak "Max Memory Used = 9Gb"). Times in seconds, wall clock of the whole java run. R1 ran on an empty package cache.
- R2 = a second master full build on a warm cache. Round 1's R1 differed from all later builds (cold cache), so R2 is the reference for int builds; this time R1 and R2 match apart from known items.
- Compare: qa.json counts and qa.txt message set (run folder masked); every output file after masking dates, times, UUIDs and the run folder; archives (zip, tgz, pack, xlsx, nested) entry by entry; package.db/.index.db as sorted rows; same tokens in another order = order-only. Skipped: qa*, fragment-usage-analysis.csv.

| IG | commit | heap | R1 | R2 | R1/R2 qa e/w/h | I1 | I2 | W first, W1a..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/UTG | d8b0804a9f | 10g | 880 | 788 | 110/69/3 (BL 54) | | | | | | | |

## Notes
- R1 vs R2 (both master): QA counts and the 4463 qa.txt lines equal. 85601 files each; all equal after masking except: used-by order in codesystem-/valueset-ref-(all-)list.csv/json (and csvs.zip, full-ig.zip) (order only, known); hl7.terminology.en.tgz `package/.index.db` (and its copy in full-ig.zip): R2 has ResourceList rows for the Bundle-hxutg1-* files that R1 lacks (F3, known pre-existing).
