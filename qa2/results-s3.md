# QA round 2 results: shard s3

Status: Phase C running (1 of 8 IGs done).

Reference jar: HL7 master 2ccca17946. Integration jar: `qa/page-only-ready` 5111080dd5. Heap 7600m for every IG. VM 15 GB / 4 CPU, swap 12 GB (not 16: the 30 GB disk allowance is shared with outputs).

| IG | commit | heap | R1 | R1 QA (e/w/h) | I1 | I2 | W1a..W6 | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|
| hl7-eu/xshare-ips-plus | adb1067250 | 7600m | 76 s (R1b 31 s, equal after masking) | 0/5/5 | 29 s, 0/5/5, = R1 | 27 s, 0/5/5, = R1 | int 12/12/-/18/18/18/19/19/19/18/17 s, master 16/13/-/18/18/19/19/21/20/19/18 s; all edits seen | 27 s, = W1f | 27 s, = W2 | 28 s, = W6; W6 = I1 | same |
| FHIR/MADFSH | 0c4b725767 | 7600m | 152 s | 4/16/3 | | | | | | | pending |
| HL7/us-fhir-ps | c20fdb208c | 7600m | 97 s | 0/7/21 | | | | | | | pending |
| hl7-eu/phoenix-sact | f9246ccc86 | 7600m | 59 s | 0/2/77 | | | | | | | pending |
| savannahghi/healthy-route-ig | 8de83993a5 | 7600m | 150 s | 4/217/862 | | | | | | | pending |
| TEHIK-EE/ig-ee-vris | ff7c9a820f | 7600m | 164 s (R1b 131 s, equal after masking) | 135/856/212 | | | | | | | pending |
| HL7/davinci-pct | 7d64232695 | 7600m | 304 s | 0/9/2 | | | | | | | pending |
| Interop-Sante/hl7.fhir.fr.core | 0e518baa85 | 7600m | 306 s | 165/337/6283 | | | | | | | pending |

## Notes
- **Page-only path not exercised in this shard.** No template used here (fhir.base.template 0.8.1/1.0.0/current, fhir2.base, hl7.base, hl7.fhir, hl7.davinci, and the local tehik, phoenix, ig-style, xshare templates built on them) declares `script-inputs`, and all have ant scripts (onLoad/onGenerate, most also onCheck). int.jar therefore logs `Rapido Mode: Complete Build (the template doesn't declare what its scripts read (script-inputs in its config.json))` for every W1 page edit and does a full rebuild, as the plan requires. Correctness of the fallback is what the W1 rows test; the page rebuild itself is not reached.
- W1a..W6 columns are W1a/W1b/W1c/W1d/W1e/W1f/W2/W3/W4/W5/W6 rebuild seconds (edit to the next "Watching for changes"), `-` = not applicable to the IG. Master times are from a master `-rapido -watch` session with the same edits and heap.
- Watch is stopped after W1f, W2 and W6 (snapshots of output/ and sources), then restarted on the same folder.

## Method notes
- fr.core: template `https://github.com/Interop-Sante/ig-style` is not downloadable through the proxy (as in round 1); ig.ini points at a local clone (104d205) for every build, both jars.
- Comparison masks as in round 1 (dates, UUIDs, run folder, archives per entry, package.db rows, line and "used by" order of `*-list.json` / `*-ref-*-list.csv`, qa*, fragment-usage-analysis.*, spec.internals, version.info, .index.db/.index.json, F4 NamingSystem links). Calibrated: two master builds each of xshare and vris compare equal.
- R1 kept as output.tgz + sha256 manifest + qa.json/qa.html + log; run folders deleted.
