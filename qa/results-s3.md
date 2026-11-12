# QA results: shard s3

Status: Phase C in progress (xshare, madfsh, usps, sact, hroute, vris, fr.core done; pct waiting on master watch check).

Reference jar: origin/master 32046bcdd9. Integration jar: `qa/integration-ready` 6c1abf463d. Heap 7600m unless noted. VM 15 GB / 4 CPU, 16 GB swap.

| IG | commit | heap | R1 | R1 QA (e/w/h) | I1 | I2 | W0..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| hl7-eu/xshare-ips-plus | adb1067250 | 7600m | 62 s | 0/5/5 | 30 s, 0/5/5 | 28 s, 0/5/5 | 29/1/14/13/15/15/21 s, all edits seen, QA 0/5/5 | 30 s | 29 s | REGRESSION: F1 (llms page order, I1/I2 only; looks intended). W2=F2, W6=F6, W6=I1 same |
| FHIR/MADFSH | 0c4b725767 | 7600m | 151 s | 4/16/3 | 37 s, 4/16/3 | 31 s, 4/16/3 | 35/2/21/-/22/22/23 s, all edits seen, QA 4/16/3 | 38 s | 36 s | REGRESSION: F1 only (I1/I2). W2=F2, W6=F6, W6=I1 same |
| HL7/us-fhir-ps | c20fdb208c | 7600m | 94 s (R1b 70 s) | 0/7/21 | 64 s, 0/7/21 | 56 s, 0/7/21 | 62/6/43/50/37/38/34 s, all edits seen, QA 0/7/21; rerun 64/6/42/34/38/35/32 s | 65 s | 67 s | REGRESSION: F1 only (I1/I2). pre-existing diffs: NamingSystem link/name chosen from a random THO package (masked); one W2 vs F2 diff of this kind (us-npi display) not reproduced on a rerun. W6=F6, W6=I1 same |
| hl7-eu/phoenix-sact | f9246ccc86 | 7600m | 53 s | 0/2/77 | 62 s, 0/2/77 | 55 s, 0/2/77 | 64/1/27/34/36/38/26 s, all edits seen, QA 0/2/77 | 64 s | 64 s | REGRESSION: F1 only (I1/I2). W2=F2, W6=F6, W6=I1 same |
| savannahghi/healthy-route-ig | 7a1e3e908a | 7600m | 99 s | 4/217/861 | 94 s, 4/217/861 | 76 s, 4/217/861 | 82/5/51/43/63/66/51 s, all edits seen, QA 4/217/861 | 88 s | 82 s | REGRESSION: F1 only (I1/I2). W2=F2, W6=F6, W6=I1 same |
| TEHIK-EE/ig-ee-vris | c1741f186c | 7600m | 121 s (R1b) | 95/876/216 | 136 s, 95/876/216 | 118 s, 95/876/216 | 129/2/95/101/100/102/98 s, all edits seen, QA 95/876/216 | 123 s | 126 s | REGRESSION: F1 only (I1/I2). W2=F2, W6=F6, W6=I1 same |
| HL7/davinci-pct | 7d64232695 | 7600m | 274 s | 0/9/2 | | | | | | pending |
| Interop-Sante/hl7.fhir.fr.core | 0e518baa85 | 7600m | 295 s | 165/337/6283 | 260 s, 165/337/6283 | 263 s, 165/337/6283 | 262/7/256/228/238/270/232 s, all edits seen, QA 165/337/6283 | 264 s | 266 s | REGRESSION: F1 only (I1/I2, en/fr llms.txt, ai.zip, full-ig.zip). W2=F2, W6=F6, W6=I1 same. Rapido reused 0 html checks for W2..W6 ("check is set up differently (tableUuid)"), output still correct |

## Findings
- **F1, REGRESSION (looks intended), HL7 #1461 watch-page-rebuild (e70bb8a, commit 709525b, in the integration base).** `llms.txt` and `llms.md` (also inside `ai.zip` and `full-ig.zip`) list the `## Pages` in a different order: int.jar sorts them by file name (`AIProcessor.loadPages`: `Arrays.sort(files)`), master takes `dir.listFiles()` order. Two master builds gave the same order here, so it is not master noise. Content is the same, only the order. Repro: full build of xshare-ips-plus with each jar, `diff output/llms.txt`. Found by code and git ancestry; no single-PR jar built for it.

- **usps NamingSystem choice (pre-existing, not counted as a finding).** Narratives that show an identifier link `NamingSystem-<x>.html` from whichever hl7.terminology package the build picks (5.0.0, 6.2.0, 7.1.0, 7.2.0, 7.5.0 seen), and the link text is that version's title or name. Two master full builds (R1, R1b) already differ this way, so cmp masks these links. In the first int watch session, after W2, the `us-npi` reference display read `NPI` (name, from an r4 package) where every full build (master and int) and the master watch showed the r5 title; a second int watch session on the same edits gave the title, and its W2 output equals F2. Treated as the same master nondeterminism.

## Method notes
- Watch edits per IG (W1 page sentence, W2 profile `^short` or VS description, W3 example value in an instance referenced by others, W4 new instance file, W5 delete it, W6 restore all) in one `-rapido -watch` session; output/ fully snapshotted after W2 and W6, input snapshotted after each step. F2/F6 are full builds of the W2/W6 input snapshots, run after the watch session (the watch is not stopped between W2 and W3).
- MADFSH has no profiles or examples on master: W2 edits the sushi-config description, W3 skipped.
- QA counts differ from build.fhir.org for sact, vris, fr.core: tx.fhir.org answers seen from here (code systems/SNOMED editions not held). Reference is our own R1, so this does not affect the comparisons.
- fr.core: first I1/I2/W ran into a full disk (No space left on device); all three were redone from scratch, results above are the clean reruns.
- vris: first R1 had 439 tx.fhir.org 502 Bad Gateway errors; rebuilt as R1b, which is the reference.
- fr.core: template `https://github.com/Interop-Sante/ig-style` cannot be downloaded as a zip through this VM's proxy (403); ig.ini points at a local git clone of it (104d205) for every build, both jars.
- Comparison masks, calibrated on two master builds of xshare (equal after masking): dates/times, UUIDs, run folder, xlsx/pack/zip/tgz compared per entry, package.db as sorted rows, key/line order of `*-list.json` and `*-ref-*-list.csv` and the order inside their "used by" lists, skipped qa*, fragment-usage-analysis.*, and archive entries `spec.internals`, `version.info`, `package/.index.db` (master is not deterministic here: `.index.db` ResourceList was empty in one master run and filled in the other).
