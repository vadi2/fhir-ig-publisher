# QA results, shard s2

Status: done. 6 IGs run, 2 skipped (SUSHI errors at HEAD). PASS: kip. REGRESSION PR #48 perf/watch-fsh-tail: spec.internals drops *.ai.html targets (aitransparency, be-medication, au-ps, uz-integ) and watch keeps Jekyll resident (uz-integ memory, JVM OOM-killed with 4 GB swap). REGRESSION PR #45 perf3/tail-restart: ig-guidance watch never settles on qa/integration-ready; fixed by f9f0579e (re-test passes). uz-integ watch only W1..W2 (VM disk/memory limits).

- master.jar: origin/master 32046bcdd9b0ec91d93db76f9897b745e406139b
- int.jar: qa/integration-ready 6c1abf463d8ae87d30871898c6abdaa63c7df673
- VM: 4 CPU, 15 GB, 16 GB swap file. Heap 7600m unless noted. Times in seconds, wall clock of the whole java run.
- R1 times of the first IGs include filling the empty package cache.

| IG | commit | heap | R1 | R1 qa e/w/h | I1 | I2 | W1..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| HL7/aitransparency-ig | 6d0a39f | 7600m | 119 | 0/0/0 | 63 | 46 | 1.2/26.6/22.3/36.6/36.4/27.0 (all edits shown) | 57 | 56 | REGRESSION: spec.internals targets drop 43 *.ai.html pages (PR #48 perf/watch-fsh-tail, commit 349fc124, also in base); pre-existing diffs: en.tgz package/.index.db 0 vs 16 rows (master M1 vs R1 differs too), warm tx cache narrative of Bundle-b3c1f2d4 identifier type "MedicareHIC" vs "United States Medicare Number" + qa-txservers use count (master 2nd build same folder shows it too) |
| FHIR/ig-guidance | e77c2fb | 7600m | 72 | 77/53/40 | 44 | 34 | int.jar: not runnable, watch never settles. int + #45 fix f9f0579e: 2.8/24.9/22.8/26.9/25.1/25.7 (all edits shown) | 40 | 42 | REGRESSION (watch) on qa/integration-ready: no -rapido -watch build ever finishes, PR #45 perf3/tail-restart; fixed by f9f0579e (re-test passes); pre-existing diffs: test data factory output (random languages, ids, signatures; second build in same folder adds 36 files), qa-txservers use count |
| hl7-be/medication | 6b4450d | 7600m | 108 | 4/39/93 | 82 | 65 | 1.4/36.6/29.6/52.6/50.0/34.9 (all edits shown) | 69 | 66 | REGRESSION: spec.internals targets drop 50 *.ai.html pages (PR #48, as aitransparency); pre-existing diffs: en.tgz package/.index.db, qa-txservers use count |
| hl7au/au-fhir-ps | 85e161a | 7600m | 229 | 1928/346/2244 | 171 | 110 | 3.8/102.0/97.9/90.7/95.0/83.0 (all edits shown; second try, first hit a tx.fhir.org timeout at W3) | 157 | 216 | REGRESSION: spec.internals targets drop 28 *.ai.html pages (PR #48, as aitransparency); pre-existing diffs: terminology.hl7.org version in identifier links of 2 Bundles varies per build (R1 7.1.0, I1 5.5.0, I2 7.5.0; master M1 7.2.0, M2 5.5.0), Signature when/data timestamps |
| sfaassn2025-source/CRA-IG | 7dc650f | | 25 (fail) | | | | | | | skip: SUSHI 3 errors at HEAD (Claim.supportingInfo category; CodeSystem url conflict with twss base dependency) |
| sfaassn2025-source/VFS-IG | 4afd1fe | | 14 (fail) | | | | | | | skip: SUSHI 1 error at HEAD (twss-case-type CodeSystem url conflict with twss base dependency) |
| KIP-infrastructure/implementation-guide | 5b5773c | 7600m | 236 | 40/2349/3539 | 224 | 227 | 82.8/167.5/162.7/230.0/239.5/169.0 (all edits shown) | 212 | 220 | PASS: only masked diffs (expansion ids/timestamps) and pre-existing en.tgz package/.index.db, qa-txservers use count; no *.ai.html pages so spec.internals is identical |
| uzinfocom-org/digital-health-integration | fa87466 | 7600m | 1197 | 25/363/7650 | 1083 | 965 | W1 30.9 (shown); W2 build finished (Differential) but the snapshot copy ran out of disk on this VM; W3..W6 not run (see notes) | | | REGRESSION: spec.internals targets drop 846 *.ai.html pages (PR #48); REGRESSION (watch memory): Jekyll kept running between watch builds holds 5.5 GB resident, swap peak 7.7 GB vs 2.5 GB on master, JVM OOM-killed at W2 with 4 GB swap (PR #48); pre-existing diffs: terminology.hl7.org version in identifier links (624 files), second build in same folder changes ~140 ValueSet expansion narratives (master M2 same), en.tgz package/.index.db |

## Notes

### aitransparency
- Masked/ignored besides the plan list: java stack frame line numbers in comparison-*/ error pages (the error is the same, frames move with code), fragment-usage-analysis.csv (has a Time (ms) column), comma-separated used-by lists in *-ref-all-list.csv/json.
- REGRESSION repro: full build of HL7/aitransparency-ig 6d0a39f with qa/integration or a jar from PR #48 head 9614fa3. definitions.json.zip!spec.internals (also definitions.xml/ttl.zip, package*.tgz!package/other/spec.internals, validator*.pack) has 248 targets; master has 291. The 43 missing are the *.ai.html pages, which are not in output/ of either build (the AI step deletes them), so the change looks intended (commit "spec.internals doesn't list the ai pages the AI step removes"), but it changes published output.
- Watch: W1..W6 all showed in output. F2 vs W2, F6 vs W6 and I1 vs W6 differ only by the pre-existing warm tx cache narrative (8 Bundle-b3c1f2d4 pages + qa-txservers.html). No stale output.

### ig-guidance
- Each build writes factories/log/BloodPressureGenerator.log under the IG folder; in -rapido -watch that counts as a change and starts a Complete Build. Master has this loop too, but each build finishes ("Finished @" 14 times in 6 min, output updated every ~25 s).
- REGRESSION repro: `-rapido -watch` on FHIR/ig-guidance e77c2fb. qa/integration: 78 Complete Builds in 28 min, every one "Not a Page Rebuild (the last build stopped early for a change ...)", 0 "Finished @", never "Watching for changes". Jar from PR #45 head 14c0cdb: 23 builds, 0 finished in 5 min. PR #48 head 9614fa3: 23 builds, 11 finished (like master). So the tail restart abandons every build when the build itself writes a watched file.
- W1..W6 could not be run with either jar (no build settles). Full builds: R1 vs I1 differs only where R1 vs M1 (second master build) differs, plus qa-txservers.html use count 141 vs 143. I2 vs M2 (second build in same folder): same extra 36 files and same diff set.

- Re-test with the #45 fix: jar from qa/integration-ready 6c1abf4 with perf3/tail-restart f9f0579e merged (clean merge; b872f882 conflicts with integration in WatchSession.java, so f9f0579e was used as the orchestrator allowed), `mvn clean package`. Watch settles: 7 "Finished @", 0 "stopped early", "Watching for changes" after every step. W1 Page Rebuild; W2..W6 Complete Builds because of the BloodPressureGenerator.log hash (known, master too). All edits reached output. (The W2 check first looked at StructureDefinition-test-patient-profile.html, but the template never renders `purpose` there; QAW2S2 is in the same 8 files in W2 and in the fresh F2 build.)
- F2 vs W2 and F6 vs W6: every differing file is in the set that already differs between two master builds (random test data factory output). No stale output.

### be-medication
- R1 vs I1/I2: only spec.internals (387 targets on master, 337 on integration, the 50 *.ai.html) and en.tgz package/.index.db (also differs R1 vs M1).
- Watch: F2 vs W2, F6 vs W6 and I1 vs W6 differ only in qa-txservers.html use count and qa.compare.txt, whose lines name temp/pages/.watch-output/ instead of output/ (same content otherwise; a QA report file). No stale output.

### au-ps
- R1 is the rerun from 10:10 (tx.fhir.org returns errors for AU SNOMED edition and au-core ci-build, same on master; no 502).
- R1 vs I1/I2: only spec.internals (267 targets on master, 239 on integration, the 28 *.ai.html) and the 2 Bundles below. R1 vs M1 and R1 vs M2 show the same 2 Bundles.
- Bundle-aups-gpvisit-retrieval and Bundle-aups-referral-endoconsult-autogen: narrative link to NamingSystem-uri picks a different terminology.hl7.org version each build (7.1.0, 7.2.0, 5.5.0, 7.5.0, master and integration alike), plus Signature.when and the signature data, which embeds the signing time.
- Watch (second try): W1 Page Rebuild; W2..W6 Complete Builds because "terminology server calls failed in the last build (94)" (tx.fhir.org has no AU SNOMED edition; this Rapido rule is on master too). F2 vs W2, F6 vs W6 and I1 vs W6: every differing file is in the set that differs between R1 and the master builds M1/M2 (the 2 Bundles). No stale output.

### kip
- R1 vs I1/I2/M1/M2: expansions.* differ only in Bundle id, lastUpdated and expansion identifier (masked; compare.py reported them because its timestamp mask overlapped the UUID). en.tgz package/.index.db and qa-txservers.html as on other IGs, master too.
- Watch (no examples in this IG: W3 edits a CodeSystem concept, W4 adds a CodeSystem, W5 deletes it): all edits shown. F2 vs W2, F6 vs W6 and I1 vs W6: same masked expansions diff plus qa-txservers use count. No stale output.
- Rapido build kinds: W1 Page Rebuild (83 s, mostly the html checks that cannot be reused), W2/W3/W6 Differential Build (8 to 13 of 1037 files), W4/W5 Complete Build (file added/removed).

### uz-integ
- R1 vs I1: 624 files differ only in the terminology.hl7.org version of the NamingSystem-uri link in example narratives (7.5.0 vs 7.1.0; master M1 also differs from R1 the same way, as au-ps), expansion identifiers (masked), en.tgz package/.index.db, and spec.internals (5155 targets on master, 4309 on integration, the 846 *.ai.html; PR #48 as the other IGs).
- R1 vs I2 (second build in same folder) adds 111 html and 32 json ValueSet expansion narratives ("Expansion from tx.fhir.org based on Loinc v2.83" vs a "based on:" list); master M2 has exactly the same set, so pre-existing. I2 and M2 also each have one ValueSet in definitions.* that differs (a different one in each: screening-ultrasound-breast-symmetry-vs in I2, screening-ultrasound-axillary-node-differentiation-vs in M2); content not inspected (compare snippet cut at 300 chars and the outputs were already deleted for disk).
- Watch memory REGRESSION (PR #48): with -rapido -watch the integration jar keeps Jekyll running between builds (commits e85860f2 "Watch mode: keep Jekyll running, and reuse what the steps after it found" and 26aaf1ac; in the integration base and in PR #48 head 9614fa3, not on master). On this IG that ruby process holds 5.5 GB resident next to a 9 GB JVM (-Xmx7600m).
  - try1, 4 GB swap: JVM OOM-killed by the memory cgroup during W2 (java 9.1 GB RSS, ruby 4.3 GB RSS + 4 GB swapped).
  - try2, 8 GB swap: survived W1, W2 (swap peak 7.7 GB of 8); the W2 snapshot copy then hit the disk limit, so W3..W6 and F2/F6 were not run.
  - master jar, same VM, W1..W2: java peak 9.2 GB, no resident ruby, swap peak 2.5 GB.
  - PR #48 jar (9614fa3) W1..W2: ruby 5.5 GB, swap peak 7.7 GB, as integration.
  - Repro: `-rapido -watch` on uzinfocom-org/digital-health-integration fa87466, -Xmx7600m, 15 GB RAM VM; after the first build watch `ps -C ruby -o rss` and `free`.
  - Watch timings for reference: W1 page edit 31 s integration (Page Rebuild) vs 474 s master (Differential Build); W2 profile edit about 570 s integration vs 928 s master.
- W1 and W2 edits reached output in all three watch runs.

