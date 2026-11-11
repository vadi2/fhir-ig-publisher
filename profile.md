# Profile: `-rapido -watch` rebuilds on uz.dhp.core (base a4d1e90, 15 GB / 4 CPU VM)

All numbers are from this VM.
- Session A: base + print-only timing marks (thread CPU, process CPU, GC time at ~150 points per build), `-Xmx7600m -XX:+UseStringDeduplication`, no `-threads`; 3 profile + 3 instance + 3 page rebuilds after a warm first build (670 s). Medians.
- JFR hot frames (the percentages) are from a second session, one profile rebuild. Network is from a logging CONNECT proxy in session A.
- The heap is tight on this VM (see bench-recipe.md): GC pauses are 27 to 35 s of an FSH rebuild and sit inside the phases (gc column).
- This VM is about 2.5x slower than the 12-core reference in PROCESS.md (SUSHI 51 s here, ~16 s there).
- `-threads 3` (one session, base jar): first profile rebuild 263.8 s, native outputs 17.5 s instead of 28.7; the second rebuild died with OutOfMemoryError in native outputs (heap too small for the workers). Everything below is without `-threads`.

## 1. Wall time (s, median of 3; min to max)
| edit | save to "Finished @" | save to file updated in output/ | Jekyll has it in staging at | detection | Rapido |
|---|---|---|---|---|---|
| profile (FSH) | 282.8 (270.6 to 290.0) | 279.8 | 172.2 | 0.4 | 136 of 811 render, 120 validate |
| instance (FSH) | 260.3 (249.6 to 266.8) | 255.4 | 159.9 | 0.4 | 112 of 811 render, 97 validate |
| page (md) | 19.4 (18.2 to 20.0) | 17.9 | 2.6 | 0.4 | Page Rebuild (1 page) |

output/ only changes in the watch output sync, the last step, so today "page updated" = "Finished" minus 2 to 5 s.

## 2. Phases in pipeline order (s). wall / main-thread CPU / GC pause are the profile edit; inst and page are wall
Scope: ALL = cost follows the whole IG, CHG = follows what changed. Cores: the publisher's own work is 1 core (main) everywhere except full-ig.zip (1 worker); process CPU is 1.5 to 3.5 cores in most phases, but the rest is G1/JIT under the tight heap. SUSHI and Jekyll are 1 core each (3 idle).
| phase | wall | main | gc | inst | page | scope, notes |
|---|---|---|---|---|---|---|
| change detection (inotify, 250 ms quiet, snapshot diff) | 0.4 | | | 0.4 | 0.4 | ALL (hashes input/), small |
| page-rebuild decision, drop kept state, reset (page: restore kept state, copy the page to temp 0.7) | 0.7 | 0.1 | | 0.6 | 0.8 | |
| SUSHI: start 1.8, import 6.8, load packages 5.7 (0.9 is a `latest` lookup), convert 36.0 (SD 12.6, CS 6.7, VS 3.8, instances 12.7), write 0.7, IG 0.4 | 51.4 | 0 | | 47.1 | skipped | ALL: every FSH file every time |
| ig.ini, template load + scan | 2.1 | 1.1 | | 1.3 | | ALL, same every rebuild |
| core package, tx connect, dependency packages (tx cache files are read lazily) | 3.5 | 2.4 | 0.3 | 3.0 | | ALL, same every rebuild |
| load IG: scan + parse every source file (SimpleFetcher.scan 52 %, loadFromJson 43 %) | 3.1 | 2.9 | 0.2 | 3.4 | | ALL |
| process loaded resources (convertToElement 58 %, parse 18 %) | 3.7 | 2.8 | 0.8 | 3.7 | | ALL |
| snapshots; package-list (cached per session) | 1.1 | 0.9 | 0.1 | 1.2 | | ALL |
| Rapido: read state.json (188 MB, ~47 % of samples = ~2.6 s), hash every resource (~35 %), decide | 5.5 | 4.0 | 0.3 | 5.6 | | ALL; state is read back from disk every rebuild |
| language, dependency info, paths, R4/R4B, comparison ids | 1.2 | 0.9 | 0.2 | 1.1 | | ALL |
| narratives (convertToElement 64 %, rendering 35 %) | 13.6 | 11.8 | 2.1 | 13.9 | | ALL: no reuse |
| validate conformance resources (incl. ~1 s packages2.fhir.org lookups) | 6.0 | 4.2 | 0.2 | 4.0 | | CHG (reused 691 of 718) + lookups |
| profile/CS checks; validate instances + the IG resource | 2.2 | 2.0 | 0.1 | 2.0 | | mostly CHG |
| translation artifacts (PublisherTranslator.genTranslations) | 5.1 | 3.8 | 0.3 | 5.1 | | ALL |
| unattributed CS scan, update IG, data file, constants, views | 0.9 | 0.5 | | 0.7 | | ALL |
| native outputs | 28.7 | 24.3 | 1.2 | 27.3 | | ALL (other session) |
| summaries before, per language | 1.0 | 1.0 | | 0.9 | | ALL (other session) |
| template onGenerate ant script | 3.1 | 1.5 | 0.1 | 2.9 | | ALL |
| html fragments, 3 languages in one pass per file: canReplay re-reads and hashes every output of every clean file (47 % of samples), replay reads recorded expansions (18 %), real generation of dirty files (rest) | 22.9 | 14.7 | 3.9 | 19.3 | | replay ALL, generation CHG; 4 s is file IO wait |
| spreadsheets (replay) | 3.4 | 3.0 | 0.2 | 3.2 | | ALL |
| summaries after: en 2.6, ru 1.2, uz 1.1, base pages 0.4 | 5.4 | 3.3 | 0.3 | 4.9 | | ALL (other session) |
| Rapido outputs record, clean temp, explicit System.gc() 2.4 | 3.1 | 0.5 | 2.4 | 2.8 | | ALL |
| search form, onJekyll script, list temp files; usage stats | 0.5 | 0.5 | | 1.0 | | ALL |
| Jekyll, kept process: temp diff 1.3 + render 1.2 to 4.1 (44 to 45 files); full site in the first build 72.6 | 2.9 | 1.2 | | 3.1 | 1.5 | CHG |
| onCheck script, spec.internals, package.tgz (0.06) | 0.2 | 0.1 | | 0.2 | 0.4 | |
| combined package (PackageReGenerator, core) | 11.5 | 7.3 | 3.1 | 8.9 | reused | ALL |
| install package.tgz in the package cache (dev + branch) | 2.1 | 1.0 | | 2.1 | reused | ALL |
| generateZips: definitions.json 6.6 (examples.json 0.03, no xml/ttl here) | 6.6 | 5.5 | 0.2 | 5.9 | reused | ALL (other session) |
| generateZips: expansions | 11.6 | 11.4 | 0.2 | 11.4 | reused | ALL (other session) |
| generateZips: validator.pack 5.8, csv/excel/schematron 1.1 | 6.9 | 6.5 | 0.1 | 6.6 | reused | ALL (other session) |
| AI step (readResources 38 %, ai.zip 26 %) | 7.4 | 6.2 | 0.3 | 7.3 | 1.8 | mostly ALL |
| html check: list 20683 files, replay 20613 records, load + check 70 (real check in the first build: 104) | 25.1 | 7.5 | 16.6 | 18.4 | 1.8 | ALL for listing/replay; GC-bound here |
| full-ig.zip (main parked, 1 worker; unchanged entries copied; first build 43) | 15.3 | 1.0 | | 14.3 | 2.3 | ALL (rewrites the whole zip) |
| QA prep: draft dependencies scan 86 %, publication check, IG publisher version lookup (network), ip review | 9.4 | 8.3 | 0.2 | 9.1 | 3.7 | ALL |
| qa.html, qa.json | 2.3 | 2.2 | | 2.6 | 1.5 | ALL |
| Rapido state save (188 MB written; not for page rebuilds) | 2.1 | 1.8 | | 2.0 | 0 | ALL |
| watch output sync: content compare of 20.6k files, 2.2 GB twice (77 %), 76 to 78 files copied | 8.6 | 4.0 | | 6.9 | 4.8 | ALL |
| total of the rows (measured wall 282.8 / 260.3 / 19.4) | 280.6 | 150 | 33 | 254.2 | 19.0 | |

## 3. Network in a warm rebuild (through the proxy; medians over the 6 FSH rebuilds)
| host | requests | new TLS connections | s waiting | what |
|---|---|---|---|---|
| packages.fhir.org | 4 | 2 | 1.03 | 1 by SUSHI (`hl7.fhir.uv.tools.r5#latest`), 3 by the publisher's package client |
| packages2.fhir.org | 8 | 1 | 0.49 | package lookups during conformance validation (PackageClient.fetchUrl on main) |
| build.fhir.org | 4 | 0 to 1 | 0.50 (0.38 to 2.63) | qas.json (205 KB) downloaded 3 times per rebuild, plus one small file |
| tx.fhir.org | 3 to 4 (7 in the first rebuild) | 1 | 0.23 | connect/capabilities, 1 to 2 validations |
| api.github.com | 2 | 2 | 0.27 | Publisher.fetchCurrentIGPubVersion, twice, at the end of every build (also page rebuilds: their only requests) |
| fhir.github.io | 1 | 0 to 1 | 0.07 | ig-registry fhir-ig-list.json (DependentIGFinder) |
Total about 22 requests and 2.6 s per FSH rebuild, all synchronous on the main thread. dhp.uz package-list.json is only fetched in the first build. No ext-tracker request was seen.

## 4. What runs for all 811 files although their inputs did not change (profile edit, wall s)
`pf.changeList` holds every file in every build (PublisherBase.addFile). Rapido only skips validation (reuse), html generation and spreadsheets (replay) for clean files. Per rebuild, for the whole IG:
- SUSHI converts all 681 resources (51.4).
- Every source file is scanned, parsed and converted (6.8); snapshots are redone (1.0).
- Rapido hashes every resource and reads the whole state file (5.5).
- Narratives (13.6), translation artifacts (5.1), native outputs (28.7), summaries (6.4) for every file.
- The html replay re-hashes every recorded output of the 675 clean files and re-reads their expansions (about 9.5 of 22.9); spreadsheet replay (3.4).
- Combined package and package cache install (13.6); all zips (25.1); the AI step reads all resources (7.4).
- The html check lists and stamps 20683 files (most of its 25.1 is GC); full-ig.zip is rewritten (15.3).
- Draft dependency scan (8 of 9.4); qa (2.3); state save (2.1); the output sync compares everything (8.6).
Proportional to the change: validation, html generation of the 136 dirty files, Jekyll (2.9).

## 5. Already being implemented elsewhere (measured, not ranked)
- Publish pages right after Jekyll: page at 172.2 instead of 279.8 s (profile), 159.9 instead of 255.4 (instance), 2.6 instead of 17.9 (page).
- Reuse unchanged archives in generateZips: 25.1 s at stake (expansions 11.6, definitions 6.6, validator.pack 5.8, rest 1.1).
- Skip native outputs for unchanged resources: 28.7 s phase, 17 % of the files are dirty.
- Reuse summaries and cross-IG fragments: 6.4 s (1.0 before, 5.4 after).
- Shrink the Rapido state file: 188 MB; read ~2.6 s, write 2.1 s, replay reads of recorded expansions ~2.6 s.

## 6. The 12 best remaining opportunities (profile edit seconds; P = shortens save to page, F = save to Finished). All publisher-only unless marked.
1. **Narratives only for render-dirty files** (P, F): 13.6 s, ~11 at stake. PublisherProcessor.generateNarratives:1231 loops all files and reconverts each to an Element. Risky: clean resources then have no generated text in memory, and native outputs, zips and the combined package serialise them; only safe together with native-skip and archive reuse, or by keeping last build's narrative per resource hash. Effort M to L.
2. **canReplay by stamp, not content hash** (P, F): ~7 s CPU + ~4 s IO wait of the html phase. RapidoTracker.canReplay:1592/fileHash:1616 reads every output of every clean file (temp is 7.2 GB). Record size + mtime when written and hash only on mismatch: the html check records already trust stamps. Low risk. Effort S.
3. **Load while SUSHI runs** (P, F): 5.6 s (template 2.1 + core/tx/packages 3.5), up to ~8 with the Rapido state read; 3 cores idle for 51 s. PublisherIGLoader.initialize:186 runs SUSHI first. Template and package loading do not read SUSHI's output; the IG resource does, so only the part before it moves. Medium risk (template writes into the IG folder, failure ordering). Effort M.
4. **Restart on a change that arrives mid-build** (P): today a second save waits for the rest of the build (monitoring is off while building, Publisher.waitForChanges) and then a whole rebuild: up to 283 s extra, 109 s of it the post-Jekyll tail. Derived from the measured build, not measured separately. Needs the tail to be abortable without saving Rapido state. Medium to high risk. Effort L.
5. **html check memory** (F): 25.1 s wall, 16.6 s of it GC pauses: the heap is full of the previous check's records plus the new build (live 6.7 of 7.4 GB; 7g fails with OutOfMemoryError exactly here). Trim what PageRebuild/HtmlCheckRecords keep (parsed LoadedFile data) before the new check. Up to ~17 s on a tight heap, little on a roomy one. Low risk. Effort M.
6. **full-ig.zip beside QA** (F): 15.3 s with the main thread parked; QA prep + qa + state save (13.8 s) do not read the zip. Start it on its worker and join before the sync: ~13 s. Low to medium risk (qa files are written after the zip today, keep that). Effort S.
7. **Draft dependency scan per resource hash; publisher version once per session** (F): 8 s of the 9.4 s QA prep (DraftDependenciesRenderer via Publisher.generateDraftDependencies), plus 2 requests / 0.27 s (Publisher.fetchCurrentIGPubVersion:2289). Page rebuilds pay 3.7 s for nothing changed. Low risk. Effort S.
8. **Sync only what was written** (F, and P until early publish lands): 8.6 s FSH, 4.8 s page. OutputFolderSync.sync:82 compares content of everything; the build already knows the fresh set ("44 written by Jekyll, 22 by the package step, 6 by the AI step") plus what the html check rewrote. ~7 s. Medium risk: deletions and files written by other steps must still be found (keep a full pass when the set is unknown). Effort S to M.
9. **Combined package off the main path** (F): 11.5 + 2.1 s install. Run genCombinedPackage (PublisherGenerator:1065) on a worker while zips/AI/html check run, or skip when package.tgz is byte-identical. Medium to high risk: it reads the shared context for expansions; the generator itself is core. Effort M.
10. **Translation artifacts for dirty files only** (P, F): 5.1 s, ~4 at stake. PublisherTranslator.genTranslations rewrites the same files for unchanged resources. Low to medium risk. Effort S to M.
11. **Keep parsed sources in memory between rebuilds** (P, F): 6.8 s parse/convert + 1.0 snapshots (+ 5.6 template/packages if 3 is not done). Structural and risky: the build mutates loaded resources (snapshots, narrative, ids), so it needs copies or a clean reload of anything touched. Effort L.
12. **No explicit System.gc() before Jekyll in watch mode** (P, F): 2.4 s (PublisherGenerator:517); and AI step reuse of package resources (readResources 38 % of 7.4 s, F). Low risk. Effort XS / S.

Not worth it: **a warm SUSHI process**. Running SUSHI's pipeline 3 times in one node process: 46.0, 42.2, 45.0 s (export 33 to 35.6 s each time), so it saves 1 to 4 s plus 1.8 s start-up; the time is SUSHI's export of all 681 resources, which is outside the publisher. qas.json x3 and the package lookups (1 to 3 s) are in core.
