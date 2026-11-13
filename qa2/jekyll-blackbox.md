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
