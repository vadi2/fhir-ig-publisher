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

## HL7/fhir-cqm @ 5b61912
- heap 7600m. QA 19 / 224 / 381 in every build. Template hl7.fhir.template (no script-inputs).
- times (s): R1 224, I1 149, I2 140, F1 128, F2 127, F6 139.
  Watch int: first 142, W1a 81.6, W1d 81.7 (Differential, 72 of 415), W1e 129.3, W1f 118.8 (Complete, "changed: ig"),
  W2 106.3 (Differential, 272 of 415), W3 101.3 (Differential), W4 126.4, W5 126.4 (Complete), W6 103.4 (Differential).
  Watch master (first run): first 208, W1a 109, W1d 130 (Differential), W1e 162, W1f 143 (Complete), W2 108, W3 106
  (Differential), W6 100 (Differential). Its W4/W5 are not comparable: my first W4 added the example without an IG
  entry, and the HL7 template's onGenerate.jira.xslt stops on a resource with an empty name ("Found multiple
  candidates ... in previous jira-spec-info"; a template behaviour, both jars). W4 now also lists the example in
  cqm.xml with a name; int ran that version. A master rerun with it is queued for the timings.
- every edit reached output/en/.
- I1 vs R1: F3 `.index.db` (hl7.fhir.uv.cqm.en.tgz) and package.db CodeSystemListRefs (R4 vs THO 7.5.0 value set
  links), which also differ between I1 and I2, F2 and W2, F6 and W6 (round 1: resolution nondeterminism).
  F2 vs W2 also differed in Bundle-measurereport-bundle-example (narrative "USEIN" vs "United States Employer
  Identification Number"): the identifier-type display nondeterminism from round 1 (R1 has the long form in 24
  places); masked with the URI/NPI displays. With it masked: F1 = W1f, F2 = W2, F6 = W6, W6 = I1 = I2 except
  package.db.
- verdict: **same** (pre-existing diffs: F3 .index.db, package.db CodeSystemListRefs, identifier-type displays).

## HL7/cqf-recommendations (cpg) @ e2fb413
- heap 7600m. QA 51 / 179 / 440 in every build. Template hl7.fhir.template (no script-inputs).
- times (s): R1 301, I1 251, I2 251, F1 234, F2 255, F6 272.
  Watch int: first 238, W1a 130.7, W1c 135.7, W1d 132.7 (Differential, 371 of 986), W1e 247.9, W1f 261.0, W2 272.0,
  W3 261.0, W4 246.5, W5 253.6, W6 253.6 (Complete, "changed: ig"). W1b n/a.
  Watch master: first 267 (the first run was stopped after that to build int.jar; the rerun is queued after Phase D).
- every edit reached output/en/.
- I1 = I2 = W6 = F6; F1 = W1f; F2 = W2. I1/I2/W6 vs R1: only `.index.db` ResourceList in hl7.fhir.uv.cpg.en.tgz (F3).
- verdict: **same** (pre-existing diffs: F3 .index.db).

## Phase D
- jar: int = qa/page-only-ready at 5111080 (Phase D started before the move to e6a0c6a2; per the plan it ran to the end
  on 5111080). master = 2ccca17. Same declared template copy, same edits and heap (7600m) on both jars.
- declared copies, from qa2/script-inputs.md: crmi, cqm, cpg: `hl7.fhir.template#current` copy with the hl7 list; ucum:
  `hl7.be.fhir.template` copy with the fhir2 list; eeqre (#tehik-template) and lars (#custom-template): `script-inputs`
  with the fhir.base list added to the local template's config.json (both are based on fhir.base and add no script).
- steps: W1a page edit, W1b xml page edit (lars), W1c intro-notes (cpg), W1d an include (ucum results-run.md, cpg
  artifact-profiles.html) or menu.xml (crmi, cqm, lars), W1e add page, W1f delete it, W2 a non-page edit (profile), W1a2
  page edit again. n/a where the IG has no such file. F1 = int full build after W1f, F2 after W1a2.
- cells: int mode/seconds | master mode/seconds. P = Page Rebuild, D = Differential, C = Complete.

| IG | W1a | W1b | W1c | W1d | W1e | W1f | W2 | W1a2 | F1 = W1f | F2 = W1a2 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| eeqre | P 5.5 / C 37.6 | n/a | n/a | n/a | C 54.1 / C 54.6 | C 55.6 / C 52.1 | C 52.6 / C 49.6 | P 5.0 / C 32.6 | yes | yes | ok |
| ucum | P 7.5 / D 22.0 | n/a | n/a | P 9.0 / D 20.5 | C 27.5 / C 32.0 | C 27.5 / C 31.0 | C 30.0 / C 28.5 | P 8.0 / D 19.5 | yes | yes | ok |
| crmi | P 37.5 / D 85.6 | n/a | n/a | D 86.6 / D 83.6 (menu.xml, declared) | C 157.7 / C 153.7 | C 143.7 / C 149.2 | C 143.7 / C 151.7 | P 35.0 / D 81.1 | yes | yes | ok |
| lars | P 48.5 / D 118.6 | D 123.2 / D 118.6 | n/a | P 48.5 / D 115.7 (menu.xml, not declared by fhir.base) | C 147.2 / C 131.2 | C 140.2 / C 139.7 | C 143.7 / C 148.2 | P 47.6 / D 118.2 | yes | yes | ok, W1b not a page rebuild |
| cqm | P 35.5 / D 80.1 | n/a | n/a | D 91.1 / D 84.7 (menu.xml, declared) | C 134.8 / C 125.8 | C 120.7 / C 124.8 | D 102.7 / D 105.8 | P 36.6 / D 88.7 | yes (package.db) | yes (package.db) | ok |
| cpg | P 59.6 / D 128.2 | n/a | P 66.5 / D 126.2 | P 66.5 / D 126.2 | C 222.7 / C 224.3 | C 227.3 / C 226.9 | C 233.3 / C 232.4 | P 61.1 / D 128.3 | yes | yes | ok |

First builds (int / master, s): eeqre 77 / 74, ucum 48 / 51, crmi 166 / 154, lars 178 / 164, cqm 151 / 139, cpg 250 /
238. F1, F2 (s): eeqre 71, 71; ucum 47, 48; crmi 171, 189; lars 159, 170; cqm 140, 132; cpg 230, 229.

Notes
- No regression. Every edit reached the output on both jars; QA counts equal in every build. int watch W1f and W1a2
  equal the int full builds F1 and F2 byte for byte after the round masks (cqm: only package.db CodeSystemListRefs,
  R4 vs THO 7.5.0 links, the round-1 nondeterminism, seen in both directions). int and master watch snapshots are equal
  at W1f and W1a2 except lars and cqm below.
- Page rebuild seconds vs master on the same page edit: eeqre 5 vs 35, ucum 8 vs 21, crmi 36 vs 83, lars 48 vs 118,
  cqm 36 vs 84, cpg 60 vs 127. Add/delete page and profile edits take the same path and time on both jars.
- Declared-path check (step 6): crmi and cqm W1d edit `input/includes/menu.xml`, listed for hl7.fhir.template. int logs
  "Complete Build (.../input/includes/menu.xml is read by the template's scripts)", then Rapido runs its Differential
  Build like master; menu change visible on every checked page. lars W1d edits menu.xml too, but its template is based on
  fhir.base, whose list does not have menu.xml (fhir.base's scripts don't read it), so a page rebuild is right there; the
  change is in index.html and security.html and W1a2 equals F2.
- lars W1b (input/pagecontent/downloads.xml): int logs "Complete Build (.../downloads.xml is transformed)" and runs a
  Differential Build, so W1b doesn't get a page rebuild. The plan expects one for W1b. Output is correct; the cost is
  123 s instead of about 48.
- eeqre: its builds have 227 failed terminology calls (fhir.ee systems). master answers every watch edit with
  "Complete Build (terminology server calls failed in the last build (227))"; int's page rebuild runs anyway and doesn't
  retry them. Output is the same as a full build here (same failures); noted because it is a behaviour change.
- lars, master watch W1a2 (Differential) vs int W1a2 (Page Rebuild): the mappings column order (L3), the text of three
  twcore 0.3.2 StructureDefinitions in package-combined.tgz (trailing space, a relative vs R4 link), and one file entry
  in qa.html. int's W1a2 equals F2 in all three, so these are on master's differential side (Phase C's master watch
  never had a differential step after a complete one on lars).
