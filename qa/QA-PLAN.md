# QA of the unsubmitted perf work on real IGs

Goal: prove that the combined unsubmitted changes (branch `qa/integration` on origin = vadi2/fhir-ig-publisher) never
change what the publisher produces, and that `-rapido -watch` rebuilds are never stale, on UTG and the 60 most recently
built IGs on build.fhir.org (`qa/shards.json`, one shard per session). Reference = current master (`origin/master`).

## Machine setup (15 GB / 4 CPU VM; see ../bench-recipe.md and ../PROCESS.md for why)
- `fallocate -l 16G /home/user/swapfile && chmod 600 /home/user/swapfile && mkswap /home/user/swapfile && swapon /home/user/swapfile` before any build (cgroup caps RAM ~14.3 GB, not swap).
- Jekyll: `gem install jekyll && ln -sf /opt/rbenv/versions/3.3.6/bin/jekyll /usr/local/bin/jekyll` (check the ruby path); SUSHI: `npm i -g fsh-sushi`.
- Publisher: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx<H> -XX:+UseStringDeduplication -jar <jar> -ig <dir> [-rapido -watch] -tx https://tx.fhir.org -package-cache-folder /home/user/qa/pcf`
  Heap H: 7600m by default; an IG whose shards.json `mem` is over 9 may need it raised (swap absorbs the rest). Use the same H for every build of an IG. No `-threads`. One publisher at a time. No mvn while a build runs.
- Jars: `git worktree add` each ref, `mvn -q -T 4 -DskipTests package`, copy `org.hl7.fhir.publisher.cli/target/org.hl7.fhir.publisher.cli-*-SNAPSHOT.jar` to `/home/user/qa/master.jar` and `/home/user/qa/int.jar`.
- IG sources: `git clone --depth 1 -b <branch> https://github.com/<repo>` into `/home/user/qa/src/<name>`; record the commit. Every build runs in its own fresh copy of that clone (`cp -a`), so no build sees another's temp/output/input-cache.

## Phase A (start immediately): reference builds with master.jar
For each IG in your shard: R1 = full build (no -rapido) on a fresh copy. Keep output/qa.json, output/qa.html, the whole output/ (or a hash manifest of it if disk is tight: `find output -type f -print0 | sort -z | xargs -0 sha256sum`), and the log. If R1 fails or doesn't finish in 90 min, mark the IG `skip: <reason>` and move on.

## Phase B: wait for the integration jar
Poll `git ls-remote origin refs/heads/qa/integration-ready` every 5 min (a plain shell loop with sleep is fine). When it exists, build int.jar from that exact commit and record its sha. Read the integration branch's `QA-INTEGRATION.md` (list of PRs it contains).

## Phase C: per IG, with int.jar
1. I1 = full build (no -rapido), fresh copy. Compare to R1.
2. I2 = a second full build in the same folder as I1 (exercises caches that persist across full builds). Compare to R1.
3. Watch sequence, fresh copy, `-rapido -watch`: wait for the first build's "Finished", then make each edit, wait for the rebuild to finish (watch the log for the next "Finished"/"Watching for changes"), and snapshot output/ after each:
   - W1 page edit: append a unique sentence to one markdown page in input/pagecontent (or input/pages, intro/notes files).
   - W2 resource edit: change a profile's description/short text (FSH: edit the .fsh source; otherwise the json/xml) so its rendered page changes.
   - W3 example edit: change a value in one example instance that something else references, if there is one.
   - W4 add a new example file (copy one, new id); W5 delete it.
   - W6 revert all edits (restore the original files).
   Each edit must show up in output/ (grep for the unique text). After W2 and after W6, stop watch, and compare output/ with a fresh full build (no -rapido) of the same sources by int.jar (F2, F6). After W6 also compare with I1.
4. Compare rules: qa.json errors/warnings/hints counts and the set of messages (by file + message text) must be equal. Output files: same file set; per file, equal after masking build dates/timestamps (genDate, "Generated", footer dates, zip entry times, `meta.lastUpdated` the publisher sets), the run folder path, ValueSet expansion identifier/timestamp, UUIDs/random Bundle.id, qa timing/report files, and the order of the "used by" lists (known to vary between master runs). Anything else that differs is a FINDING.
5. For each FINDING, check whether master shows it too: rerun the same step with master.jar (for watch findings, the same edit sequence with master.jar `-rapido -watch`). If master shows the same difference it's `pre-existing`, otherwise `regression`. For a regression, find the PR: build jars from the single PR heads listed in QA-INTEGRATION.md that touch the area and rerun the failing step; name the PR and the smallest reproduction.

## Reporting
- Results file: `qa/results-<shard>.md` on the notes branch `claude/ig-publisher-perf-41aejs-notes` (fetch + rebase on that branch before each push; plain push, never force; terse commit messages, no footers, no model names, no em dashes). Update it after each IG: IG, commit, heap, R1/I1/I2/W*/F* times, QA counts, verdict (`same`, `pre-existing diffs: ...`, `REGRESSION: ...`, `skip: ...`), and for each regression the PR and a reproduction.
- Don't fix code, don't open PRs, don't comment on GitHub, never touch HL7/* or hapifhir/* repos (cloning public IG repos read-only is fine). Don't message other sessions. When your shard is done, write a final summary line at the top of your results file and stop.
