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
| HL7/fhir-consent-management | aad3563 | 7600m | 75 | 2/0/0 | 76 | 2/0/0 | 75 | W1 8.6, W2 43.8, W3 42.5, W4 55.8, W5 51.5, W6 41.1 | 75 | 75 | pass; F1 (intended), F4 pre-existing |
| HL7/emedicinal-product-info | 66968b7 | 7600m | 135 | 7/0/0 | 136 | 7/0/0 | 118 | W1 23.0, W2 79.8, W3 82.7, W4 109.5, W5 109.1, W6 85.8 | 119 | 117 | pass; F1 (intended), F3 pre-existing; comparison pages differ only in embedded Java stack traces |
| hyweb-cdc/niis | b343858 | 7600m | 116 | 0/0/0 | 99 | 0/0/0 | 100 | W1 9.4 MISSING, W2 73.7, W3 72.7, W4 73.3, W5 84.5, W6 68.4 (NOT OK) | 93 | 94 | pass (F3); W1 redone on index.md: change in output after 8.7 s, revert 8.0 s |
| uzinfocom-org/digital-health-ig | 31e406f | 7600m | 748 | 5/268/9446 | 715 | 5/268/9446 | 695 | W1 21.5, W2 192.0, W3 183.7 (NOT OK) | 670 |  | FAIL (F5 regression, PR #48): watch W4 OOM-killed with int, master survives; W1-W3 ok; full-build compares only F1, F3 and F6 (pre-existing) |
| HL7/US-Core | 3a65397 | 10g | 498 | 7/20/1 | 408 | 7/20/1 | 271 | W1 24.0, W2 294.7, W3 270.6, W4 269.2, W5 272.6, W6 274.6 | 409 | 370 | pass (F3, F4) |

## Findings

**F1. spec.internals drops `*.ai.html` targets (int vs master; intended, #48 perf/watch-fsh-tail).**
Master lists the top-level `*.ai.html` pages (e.g. `Observation-example.ai.html`, `ImplementationGuide-<id>.ai.html`) in spec.internals "targets"; int does not. 
The AI step deletes those pages, so they are not in output/ with either jar. Comes from commit 349fc12 "spec.internals doesn't list the ai pages the AI step removes" (in #48, and the older copy in the base branch). 
Shows up in every archive that holds spec.internals: definitions.*.zip, package*.tgz, <id>.en.tgz (.index.db), validator*.pack, full-ig.zip. No other content diff in those archives.

**F2. In watch mode, html-check messages in qa.txt name `temp/pages/.watch-output/...` instead of `output/...` (pre-existing).**
sample-ig: the W2/W6 qa.txt has `ERROR: <dir>/temp/pages/.watch-output/en/StructureDefinition-myobservation.html: The page skips a heading level` where the full build F2/F6 has `<dir>/output/en/...`. The master watch (sample-ig, same edits) shows the same paths, so it is not caused by the integration branch. Same message set and counts otherwise.

**F3. `package/.index.db` ResourceList rows in the package tgz files are filled in some runs and empty in others (pre-existing, master flake).** sample-ig: master R1 0 rows, master R1b 2 rows, every int build 2. spenser: jct.fhir.spenser.fr.tgz has 0 rows with master R1 and 13 with int I1, while en/nl/pt/package.tgz have 0 with both. Master varies between its own two runs, so not caused by the integration branch.

Master watch on sample-ig for reference (same spec): W1 22.2 s on master vs 5.7 s on int.

**F4. Which terminology.hl7.org version an Identifier link to `NamingSystem-npi.html` points at changes from run to run (pre-existing, master too).**
fhir-consent-management: the NPI link in example narratives (Consent, AuditEvent, Bundle, DocumentReference examples and their pages, ai.zip) is `terminology.hl7.org/5.0.0/` (label "NPI"), `5.3.0`, `6.5.0` or `7.1.0` (label "United States National Provider Identifier"). Master R1 had 6.5.0 and master R1-txfail had 5.0.0. Int builds: I1 5.3.0, I2 5.0.0, F2 7.1.0, F6 5.3.0. The package cache holds hl7.terminology.r4 5.0.0, 5.3.0, 6.1.0, 6.5.0, 7.1.0, 7.4.0 and 7.5.0. Watch outputs mix versions across pages, because pages that are not regenerated keep the version from the earlier build. With the version and label masked, the only remaining diffs are F1 (R1 vs I1/I2) and none at all (W2-F2, W6-F6, I1-W6).

fhir-consent-management watch: the first run's W4 used an instance that my spec wrote wrong (missing Consent.identifier and source[x]). SUSHI reported 2 errors, the build failed, the watch kept running, and the test timed out. That run is kept as W-badspec, not counted. Watch redone from scratch with W4 = a copy of ConsentExample.

Disk full at 17:35 (compare extracting large tgz outputs while US-Core R1 loaded packages). Lost: digital-health-ig watch, niis W1 redo, US-Core R1 (exit 1). compare.py now reads tgz in place; all three are rerun from scratch, the failed runs are kept as *-diskfull and not counted.

US-Core: F4 here also hits NamingSystem dui, uri and CLIA links (master R1 npi 6.5.0, int I1 7.5.0, int I2 6.5.0). The comparison-v*/xx-* and ipa/ips comparison pages embed Java stack traces as on emedicinal. First F2/F6 failed in Jekyll (menu.xml missing): my W source snapshot excluded fsh-generated/, which US-Core commits as source (no input/fsh). Test harness bug; snapshot fixed, F2/F6 rebuilt. package.db CodeSystemListRefs provenance-agent-type and service-type link to hl7.org/fhir/R4 in master R1 and int F6, and to terminology.hl7.org/7.5.0 in int I1, I2 and W6, so it varies run to run within int (F4 family, not a regression). W6-F6 package.db also differs in Metadata genDate (`Thu, Nov 12, 2026 20:40+0000`), only because the two builds ran on different days; my date mask does not cover that format. F2/F6 rebuilt on the fixed snapshot (exit 0, qa equal to W2/W6). Over the full diff lists (not just the 60 in the cmp files), W2-F2 and W6-F6 leave only package.db (and its copy in full-ig.zip) after the F4 and stack-frame masks: the R4 vs THO link above plus genDate. Master R1 vs master MR2 also differs in package.db, .index.db and the list files.

**F5. REGRESSION, PR #48 (perf/watch-fsh-tail, head 9614fa3): digital-health-ig watch, the publisher is killed for memory at W4 because Jekyll is kept running between builds.**
Heap 7600m, 16 GB machine, 14.3 GB cgroup memory limit, 2 GB swap. At W4 Rapido found 14077 temp files changed (W2 120, W3 151) and handed them to the kept-running Jekyll; java RSS 9.3 GB plus the Jekyll driver 4.4 GB and growing (3.7 GB peak on the first full build) went over the limit and the kernel killed java. The kept-running Jekyll comes from the base branch (e85860f2, ae762436, 26aaf1ac), not master. A master watch on the same IG with the same heap and swap is queued to see whether master gets through W4.
Classified: master watch MW (same IG, same edits, -Xmx7600m, same 14.3 GB cgroup limit, same 2 GB swap) completed every step, W4 included (W4 684 s, Complete Build, changed: ig). Peak java RSS is the same on both (master 9227/9112 MB, int 9344/9305 MB); the difference is Jekyll: master runs a fresh `jekyll build` per step and peaks at 3.7 GB, int keeps the Jekyll driver running and it had grown to 4.45 GB when the JVM was killed at W4. The kept-running Jekyll comes from e85860f2, ae762436 and 26aaf1ac, which are in PR #48 head 9614fa3 and not on master (results-s2 found the same on digital-health-integration). Repro: digital-health-ig, `-rapido -watch`, -Xmx7600m, 14.3 GB memory limit, 2 GB swap; after the first build edit a page, a profile description and an example, then add a new example (input/examples): the JVM is OOM-killed during that build. For scale, int watch steps are much faster when they survive: W1 21.5 s (master 396 s), W2 192 s (master 497 s), W3 184 s (master 443 s).

**F6. digital-health-ig: ValueSet expansions written by a build that reuses the folder differ from a fresh build (pre-existing).**
In definitions.json.zip, validator*.pack, package-combined.tgz and the ValueSet pages, I2 (second build in the I1 folder) and the watch W2 output have `warning-experimental` and lack `used-valueset` / `includeDefinition` that fresh builds (R1, I1, F2) have. I2 also lacks one INFORMATION message (`The value set expansion is too large` on medication-classification-vs), so hints 9445 vs 9446. Master built twice in one folder is queued to see if master does the same.
Classified: master built twice in one folder (MA1 then MA2) shows the same 41 differing files (expansions, ValueSet pages with the supplement list in the expansion header) and drops the same `expansion too large` INFO (hints 9446 vs 9445). Int I2 vs master MA2 differs only by F1 and list order.

## Compare method notes

- Masks beyond the plan list: run-dependent `fragment-usage-analysis.csv` inside full-ig.zip (timing data); sqlite package.db compared as masked rows; key order in json maps and token order in csv "Used" columns treated as order-only ("used by" lists).
- Order-only diffs seen on both jars between runs (codesystem-ref-*.json/csv, llms.txt, en/ai.zip, package.db row order) are not counted.
- fhir-consent-management: first master R1 hit tx.fhir.org 502s (60 tx errors); redone. tx-error messages are listed separately in each compare.
- niis: master R1 fails under LC_ALL=C (non-ASCII image file name, Java file name encoding ANSI_X3.4-1968); pre-existing on master, related to #25 ws10-nonascii-names. All builds use C.UTF-8.
- emedicinal-product-info: three `comparison-v1.0.0/xx-*.html` pages embed a Java stack trace (master too). Between master and int the frames differ only by line numbers; between watch and full builds by the watch call path (`lambda$execute$0`, `completeBuild`). With the frames masked, nothing is left except F1/F3.
- niis: my first W1 edited `input/pagecontent/description.md`, which is not in the IG page list. The fresh full build F2 of the same sources does not contain the marker either, so watch and full build agree; the test itself was wrong. W1 redone on index.md (second redo, after the disk-full one): output_ok, 8.7 s.
