# Bench recipe: `-rapido -watch` on uz.dhp.core (15 GB / 4 CPU cloud VM)

Final. Measured on base `claude/ig-publisher-perf-41aejs` @a4d1e90. Driver: `bench/session.py` on this branch.

## Install (once, ~15 min with the jar)
```
gem install jekyll && ln -sf /opt/rbenv/versions/3.3.6/bin/jekyll /usr/local/bin/jekyll   # gem's bin dir is not on PATH
npm i -g fsh-sushi                                                                         # SUSHI 3.21.0
mkdir -p /home/user/bench/runs /home/user/bench/pcf && cd /home/user/bench
git clone https://github.com/uzinfocom-org/digital-health-ig seed && git -C seed checkout 2c39cd0a
(cd <checkout> && mvn -q -T 4 -DskipTests package) && cp <checkout>/org.hl7.fhir.publisher.cli/target/org.hl7.fhir.publisher.cli-*-SNAPSHOT.jar base.jar
fallocate -l 8G swapfile && chmod 600 swapfile && mkswap swapfile && swapon swapfile     # required, see Memory
git -C <checkout> show origin/claude/ig-publisher-perf-41aejs-notes:bench/session.py > session.py
```
## Publisher command (the driver builds it; identical flags for both jars; no `-threads`)
```
IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -Xlog:gc:file=gc.log \
  -jar <jar> -ig . -rapido -watch -tx https://tx.fhir.org -package-cache-folder /home/user/bench/pcf
```
## Warm the caches once, then run sessions (40 to 45 min each with the default steps)
```
cd /home/user/bench/runs; S="python3 ../session.py --seed ../seed --work ig --pcf ../pcf --heap 7600m --jvm=-XX:+UseStringDeduplication"
$S --jar ../base.jar --label warm && rm -rf ../seed/input-cache && cp -a ig/input-cache ../seed/   # tx cache now travels with the seed
$S --jar ../base.jar --label base; $S --jar ../change.jar --label change; $S --jar ../change.jar --label change; $S --jar ../base.jar --label base
```
- The warm run also fills `pcf` (publisher packages) and `~/.fhir/packages` (SUSHI's own cache). Its first build asks tx.fhir.org for everything (about 2.5 min longer).
- Each session copies the seed to `--work` (without output/temp/template/fsh-generated and without Rapido's tracker files), so it starts with "Complete Build (no state from a previous build)".
- Steps: `first,prof1,prof2,restore-prof,inst1,inst2,restore-inst,page1,restore-page` (`--steps` to change; prof3, inst3, page2, page3 exist).
- Results in `runs/<label>-<hhmmss>/`: summary.txt, result.json, publisher.log (seconds since java started), gc.log, rss.log.
  Per build: `finished` = edit to "Finished @"; `page` = edit to the edited text being in the checked file under output/ (polled every 50 ms); `detected`; GC pause seconds; peak RSS; swap; `swapin`; the Rapido line; every timed log line with the seconds until the next. Exit code 1 if an edit did not reach output/.
- One publisher at a time, and no mvn while a session runs: every command shares one 13.35 GiB memory cgroup.

## Timings on this VM (base, medians of 3)
| build | to "Finished @" | to output/ | GC pauses |
|---|---|---|---|
| first (complete) | 667 to 690 s | | 61 to 67 s |
| profile edit (136 of 811 render, 120 validate) | 282.8 s (270.6 to 290.0) | 279.8 s | 27 to 35 s |
| instance edit (112 render, 97 validate) | 260.3 s (249.6 to 266.8) | 255.4 s | 26 to 35 s |
| page edit (Page Rebuild) | 19.4 s (18.2 to 20.0) | 17.9 s | 0.2 to 0.4 s |
SUSHI is 47 to 51 s of every FSH rebuild here (one slow core; ~16 s on the 12-core reference), so totals are about 2.5x those in PROCESS.md. Rebuild-to-rebuild spread is about 7 %: compare medians.

## Memory: why these flags
- The kept Jekyll holds 3.5 GiB for the whole session; java's RSS is heap + 1.3 to 1.9 GiB; SUSHI adds 0.8 GiB.
- `-Xmx7g`: OutOfMemoryError in the html check (first build without string deduplication, first rebuild with it). `-Xmx8g` and `9g`: killed by the cgroup (java 9.6 to 10.3 GiB + Jekyll 3.5). 7600m with deduplication is the largest heap that runs; deduplication cuts the first html check's live set from 7.5 to about 5.9 GB.
- Even so java 9.1 to 9.5 GiB + Jekyll + SUSHI is at the limit: the kernel keeps 2 to 4.4 GB in the swap file, and without it the session is killed. After the first rebuild a build reads under 70 MB back (`swapin`); a build with hundreds of MB there was slowed, so look at that column. Do not raise the cgroup limit and do not use zram.
- GC share: pauses are 10 to 12 % of an FSH rebuild (16.6 s of them in the html check, where the heap is fullest). Both jars pay it, so A/B deltas hold; absolute phase times include it. A change that keeps more in memory can tip the session into OutOfMemoryError: that is a finding, not noise.
- Tried and dropped: G1 periodic GC with MaxHeapFreeRatio (43 s of pauses instead of 27 to 35), ParallelGC (same live set and RSS), jemalloc and Ruby GC settings for Jekyll (still 3.5 GiB), JFR during a timed session (its memory pushes the run into swap: 380 s instead of 283).

## -threads: do not pass it here
The base uses worker threads for native outputs, spreadsheet prefetch and the fallback full-ig.zip only with `-threads` (capped at cores - 1 = 3). With `-threads 3`: first build 619.6 s, first profile rebuild 263.8 s (native outputs 17.5 s instead of 28.7), then the second rebuild died in native outputs with `OutOfMemoryError` ("ig-native-1: Retried waiting for GCLocker too often allocating 11534338 words"). The heap has no room for three workers. A session without it ran all 9 rebuilds. If your change is about threads, say so and expect this.

## Pitfalls
- `--jvm` needs the `=` form (its value starts with `-`); several JVM flags go in one quoted `--jvm=...`.
- After a crash or Ctrl-C: `pkill -x java; pkill -f jekyll-driver` before the next session. Deleting the old `--work` folder (100k files) can take minutes; the driver does it at start.
- Disk: a work folder is 9.5 GB (temp 7.2, output 2.2) plus the 8 GB swap file; `df` shows the session allowance.
- tx.fhir.org and packages.fhir.org are asked on every rebuild (about 22 requests, 2.6 s); an outage shows as failed or slow validation, not as a publisher regression.
- Output check against a fresh build: same jar and flags, no `-watch`, its own copy of the seed, after the watch session has ended (not in parallel). Not run by me; its memory need is that of a session's first build.
