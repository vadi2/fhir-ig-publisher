# QA round 2: page-only watch rebuilds without the Jekyll driver (rework of HL7 #1461)

Goal: prove that the rework branch (ref `qa/page-only-ready` on vadi2/fhir-ig-publisher, pushed by the dev session when its
PR is ready; it may move forward as fixes land) never changes what the publisher produces in ordinary builds, and that
`-rapido -watch` rebuilds are never stale, on UTG and the IGs in `qa/shards.json` (one shard per session).
Reference = HL7 master at 2ccca179460d9bb9f4185a59a6fd66d4d9330951 (fetch it from https://github.com/HL7/fhir-ig-publisher,
read-only). Known pre-existing master diffs from round 1 are in `qa/results-*.md` (F2 watch qa.txt paths, F3 .index.db
ResourceList rows, F4 terminology.hl7.org version in NamingSystem links, F6 ValueSet expansion params on folder reuse,
comparison-page stack traces, LC_ALL=C non-ASCII file names): read them so you don't re-investigate.

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
Poll `git ls-remote https://github.com/vadi2/fhir-ig-publisher refs/heads/qa/page-only-ready` every 10 min (a plain shell loop with sleep is fine). When it exists, build int.jar from that exact commit and record its sha. If the orchestrator later tells you the ref moved, rebuild int.jar and rerun the steps it names.

## Phase C: per IG, with int.jar
1. I1 = full build (no -rapido), fresh copy. Compare to R1.
2. I2 = a second full build in the same folder as I1 (exercises caches that persist across full builds). Compare to R1.
3. Watch sequence, fresh copy, `-rapido -watch`: wait for the first build's "Finished", then make each edit, wait for the rebuild to finish (watch the log for the next "Finished"/"Watching for changes"), and snapshot output/ after each:
   - W1 page edits (this is the feature under test, so do all that apply, one rebuild each, unique marker each):
     W1a append a sentence to a markdown page in input/pagecontent (or input/pages); W1b edit an html/xml page;
     W1c edit an intro/notes file of a resource (input/intro-notes or pagecontent/*-intro.md); W1d edit an include
     or _data file the template or pages read (input/includes/*, menu.xml); W1e add a new page listed in the IG,
     then W1f delete it. Each must show in output/ and after W1f output/ must equal a fresh full build (F1).
   - W2 resource edit: change a profile's description/short text (FSH: edit the .fsh source; otherwise the json/xml) so its rendered page changes.
   - W3 example edit: change a value in one example instance that something else references, if there is one.
   - W4 add a new example file (copy one, new id); W5 delete it.
   - W6 revert all edits (restore the original files).
   Each edit must show up in output/ (grep for the unique text). After W1f, after W2 and after W6, stop watch, and compare output/ with a fresh full build (no -rapido) of the same sources by int.jar (F2, F6). After W6 also compare with I1.
4. Compare rules: qa.json errors/warnings/hints counts and the set of messages (by file + message text) must be equal. Output files: same file set; per file, equal after masking build dates/timestamps (genDate, "Generated", footer dates, zip entry times, `meta.lastUpdated` the publisher sets), the run folder path, ValueSet expansion identifier/timestamp, UUIDs/random Bundle.id, qa timing/report files, and the order of the "used by" lists (known to vary between master runs). Anything else that differs is a FINDING.
5. For each FINDING, check whether master shows it too: rerun the same step with master.jar (for watch findings, the same edit sequence with master.jar `-rapido -watch`). If master shows the same difference it's `pre-existing`, otherwise `regression`. For a regression give the smallest reproduction (IG, commit, edit, file, expected vs actual).
6. Also record per IG: rebuild time of each W step for int vs master `-rapido -watch` (same heap), and whether the template has onCheck/ant script steps (a template that does not declare what its scripts read must get a full build).

## Reporting
- Results file: `qa2/results-<shard>.md` on the notes branch `claude/ig-publisher-perf-41aejs-notes` (fetch + rebase on that branch before each push; plain push, never force; terse commit messages, no footers, no model names, no em dashes). Update it after each IG: IG, commit, heap, R1/I1/I2/W*/F* times, QA counts, verdict (`same`, `pre-existing diffs: ...`, `REGRESSION: ...`, `skip: ...`), and for each regression the PR and a reproduction.
- Don't fix code, don't open PRs, don't comment on GitHub, never touch HL7/* or hapifhir/* repos (cloning public IG repos read-only is fine). Don't message other sessions. When your shard is done, write a final summary line at the top of your results file and stop.

## Survive context compaction
Auto-compaction is on. Keep `/home/user/qa/PROGRESS.md` current after every step (phase, current IG and step, jar paths and shas, heap per IG, paths of logs/snapshots, what's done, what's next, open findings), and re-read it (plus this plan) whenever you're unsure where you are. Push the results file after each IG so nothing lives only in your context.

## Phase D: page-only path with declared templates (added 18:20)
Phases B/C never reached the page rebuild: no real template declares `script-inputs`, so int always fell back (correct, but untested path).
1. Wait for qa2/script-inputs.md (dev session writes it: per base template, the `script-inputs` list and the recipe for a local copy + ig.ini override).
2. Per IG in the shard (all where a declaration is possible; skip ones whose template can't be declared, say why): use the declared copy for every build below, both jars.
3. int `-rapido -watch`: first build, then W1a..W1f, then W2 (a non-page edit), W1a again (page edit after a differential build). Log per step: mode (int must log Page Rebuild for W1a/W1b/W1c; W1d edit under a declared path, W1e add, W1f delete: whatever it logs, and why), seconds, edit visible in output.
4. Master `-rapido -watch` same template copy, same edits, same heap: seconds per step.
5. F1 = int full build (no watch) of the sources after W1f; F2 after the final W1a. Compare W1f vs F1 and final vs F2 with the round masks: output files and qa.xml issue sets. Any difference not in F1-F6 known lists = REGRESSION (stale page, missing page, wrong menu/toc, QA message lost or added).
6. Also once per shard: edit a file listed in script-inputs (e.g. menu.xml) and confirm a full build, output correct.
Results: "## Phase D" section in qa2/results-<shard>.md, one table row per IG plus notes.
