# QA results: shard s3

Status: Phase A (master reference builds) in progress. Waiting for `qa/integration-ready`.

Reference jar: origin/master 32046bcdd9. Integration jar: (pending). Heap 7600m unless noted. VM 15 GB / 4 CPU, 16 GB swap.

| IG | commit | heap | R1 | R1 QA (e/w/h) | I1 | I2 | W0..W6 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|
| hl7-eu/xshare-ips-plus | adb1067250 | 7600m | 62 s | 0/5/5 | | | | | | pending |
| FHIR/MADFSH | 0c4b725767 | 7600m | 151 s | 4/16/3 | | | | | | pending |
| HL7/us-fhir-ps | c20fdb208c | 7600m | 94 s | 0/7/21 | | | | | | pending |
| hl7-eu/phoenix-sact | f9246ccc86 | 7600m | | | | | | | | pending |
| savannahghi/healthy-route-ig | 7a1e3e908a | 7600m | | | | | | | | pending |
| TEHIK-EE/ig-ee-vris | c1741f186c | 7600m | | | | | | | | pending |
| HL7/davinci-pct | 7d64232695 | 7600m | | | | | | | | pending |
| Interop-Sante/hl7.fhir.fr.core | 0e518baa85 | 7600m | | | | | | | | pending |

## Method notes
- Watch edits per IG (W1 page sentence, W2 profile `^short` or VS description, W3 example value in an instance referenced by others, W4 new instance file, W5 delete it, W6 restore all) in one `-rapido -watch` session; output/ fully snapshotted after W2 and W6, input snapshotted after each step. F2/F6 are full builds of the W2/W6 input snapshots, run after the watch session (the watch is not stopped between W2 and W3).
- MADFSH has no profiles or examples on master: W2 edits the sushi-config description, W3 skipped.
