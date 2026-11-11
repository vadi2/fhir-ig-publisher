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
| profile (no PR) | session_013FrhV9XtpZJzHaw5CUT5RR | notes branch | n/a | running |
| early-publish | session_013FrhV8MAcq78uViFCFE1ve | perf3/early-publish | - | running |
| archives-reuse | session_013FrhV89txD9eAkM8cnaV3n | perf3/archives-reuse | - | running |
| native-skip | session_013FrhV7287Tkr83evM7MsqW | perf3/native-skip | - | running |
| summaries-reuse | session_013FrhV9QcWhADhYPtwEjkoZ | perf3/summaries-reuse | - | running |
| rapido-state | session_013FrhV9krvJaocgAxaWRabs | perf3/rapido-state | - | running |

## Wave 2 candidates (5 PR slots left; decide after profile.md lands)
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
