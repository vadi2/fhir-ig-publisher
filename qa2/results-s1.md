# QA round 2 results, shard s1

Status: Phase C in progress (6 of 8 IGs done); Phase D (added by the orchestrator 18:20) queued after Phase C.

- master.jar: HL7 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb (seen 17:09 UTC)
- VM: 4 CPU, 15 GB, swap file 8 GB (16 GB does not fit the ~30 GB disk allowance). All builds LC_ALL=C.UTF-8, IG_GATE_OFF=1, MALLOC_ARENA_MAX=2, -XX:+UseStringDeduplication, no -threads. Times in seconds (R*/I*/F*: whole java run; W*: edit to "Finished @").
- R1 outputs kept as output.tar.xz + sha256 manifest; compare masks per plan rule 4 (dates, run path, UUIDs, lastUpdated, qa files, order-only lists).

## Phase A (master R1)

| IG | commit | heap | R1 s | R1 qa e/w/h | output files |
|---|---|---|---|---|---|
| ChitSR/sample-ig | 0701fa3 | 7600m | 64 | 1/6/3 | 1252 |
| HL7/demo-collect | ca65805 | 7600m | 95 | 0/0/0 | 2315 |
| costateixeira/spenser | 21a4640 | 7600m | 65 | 58/20/139 | 4689 |
| HL7/fhir-consent-management | aad3563 | 7600m | 156 | 2/0/0 | 3161 |
| HL7/emedicinal-product-info | 7ab31d3 | 7600m | 169 | 5/0/21 | 5564 |
| hyweb-cdc/niis | b343858 | 7600m | 128 | 0/0/0 | 1340 |
| uzinfocom-org/digital-health-ig | f0ae8ac | 7600m | 794 | 5/268/9543 | 23017 |
| HL7/US-Core | d85ba3f | 10g | 697 | 7/17/1 | 17145 |

No IG in s1 is skipped (TW Core is s6; the two SUSHI failures are s2).

## Phase C (int.jar)

Watch: S1 = W1a..W1f, stop (F1); restart, W2, stop (F2); restart, W3..W6, stop (F6). A W1 step is left out when the IG has no such file that reaches output. Every edit was checked in output/ (marker present; after W1f/W5 the page gone; after W6 all markers absent). Master watch (MW) is the same spec with master.jar, same heap. Mode: int first logs a page-rebuild decision; "gate: scripts" = it logged `Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))`, i.e. no page-only rebuild, then the normal Rapido build ran (Differential or Complete, same as master).

| IG | heap | R1 | I1 | I2 | qa e/w/h R1=I1=I2 | W int (s, mode) | W master (s, mode) | F1/F2/F6 | template scripts | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| ChitSR/sample-ig | 7600m | 64 | 34 | 34 | 1/6/3 | W1a 21.6, W1c 20.0, W1d 19.7, W1e 23.2, W1f 21.2, W2 22.0, W3 21.4, W4 24.1, W5 21.3, W6 19.5 (every step gate: scripts) | W1a 20.5, W1c 20.0, W1d 20.1 (Differential), W1e 24.6, W1f 21.0 (Complete), W2 21.8, W3 22.3 (Differential), W4 23.9, W5 22.0 (Complete), W6 20.1 (Differential) | 33/33/33 | yes (hl7.au.fhir.template): no page-only rebuild, as intended | same; pre-existing F3 (R1 vs I1/I2), F2 (watch qa.txt paths) |
| HL7/demo-collect | 7600m | 95 | 44 | 37 | 0/0/0 | W1a 18.1, W1d 22.2 (gate: scripts, then Differential), W1e 29.2, W1f 26.7, W2 29.1, W3 31.7, W4 28.6, W5 26.5, W6 27.9 (gate: scripts, then Complete changed: ig) | W1a 20.7, W1d 25.1 (Differential), W1e 31.1, W1f 30.6, W2 32.9, W3 33.8, W4 30.6, W5 29.1, W6 28.4 (Complete, changed: ig) | 45/45/47 | yes (local ig-template): no page-only rebuild, as intended | same; pre-existing F3 (R1 vs I1/I2) |
| costateixeira/spenser | 7600m | 65 | 45 | 50 | 58/20/139 | W1a 23.9, W1d 23.7 (gate: scripts, then Differential 49/75), W1e 39.0, W1f 35.6, W2 41.6 (Complete), W3 29.2 (Differential 54/75), W4 38.6, W5 34.4, W6 36.1 (Complete) | W1a 24.7, W1d 22.6 (Differential), W1e 39.0, W1f 35.0, W2 42.3 (Complete), W3 32.6 (Differential), W4 47.4, W5 41.8, W6 38.8 (Complete) | 43/47/53 | yes (local ig-template): no page-only rebuild, as intended | same; pre-existing F3 (R1 vs I1/I2) |
| HL7/fhir-consent-management | 7600m | 156 | 64 | 58 | 2/0/0 | W1a 32.0, W1c 29.7 (gate: scripts, then Differential 51/106), W1e 45.3, W1f 41.9, W2 50.3 (Complete), W3 42.4 (Differential 64/106), W4 48.4, W5 46.8, W6 43.6 (Complete) | W1a 31.6, W1c 33.4 (Differential), W1e 48.2, W1f 44.8, W2 52.8 (Complete), W3 41.2 (Differential), W4 62.7, W5 49.6, W6 46.2 (Complete) | 69/68/64 | yes (hl7.fast.template): no page-only rebuild, as intended | same; pre-existing F4 (NPI link version/label), F3 |
| HL7/emedicinal-product-info | 7600m | 169 | 121 | 107 | R1 5/0/21, I1=I2 5/0/0 (R1 tx error, see notes) | W1a 52.6, W1d 43.3 (gate: scripts, then Differential 56/194), W1e 90.4, W1f 88.8, W2 96.8 (Complete), W3 79.6 (Differential 83/194), W4 92.0, W5 94.4, W6 100.3 (Complete) | W1a 51.9, W1d 48.4 (Differential), W1e 98.5, W1f 99.8, W2 98.1 (Complete), W3 78.5 (Differential), W4 92.5, W5 87.8, W6 93.7 (Complete) | 108/110/116 | yes (hl7.fhir.template): no page-only rebuild, as intended | same; pre-existing F3, comparison-page stack traces, master R1 tx flake |
| hyweb-cdc/niis | 7600m | 128 | 99 | 89 | 0/0/0 | W1a 53.6, W1b 51.4, W1d 48.7 (gate: scripts, then Differential), W1e 66.3, W1f 63.8, W2 74.2 (Complete), W3 65.5 (Differential), W4 72.4, W5 69.7, W6 70.1 (Complete) | W1a 51.5, W1b 47.4, W1d 46.6 (Differential), W1e 67.1, W1f 72.9, W2 71.6 (Complete), W3 64.3 (Differential), W4 71.6, W5 67.5, W6 68.6 (Complete) | 86/87/94 | yes (local custom-template on fhir.base): no page-only rebuild, as intended | same; F3, plus run-to-run column order in one ValueSet expansion (N1, see notes) |

### Per-IG notes

- sample-ig: W1b n/a (background.xml is not listed in ig.xml, so it makes no page). R1-I1 and R1-I2: only package/.index.db ResourceList rows (F3: R1 0 rows, int 2) in the en.tgz and its copy in full-ig.zip. W1f-F1, W2-F2, W6-F6, I1-W6: no output diffs; qa counts equal; message sets differ only by F2 (2 html-check messages name temp/pages/.watch-output/ instead of output/).
- demo-collect: W1b/W1c n/a (no html/xml page, no intro/notes); W1d = input/data/features.yml footer label. R1-I1, R1-I2: only .index.db ResourceList (F3: R1 0 rows, int 29). W1f-F1, W2-F2, W6-F6, I1-W6: no diffs, qa message sets equal.
- spenser (en/nl/pt/fr): W1b/W1c n/a. R1-I1, R1-I2: only .index.db ResourceList in jct.fhir.spenser.fr.tgz (F3, as in round 1). W1f-F1, W2-F2, W6-F6, I1-W6: no diffs, qa message sets equal.
- fhir-consent-management: W1b/W1d n/a (no html/xml page; the two includes are not read by any page or the template). Unmasked, R1-I1/R1-I2 have 168 differing files and W6-F6 162: all are F4 (NamingSystem-npi link `terminology.hl7.org/5.0.0` label "NPI" vs `6.5.0` "United States National Provider Identifier", in example narratives, their json/xml/ttl/md pages and ai.zip). With that version and label masked: R1-I1, R1-I2 only .index.db (F3); W1f-F1, W2-F2, W6-F6, I1-W6 no diffs except "used by" order in codesystem/valueset-ref-all-list.csv (order-only). qa message sets equal everywhere. W3 checked in Consent-ConsentExample.json (the narrative does not show the identifier).
- emedicinal-product-info: W1b/W1c n/a; W1d = input/includes/fragment-css.html (the template includes it in every page; fragment-post-footer.html, my first choice, is not read by the template). W3 checked in Organization-org-wonderpharma.json (narrative does not show the address line). R1 has 21 INFORMATION messages that I1/I2/all F builds lack: `...language: The value provided ('en') was not found in the value set 'Language Value Set (two-letter code)' ... (error message = Error converting output stream to byte array)`, one per Bundle/Composition; that is a terminology call failing during master R1 (round 1 R1 also had 0 hints), not int. Output: with UUIDs (incl. `hc<uuid>` anchors) and F4 masked, R1-I1/R1-I2 differ only in 3 comparison-v1.0.0/xx-*.html pages (+ copies in full-ig.zip), which embed Java stack traces whose line numbers differ (`Publisher.execute(Publisher.java:243)` vs `233`; same as round 1), and .index.db (F3). W1f-F1, W2-F2, W6-F6, I1-W6: only those 3 stack-trace pages. qa message sets equal for all int builds.
- niis: W1c n/a (no intro/notes files; input/includes/quickstart-*.md are not read). W1b = input/pagecontent/downloads.xml. All checks ok (round 1 had W1 MISSING on niis; not seen here). N1: the only content diff in R1-I1, R1-I2, W1f-F1 and I1-W6 is ValueSet-twniis-operationoutcome-values.html and expansions.json (+ copies in expansions.json.zip and full-ig.zip): the two ConceptMap columns of the expansion table (IssueType, IssueSeverity) come in either order. The order varies between fresh int builds of the same sources (first column: R1 severity, I1 type, I2 type, F1/F2/F6 severity, watch S1 type, S2/S3 severity), so it is run-to-run ordering, not stale watch output; a second master build to show master varying too is queued. W2-F2 and W6-F6: no diffs. qa message sets equal everywhere.
