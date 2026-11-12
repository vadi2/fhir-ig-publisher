# perf3 orchestrator state (survives compaction; also pushed to origin/claude/ig-publisher-perf-41aejs-notes)

## Task (from Vadim, 2026-11-11)
Speed up `-rapido -watch` further, toward instant. I (orchestrator) think and delegate ALL work to FABLE agents
(cloud sessions via create_session, model claude-fable-5-2, each its own VM; Vadim corrected Opus -> Fable 14:20,
Fable weekly quota is usable until Tuesday). Each agent: implement, bench, run the
pr-review-toolkit loop (code-simplifier as is; others: drop nits, verify empirically, fix confirmed only), then PR to
vadi2/fhir-ig-publisher (never HL7), then unsubscribe from the PR. Agents must not stream results to me. Up to 10
PRs. A 30-minute watcher checks agents aren't stuck and that open PRs' CI passes (fix via the owning agent).
Commit style: terse, no footers/attribution/model names, no em dashes.

## Setup done
- Base branch (PR base): `claude/ig-publisher-perf-41aejs` @a4d1e90f = weak-edges + watch-fsh-tail + native-outputs-threads
  + rapido-reused-signposts (#1460). NOT in base: rapido-ig-entry-text (#1466, conflicts in RapidoTracker). Compiles.
- Notes branch: `claude/ig-publisher-perf-41aejs-notes` (PROCESS.md, this file; profiler adds bench-recipe.md, profile.md).
  Local worktree: /home/user/perf3-state/notes-wt (main repo: /home/user/fhir-ig-publisher).
- Fork has no CI: fork PRs show 0 check runs (workflows only target master; real tests are HL7's Azure pipeline).
  So "CI" = the agent's local full publisher.core suite; watcher still checks get_check_runs/get_status in case.
- Child branch names: perf3/<slug>. Plugin install verified: `claude plugin install pr-review-toolkit`.
- Watcher: two hourly scheduled tasks firing into this session: trig_0126Fp1UmpBHLx5MBc32MKtz (min 17), trig_017gZTMAVWsXJZ2Vox7gh9So (min 47).

## Sessions (wave 1, Fable, launched 14:21 UTC; the six Opus sessions from 14:17 were archived, nothing pushed)
| slug | session id | branch | PR | status |
|---|---|---|---|---|
| profile (no PR) | session_013FrhV9XtpZJzHaw5CUT5RR | notes branch | n/a | DONE (profile.md 18:16). Ignore it in ticks. |
| replay-stamp (wave 2) | session_013FrhV8oUf4Hg2ZFmpfFFgD | perf3/replay-stamp | none | DROPPED: PR #39 (rapido-state) already trusts file stamps in canReplay (7.5 -> 0.3 s) |
| early-publish | session_013FrhV8MAcq78uViFCFE1ve | perf3/early-publish | #42 | DONE. PR 22:29, 0 checks, clean. page in output: prof 270.2 -> 170.1, inst 262.8 -> 156.1, page 23.8 -> 2.7 s. Also moves System.gc() after early publish. |
| archives-reuse | session_013FrhV89txD9eAkM8cnaV3n | perf3/archives-reuse | #41 | DONE. PR 22:14, 0 checks, clean. archive step 28.7 -> 4.2 s (prof), 28.9 -> 3.5 (inst); whole-rebuild delta 46-52 s has ~20-25 s unexplained (disturbed base runs). +2732 lines. Combined package NOT kept (core mutates resources). |
| native-skip | session_013FrhV7287Tkr83evM7MsqW | perf3/native-skip | - | running |
| summaries-reuse | session_013FrhV9QcWhADhYPtwEjkoZ | perf3/summaries-reuse | - | running |
| rapido-state | session_013FrhV9krvJaocgAxaWRabs | perf3/rapido-state | #39 | DONE. PR open 19:54, 0 checks, mergeable clean. prof -14.8 s, inst -10.3 s, page 0 |
| narrative-skip (wave 2, stacked: PR base perf3/native-skip) | session_013FrhV6FEdUbpxzt5WmGXJF | perf3/narrative-skip | none | DROPPED 22:04: saves ~2 s of 272 s, not provably equal to a fresh build (~65 USD) |
| translations-dirty (wave 2) | session_013FrhV5h5r3K9VwTsXxmcW2 | perf3/translations-dirty | #43 | DONE. PR 00:46, 0 checks, clean. translation step 4.96 -> 0.22 s; P/F -4 to -5 s on FSH edits; GC change left to #42 |
| qa-prep-reuse (wave 2) | session_013FrhV6SxKUeigqo6vSYCQF | perf3/qa-prep-reuse | #40 | DONE. PR 22:02, 0 checks, clean. QA prep 7.6 -> 2.3 s (prof), 8.1 -> 2.2 (inst), 3.8 -> 1.0 (page); page rebuild 19.6 -> 16.2 s |
| tail-restart (wave 2, stacked: PR base perf3/early-publish) | session_013FrhV6xnSEZUwAnAQoiLim | perf3/tail-restart | - | running (launched 19:18) |

## Wave 2 (launched 18:19 from profile.md; 10 PRs max = 5 wave 1 + 5 wave 2)
- replay-stamp = profile opp 2 (canReplay by size+mtime, ~11 s before Jekyll). Given to the profiler session by
  send_message; told to test-push perf3/replay-stamp first and end "blocked (push refused)" if its outcome branch
  setting forbids it -> then launch a fresh session for it (base branch, outcome_branch perf3/replay-stamp).
- narrative-skip = opp 1; translations-dirty = opp 10 (+12 System.gc); qa-prep-reuse = opp 7.
- tail-restart = opp 4: launch when perf3/early-publish is on origin: create_session source_revision
  perf3/early-publish, outcome_branch perf3/tail-restart, PR base perf3/early-publish. Brief: a change saved while
  a build runs (monitoring is off while building, Publisher.waitForChanges) waits for the whole tail (109 s after
  Jekyll on the VM); make the post-publish tail abortable at safe points without saving Rapido state or leaving
  half-written outputs, then start the next build; mid-build restart before Jekyll only if safe.
- Reserve if a PR slot frees up (a session drops): opp 6 full-ig.zip beside QA (~13 s F), opp 8 sync only what was
  written (~7 s F), opp 5 html check memory (GC-bound, mostly small-heap). Rejected: opp 3 load while SUSHI and
  opp 11 kept parsed sources (item 6 tried and dropped), opp 9 combined package (core + shared context), warm SUSHI
  (profile: saves 1 to 4 s only).

## Old wave 2 candidates (superseded by the section above)
- narrative-skip: skip narrative generation for clean resources (6.4 s); stack on perf3/native-skip (its PR lists what
  reads a clean resource's narrative).
- tail-restart: a change arriving during the post-publish tail (or mid-build) abandons it and starts the next build;
  stack on perf3/early-publish.
- warm SUSHI (keep node + FHIR definitions loaded between runs) if SUSHI is >= ~5 s of an FSH rebuild.
- load phase: whatever profile.md shows is redone per rebuild (context init, tx cache parse 0.7 s, snapshots).
- Jekyll / html check / AI / full-ig.zip residue after item 2, per profile.md.
- zips on a worker thread while Jekyll runs (round2-C idea 3) if Jekyll still leaves the JVM idle.
- The profiler session keeps its bench rig: give it a wave-2 idea via send_message when profile is done.

## Watcher procedure (each tick; keep it cheap)
1. get_session for each session not marked done/dropped. status_bucket working -> fine. If idle/blocked/failed and no
   PR: read its last assistant message (list_events kinds=["assistant"], limit 100) and decide: nudge with
   send_message (specific), or mark dropped/blocked here. If working but updated_at is > 45 min old: check events,
   interrupt + nudge if stuck in a loop.
2. list_pull_requests vadi2/fhir-ig-publisher state=open, fields number,title,head; for head perf3/*: record PR in the
   table; get_check_runs (and get_status). If a check fails: send_message to the owning session with the failure,
   asking it to fix, push, and unsubscribe again. No checks at all is expected on the fork.
3. If profile.md is on the notes branch and wave 2 not launched: `git fetch origin claude/ig-publisher-perf-41aejs-notes`,
   read profile.md, pick wave 2 (max 10 PRs total), launch with create_session (same shape as wave 1; brief = read
   PROCESS.md + idea-specific text; stacked ideas use the parent branch as source_revision and PR base).
4. Update this file, commit, push (plain push, notes branch).
5. When all sessions are done/dropped and every PR is recorded: delete both scheduled tasks, give Vadim the final
   summary (PR list with numbers, what was dropped and why, no CI on fork).

## Log
- 14:19 wave 1 launched on Opus. 14:21 archived and relaunched on Fable per Vadim.
- 14:48 tick: all 6 working, no PRs, no profile yet. Several hit bench OOM (7g heap OOM, 9g cgroup kill). Found: cgroup
  caps RAM at 14.3 GB but swap is uncapped and swapon works as root -> added to PROCESS.md and sent to all 6
  (send_message priority next). Future wave-2 briefs: PROCESS.md already carries the swap fix.
- 15:18 tick: all 6 working (rapido-state shows IDLE between background builds: normal, bucket still working). No PRs,
  no profile/bench-recipe yet; sessions still settling on a heap (8g/7600m/9g). Swap fix now truly verified: a process
  touched 16 GiB under the 14.3 GB cap with a swap file on and survived.
- 15:47 tick: all 6 working, no PRs, no bench-recipe/profile yet. Profiler: 7g + string dedup got through a first
  build with swap (java 8.1 GiB, Jekyll 3.5 GiB), now timing rebuilds. native-skip: tests pass, mutation checks.
  NOTE: list_events is expensive (~10k tokens per 25 events, thinking signatures). Use it only for a session that is
  idle without a PR or silent > 45 min; get_session's task_summary is enough otherwise.
- 16:18 tick: all 6 working. summaries-reuse pushed perf3/summaries-reuse (PR expected next). Profiler still tuning
  heap (trial 7) after 2 h -> told it to stop tuning, push the recipe as is and do profile.md (profile+page edit first).
  Decided NOT to launch warm-SUSHI: r3-notes/item6 says SUSHI is only ~4 s on DHP, and item 6 (SUSHI alongside start-up,
  kept state, one package cache manager) was already tried and dropped. Wave 2 still waits for profile.md.
- 16:47 tick: all 6 working, no PRs. Branches pushed: summaries-reuse, native-skip, rapido-state. bench-recipe.md +
  bench/session.py landed (interim): -Xmx7600m + string dedup + swap; on this VM first build ~688 s, profile edit
  272-295 s (~2.5x Vadim's machine), SUSHI ~49 s per FSH rebuild here (suspicious vs ~4 s in item6 notes: check
  profile.md's SUSHI breakdown before acting), one bench session ~45 min, so an A/B (4 sessions) is ~3 h. Expect PRs
  late. Cheap tick recipe: git ls-remote perf3/* + list PRs; get_session only for sessions whose branch didn't move.
- 17:18 tick: no PRs. Heads: native-skip aaa2fcfa (unchanged), rapido-state faa64d37 (moved), summaries-reuse bceb1113
  (unchanged). Profiler now in profiling runs; early-publish in A/B chain; archives-reuse 51 tests pass, building jars.
  Next tick: get_session native-skip + summaries-reuse if their heads still haven't moved.
- 17:48 tick: no PRs (highest PR# is 38). All 6 working: profiler session A rebuilds (~30 min left, profile.md after);
  early-publish A/B + review round 2; archives-reuse fixing 4 tests, reviewers launched; native-skip base run b1
  (change cuts native phase 30 -> 10 s on the VM); summaries-reuse and rapido-state: review done, A/B running, ~3 h.
  PRs expected ~20:30-21:00 UTC. Until ~20:00 ticks can be just: notes branch ls-tree (profile.md?) + ls-remote
  refs/pull (new PR# > 38?) + perf3 heads; skip get_session unless something looks off.
- 18:17 tick: profile.md landed (profile edit on VM 282.8 s to Finished, page in Jekyll staging at 172.2; page edit
  19.4 / 2.6). No PRs yet (highest 38), perf3 heads unchanged. Launched wave 2 (see section). get_session create
  shape: model claude-fable-5-2, source_url https://github.com/vadi2/fhir-ig-publisher, source_revision, outcome_branch
  perf3/<slug>, permission_mode auto, tags [perf3].
- 18:52 tick: no PRs (highest 38). Wave-2 branches exist (narrative-skip, translations-dirty, qa-prep-reuse);
  native-skip head moved to 629ed289. The profiler did NOT take replay-stamp: a send_message is peer data, so it held
  the new task "for go-ahead". LESSON: new tasks only via create_session; send_message only for info or fixes inside
  a session's own task. Launched a fresh replay-stamp session and told the profiler the task is withdrawn.
  early-publish still benchmarking (c2 done, c3 running), branch not pushed -> tail-restart still pending.
- 19:17 tick: no PRs (highest 38). perf3/early-publish pushed (3a04f3ac) -> launched tail-restart. native-skip head
  1686b40d (moving). All 10 PR slots now have an owner: 5 wave 1 + replay-stamp, narrative-skip, translations-dirty,
  qa-prep-reuse, tail-restart. Launch nothing more unless a session drops (then see Reserve).
- 19:48 tick: no PRs (highest 38). archives-reuse pushed (498c93f3); narrative-skip moving (7d5a7513). summaries-reuse
  (verifying list reuse in a real rebuild) and rapido-state (fresh vs watch output compare) working. replay-stamp and
  tail-restart have no branch yet (just launched). NOTE: get_session rate_limit_info now says status allowed_warning,
  seven_day_overage_included (weekly Fable quota getting close; resets Tue 2026-11-17 12:00 UTC). Told Vadim.
  If sessions start failing on rate limits: do not relaunch; record and report.
- 20:18 tick: PR #39 = rapido-state (state kept in memory, expansions stored once by hash, stamps before re-hash,
  atomic state write; prof 245.1 -> 230.3, inst 234.7 -> 224.4, page unchanged; 2 review rounds). No checks (expected).
  Its tests line: 2 ParallelFolderZipTest errors (InvalidPath, non-ASCII name under POSIX locale) "not run on the
  base" -> I am running that class on the base in /home/user/fhir-ig-publisher (log: scratchpad/pfz-base.log); if it
  passes on base, message rapido-state to look again. replay-stamp dropped itself (duplicate of #39), cost ~1 USD.
  One PR slot is free (9 owners). Decision: do NOT fill it now (quota warning; reserve ideas are tail-only and would
  conflict with tail-restart/early-publish). If tail-restart drops, launch opp 6 (full-ig.zip beside QA) from base.
- 20:40: ParallelFolderZipTest on the base @a4d1e90f in my checkout: 16 run, 2 errors, the same two tests and the same
  InvalidPathException as PR #39 reports -> pre-existing (POSIX locale), not from #39. Added to PROCESS.md rule 3.
- 20:47 tick: no new PRs (highest 39). summaries-reuse: 495/495 list sets reused, waiting on last base + change bench
  sessions. translations-dirty: translate phase 5.1 -> 0.2 s on pair 1, 4 timed runs left. qa-prep-reuse: review round
  1 settled, waiting on last two bench sessions. tail-restart pushed a branch (451d3bf1); narrative-skip moving.
  Not checked this tick: early-publish, archives-reuse, native-skip, narrative-skip, tail-restart (check next tick
  if no PR/head movement).
- 21:17 tick: no new PRs (highest 39). early-publish: waiting on base session b2, then "drill" + fresh build (still
  Fable). archives-reuse: base session 2 running (cost ~99 USD so far). native-skip: n2 rerun after a tx.fhir.org 502.
  archives-reuse and native-skip now run on claude-opus-5-5 (get_session: user_switched_model) and this orchestrator
  was switched to Opus 5.5 by Vadim at ~21:00: so Vadim is moving work off Fable (quota). Do not switch models
  myself; new sessions, if any, should use claude-opus-5-5 unless Vadim says otherwise.
- 21:47 tick (cheap): no new PRs (highest 39). Moving: narrative-skip 43e03258, qa-prep-reuse 8e8b5861. All sessions
  were seen working at 20:47/21:17; no get_session this tick.
- 22:17 tick: PRs #40 (qa-prep-reuse) and #41 (archives-reuse) opened, no checks (expected), both mergeable clean.
  Open PRs: #39, #40, #41. Dropped: replay-stamp. Still working: early-publish, native-skip, summaries-reuse,
  narrative-skip, translations-dirty, tail-restart.
- 22:49 tick: PR #42 (early-publish) opened, no checks, clean. Open: #39 #40 #41 #42. #42 moves System.gc() ->
  told translations-dirty to leave out its optional GC commit. tail-restart stacks on #42 (its PR base perf3/early-publish).
- 23:17 tick: no new PRs (highest 42). summaries-reuse: fresh build for output compare, then test + PR.
  translations-dirty: waiting on output check. native-skip: n3 bench running; its output compare vs fresh was at
  "1,205 diffs after masking" (still investigating; watch for a drop or a PR that explains them). All three now on
  Opus 5.5 (Vadim switched them). tail-restart moved (14c0cdb0).
- 23:47 tick: no new PRs (highest 42). narrative-skip DROPPED (only ~2 s, not provably equal). 2 slots free now
  (replay-stamp, narrative-skip). Decision: launch nothing more. Remaining reserve ideas (full-ig.zip beside QA, sync
  only what was written, html check memory) only shorten save->Finished, not save->page (early-publish already took
  the tail off the page path), they overlap tail-restart/#42, and sessions cost 30-100 USD each. Told Vadim; launch
  only if he asks.
- 00:18 tick: no new PRs (highest 42), no branch moved. summaries-reuse: base watch session for compare, then PR.
  translations-dirty: base-jar watch ~40 min, PR draft ready. tail-restart: A/B bench ~2 h left (from 23:10), 108 USD.
  native-skip: idle since 22:40 with "n3 build running" (98 min) -> nudged (check n3, carry on; drop if the 1,205 output
  diffs are not the base's own). If still silent at 01:17, read its last assistant events.
- 00:47 tick: PR #43 (translations-dirty) opened, no checks, clean. Open: #39-#43. native-skip answered the nudge
  (active 00:44, "waiting on b2 to finish"). Remaining: native-skip, summaries-reuse, tail-restart.
- 01:21 tick (cheap): no new PRs (highest 43), no head moved. native-skip was active at 00:44 (waiting on b2);
  summaries-reuse active 00:17; tail-restart bench due ~01:10. Check all three at 01:47 if still no PR.
