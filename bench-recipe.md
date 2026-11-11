# Bench recipe: `-rapido -watch` on uz.dhp.core (15 GB / 4 CPU cloud VM)

Measured by the profiling session on base `claude/ig-publisher-perf-41aejs` @a4d1e90. Driver: `bench/session.py` (this branch).
Status: INTERIM (16:45 UTC). Flags and driver are usable now; `-threads` advice and timings get one more update.

## Install (once, ~15 min including the jar)
```
gem install jekyll && ln -sf /opt/rbenv/versions/3.3.6/bin/jekyll /usr/local/bin/jekyll   # gem's bin dir is not on PATH
npm i -g fsh-sushi                                                                         # SUSHI 3.21.0
mkdir -p /home/user/bench && cd /home/user/bench
git clone https://github.com/uzinfocom-org/digital-health-ig seed && git -C seed checkout 2c39cd0a
(cd <publisher checkout> && mvn -q -T 4 -DskipTests package) && cp <checkout>/org.hl7.fhir.publisher.cli/target/org.hl7.fhir.publisher.cli-*-SNAPSHOT.jar base.jar
fallocate -l 8G swapfile && chmod 600 swapfile && mkswap swapfile && swapon swapfile     # safety valve, see Memory
git -C <publisher checkout> show origin/claude/ig-publisher-perf-41aejs-notes:bench/session.py > session.py
```
Never run mvn (or anything heavy) while a session runs: all your commands share one 13.35 GiB memory cgroup.

## Publisher command (the driver builds it; same flags for both jars)
```
IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -Xlog:gc:file=gc.log \
  -jar <jar> -ig . -rapido -watch -tx https://tx.fhir.org -package-cache-folder /home/user/bench/pcf
```

## Warm the caches (once, ~35 min), then run sessions
```
JVM="--jvm=-XX:+UseStringDeduplication"
mkdir -p runs pcf && cd runs
python3 ../session.py --jar ../base.jar --seed ../seed --work ig --pcf ../pcf --heap 7600m "$JVM" --label warm
rm -rf ../seed/input-cache && cp -a ig/input-cache ../seed/      # tx cache (git-ignored) now travels with the seed
python3 ../session.py --jar ../base.jar   --seed ../seed --work ig --pcf ../pcf --heap 7600m "$JVM" --label base
python3 ../session.py --jar ../change.jar --seed ../seed --work ig --pcf ../pcf --heap 7600m "$JVM" --label change
```
The first run fills `pcf` (publisher packages), `~/.fhir/packages` (SUSHI's own cache) and the tx cache. Each session
copies the seed to `--work` (without output/temp/template/fsh-generated), so every session starts with a complete build.
Default steps: `first,prof1,prof2,restore-prof,inst1,inst2,restore-inst,page1,restore-page` (`--steps` to change;
prof3/inst3/page2/page3 exist). Results: `runs/<label>-<hhmmss>/{summary.txt,result.json,publisher.log,gc.log,rss.log}`.
Per build: `finished` = edit to "Finished @"; `page` = edit to the edited text being in the checked output file
(polled every 50 ms); `detected`; GC pause seconds; peak RSS; swap; the Rapido line; every timed log line with the
seconds to the next one. Exit code 1 and `output_ok: false` if an edit did not reach output/.

## Timings on this VM (base jar)
First build 687 to 689 s (complete build; SUSHI 47, Jekyll full 85, html check 120). Profile edit 272 to 295 s
(136 of 811 files to render, 120 to validate). Instance and page edits: see profile.md when it lands. A session with the
default steps takes roughly 45 min. Deleting and copying the IG before the first build adds 1 to 6 min.

## Memory: why these flags
- Kept Jekyll (jekyll-driver.rb) holds 3.5 GiB for the whole session; java's RSS is heap + 1.3 to 1.9 GiB.
- `-Xmx7g`: OutOfMemoryError in the html check (first build without string dedup; first rebuild with it).
  `-Xmx8g`/`9g`: cgroup OOM kill (java 9.6 to 10.3 GiB + Jekyll 3.5 GiB). 7600m with string deduplication is the
  largest heap that fits; deduplication cuts the html check's live set from 7.5 to about 5.9 GB.
- Tried and dropped: G1 periodic GC + MaxHeapFreeRatio (GC pauses 43 s instead of 23 to 30 s per profile rebuild, swap
  still used), ParallelGC (same live set, same RSS), jemalloc and Ruby GC settings for Jekyll (still 3.5 GiB).
- java 9 to 9.4 GiB + Jekyll 3.5 + SUSHI 0.8 is at the limit, so the kernel keeps 1 to 3.5 GB in the swap file. Without
  the swap file the session is killed. `swapin` in summary.txt is what a build read back from swap; main-thread CPU
  is within a few percent of wall in every phase, so the effect is small, but compare medians, not single builds.
  Do not raise the cgroup limit and do not use zram.
- GC share: GC pauses are 23 to 30 s of a 272 to 283 s profile rebuild (8 to 11 %) and 67 s of the first build. The heap
  is tight: both jars pay it, so A/B deltas hold, but absolute phase times include it.

## -threads
The base does native outputs, spreadsheet prefetch and the fallback full-ig.zip on worker threads only with `-threads`
(capped at cores - 1 = 3). Not yet validated on this VM (memory): until this section is updated, run without it, as
PROCESS.md does. Driver option: `--threads 3`.

## Pitfalls
- `--jvm` needs the `=` form (its value starts with `-`).
- SUSHI alone is ~49 s of every FSH rebuild on this VM (single core; 16 s on the 12-core reference): totals here are
  about 2.5x the numbers in PROCESS.md. Compare jars only within this VM.
- Deleting the previous `--work` folder can take minutes after a session (100k files); the driver does it at start.
- A session killed mid-way leaves java and a `jekyll-driver` ruby: `pkill -x java; pkill -f jekyll-driver` first.
- Fresh-build comparison (output check): same jar, no `-watch`, own copy of the seed, `-Xmx7600m` + dedup fits because
  no Jekyll is kept. tx.fhir.org must be reachable; one outage shows as "Complete Build" or failed validations.
- Do not edit files in `--work` by hand during a session; the driver waits for "Watching for changes" before each edit.
