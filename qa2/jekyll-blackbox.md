# Jekyll as a black box: incremental page rebuilds (perf4/jekyll-blackbox)

Goal: cut the Jekyll step of `-rapido -watch` page rebuilds on large IGs while running the `jekyll` CLI as an
external process with documented features only (Grahame's constraints on #1461).

## Setup
- Staging folder: `temp/pages` after the first `-rapido -watch` build of uz.dhp.core (2c39cd0a,
  fhir2.base.template#current) by the HL7 master jar 2ccca17. 101,236 files, 2.7 GB; `_includes` 78,548 files
  (2.2 GB), `_data` 13 files (4.5 MB), no `_layouts` (page stubs inline the layout and include fragments), 14,600
  files with front matter, 10,944 rendered pages.
- Jekyll 4.4.1, Liquid 4.0.4, Ruby 3.3.6. 4 vCPU, 15 GB VM. Standalone runs: `jekyll build --destination <d>` in
  the staging folder, nothing else running. Medians of 3 unless noted.
- In the publisher (master, java resident with -Xmx7600m): Jekyll took 106.9 s on the first build and 103.9 s on a
  page-edit rebuild (master rebuilds everything on a page edit).
- The template's includes read only `site.data` (587 uses); no `site.pages`, `site.static_files`, `site.time`,
  `{% link %}`, `{% post_url %}` or `include_relative`. Liquid 4.0.4 has no `render` tag.

## (b) Jekyll's own `--incremental`
| case | wall s (runs) | pages rendered | output vs full build |
|---|---|---|---|
| full `jekyll build` | 72.2 (72.2, 73.7, 72.2; a 4th with --profile 84.6) | 10,944 | reference |
| `--incremental`, no `.jekyll-metadata` | 69.9 | 10,944 | identical |
| `--incremental`, nothing changed | 4.6 (4.6, 4.1, 4.6) | 0 | identical |
| one page's content (`_includes/en/index.md` rewritten) | 4.9 (5.3, 4.9, 4.9) | 1 (`en/index.html`) | identical (0 diffs, full tree) |
| one resource fragment (`_includes/StructureDefinition-uz-core-specimen-snapshot-en.xhtml`) | 5.0 | 1 | identical (0 diffs) |
| two output pages missing from the destination | 5.1 | 2 | (re-rendered, as documented) |
| a shared include (`fragment-footer.html`) | 72.6 | 10,944 | (all re-rendered, correct) |
| a `_data` file (`info.json` copyrightyear) | 4.2 | 0 | **stale** (`2025+` kept, full build has `2025+x`) |

- `--profile` phases, full build: READ 2.35 s, RENDER 77.4 s, CLEANUP 0.32 s, WRITE 3.3 s. No change with
  `--incremental`: READ 1.9 s, RENDER 0.84 s (the regenerate checks), CLEANUP 0.33 s, WRITE 0.39 s.
- Peak RSS: full build 3.5 GB, incremental 0.25 GB. `.jekyll-metadata` is 4.4 MB.
- What Jekyll 4.4.1 tracks (`regenerator.rb`, `tags/include.rb`, `renderer.rb`): each page's and static file's
  mtime, its layouts, and every `include`/`include_relative` it rendered (dynamic names included, recorded at render
  time); a page whose destination file is missing is rendered again. Not tracked: `_data`, `_config.yml`,
  `site.*` collections, plugins.

## (a) Reduced staging site
`_config.yml` hard-linked, `_data` and `_includes` symlinked, only the listed pages hard-linked, `jekyll build` into
an empty folder.
| case | wall s | output vs full build |
|---|---|---|
| `en/index.html` alone | 1.1 (1.09, 1.09, 1.11) | identical |
| 400 random pages (3 different samples) | 3.0, 3.3, 2.6 | identical (0 of 1,200 differ) |

Setup (links) 0.01 s. Breaks with: `site.pages`/`site.html_pages`/`site.static_files`/collections loops (see a
subset), `{% link %}`/`{% post_url %}` to a page not in the subset (error), `include_relative` to a file not
linked, plugins (sitemap etc.), `site.time`. None is used by fhir2.base.template's includes.
The deciding problem: (a) needs the publisher to know every page that includes a changed fragment. Only Jekyll's
own dependency record knows that (an IG page can `{% include %}` another page's fragment), and reading
`.jekyll-metadata` (a Marshal dump) would be reading Jekyll internals. (b) gets it from Jekyll itself.

## What the publisher rewrites between watch builds (master 2ccca17, same session, hard-link snapshots of temp/pages)
| rebuild | Rapido | files rewritten | of which | Jekyll in the publisher |
|---|---|---|---|---|
| first | complete | all 101,236 | | 72.6 s |
| profile edit | differential (193 of 811) | 10,428 + 28 new | `_includes` 6,978, en/ru/uz stubs 836 each, root 832 (resource json), assets 97, `_data` all 13 | 67.4 s |
| page edit (index.md) | differential (94 of 811) | 9,936 (874 MB) | `_includes` 6,576, stubs 836 x3, root 741, assets 97, `_data` 13, `_rapido/state.json` (188 MB) | 70.7 s |
- Temp is cleared only on a Rapido complete build; a differential build writes into it again. Most rewrites are in
  place (same inode). Of the 390 that were recreated, 389 are byte-identical (the other is package.db).
- So "rewrite only when the content changed" would mean changing every writer (FileUtilities calls, template Ant
  scripts copying assets, xslt writing _data). Instead the Jekyll step hashes what changed since Jekyll's last run
  (size+mtime differ) and gives a file with the same SHA-256 its old mtime back. SHA-256 here (SHA-NI): 1.9 GB in
  2.1 s warm; 2.4 GB with a cold cache 10 to 17 s (first build only hashes everything).
- Between two separate sessions every page stub differs (per-session UUID, `HierarchicalTableGenerator.uuid`), and
  4 `_data` files; a Rapido complete build therefore always renders every page.

## Things that rule out reusing the staging folder as Jekyll's destination
- `HTMLInspector.fillPublishBox` rewrites Jekyll's pages in place (marker -> publish box). A page whose box is
  already filled reads as `hl7State=false` (no publish box), which changes QA. OutputFolderSync also moves changed
  files out of staging. So Jekyll gets its own destination `temp/pages/.watch-jekyll` (Jekyll skips dot names:
  checked, output identical to a full build elsewhere, 0 diffs) and the build gets a copy in staging (all 20,698
  output files: 3.6 s; per rebuild only what changed since the last copy, by stamps).

## Decision
(b), stacked on `perf4/watch-page-only` (5111080): on HL7 master alone a rebuild always changes `_data` (the build
minute in `_data/fhir.json`, resource dates), so only the page-only rebuild (which writes just the page's files in
`_includes`) can be incremental. (a) not implemented: it needs to know every page that includes a changed fragment,
which only Jekyll's own record knows.
Rule: incremental only if Jekyll's last run in the session succeeded, `.jekyll-metadata` and `.watch-jekyll` exist,
no `_plugins`/`Gemfile`/`BUNDLE_GEMFILE`, no plugin/collection/include/dir keys in `_config.yml`, and every file
added/changed/deleted since (by size+mtime, then SHA-256) is under `_includes/`. `_config.yml` with `keep_files`:
Jekyll writes to staging as before. Integration found on the way: `PublisherGenerator.cleanOutput` deletes top-level
temp files the build didn't write (so `.jekyll-metadata` is parked in `.watch-jekyll-state`), and PageRebuild's temp
record must skip Jekyll's folders and be taken after mtimes are reset.

## Equivalence (change jar, -rapido -watch: complete build, then a page edit = Page Rebuild; then a full `jekyll build` of the same temp/pages compared with `.watch-jekyll`)
Templates made to declare `script-inputs` (thin local child template, as for the page-only bench), else no page rebuild.
| IG | template | page rebuild Jekyll | files | diffs |
|---|---|---|---|---|
| uz.dhp.core (4 page rebuilds) | fhir2.base.template | incremental, 8.2 / 6.4 / 5.1 s (complete: 70.7 s) | 21,312 | 0 |
| ChitSR/sample-ig | hl7.au.fhir.template | incremental, 2 files | 1,201 | 0 |
| HL7/aitransparency-ig | hl7.fhir.template | incremental, 2 files | 2,594 | 0 |
| IHE/ITI.DIS | local child of ihe.fhir.template | incremental, 1 file | 1,803 | 0 |
| hl7au/au-fhir-encounter | hl7.au.sparked.template | incremental, 2 files | 1,251 | 0 |
| hl7-eu/xshare-ips-plus | local child of fhir.base.template | incremental, 1 file | 695 | 0 |
(The only other difference seen: `full-ig.zip`, which the publisher writes into temp after Jekyll and the page rebuild removes before it.)
