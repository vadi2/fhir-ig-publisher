# QA round 2 results, shard s5

Status: Phase C in progress (4 of 8 IGs done: tx-eco, cds-hooks, pocd, eu-lab; no regression so far, one open item on pocd being rechecked with master).

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: qa/page-only-ready 5111080dd56c99f6aa6b1ad62a56978f9d0959bb (seen 17:12 UTC)
- VM: 4 CPU, 15 GB, swap file 6 GB (not 16: about 30 GB writable disk). All runs with LC_ALL=C.UTF-8 (nw-gmsa needs a UTF-8 locale, see round 1).
- eu-lab: SUSHI can't fetch hl7.fhir.eu.extensions.r4#current (as in round 1); seeded ~/.fhir/packages from the publisher's copy (date 20261029172704), same package for every build.
- Templates: every IG's template chain ends in fhir2.base.template, whose config.json declares ant script targets onLoad/onGenerate/onCheck (scripts/ant.xml); hl7.base.template overrides the script with ant-hl7.xml, hl7.davinci.template with ant-davinci.xml. So all 8 IGs have ant script steps.
- Times in seconds: full builds = wall clock of the java run; W steps = edit to "Finished @" in the log (1 s polling), int watch (W) and master watch (WM) with the same specs and heap. W0 = first watch build.
- Feature coverage: the rework only does a page-only rebuild when the template declares `script-inputs` in config.json; no template in this shard does, so every int W step logs `Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))` and then runs the usual Rapido logic (differential or complete build, as on master). That fallback is what plan step 6 asks for, and it holds for all IGs so far. To exercise the page rebuild itself, an extra variant runs after the plan's steps on tx-eco, pocd and eu-lab with a local template that adds `"script-inputs": ["input/images-source", "package-list.json", "publication-request.json"]` (what the fhir2.base/hl7.base ant scripts read from the IG folder), comparing every page step with a fresh full build.
- Compare: cmp.py masks dates/times, run path, UUIDs, sqlite dumps for .db files, order-insensitive for ref lists/llms; `--known` additionally masks the THO version in terminology.hl7.org links (F4) and Java stack trace line numbers in comparison pages.
- Watch steps per IG are listed below each IG (W1b/W1c/W1d/W3 only where the IG has such a file).

| IG | commit | heap | R1 | R1 qa e/w/h/links | I1 | I2 | W1..W6 (int) | W1..W6 (master) | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/fhir-tx-ecosystem-ig | 6d88f37e | 7600m | 77 | 0/6/15/0 | 38 | 31 | W0 35, W1a 25, W1d 21, W1e 27, W1f 24, W2 22, W4 28, W5 27, W6 23 | W0 50, W1a 39, W1d 39, W1e 40, W1f 41, W2 39, W4 42, W5 47, W6 37 | 33 | 35 | 35 | pre-existing diffs: F3 only (R1 vs I1/I2) |
| HL7/cds-hooks-library | 19a388ba | 7600m | 88 | 0/0/0/0 | 38 | 32 | W0 32, W1a 15, W1d 15, W1e 22, W1f 21, W2 22, W4 22, W5 21, W6 21 | W0 32, W1a 16, W1d 16, W1e 27, W1f 22, W2 23, W4 24, W5 22, W6 24 | 33 | 33 | 35 | pre-existing diffs: F3 only (R1 vs I1/I2) |
| HL7/uv-pocd | 383969ea | 7600m | 60 | 0/0/4/0 | 54 | 49 | W0 62, W1a 25, W1b 27, W1c 24, W1d 24, W1e 42, W1f 42, W2 32, W3 29, W4 42, W5 43, W6 32 | W0 60, W1a 27, W1b 27, W1c 29, W1d 27, W1e 47, W1f 43, W2 33, W3 31, W4 42, W5 43, W6 32 | 56 | 57 | 61 | open: package.db CodeSystemListRefs link (see pocd), master reruns pending |
| hl7-eu/laboratory | 4571ab84 | 7600m | 187 | 5/11/62/0 | 113 | 88 | W0 108, W1a 49, W1b 38, W1c 38, W1d 36, W1e 60, W1f 58, W2 60, W3 48, W4 60, W5 59, W6 56 | W0 110, W1a 53, W1b 40, W1c 39, W1d 39, W1e 61, W1f 61, W2 68, W3 53, W4 66, W5 61, W6 59 | 108 | 105 | 114 | pre-existing diffs: F4 (THO version of NamingSystem-uri link in Bundle/Composition/DiagnosticReport narratives, 186 files), comparison-page stack trace line numbers, F3 |
| HL7/fhir-bfdr | 1f981246 | 7600m | 362 | 291/365/791/7 | | | | | | | | |
| HL7/davinci-pdex-plan-net | 8d9037d0 | 9g | 234 | 11/865/8/0 | 100 | 82 | first watch build fails on a fresh copy (pre-existing, see plan-net); rerun with a full build first pending | same failure | | | | pre-existing diffs (R1 vs I1/I2): FINDING-1 family (NPI display), F3; watch pending |
| aehrc/smart-forms-ig | 95651ff7 | 9g | 283 | 146/294/119/0 | 239 | 205 | W0 250, W1a 159, W1c 148, W1d 155, W1e 208, W1f 199, W2 191, W3 187, W4 186, W5 192, W6 198 | W0 252, W1a 150, W1c 147, W1d 157, W1e 214, W1f 205, W2 197, W3 196, W4 195, W5 195, W6 195 | 234 | 225 | 228 | pre-existing diffs: FINDING-3 (AdministrativeGender map table column order), package.db link to one of the same-URL AdministrativeGender ValueSets (random on master too), F3 |
| nw-gmsa/nw-gmsa.github.com | 3aff5f8f | 9g | 562 | 93/3638/1865/10 | | | | | | | | |

## Notes per IG

### tx-eco (HL7/fhir-tx-ecosystem-ig)
- Steps: W1a conformance.md, W1d menu.xml, W1e/W1f new page qa2-newpage.md added to and removed from the IG resource, W2 FeatureDefinition-TestCasesVersion description, W4/W5 new FeatureDefinition, W6 revert. No html/xml pages, intro files or cross-referenced examples (no W1b/W1c/W3). Every edit showed in output/ and was gone after its removal/revert.
- R1 vs I1, I2: same qa counts and 21 messages; only F3 (en.tgz package/.index.db ResourceList: 0 rows in R1, 3 in I1/I2/F*).
- W1f vs F1, W2 vs F2, W6 vs F6, W6 vs I1: identical after masking (qa same).
- Int watch: every step `Complete Build (the template doesn't declare ...)` then the master Rapido path (Differential 23 of 23 for page/resource edits, Complete for IG change / added or removed files). Master watch: the same Rapido decisions. The master WM times are about 15 s higher per step here (it was the first watch run on the VM; not investigated); for the other IGs master and int times are close.

### cds-hooks (HL7/cds-hooks-library)
- Steps: W1a patient-view.md, W1d include fhir-version-note.md (checked in patient-view and order-dispatch), W1e/W1f new page via sushi-config.yaml pages, W2 CDSHookPatientViewContext description (FSH), W4/W5 new Basic instance (FSH), W6 revert. All edits showed and were removed.
- R1 vs I1, I2: same qa (0 messages); only F3. W1f/W2/W6 vs F1/F2/F6 and W6 vs I1: identical after masking.
- Int watch: all steps fall back as in tx-eco; master watch makes the same decisions (differential 27 of 35 for page edits, complete builds after the sushi config change).

### eu-lab (hl7-eu/laboratory)
- Steps: W1a background.md, W1b map-ehdsdevice.xml, W1c StructureDefinition-Specimen-eu-lab-intro.md, W1d input/includes/img.html (in logicalmodels.html), W1e/W1f page via sushi-config.yaml, W2 Specimen-eu-lab description, W3 given name of pat-lab-example (referenced 28 times), W4/W5 new Basic instance, W6 revert. All showed and were removed.
- R1 vs I1/I2: same qa (65 messages). 188 files differ: all F4 (identifier system urn:ietf:rfc:3986 narrative link to terminology.hl7.org/6.3.0 vs 7.3.0 NamingSystem-uri, in the lab report Bundles/Composition/DiagnosticReport), Java stack trace line numbers in two comparison-v2.1.0/xx-* pages (the publisher's code moved), and F3. Nothing else.
- W1f/W2/W6 vs F1/F2/F6 and W6 vs I1: identical after the `--known` masks (F4 only, about 186 files each time; the THO version also varies between int full builds).
- Template is the local ig-template (base hl7.fhir.template). Int watch falls back on every step; the eu-lab W2 here was a normal Rapido build, not forced complete by failed terminology calls as in round 1.

### pocd (HL7/uv-pocd)
- Steps: W1a guidance.md, W1b downloads.xml, W1c StructureDefinition-NumericObservation-intro.md, W1d menu.xml, W1e/W1f page added to and removed from PoCD.xml, W2 NumericObservation description (xml), W3 operating-hours of Device-PhysioMonitorMdsDevice (parent of the VMD devices), W4/W5 new Device listed in PoCD.xml (find-other-resources is false), W6 revert. All showed and were removed.
- R1 vs I1/I2: same qa (4 messages). Only package.db (and its copy in full-ig.zip): table CodeSystemListRefs, the row for ValueSet observation-interpretation with no version: R1 links `http://hl7.org/fhir/R4/valueset-observation-interpretation.html` ("Observation Interpretation Codes"), I1/I2/F* link the local `ValueSet-observation-interpretation.html` ("Observation interpretation codes").
- W2 vs F2 differs in the same row (W2 has the R4 link, F2 the local one); W1f vs F1, W6 vs F6, W6 vs I1 identical. Every master watch snapshot (W1f, W2, W6) has the R4 link, every int full build the local link, int watch W1f/W6 local and W2 R4. Same class as round 1's package.db CodeSystemListRefs note (s1, US-Core: R4 vs THO link varies between master and int runs). Master reruns (R1b, R1c) are queued to see whether a master full build can produce the local link.

### plan-net (HL7/davinci-pdex-plan-net)
- R1 vs I1/I2: same qa counts and 884 messages. 1258 files differ; after the `--known` masks only F3 is left. The rest is the identifier system display in narratives: R1 (master) renders `http://hl7.org/fhir/sid/us-npi#NPI#...`, I1 and I2 (int) `us-npi#United States National Provider Identifier#...` (round 1's FINDING-1 family; round 1 saw master full builds render the full name too). A master rerun (R1b) is queued to confirm on this round's master.
- Watch: the first `-rapido -watch` build on a fresh copy fails with both jars in the template's ant-davinci.xml:45 ("Could not find file .../output/en/index.html to copy"; the watch build writes its pages to a staging folder, output/ is still empty), then idles. Same as round 1's master. Since master fails too it is not a regression, but it means a fresh-copy watch can't be tested here. Workaround queued: a full build in the copy first, then `-rapido -watch` in the same folder (int and master), with the same steps (W1a implementation.md, W1b examples.xml, W1e/W1f new page in pagecontent (pages are auto-listed), W2 PlannetEndpoint description, W3 Hospital organization name (referenced by 8), W4/W5 new Location, W6 revert).

### smart-forms (aehrc/smart-forms-ig)
- Steps: W1a general-requirements.md, W1c intro-notes ActorDefinition-SHCApp-notes.md, W1d include sd-list-generator.md (in profiles-and-extensions.html), W1e/W1f new page in pagecontent (auto-listed), W2 SHCAllergyIntolerance description (FSH), W3 family name of Patient-pat-sf (json example referenced by the others), W4/W5 new Practitioner json, W6 revert. All edits showed and were removed. W3 shows in Patient-pat-sf.json; my extra check for it in en/Patient-pat-sf.html fails with both jars because the narrative prints name.text, not family (spec error, not a finding).
- Every watch step is a complete build on both jars: int first logs the script-inputs fallback, then Rapido says `Complete Build (terminology server calls failed in the last build (62))` on both, so no differential path here.
- qa counts and the 559 messages are the same in every build and step.
- All remaining diffs (R1 vs I1: 182, R1 vs I2: 24, W1f vs F1: 180, W2 vs F2 and W6 vs F6: 22, W6 vs I1: 180) are in one family: the ValueSet administrative-gender pages and the three Questionnaires that contain it (map table v2/v3 column order, round 1's FINDING-3), expansions.json/xml, and package.db. In package.db the CodeSystemListRefs row for administrative-gender links one of four ValueSets that share the URL: R1 (master) AssemblyInstructions_, I1 PatientDetails_, I2 and F2 the plain one, F1 and F6 AboriginalTorresStraitIslanderHealthCheck_, master watch W1f/W2/W6 AssemblyInstructions_, plain, AssemblyInstructions_. So it is random on master too: pre-existing. Plus F3 in R1 vs I2.
