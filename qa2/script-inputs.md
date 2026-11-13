# script-inputs for the base templates

Read from the ant scripts of the #current packages on build.fhir.org (13 Nov 2026). Paths are relative to the IG root. None of these templates has an onJekyll target, so all of them can be declared.

| template | its scripts | script-inputs |
|---|---|---|
| fhir.base.template | ant.xml | `input/examples`, `input/extensions`, `input/models`, `input/profiles`, `input/resources`, `input/structures`, `input/vocabulary` (onLoad lists `*spreadsheet.xml` there), `input/images-source` (onGenerate runs plantuml on it) |
| fhir2.base.template | ant.xml | fhir.base list + `input/includes/menu.xml`, `input/fsh-generated/includes/menu.xml` (onGenerate menu QA) |
| hl7.base.template | ant-hl7.xml (imports ant.xml) | fhir2.base list + `package-list.json`, `publication-request.json` (onGenerate jira spec) |
| hl7.fhir.template | none (hl7.base's) | hl7.base list |
| hl7.davinci.template | ant-davinci.xml (imports ant-hl7.xml) | hl7.base list + `fsh-generated/includes/menu.xml` (onCheck reads it, spelled without `input/`) |
| ihe.fhir.template | none (fhir.base's) | fhir.base list |
| hl7.au.base.template, hl7.au.fhir.template | none run (au.base ships scripts/ant-hl7.xml, but no config names it) | fhir2.base list |

Notes
- Nothing in these scripts reads pages or `input/data` (fhir2's copy of `input/data` is commented out), so a page edit is never in a declared path.
- Davinci's onCheck also reads `output/en/index.html` and the pages named in menu.xml. That needs no declaration: a page rebuild runs onCheck again and falls back to a complete build if its messages differ from the build's. It reads `output/`, not the watch staging folder, so under -watch it sees the previous build's pages on both jars.
- Inherited declarations (on perf4/watch-page-only from e6a0c6a2): a declaration covers the scripts of its template and the ones before it. A template with active content (names a script, targets or otherScripts, overwrites an ant script, or ships files outside the allowlist) that doesn't declare drops the declarations before it. So a declaration in hl7.base alone would not cover davinci; declare on the template the IG names.
- Local templates built on these: their base's list, plus what their own scripts read. A local template with its own script must declare.

## Recipe for a QA shard
1. `template = X#ver` in the IG's ig.ini names the template. Copy it from the package cache: `cp -r <pcf>/X#ver /qa/tpl-X` (the package cache layout with config.json next to package/ works as is).
2. In `/qa/tpl-X/config.json` (create it as `{}` if absent; hl7.fhir, ihe and au have none) add `"script-inputs": [ ...the list for X above... ]`. Only the template named in ig.ini needs it.
3. In ig.ini under `[IG]` add `X#ver = /qa/tpl-X`. Use the same IG copy for both jars; master ignores the key.
4. Check the log: `Load Template X#ver from local folder /qa/tpl-X (override in ig.ini)`, then after a page edit `Rapido Mode: Page Rebuild (1 page changed)`. `Complete Build (the template doesn't declare ...)` means the key is missing or was dropped. Editing the copy itself triggers a complete build (watch mode watches it).
