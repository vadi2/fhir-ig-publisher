# QA results: shard s3

Status: Phase A done (all 8 R1 built). Waiting for `qa/integration-ready`.

Reference jar: origin/master 32046bcdd9. Integration jar: (pending). Heap 7600m unless noted. VM 15 GB / 4 CPU, 16 GB swap.

| IG | commit | heap | R1 | R1 QA (e/w/h) | I1 | I2 | W0..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| hl7-eu/xshare-ips-plus | adb1067250 | 7600m | 62 s | 0/5/5 | | | | | | pending |
| FHIR/MADFSH | 0c4b725767 | 7600m | 151 s | 4/16/3 | | | | | | pending |
| HL7/us-fhir-ps | c20fdb208c | 7600m | 94 s | 0/7/21 | | | | | | pending |
| hl7-eu/phoenix-sact | f9246ccc86 | 7600m | 53 s | 0/2/77 | | | | | | pending |
| savannahghi/healthy-route-ig | 7a1e3e908a | 7600m | 99 s | 4/217/861 | | | | | | pending |
| TEHIK-EE/ig-ee-vris | c1741f186c | 7600m | 121 s (R1b) | 95/876/216 | | | | | | pending |
| HL7/davinci-pct | 7d64232695 | 7600m | 274 s | 0/9/2 | | | | | | pending |
| Interop-Sante/hl7.fhir.fr.core | 0e518baa85 | 7600m | 295 s | 165/337/6283 | | | | | | pending |

## Method notes
- Watch edits per IG (W1 page sentence, W2 profile `^short` or VS description, W3 example value in an instance referenced by others, W4 new instance file, W5 delete it, W6 restore all) in one `-rapido -watch` session; output/ fully snapshotted after W2 and W6, input snapshotted after each step. F2/F6 are full builds of the W2/W6 input snapshots, run after the watch session (the watch is not stopped between W2 and W3).
- MADFSH has no profiles or examples on master: W2 edits the sushi-config description, W3 skipped.
- QA counts differ from build.fhir.org for sact, vris, fr.core: tx.fhir.org answers seen from here (code systems/SNOMED editions not held). Reference is our own R1, so this does not affect the comparisons.
- vris: first R1 had 439 tx.fhir.org 502 Bad Gateway errors; rebuilt as R1b, which is the reference.
- fr.core: template `https://github.com/Interop-Sante/ig-style` cannot be downloaded as a zip through this VM's proxy (403); ig.ini points at a local git clone of it (104d205) for every build, both jars.
- Comparison masks, calibrated on two master builds of xshare (equal after masking): dates/times, UUIDs, run folder, xlsx/pack/zip/tgz compared per entry, package.db as sorted rows, key/line order of `*-ref-*-list.json/csv`, skipped qa*, fragment-usage-analysis.*, and archive entries `spec.internals`, `version.info`, `package/.index.db` (master is not deterministic here: `.index.db` ResourceList was empty in one master run and filled in the other).
