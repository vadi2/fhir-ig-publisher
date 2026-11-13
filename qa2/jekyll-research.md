# Jekyll partial re-rendering: research (2026-11-13)

## 1. What Jekyll supports today
- **Releases:** the latest is 4.4.1 (2025-01-29). A 4.5.0 release PR has been open since April 2026 and is still unreleased ([#9966](https://github.com/jekyll/jekyll/pull/9966)); it was retitled from 5.0.0 to 4.5.0 in September 2026. Nothing in it touches incremental builds. ([rubygems](https://rubygems.org/gems/jekyll/versions), [releases](https://github.com/jekyll/jekyll/releases))
- **`--incremental` / `incremental: true`:** the docs still call it *"experimental… will not work correctly in every scenario"* ([docs](https://jekyllrb.com/docs/configuration/incremental-regeneration/)).
  - **What it tracks** (read in `lib/jekyll/regenerator.rb`, `renderer.rb`, `tags/include.rb`): each page's mtime, plus layouts and includes as dependencies, stored in `.jekyll-metadata`.
  - **What it does not track:** `_data`, `site.pages`/`site.posts` loops, and plugin- or generator-made pages.
  - **It is not really partial.** `Site#process` still runs reset, read and generate over the whole site. Only render and write are skipped for pages it considers unchanged.
  - **Public hooks into it:** front matter `regenerate: true`, and `site.regenerator.force(path)`, which is a public method.
- **Known bugs and history:**
  - `_data` changes are ignored ([#9772](https://github.com/jekyll/jekyll/issues/9772), open; also [#7682](https://github.com/jekyll/jekyll/issues/7682)).
  - The data-file fix ([#8771](https://github.com/jekyll/jekyll/pull/8771), 4.3.0) was reverted in 4.3.1 ([#9170](https://github.com/jekyll/jekyll/pull/9170)) because it broke `site.<collection>` ([#9165](https://github.com/jekyll/jekyll/issues/9165)). On #9772 ashmaroli said: *"couldn't come up with a viable solution that would maintain backwards-compatibility."*
  - A stale `.jekyll-metadata` is reused across runs and environments ([#9755](https://github.com/jekyll/jekyll/issues/9755), open, no response).
  - mtimes are unreliable ([PR #9166](https://github.com/jekyll/jekyll/pull/9166), still a draft and parked "until v5").
  - On the forum, users report stale includes and collections, and slow metadata writes ([1](https://talk.jekyllrb.com/t/includes-and-collections-not-updating-properly/10200), [2](https://talk.jekyllrb.com/t/building-just-specific-pages-when-developing/4659)).
  - **Verdict:** effectively unmaintained.
- **`--watch`, `serve`, livereload:** these re-run the whole `Site#process` on every change. They save nothing beyond what `--incremental` saves.
- **Plugin API:** hooks are `:site` after_reset/post_read/pre_render/post_render/post_write, plus per-page hooks. `Site#process` is public. There is **no supported "render these N pages" API**: `render_regenerated` is private.

## 2. Proposals to make incremental builds reliable
- [#4269](https://github.com/jekyll/jekyll/pull/4269) "Incremental regen improvements": closed 2024, not merged.
- [#9148](https://github.com/jekyll/jekyll/pull/9148) "Extract incremental logic": its reviews were never done, and the author closed it in 2024.
- [#9166](https://github.com/jekyll/jekyll/pull/9166): a digest-based check, still a draft. In that thread ashmaroli asked for a **pluggable cache-strategy interface** (polymorphism over conditionals) and said a strategy "could be a plugin".
- The [v5 roadmap #9156](https://github.com/jekyll/jekyll/issues/9156) has one build item, "SHA-digest incremental". It says nothing about dependency tracking, and its last comment is from December 2024.
- **Blockers:** SemVer and backwards compatibility, and very little maintainer time.

## 3. Black-box alternatives
1. **Reduced staging site.** Hard-link `_config.yml`, `_layouts`, `_includes`, `_data`, assets and the changed pages into a temporary source, run `jekyll build` on it, and copy the outputs back.
   - **It matches a full build when** templates only read `page.*`, `site.data.*` and includes. IG templates mostly drive navigation and TOC from `_data`, not `site.pages`, and relative links are plain strings.
   - **What breaks:**
     - `site.pages`, `site.html_pages`, `site.static_files` and collection loops see only the subset.
     - `{% link %}` and `{% post_url %}` raise errors when their target is missing.
     - `include_relative`.
     - Plugins such as sitemap.
     - `site.time`.
   - **Mitigation:** byte-compare the reduced output against a full build in CI, and fall back to a full build whenever a layout, include or `_data` file changes, or a template uses one of the constructs above.
2. **Drive Jekyll's own `--incremental` from the Publisher**, using documented behaviour only:
   - Rewrite a staging file only when its content changed, so its mtime is preserved.
   - Inject the dependencies the Publisher knows about by setting `regenerate: true` on dependent pages, or by touching them.
   - Delete `.jekyll-metadata` before every full build.
   - **Risk:** read and generate still cover all 80k pages, so measure first. The 3 s goal may not be reachable this way.
3. **Plugins:** `jekyll-incremental` (envygeeks) is dead: last release 2017, targets Jekyll 3 ([rubygems](https://rubygems.org/gems/jekyll-incremental)). No maintained alternative exists.

## 4. Who and where
- **Core team** ([team page](https://jekyllrb.com/team/)):
  - Matt Rogers **@mattr-**: the only clearly active maintainer. He merged CI and release work in 2026, including [#9760](https://github.com/jekyll/jekyll/pull/9760) and [#9954](https://github.com/jekyll/jekyll/pull/9954).
  - Ashwin Maroli **@ashmaroli**: owns the incremental code. His last visible activity is early 2025.
- **Emeritus:** Parker Moore **@parkr** still reviews (approved #9760 in April 2026).
- **Recent merges are mostly docs PRs** ([commits](https://github.com/jekyll/jekyll/commits/master)).
- **Venue:**
  - The issue chooser sends Q&A to [talk.jekyllrb.com](https://talk.jekyllrb.com/).
  - Feature requests go in a GitHub issue using the [feature_request template](https://github.com/jekyll/jekyll/blob/master/.github/ISSUE_TEMPLATE/feature_request.md). The template asks "why not a plugin?" and points to the "90% of users" philosophy.
  - [Discussions](https://github.com/jekyll/jekyll/discussions) are enabled but almost dead.
- **Comment on rather than duplicate:** #9772 (data files) and #9156 (v5). A new feature issue would be best, cross-linking #9772, #9166 and #9148.

## 5. Recommendation
Upstream support is unlikely within the next 6–12 months. Jekyll has one active maintainer and an unreleased minor version. Its own data-file fix was reverted, and the incremental refactors were abandoned.

File the request anyway, because it is cheap. Frame it as the plugin interface that ashmaroli himself suggested, not as "fix incremental". Meanwhile ship option 1, with a CI equivalence check, or measure option 2.

**Draft issue: "feat: public API to render a subset of pages (pluggable regeneration strategy)"**

> The HL7 FHIR IG Publisher generates Jekyll sites of 10k–80k pages and runs `jekyll build` on them. In authoring/watch mode a single source change affects a handful of pages, but a full build takes ~70 s; rendering only affected pages takes ~3 s. The caller already knows exactly which pages are affected (it owns the dependency graph, including `_data`), so we don't need Jekyll to infer dependencies — we need a supported way to tell it. Today the only route is `--incremental`, which is mtime-based, ignores `_data` (#9772), and can't be steered except via `regenerate: true`; anything finer means monkey-patching private methods (`Site#render_regenerated`), which we don't want to ship. Minimal ask: (a) a public, documented regeneration-strategy interface — e.g. `site.regenerator = MyStrategy.new(site)` or a `regenerator_class` config — that decides `regenerate?(item)` (as suggested in #9166), **or** (b) `jekyll build --only path1,path2` / `Site#process(only: [...])` that reads the full site (so `site.pages`/`site.data` stay complete) but renders and writes only the listed pages, leaving other output untouched. Either would make external incremental builds possible without relying on internals. We are happy to contribute the PR and tests.
