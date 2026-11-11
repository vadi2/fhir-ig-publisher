# Round 4 ("perf3") shared process. Follow exactly.

Work for Vadim Peretokin (vadi2) on the FHIR IG Publisher. Goal: `-rapido -watch` rebuilds on large IGs that are as
close to instant as possible, never stale or wrong. You are one of several independent cloud sessions; each owns one
idea and one branch. You own your VM. Nobody is watching: work autonomously to the end.

## Where things are
- Repo: vadi2/fhir-ig-publisher (fork of HL7/fhir-ig-publisher). Publisher only: no changes to org.hl7.fhir.core.
  If the only real fix is in core, write that down and stop.
- Base branch: `claude/ig-publisher-perf-41aejs` = upstream master + #1461 (watch rebuilds only changed pages, keeps
  Jekyll) + perf/watch-fsh-tail (FSH edits reuse kept Jekyll, html check records, AI step; build time pinned per
  watch day) + perf/weak-edges (weak dependency edges) + perf/native-outputs-threads (native outputs with -threads)
  + #1460 (signposts of reused validations). Branch from it; your PR's base is it.
- Earlier analysis (read what touches your idea): `git show origin/r3-handoff:round2-C-render-report.txt` (render and
  generation phase costs, ranked ideas), `...:round2-B-validation-report.txt`, `...:item2-tail.txt`, and
  `origin/r3-notes/<item>:*.txt`. They mention a local machine (~/Programs, bench.lock, taskset): ignore that part.
- Notes branch: `claude/ig-publisher-perf-41aejs-notes` (this file; later `bench-recipe.md` and `profile.md` from the
  profiling session). Fetch it again before you benchmark.

Known numbers on uz.dhp.core (older base, 12 cpus, -Xmx10g): profile edit rebuild 172 s of which loading/SUSHI ~16,
validation 6-10, narratives 6.4, native outputs 10.1, html 12.1, spreadsheets 2.4, summaries 3.3, Jekyll 25,
package + zips 15-17, html check + AI + full-ig.zip ~65. The stacked work brings a profile edit to roughly 100 s.

## Rules
1. Measure first. Before changing code, confirm on the base that the cost you are after exists (phase timings from
   the log, JFR, or temporary instrumentation that you do not commit). If the saving turns out under about 1 s per
   rebuild, or the idea is unsafe, stop: no PR; say so in your last message with the numbers.
2. Smallest correct change. Output must equal a fresh complete (non-rapido) build apart from timestamps, QA error and
   warning counts equal. A build that can't prove reuse is safe does the full work. Builds without -rapido/-watch
   are unchanged unless your brief says otherwise.
3. Tests: mutation-checked (every new test fails with its change reverted). Run the org.hl7.fhir.publisher.core
   suite in full. Known on this VM: GitUtilitiesTests.testGetGitSource[4] fails on the base too (git URL rewrite);
   5 tests need a non-root user. Failures already on the base are not yours: note them.
4. Benchmark A/B: base jar vs your jar, interleaved sessions, >= 3 rebuilds of each edit kind each, medians. Never
   invent or extrapolate numbers. If it can't run, write "bench: pending (why)" in the PR.
5. Review loop (after tests and bench, before the PR), with the PR review toolkit, NOT the built-in /code-review:
   - `claude plugin install pr-review-toolkit` (works here; it installs from anthropic-plugin-directory). Its files:
     `~/.claude/plugins/cache/anthropic-plugin-directory/pr-review-toolkit/*/{commands/review-pr.md,agents/*.md}`.
   - Run every toolkit agent on the full branch diff (base..HEAD): code-reviewer, silent-failure-hunter,
     pr-test-analyzer, comment-analyzer, type-design-analyzer, code-simplifier. If the plugin's agent types are not
     selectable in your running session, start one general-purpose subagent per agent with that agent's .md as brief.
   - code-simplifier: apply what it gives as is (tests must still pass).
   - All the others: ignore nit findings. For each non-nit finding, verify it empirically (a failing test, a
     reproduction, a measurement, or the core source it relies on). Implement only the ones that check out, each as
     a new commit. Write down what you rejected and why.
   - Repeat until a round confirms nothing (at most 5 rounds).
6. Commits: terse messages. No footers, no attribution lines, no model names, no em dashes. Never force push or
   rewrite pushed history; fixes are new commits. Push only to your own branch.
7. PR: to vadi2/fhir-ig-publisher ONLY (never HL7/*, never hapifhir/*), base `claude/ig-publisher-perf-41aejs`, head
   your branch, ready for review (not draft). At most one PR. Description: short symptom intro, `### Cause`,
   `### Fix`, one tests line, a numbers table (base, this PR, saved), then one line on review rounds. No em dashes.
   Never comment on, review or edit any other PR or issue anywhere.
8. Right after creating the PR, call `unsubscribe_pr_activity` for it (also if you never subscribed). Do not watch
   it and do not wait for events. The orchestrator checks PRs itself and will message you if something needs fixing.
9. Do not message the orchestrator or any other session (no send_message). Do not stream progress anywhere. End
   with one line: `<slug>: PR #<n> | dropped (<reason>) | blocked (<reason>)`, plus the numbers table.
10. Keep a short notes file in your scratchpad (state, decisions, numbers) so you survive context compaction.

## Bench (uz.dhp.core)
- IG: https://github.com/uzinfocom-org/digital-health-ig at 2c39cd0a (template fhir2.base.template#current).
- Needs JDK 21 (installed), Ruby + Jekyll (`gem install jekyll`), SUSHI (`npm i -g fsh-sushi`), tx https://tx.fhir.org.
- Jar: `mvn -T 4 -DskipTests package`, then `org.hl7.fhir.publisher.cli/target/org.hl7.fhir.publisher.cli-*.jar`.
- Session, on a fresh copy of the IG with a warm tx cache and its own package cache folder:
  `IG_GATE_OFF=1 java -Xmx<N>g -jar <jar> -ig . -rapido -watch -tx https://tx.fhir.org -package-cache-folder <dir>`
  Steps: first build, prof1, prof2, restore-prof, inst1, inst2, restore-inst, page1, restore-page.
  prof edit: UZCoreSpecimen `* status ^short` text change, seen in output/en/StructureDefinition-uz-core-specimen.html.
  inst edit: UZCoreObservation blood-pressure example component valueQuantity 120 -> 13n, seen in
  output/Observation-blood-pressure-example.json. page edit: a sentence in a page under input/pagecontent.
  Time each from the edit to "Finished @" in the log (and to whatever your idea makes earlier), and check the
  edited text is in output. Sessions base, change, change, base (interleaved). Same flags for both jars.
- OOM FIX (verified by the orchestrator 14:50 UTC): the memory cgroup caps RAM at 14.3 GB but swap is not capped
  (memory.memsw.limit_in_bytes is unlimited) and root can add swap. Before any IG build run:
  `fallocate -l 16G /home/user/swapfile && chmod 600 /home/user/swapfile && mkswap /home/user/swapfile && swapon /home/user/swapfile`
  Then the cgroup swaps cold pages instead of killing java or Jekyll. Use the same heap for both jars (-Xmx9g is a
  reasonable start; 7g ran out of heap), and watch `vmstat 5` si/so during measured rebuilds: if swapping is more
  than a trickle, lower the heap and say so.
- Memory: the VM has 15 GB and 4 CPUs. -Xmx10g was OOM-killed by the cgroup in earlier sessions (java RSS ~12 GB +
  Jekyll ~1.9 GB). Start with -Xmx7g; if that doesn't work find the largest heap that does and say which you used.
  One publisher at a time. If `bench-recipe.md` is on the notes branch, use its settings and driver script.
- Output check: after the last restore, compare output/ with a fresh complete non-watch build by the same jar
  (mask the known timestamps); QA counts equal across base, change and fresh.
