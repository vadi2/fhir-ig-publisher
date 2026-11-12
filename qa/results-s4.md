# QA results, shard s4

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673 (sha256 49133bd5ec0f188b73dde7e000bcda234df1db6e26f8d487e98e9a1f87f83890)
- VM: 4 CPU, 15 GB, 16 GB swap file. Every build: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx<heap> -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org`, shared package cache, no -threads.
- Times in seconds: whole java run for R1/I1/I2/F2; for the watch, first build, then each edit to "Finished @" (one watch session, W1..W6 in order).
- W1 page text; W2 profile text (description/short); W3 example field; W4 new example; W5 delete it; W6 revert to clean source. Every step's new text was found in output (OK).
- F6 = I1 (same command on the same clean sources), so W6 is compared to I1 and to R1.
- Comparison: qa.txt messages and counts; every output file after masking dates, times, UUIDs, run folder; archives entry by entry, sqlite files dumped, JSON parsed.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | I2 qa | watch first/W1..W6 | W2 qa | W6 qa | F2 | F2 qa | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7Chile/MHD-CL | 360872e | 7600m | 133 | 0/28/5 | 67 | 0/28/5 | 55 | 0/27/5 | 57 / 3 / 33 / 31 / 34 / 33 / 30 | 0/27/5 | 0/27/5 | 73 | 0/28/5 | REGRESSION: llms.txt page order (HL7 #1461, likely intended); pre-existing diffs: SCT edition warning missing on reused folders, package .index.db rows |
| hl7-eu/readi | 0dd3e92 | 7600m | 72 | 165/59/55 | 54 | 165/59/55 | 50 | 165/59/55 | 55 / 2 / 30 / 27 / 40 / 34 / 27 | 165/59/55 | 165/59/55 | 51 | 165/59/55 | REGRESSION: spec.internals drops the .ai.html targets (#48, intended); llms.txt page order (HL7 #1461); pre-existing diffs: THO version in narrative links |
| RIVO-Noord/zorgviewer-ig | aab62e4 | 7600m | 139 | 669/137/128 | 126 | 669/137/128 | 92 | 669/137/128 | 131 / 6 / 90 / 72 / 72 / 69 / 71 | 669/137/128 | 669/137/128 | 115 | 669/137/128 | REGRESSION: llms.txt page order (HL7 #1461) only; pre-existing diffs: watch QA paths under temp/pages/.watch-output, "used by" csv order |
| HL7/davinci-ra | f003bae | 7600m | 188 | 0/0/0 | 96 | 0/0/0 | 86 | 0/0/0 | 93 / 10 / 60 / 50 / 67 / 68 / 61 | 0/0/0 | 0/0/0 | 98 | 0/0/0 | REGRESSION: spec.internals drops .ai.html targets (#48, intended); llms.txt page order (HL7 #1461); pre-existing diffs: THO version and identifier display in narratives |
| HL7/imaging-service-request-ig | def90dc | 7600m | 111 | 195/225/244 | 83 | 195/225/244 | 79 | 195/225/244 | 96 / 2 / 48 / 38 / 68 / 69 / 41 | 195/225/244 | 195/225/244 | 83 | 195/225/244 | REGRESSION: spec.internals drops .ai.html targets (#48, intended); llms.txt page order (HL7 #1461); pre-existing diffs: THO version and identifier display in narratives |
| hl7au/au-fhir-core | 3de0d01 | 7600m | 171 | 62/220/47 |  |  |  |  |  |  |  |  |  |  |
| sfaassn2025-source/base | e11fe94 | 7600m | 354 | 0/3/2 |  |  |  |  |  |  |  |  |  |  |

## Details

### HL7Chile/MHD-CL

- **REGRESSION (order only), HL7 #1461 watch-page-rebuild (e70bb8a)**: `llms.txt`, and `llms.md`/`llms.txt` in `ai.zip` and `full-ig.zip`, list the pages in file name order (index, artifacts, cambios, intercambio, transacciones-mhd) instead of master's `File.listFiles()` order (index, intercambio, cambios, transacciones-mhd, artifacts). Master gives the same order in two fresh builds; I1, I2, W6 and F2 all give the name order. Cause: `AIProcessor.loadPages` now does `Arrays.sort(files)` ("the lists of pages and the zip don't depend on the order the file system lists them in"), added in the merge 46cb7b7 (perf2/ws2-jekyll) on that branch. A jar built from e70bb8a alone gives the name order. Likely intended (deterministic), but it changes output; neither order follows the IG's page order. Repro: full build of HL7Chile/MHD-CL@360872e with master and with e70bb8a, `diff output/llms.txt`.
- pre-existing: "WARNING: IG: The IG is not for the international realm, and it uses SNOMED CT, so it should fix the SCT edition in the expansion parameters" is missing (28 to 27 warnings) in I2, W2 and W6. Master does the same: a second master full build in the same folder (R2m) and the master watch session both have 27. Fresh folders (R1, I1, F2) have 28.
- pre-existing: `package.tgz/package/.index.db` (also in full-ig.zip) has an empty ResourceList in R1 but 8 rows in I1, I2, W6 and a second fresh master build (R1b). Master isn't consistent here, so not from the integration.
- Otherwise identical: W2 output = F2 output, W6 output = I1 output (0 differing files), I1/I2 vs R1 only the above.

### hl7-eu/readi

- **REGRESSION (intended), #48 perf/watch-fsh-tail (9614fa3), commit 349fc124 "spec.internals doesn't list the ai pages the AI step removes"**: `spec.internals` in definitions.{json,xml,ttl}.zip, package.tgz, hl7.eu.fhir.readi.en.tgz, validator.pack, validator-hl7.eu.fhir.readi.pack and full-ig.zip no longer lists the `*.ai.html` targets (StructureDefinition-researchStudy-readi.ai.html etc.). Master lists them although they aren't in its output (0 `.ai.html` files in R1). A jar from 9614fa3 alone gives the same spec.internals as int.jar. Repro: full build of hl7-eu/readi@0dd3e92 with master and 9614fa3, compare `package/other/spec.internals` in output/package.tgz.
- **REGRESSION (order only), HL7 #1461**: llms.txt, en/llms.txt and en/ai.zip page order, as for MHD-CL.
- pre-existing: the narrative of Bundle-readi-sample-1-r4 (json/xml/ttl, its html pages, examples.*.zip, package.db, packages) links NamingSystem-uri to terminology.hl7.org 7.5.0 in some builds and 7.1.0 in others, for master too (R1 and a second fresh master build: 7.5.0; a second master build in the same folder: 7.1.0). int.jar: I1 7.5.0, I2 7.1.0, F2 7.1.0, W2 7.1.0, W6 7.5.0. Not tied to the folder or the jar.
- QA identical everywhere (165/59/55, 3 broken links, same 282 messages). W2 output = F2, W6 output = I1. No 502s.

### RIVO-Noord/zorgviewer-ig

- **REGRESSION (order only), HL7 #1461**: llms.txt and ai.zip page order, as for MHD-CL.
- pre-existing: in the watch builds (W2, W6) the html-check messages name `temp/pages/.watch-output/<page>.html` instead of `output/<page>.html` (12 messages, same text otherwise). A master watch session (W1, W2) gives the same paths.
- not a difference: "index.html ... Line 350, column 2687" vs 2688 in the watch QA: the page embeds the absolute run folder (`background-image: url(<run folder>/temp/pagesdata:image/png...`, master too), and the watch folder name is one character shorter.
- masked: the order of the "used by" lists in codesystem-ref-all-list.csv, codesystem-ref-list.csv, valueset-ref-all-list.csv (and csvs.zip, full-ig.zip) changes between any two builds, master included.
- QA counts identical everywhere (669/137/128, 43 broken links). W2 output = F2 and W6 output = I1 apart from the csv order.
- Rapido: every watch resource edit (W2..W6) was a Complete Build because "terminology server calls failed in the last build (5)" (an STU3 IG with tx calls that fail every time, no 502s); master does the same, and also for W1 (int.jar: Page Rebuild, 6 s, master 72 s).

### HL7/davinci-ra

- **REGRESSION (intended), #48**: spec.internals in definitions.{json,xml}.zip, package.tgz, package.r4.tgz, package.r4b.tgz, hl7.fhir.us.davinci-ra.en.tgz, validator*.pack, full-ig.zip no longer lists the `*.ai.html` targets, as for readi.
- **REGRESSION (order only), HL7 #1461**: llms.txt, en/llms.txt, en/ai.zip page order.
- pre-existing: up to 255 files per pair differ only in narrative text that depends on which terminology.hl7.org version a NamingSystem comes from: link `terminology.hl7.org/5.0.0|7.2.0|7.5.0/NamingSystem-*.html` and display `NPI` vs `United States National Provider Identifier`, `URI` vs `Uniform Resource Identifier (URI)`. Master varies the same way: R1 vs a second fresh master build 247 files, the second fresh vs a master rebuild in the same folder 25 files; with these strings normalised, master vs master 2 and 0 files, and every int.jar pair only the items in this list.
- noise: comparison-v2.0.0/xx-79-... and comparison-v2.1.0/xx-128-ra-parameters-cc-remark-patch... show a Java stack trace (an exception in the comparison, master too); the frames differ with the code and the call path (watch: `completeBuild`).
- masked: "used by" order in valueset-ref-all-list.csv / csvs.zip.
- QA identical everywhere (0/0/0, same suppressed counts). No 502s.

### HL7/imaging-service-request-ig

- **REGRESSION (intended), #48**: spec.internals in definitions.{json,ttl,xml}.zip, package.tgz, hl7.fhir.uv.imaging-service-request-ig.en.tgz, validator*.pack, full-ig.zip no longer lists the `*.ai.html` targets, as for readi.
- **REGRESSION (order only), HL7 #1461**: llms.txt, en/llms.txt, en/ai.zip page order.
- pre-existing: Bundle-example-order-bundle and ImagingStudy-example-requested-procedure-imaging-study (json/ttl/xml, en/*.html, .md, examples.*.zip, packages) differ only in the NamingSystem-dui narrative link: `terminology.hl7.org/7.5.0` vs `5.0.0` and display `DICOM Unique Id` vs `DUI`. Same mechanism as davinci-ra, where master varies the same way.
- pre-existing: package.db nondeterministic content, as for the other IGs.
- masked: "used by" order in valueset-ref-all-list.csv, codesystem-ref-all-list.csv, csvs.zip.
- QA identical everywhere (195/225/244, 669 messages). No 502s.
