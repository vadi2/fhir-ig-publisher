# QA results, shard s5

Status: in progress (Phase C)

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, 15 GB, swap file 4 GB (shrunk from 16 GB, then 10 GB, to free disk; swap stayed unused). At about 10:05 the disk filled during the bfdr watch run (W0 died with "No space left on device"); old run outputs were deleted, the swap shrunk, and bfdr Phase C (I1 onward) restarted from scratch at 10:32. Only res/ manifests are kept for finished IGs.. Times in seconds, wall clock of the whole java run (W*: edit to "Finished @"; W0 = first watch build).
- Watch sequence: one watch session runs W1..W6 without a restart; its output is snapshotted after each step. F2 and F6 are separate fresh full builds run afterwards with the W2 and W6 edits applied.
- Compare: file set + content after masking dates/times, run path, UUIDs; qa counts + message set from qa.xml.
- eu-lab: SUSHI couldn't fetch hl7.fhir.eu.extensions.r4#current, so it was seeded from build.fhir.org (1.4.0-ci, 2026-10-29) before R1; same package for every build.
- tx-eco and cds-hooks have no example referenced from another resource, so they have no W3. tx-eco has no FSH; its W2/W4 edit a FeatureDefinition, rendered as Binary-FeatureDefinition-*.json.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I1 qa | I2 | W0, W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/fhir-tx-ecosystem-ig | 29546b35 | 7600m | 92 | 0/6/15 | 40 | 0/6/15 | 36 | 37, 2.4/26/-/33/33/24 | 42 | 40 | same |
| HL7/cds-hooks-library | 8afbe0fe | 7600m | 91 | 7/0/0 | 43 | 7/0/0 | 34 | 39, 1.0/21/-/27/25/20 | 43 | 39 | same |
| HL7/uv-pocd | 8e377d42 | 7600m | 77 | 0/0/4 | 74 | 0/0/4 | 62 | 78, 1.2/28/24/63/58/29 | 71 | 71 | same |
| hl7-eu/laboratory | 4571ab84 | 7600m | 168 | 5/11/62 | 130 | 5/11/62 | 91 | 136, 3.6/77/65/64/72/61 | 112 | 114 | pre-existing diffs: FINDING-1 (NamingSystem link version for urn:ietf:rfc:3986 varies between runs on master too), UUIDs, used-by order |
| HL7/fhir-bfdr | 1f981246 | 7600m | 406 | 291/365/791 | 291 | 291/365/791 | 278 | 297, 7.1/208/185/246/265/212 | 290 | 287 | pending: FINDING-1 family (identifier system display SSN/NPI varies), master rerun queued |
| HL7/davinci-pdex-plan-net | 8d9037d0 | 9g | 275 | 11/865/8 | 104 | 11/865/8 | 90 | 101, 5.6/55/51/76/77/54 | 94 | 117 | pending: FINDING-2 (watch differential build changes a link in package-combined.tgz), master watch rerun queued |
| aehrc/smart-forms-ig | 95651ff7 | 9g | 309 | 146/294/119 | 280 | 146/294/119 | 256 | 267, 4.1/249/235/240/246/248 | 273 | 265 | pre-existing diffs: FINDING-3 (column order of the AdministrativeGender map table varies between runs on master too), used-by order |
| nw-gmsa/nw-gmsa.github.com | 7ec7380b | 9g | 553 | 85/3543/1674 | 545 | 85/3543/1674 | 537 | 642, 13/522/390/515/552/439 | 562 | 557 | pending: FINDING-2 (same pattern as plan-net), master watch rerun queued |

## Notes per IG

### tx-eco
- I1, I2, F6 vs R1: same qa counts and messages (21). 31 files differ: build timestamps inside binaries (validator.pack, package.tgz, .index.db, package.db, xlsx entry times), timings in fragment-usage-analysis.csv, and llms.txt / ai.zip page list order.
- llms.txt order: int lists the pages in name order in every build; master in file system order. Intended: AIProcessor sorts the files ("in name order: the lists of pages and the zip don't depend on the order the file system lists them in"), from the ws2-jekyll merge inside HL7 #1461 watch-page-rebuild. Not a regression.
- W2 vs F2, W6 vs F6, W6 vs I1: same qa and messages; 22 files differ, all build timestamps and timings as above.
- Watch: W1 page rebuild in 2.4 s, the edit is in en/ecosystem.html (multilingual IG; root ecosystem.html is the language redirect). W2 differential build, W4/W5 complete builds, new file appears and is gone again; W6 removes both edits.

### cds-hooks
- I1, I2, F6 vs R1: same qa counts and messages (7). 55 files differ: build timestamps inside binaries (xlsx, packages, spec.internals, packs, package.db), fragment-usage-analysis.csv timings, and llms.txt / en/llms.txt / ai.zip page order (same entries, name order as in tx-eco).
- W2 vs F2, W6 vs F6, W6 vs I1: same qa and messages; 46 files differ, timestamps and timings only.
- Watch: W1 page rebuild 1.0 s (edit in en/patient-view.html), W2 differential build (28 of 35 files), W4/W5 complete builds add and remove Basic-QaNewW4; W6 removes both edits.

### pocd
- I1, I2, F6 vs R1: same qa counts and messages (4). 143 files differ: timestamps inside binaries (xlsx, packages, packs, spec.internals), fragment-usage-analysis.csv timings, llms.txt page order, and the "used by" order in valueset-ref-list / valueset-ref-all-list (csv, json) and codesystem-ref-all-list.json (same entries, checked with an order-insensitive compare).
- W2 vs F2, W6 vs F6, W6 vs I1: same qa and messages; the same classes of diffs (used-by order also varies between two int builds).
- Watch: W1 page rebuild 1.2 s (en/guidance.html), W2 differential (47 of 103 files), W3 differential (70 of 103), W4/W5 complete builds add and remove Patient-QaNewW4; W6 removes all edits.

### nw-gmsa
- Built with LC_ALL=C.UTF-8 (all runs, both jars). Under the VM's POSIX locale master R1 stops after 80 s: "Unable to read input/images/NW GLH Genomic Testing Request Form \u2013 Rare Disease.png: its name can't be decoded with the file name encoding 'ANSI_X3.4-1968' ... run the IG Publisher with a UTF-8 locale". An int run under the POSIX locale is queued as an extra data point (#25 touches non-ASCII names).
- qa counts and messages (3247, 10 broken links) same in every build and watch step.
- R1 vs int: R1 output was removed (disk), manifests only. Apart from timestamps, ref list order and llms order, the differing files are the examples Observation-m2-diagnostic-implication, Observation-brca1-variant and Bundle-ctdna9737383222-eulab-document. Their hash changes in nearly every build, including watch steps that don't touch them, and W6 vs F6 is identical after the UUID mask: random UUIDs in the narrative.
- FINDING-2 again: after each Rapido differential build (W3, W6) package-combined.tgz holds a different StructureDefinition-Composition-eu-lab-2.1.0.json: the snapshot definition of element 82 loses its trailing whitespace. R1, I1, F2, F6 and the complete watch builds (W2, W4, W5) all have the master hash. Everything else in W6 vs F6 is the same after masking.
- Watch: W1 page rebuild 13 s (root check fails falsely), W2 complete build (terminology calls failed before, by design), W3 differential (398 of 601 files), W4/W5 complete builds add and remove Basic-QaNewW4; W6 removes all edits.

### smart-forms
- qa counts and messages (559) same in every build and watch step.
- R1 vs F6 (both outputs kept): 149 differing text files and archive entries; 35 are "used by" order and llms.txt order. The other 114 are ValueSet-administrative-gender and the Questionnaires AboriginalTorresStraitIslanderHealthCheck, AssemblyInstructions and PatientDetails (json/xml/ttl, html, and copies in packages and zips): the narrative table of the AdministrativeGender v3 map has its columns in a different order (same cells).
- FINDING-3: that order is not stable within int either. Hash of ValueSet-administrative-gender.json: R1 = F2 = W1..W4, I1 = I2 = F6 = W5 = W6. Classified PRE-EXISTING: master R1b has the R1 order, master R1c has the I1/F6 order.
- W6 vs F6: same after masking except ref list order.
- Watch: W1 page rebuild 4.1 s (en/general-requirements.html; root check fails falsely). W2..W6 are all complete builds because terminology calls fail in each build (by design), so the differential path is not exercised. W4/W5 add and remove Practitioner-QaNewW4; W6 removes all edits.

### plan-net
- qa counts and messages (884) same in every build and watch step.
- R1 vs I1 content (R1 output kept this time): all 1246 differing text files and archive entries are the same after masking, except "used by" order in the ref lists and llms.txt page order (intended, see tx-eco).
- FINDING-2: after each Rapido differential build (W2, W3, W6) package-combined.tgz holds a different StructureDefinition-us-core-location-7.0.0.json and StructureDefinition-us-core-organization-7.0.0.json: the snapshot comment of the address element links "[Location](location.html#)" where every full build (R1, I1, F2, F6) and the complete watch builds (W4, W5) have "[Location](http://hl7.org/fhir/R4/location.html#)". W6 vs F6: everything else same after masking (ref list order only). Classification pending: the same watch sequence with master.jar is queued; if master differs, the PR will be found with single PR head jars after the builds finish.
- Watch: W1 page rebuild 5.6 s (en/implementation.html; root check fails falsely), W2 and W3 differential, W4/W5 complete builds add and remove Basic-QaNewW4; W6 removes all edits.

### bfdr
- qa counts (291/365/791, 7 broken links) and the 1441 messages same in every build and watch step.
- FINDING-1 family: the display of identifier systems in narratives varies. F6 (int full build) renders every SSN and NPI identifier with the full name ("United States Social Security Number#...", 212x; "us-npi#United States National Provider Identifier#", 648x). W6 (int watch, after reverting all edits) renders 12 SSN identifiers as "SSN#" and 568 NPI as "us-npi#NPI#" and the rest with the full name. Affected: en/ narratives of Patient-patient-mother-jada-ann-quinn-w-edit, RelatedPerson-relatedperson-mother-jada-ann-quinn, Observation-observation-planned-to-deliver-at-home-babyg-quinn and the NPI pages, their json/xml/ttl/md and copies in full-ig.zip, packages and ai.zip. Same cause class as eu-lab (which NamingSystem the identifier system resolves to). R1 output was not kept (disk), so a master rerun is queued to see what master renders.
- W6 vs F6 inner check over all 6875 differing text files and archive entries: 6713 same after masking UUIDs, build timestamps and the identifier display / THO version, 20 differ only in "used by" order (codesystem/valueset ref lists), 142 are us-core-comparisons/xx-* pages that embed the pre-existing exception "Found a slice at 'Observation.category' but there was no definition for the slicing", with stack frames that differ by call path.
- Watch: W1 page rebuild 7.1 s (edit in en/usage.html; root is the language redirect, so the root check fails falsely), W2 differential (299 of 524 files), W3 differential (416 of 524), W4/W5 complete builds add and remove Basic-QaNewW4; W6 removes all edits.

### eu-lab
- qa counts and messages (65) same in every build and watch step.
- FINDING-1: the narrative link for identifier system urn:ietf:rfc:3986 ("Uniform Resource Identifier (URI)") points to a different hl7.terminology.r4 version. R1 (master): terminology.hl7.org/7.1.0/NamingSystem-uri.html; I1, I2, F2, F6 (int): 6.2.0 (24 html pages plus the narratives in Bundle/Composition/DiagnosticReport json/xml/ttl/md). The IG loads several THO versions (7.5.0, 7.1.0, 7.2.0, 6.2.0, 7.3.0, 6.3.0). In the watch session Bundle-BundleHepatitisPanel.json changes at every step: W1, W2, W4 each differ from all full builds, W5 equals R1 (7.1.0), W6 equals I1 (6.2.0). So the version the link points to is not stable. Classified PRE-EXISTING: master full builds R1, R1b, R1c link 7.1.0, 6.3.0, 7.1.0. In a master watch sequence (WM) the Bundle links 7.5.0, 7.3.0, 6.3.0, 6.3.0, 7.1.0, 6.3.0, 7.5.0 at W0..W6; a second int watch sequence (WI) 7.2.0, 7.2.0, 7.3.0 x4, 6.2.0. Which of the loaded hl7.terminology.r4 versions the NamingSystem resolves to is not deterministic on master.
- Other diffs: random UUIDs in comparison-v2.1.0 table scripts (cb/pp prefixed, not masked by the manifest; identical after masking), comparison-v2.1.0/xx-* pages embed the same pre-existing exception ("Found a slice at 'Composition.section' but there was no definition for the slicing") whose stack differs by call path in watch, "used by" order in ref lists, timestamps, timings.
- Watch: W1 page rebuild 3.6 s; W2 complete build because 5 terminology calls failed in the build before (by design); W3 differential (87 of 141); W4/W5 complete builds add and remove Basic-QaNewW4; W6 removes all edits. All step checks pass.
