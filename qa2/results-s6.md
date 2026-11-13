# QA round 2 results, shard s6

Status: Phase C in progress. int.jar = `qa/page-only-ready` 5111080 (5 commits on master 2ccca17).

Setup: master 2ccca17 (master.jar). 6 GB swap (disk is ~30 GB), Jekyll 4.4.1, SUSHI (npm latest), JDK 21. Every build:
`LC_ALL=C.UTF-8` (lars has non-ASCII file names), `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m
-XX:+UseStringDeduplication`, `-tx https://tx.fhir.org`, shared package cache. Heap 7600m for every IG.

## TW Core (tw.gov.mohw.twcore)
skip: no public source (qas.json repo path is base64, not a GitHub repo; as round 1).

## Phase A: R1 (master.jar, full build)
| IG | commit | R1 s | QA errs/warn/hints |
|---|---|---|---|
| TEHIK-EE/ig-ee-qre | 9f4140f | 97 | 40 / 103 / 1045 |
| hl7-be/ucum-test | af477f6 | 80 | 0 / 33 / 12 |
| HL7/crmi-ig | 89c464e | 209 | 17 / 25 / 235 |
| omnifhir/lars | 39e03bf (new since round 1) | 222 | 0 / 18 / 14 |
| HL7/fhir-cqm | 5b61912 | 224 | 19 / 224 / 381 |
| HL7/cqf-recommendations | e2fb413 | 301 | 51 / 179 / 440 |

Compare (bin/cmp.py, per QA-PLAN step 4): archives entry by entry, sqlite dbs as sorted rows; masks dates/times, run
folder, UUIDs, `maxMemory`, `Nms` timings, the watch staging path `temp/pages/.watch-output` and zero-width spaces in qa
paths (F2), the tx log link in qa.html messages (cached tx calls in watch builds have no `qa-tx.html#lN` link),
"used by" order (`*-ref-*list.csv/json`, also inside fields), llms.txt line order, `*.ai.html` in spec.internals,
stack frames, identifier-type displays (URI/NPI) and CLD `<a name="X"> </a><a name="hcX"> </a>` pairs plus `#lN`
line anchors (cqm round-1 known). `.index.db` (F3) is NOT masked and is listed when it differs.
Edits: unique marker per step, checked in output/ after each rebuild. W1b/W1c only where the IG has an html/xml
page or an intro file. F1/F2/F6 = int full builds of a pristine copy with the edits up to W1f/W2/W6 applied.

## TEHIK-EE/ig-ee-qre @ 9f4140f
- heap 7600m. QA 40 / 103 / 1045 in every build. Template tehik-template (base fhir.base.template, ant onCheck,
  no script-inputs).
- times (s): R1 97, I1 76, I2 72, F1 78, F2 79, F6 75.
  Watch int: first 75, W1a 34.0, W1e 51.6, W1f 56.1, W2 51.6, W3 51.1, W4 54.1, W5 60.1, W6 60.1.
  Watch master: first 83, W1a 35.5, W1e 55.6, W1f 50.1, W2 53.6, W3 51.1, W4 52.6, W5 50.6, W6 52.1.
  All Complete Builds on both jars (227 failed tx calls; int also logs "the template doesn't declare what its
  scripts read (script-inputs in its config.json)", so W1a gets no Page Rebuild, as the plan requires).
  W1b/W1c/W1d n/a (md pages only, no intros, no input/includes).
- every edit reached output/ (and W1f/W5/W6 removals are gone).
- I1 = I2 = W6 = F6; F1 = W1f; F2 = W2 (0 differences). I1/I2/W6 vs R1: only `.index.db` ResourceList in
  package.tgz (R1 0 rows, int 6). Master watch W6 also has 6 rows: F3, pre-existing.
- verdict: **same** (pre-existing diffs: F3 .index.db).

## hl7-be/ucum-test @ af477f6
- heap 7600m. QA 0 / 33 / 12 in every build. Template hl7.be.fhir.template (ant onCheck, no script-inputs).
- times (s): R1 80, I1 63, I2 51, F1 52, F2 57, F6 57.
  Watch int: first 53, W1a 21.5 (Differential), W1d 21.0 (Differential), W1e 32.5, W1f 30.1, W2 29.5 (Complete,
  "changed: ig"), W3 25.5 (Differential), W4 33.6, W5 32.1, W6 38.1 (Complete).
  Watch master: first 54, W1a 25.0, W1d 23.0 (Differential), W1e 37.6, W1f 40.1, W2 36.6 (Complete), W3 26.0
  (Differential), W4 35.5, W5 32.1, W6 33.0 (Complete).
  int logs "the template doesn't declare what its scripts read" for the page edits and runs a Differential Build
  instead of a Page Rebuild. W1b/W1c n/a.
- every edit reached output/en/.
- I1 = I2 = W6 = F6; F1 = W1f; F2 = W2. I1/I2/W6 vs R1: only `.index.db` ResourceList in
  hl7.fhir.be.ucum-test.en.tgz (R1 0, int 12; master watch W6 also 12): F3, pre-existing.
- verdict: **same** (pre-existing diffs: F3 .index.db).

## HL7/crmi-ig @ 89c464e
- heap 7600m. QA 17 / 25 / 235 in every build. Template hl7.fhir.template (hl7.base ant scripts, no script-inputs).
- times (s): R1 209, I1 180, I2 185, F1 174, F2 180, F6 172.
  Watch int: first 192, W1a 91.6, W1d 91.6 (Differential, 59 of 264), W1e 163.2, W1f 153.2, W2 156.2 (Complete,
  "changed: ig"), W3 91.6 (Differential), W4 152.2, W5 155.8, W6 158.3 (Complete).
  Watch master: first 200, W1a 94.6, W1d 92.1, W1e 163.3, W1f 162.7, W2 164.7, W3 97.2, W4 163.3, W5 161.8, W6 161.3
  (same build kinds). W1b/W1c n/a.
- every edit reached output/en/.
- I1 = I2 = W6 = F6; F1 = W1f; F2 = W2. I1/I2/W6 vs R1: only `.index.db` ResourceList in hl7.fhir.uv.crmi.en.tgz
  (R1 0, int 129; master watch W6 also 129): F3, pre-existing.
- verdict: **same** (pre-existing diffs: F3 .index.db).

## omnifhir/lars @ 39e03bf
- heap 7600m. QA 0 / 18 / 14 in every build. Template #custom-template (local, base fhir.base.template; no
  script-inputs).
- times (s): R1 222, I1 250, I2 189, F1 178, F2 169, F6 164.
  Watch int: first 192, W1a 126.7, W1b 123.2, W1d 118.1 (Differential, 25 of 118), W1e 144.2, W1f 143.2, W2 152.8
  (Complete, "changed: ig"), W3 142.2 (Differential), W4 156.3, W5 155.8, W6 151.3 (Complete).
  Watch master: first 178, W1a 131.7, W1b 130.7, W1d 129.2, W1e 147.2, W1f 146.7, W2 145.7, W3 134.7, W4 146.2,
  W5 144.2, W6 150.3 (same build kinds). W1c n/a. (Round 1's L2 needed #1466, which is not in this branch: W2 is a
  Complete Build here on both jars.)
- every edit reached output/.
- I1 = I2; I1/I2 vs R1: only `.index.db` ResourceList in package.tgz (F3). Every watch snapshot (W1f, W2, W6) differs
  from every full build (F1, F2, F6, I1, R1) in one place: the column order in
  StructureDefinition-organization-lars-mappings.html (watch: LabMonthlyReportModel first; full builds:
  LabDailyReportModel first). Master's watch W6 differs from R1 the same way: round 1's L3, pre-existing.
- verdict: **same** (pre-existing diffs: F3 .index.db, L3 mappings column order in watch builds).
