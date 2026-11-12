# QA results, shard s1

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: 6c1abf463d8ae87d30871898c6abdaa63c7df673 (qa/integration-ready)
- VM: 4 CPU, 15 GB, 8 GB swap file (shrunk from 16 GB during Phase A: disk was nearly full). Times in seconds, wall clock of the whole java run (W*: edit to "Finished @").
- All builds run with LC_ALL=C.UTF-8 (master fails on niis under the C locale, see notes).
- Watch sequence: watch stopped after W2 (for F2) and restarted on the same folder for W3..W6.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| ChitSR/sample-ig | 0701fa3 | 7600m | 78 | 1/6/3 | 35 | 1/6/3 | 32 | W1 5.7, W2 20.0, W3 20.2, W4 24.0, W5 22.0, W6 18.2 | 36 | 36 | pass; F1 (intended), F2 pre-existing |
| HL7/demo-collect | 0cd5dc5 | 7600m | 107 | 0/0/0 | 48 | 0/0/0 | 51 | W1 1.5, W2 26.7, W3 26.9, W4 39.2, W5 30.4, W6 29.8 | 53 | 54 | pass; only diff vs master = F1 (intended, #48) |
| costateixeira/spenser | 63c79e0 | 7600m | 63 | 58/20/139 | 58 | 58/20/139 | 51 | W1 1.7, W2 27.5, W3 21.9, W4 53.8, W5 50.7, W6 30.6 | 49 | 56 | pass; F1 (intended), F3 pre-existing |
| HL7/fhir-consent-management | aad3563 | 7600m | 75 | 2/0/0 | 76 | 2/0/0 | 75 |  |  |  |  |
| HL7/emedicinal-product-info | 66968b7 | 7600m | 135 | 7/0/0 |  |  |  |  |  |  |  |
| hyweb-cdc/niis | b343858 | 7600m | 116 | 0/0/0 |  |  |  |  |  |  |  |
| uzinfocom-org/digital-health-ig | 31e406f | 7600m | 748 | 5/268/9446 |  |  |  |  |  |  |  |
| HL7/US-Core | 3a65397 |  |  |  |  |  |  |  |  |  |  |

## Findings

**F1. spec.internals drops `*.ai.html` targets (int vs master; intended, #48 perf/watch-fsh-tail).**
Master lists the top-level `*.ai.html` pages (e.g. `Observation-example.ai.html`, `ImplementationGuide-<id>.ai.html`) in spec.internals "targets"; int does not. 
The AI step deletes those pages, so they are not in output/ with either jar. Comes from commit 349fc12 "spec.internals doesn't list the ai pages the AI step removes" (in #48, and the older copy in the base branch). 
Shows up in every archive that holds spec.internals: definitions.*.zip, package*.tgz, <id>.en.tgz (.index.db), validator*.pack, full-ig.zip. No other content diff in those archives.

**F2. In watch mode, html-check messages in qa.txt name `temp/pages/.watch-output/...` instead of `output/...` (pre-existing).**
sample-ig: the W2/W6 qa.txt has `ERROR: <dir>/temp/pages/.watch-output/en/StructureDefinition-myobservation.html: The page skips a heading level` where the full build F2/F6 has `<dir>/output/en/...`. The master watch (sample-ig, same edits) shows the same paths, so it is not caused by the integration branch. Same message set and counts otherwise.

**F3. `package/.index.db` ResourceList rows in the package tgz files are filled in some runs and empty in others (pre-existing, master flake).** sample-ig: master R1 0 rows, master R1b 2 rows, every int build 2. spenser: jct.fhir.spenser.fr.tgz has 0 rows with master R1 and 13 with int I1, while en/nl/pt/package.tgz have 0 with both. Master varies between its own two runs, so not caused by the integration branch.

Master watch on sample-ig for reference (same spec): W1 22.2 s on master vs 5.7 s on int.

## Compare method notes

- Masks beyond the plan list: run-dependent `fragment-usage-analysis.csv` inside full-ig.zip (timing data); sqlite package.db compared as masked rows; key order in json maps and token order in csv "Used" columns treated as order-only ("used by" lists).
- Order-only diffs seen on both jars between runs (codesystem-ref-*.json/csv, llms.txt, en/ai.zip, package.db row order) are not counted.
- fhir-consent-management: first master R1 hit tx.fhir.org 502s (60 tx errors); redone. tx-error messages are listed separately in each compare.
- niis: master R1 fails under LC_ALL=C (non-ASCII image file name, Java file name encoding ANSI_X3.4-1968); pre-existing on master, related to #25 ws10-nonascii-names. All builds use C.UTF-8.
