# String deduplication and memory: uz.dhp.core, `-rapido -watch`, 15 GB / 4 CPU VM

Measured 2026-11-12 on master @32046bc (not the perf branch: master renders 193 of 811 files on a profile edit
and does a Differential Build, not a Page Rebuild, on a page edit, so times are longer than in bench-recipe.md).

## Answer
At the same -Xmx9g, `-XX:+UseStringDeduplication` makes the live heap 0.5 to 0.7 GB (10 to 14 %) smaller at
identical program points (System.gc() before the html check: 5.30 to 4.78 GB on the first build, 4.92 to 4.22..4.38
on rebuilds), but peak RSS is the same (java 10.3 GiB, total 13.1 GiB either way: G1 grows the heap to -Xmx), and it
costs 3 to 7 % per rebuild (+15 to +33 s, +73 s per 4-build session, of which about 6 s is GC pause; the dedup thread
is busy about 83 s per session). Its use is a smaller heap: the smallest -Xmx that completes first+prof1 is 7g with
deduplication and 7500m without (7g without OOMs in the first build's html check, twice). At those minimums dedup
gives about 0.25 GiB lower java and total RSS at similar speed (first build 607 and 652 s vs 657 s, prof1 520 and 455 s
vs 529 s), so on a box that has to run near its heap minimum dedup is worth having; with heap to spare it is not.

## Commands
```
fallocate -l 16G /home/user/swapfile && chmod 600 /home/user/swapfile && mkswap /home/user/swapfile && swapon /home/user/swapfile
cd /home/user/fhir-ig-publisher && mvn -q -T 4 -DskipTests package     # master @32046bc
cp org.hl7.fhir.publisher.cli/target/org.hl7.fhir.publisher.cli-3.0.4-SNAPSHOT.jar /home/user/bench/base.jar
git clone https://github.com/uzinfocom-org/digital-health-ig /home/user/bench/seed && git -C /home/user/bench/seed checkout 2c39cd0a
cd /home/user/bench/runs
# warm, not counted (fills tx cache, pcf, ~/.fhir/packages), then the tx cache travels with the seed
python3 ../session.py --jar ../base.jar --seed ../seed --work ig --pcf ../pcf --heap 9g --steps first,prof1 --label warm-9g \
  "--jvm=-XX:+UseStringDeduplication -Xlog:gc*,stringdedup*=debug:file=<gcd log>:uptime,tags"
rm -rf ../seed/input-cache && cp -a ig/input-cache ../seed/
# sessions, in this order: d1, n1, d2, n2 (dedup = the --jvm flag present, no dedup = absent; nothing else differs)
python3 ../session.py --jar ../base.jar --seed ../seed --work ig --pcf ../pcf --heap 9g --steps first,prof1,prof2,page1 --label d1-9g \
  "--jvm=-XX:+UseStringDeduplication -Xlog:gc*,stringdedup*=debug:file=<gcd log>:uptime,tags"
python3 ../session.py ... --label n1-9g "--jvm=-Xlog:gc*,stringdedup*=debug:file=<gcd log>:uptime,tags"
```
session.py runs `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx9g -Xlog:gc:file=<out>/gc.log <--jvm> -jar base.jar -ig . -rapido
-watch -tx https://tx.fhir.org -package-cache-folder /home/user/bench/pcf` (no `-threads`). On JDK 21 the selector
`gc+stringdedup*` does not exist ("No tag set matches selection"), the tag is `stringdedup`, hence `stringdedup*=debug`.
Min-heap runs: the same with `--steps first,prof1`, killed on OutOfMemoryError.

## Numbers at -Xmx9g (medians of 2 sessions per config; the two values in brackets)
Live set: "full GC" = largest heap after a Full GC in the build (the true live set at that moment; G1 ran 3 to 7 full
GCs per build at 9g, all in or next to the html check, so this is the html check's live set). "System.gc()" = heap
after the publisher's own System.gc() calls, which fall at the same program point in every run (after "Reclaiming
memory...", just before the html check, and after "Final .zip built"); n1 and n2 agree there within 4 MB, so it is
the cleanest comparison. Mixed GC "after" values are not a live set (they leave old regions uncollected): listed for
completeness only.

| build | metric | dedup | no dedup | dedup - no dedup |
|---|---|---|---|---|
| first | wall to Finished @ | 613.0 s (608.5, 617.6) | 610.4 s (606.9, 613.9) | +2.6 s |
| first | GC pause | 30.2 s (30.6, 29.7) | 31.4 s (32.2, 30.6) | -1.2 s |
| first | peak java RSS | 10.25 GiB (10.29, 10.21) | 10.29 GiB (10.19, 10.38) | 0 |
| first | peak total RSS (java + Jekyll + SUSHI) | 12.86 GiB (12.86, 12.86) | 12.89 GiB (12.82, 12.96) | 0 |
| first | peak swap used / swap-in | 1.36 GiB / 1296 MiB | 1.31 GiB / 1285 MiB | 0 |
| first | peak heap after any GC | 8.96 GiB | 8.98 GiB | 0 (heap full) |
| first | live, System.gc() before html check / after final zip | 4.78 / 5.15 GB | 5.30 / 5.69 GB | -0.52 / -0.54 GB (-10 %) |
| first | live, largest full GC (html check) | 5.17 GiB (5.24, 5.09) | 6.36 GiB (5.77, 6.95) | -1.2 GiB (noisy) |
| first | full GCs | 5 | 6.5 | |
| first | deduplicated | 1029 MiB (1169, 888) | 0 | |
| prof1 | wall to Finished @ | 497.4 s (504.8, 490.1) | 482.1 s (474.1, 490.2) | +15.3 s (+3.2 %) |
| prof1 | GC pause | 23.9 s (26.9, 20.8) | 21.9 s (21.2, 22.6) | +2.0 s |
| prof1 | peak java / total RSS | 10.38 / 13.05 GiB | 10.41 / 13.14 GiB | 0 |
| prof1 | peak swap used / swap-in | 1.44 GiB / 1092 MiB | 1.45 GiB / 1101 MiB | 0 |
| prof1 | live, System.gc() before html check / after final zip | 4.28 / 4.65 GB (4.34, 4.22 / 4.71, 4.58) | 4.92 / 5.30 GB | -0.64 / -0.65 GB (-13 %) |
| prof1 | live, largest full GC (html check) | 4.54 GiB (4.62, 4.47) | 5.96 GiB (5.17, 6.74) | -1.4 GiB (noisy) |
| prof1 | deduplicated | 1244 MiB (1006, 1482) | 0 | |
| prof2 | wall to Finished @ | 492.1 s (473.5, 510.6) | 459.1 s (449.6, 468.6) | +33.0 s (+7.2 %) |
| prof2 | GC pause | 24.2 s (22.2, 26.1) | 19.8 s (20.3, 19.2) | +4.4 s |
| prof2 | peak java / total RSS | 10.43 / 13.07 GiB | 10.34 / 12.91 GiB | +0.1 / +0.16 |
| prof2 | peak swap used / swap-in | 2.24 GiB / 804 MiB | 2.50 GiB / 1267 MiB | |
| prof2 | live, System.gc() before html check / after final zip | 4.33 / 4.69 GB | 4.92 / 5.29 GB | -0.59 / -0.60 GB (-12 %) |
| prof2 | live, largest full GC (html check) | 4.65 GiB (4.63, 4.68) | 5.22 GiB (5.28, 5.17) | -0.57 GiB |
| prof2 | deduplicated | 1130 MiB (1238, 1022) | 0 | |
| page1 | wall to Finished @ | 394.0 s (371.3, 416.6) | 371.8 s (365.6, 377.9) | +22.2 s (+6.0 %) |
| page1 | GC pause | 21.9 s (22.0, 21.7) | 21.3 s (19.3, 23.3) | +0.6 s |
| page1 | peak java / total RSS | 10.46 / 13.03 GiB | 10.34 / 12.99 GiB | +0.1 / 0 |
| page1 | peak swap used / swap-in | 2.38 GiB / 858 MiB | 2.68 GiB / 548 MiB | |
| page1 | live, System.gc() before html check / after final zip | 4.12 / 4.50 GB | 4.82 / 5.20 GB | -0.69 / -0.69 GB (-14 %) |
| page1 | live, largest full GC (html check) | 4.46 GiB (4.51, 4.40) | 5.22 GiB (5.29, 5.15) | -0.76 GiB |
| page1 | deduplicated | 1233 MiB (1352, 1113) | 0 | |

Per session (4 builds), d1, d2 vs n1, n2: wall 1958, 2035 vs 1896, 1951 s (median +73 s, +3.8 %); GC pause 101.7, 98.3
vs 93.0, 95.7 s; deduplicated 4765, 4506 MiB; peak java RSS 10.43, 10.51 vs 10.30, 10.51 GiB; peak total RSS 13.16,
13.17 vs 13.16, 13.18 GiB; peak swap 2.56, 2.41 vs 2.39, 3.25 GiB; swap-in 4045, 4053 vs 4061, 4340 MiB; largest
live set 5.24, 5.09 vs 5.77, 6.95 GiB (first build, html check, both configs). Dedup thread busy time (sum of its
"Process end" times): 84.6, 81.7 s per session.

## Smallest heap that completes first+prof1 (500 MB steps)
| -Xmx | dedup | no dedup |
|---|---|---|
| 9g | OK (4 sessions above) | OK (4 sessions above) |
| 7500m | not run | OK: first 656.8 s (GC 68.4 s, 20 full GCs), prof1 528.9 s (GC 36.7 s); java RSS 8.67 / 8.75 GiB, total 11.97 / 12.03 GiB; no swap |
| 7g | OK, twice: first 606.8 / 651.6 s (GC 54.5 / 66.4 s, 15 / 20 full GCs), prof1 519.6 / 454.9 s (GC 38.9 / 34.8 s); java RSS 8.43-8.51 GiB, total 11.62-11.74 GiB; no swap | OOM twice in the first build's html check: at 571 s (main thread in TerminologyCache.save, heap 7161M->7161M after back-to-back full GCs) and at 587 s (in HTMLInspector.check) |
| 6500m | OOM in the first build's html check, 588 s (XhtmlParser.parseElement) | not run |

So deduplication lowers the minimum heap by 500 MB (7500m to 7g) and, at the respective minimums, java RSS by about
0.25 GiB (8.42 to 8.51 vs 8.67 / 8.75) and total RSS by about 0.3 GiB (11.62 to 11.74 vs 11.97 / 12.03). Times at the
minimums are within run-to-run spread of each other (the two dedup 7g runs differ by 45 s on the first build and 65 s
on prof1).

## Against what we believed
- "-Xmx7g without dedup: the first html check OOMs": confirmed (twice).
- "with dedup 7600m is the largest heap that survives the cgroup": not tested as such. With the 16 GB swap file both
  configs ran at 9g with java 10.2 to 10.5 GiB + Jekyll 2.9 to 3.4 GiB; the kernel kept 1.3 to 3.3 GB in swap and read
  0.5 to 1.3 GB back per build, and nothing was killed.
- "dedup cut the first html check's live set from ~7.5 to ~5.9 GB": on master the first html check's live set is
  5.8 to 7.0 GB without and 5.1 to 5.2 GB with (largest full GC; noisy because a full GC in the middle of the html
  check catches a varying working set). At fixed program points the saving is 0.5 to 0.7 GB, not 1.6.
- With dedup at 7g first+prof1 completes on master (bench-recipe.md saw the first rebuild OOM at 7g with dedup on
  the perf branch).

## Caveats
- Swap: at 9g every build read 0.5 to 1.3 GB back from swap (Jekyll and cold java pages), in both configs alike. That
  inflates wall times (the 9g rebuilds are slower than the 7g/7500m ones despite less GC) and adds noise; the
  rebuild-time cost of dedup was in the same direction in all six rebuild pairs (+15 to +33 s), but its size is
  uncertain to about +-15 s. The min-heap runs did not swap.
- RSS at a fixed -Xmx does not show dedup's saving: G1 commits the whole heap in the html check either way. The saving
  is visible in the live set and in how small -Xmx can go.
- tx.fhir.org: every build asks it (~22 requests); no errors or unusual slowness in these runs, but a slow tx server
  shows up in validation time and would hit both configs at random.
- Two sessions per config at 9g, one min-heap run per size except 7g (two of each). 7500m was not run with dedup and
  6500m not without (the outcome is implied by the neighbours).
- Master, not the perf branch: the perf branch keeps more between rebuilds, so its live set (and the dedup saving in
  absolute terms) may be larger.
- Files: /home/user/bench/runs on the measuring VM (gone with it): session folders, gcd-*.log, *.gcan.json; analysis
  scripts were bench/tools/gcan.py and table.py there.
