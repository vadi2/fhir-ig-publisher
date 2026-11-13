# QA round 2 results, shard s4

**Status: Phase A done (7/7 master reference builds OK). Waiting for `qa/page-only-ready`.**

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951 (sha256 56b2e4adb4af80ef5b495b3013b726df254243b5437eb500b2c7c6a5ae5b238f)
- VM: 4 CPU, 15 GB, 8 GB swap. Every build: `IG_GATE_OFF=1 MALLOC_ARENA_MAX=2 java -Xmx7600m -XX:+UseStringDeduplication -jar <jar> -ig . -tx https://tx.fhir.org -package-cache-folder /home/user/qa/pcf`, fresh `cp -a` of the clone, no -threads.
- Times in seconds (whole java run). QA = err/warn/hints from qa.txt.

| IG | commit | heap | R1 | R1 qa | verdict |
|---|---|---|---|---|---|
| HL7Chile/MHD-CL | 360872e | 7600m | 157 | 0/28/5 (0 broken links) | pending int.jar |
| hl7-eu/readi | 1928b88 | 7600m | 86 | 165/58/39 (3 broken links) | pending int.jar |
| RIVO-Noord/zorgviewer-ig | 43e4dab | 7600m | 185 | 669/137/128 (43 broken links) | pending int.jar |
| HL7/davinci-ra | e3d2d69 | 7600m | 413 | 0/0/0 (0 broken links) | pending int.jar |
| HL7/imaging-service-request-ig | def90dc | 7600m | 129 | 195/225/244 (5 broken links) | pending int.jar |
| hl7au/au-fhir-core | 3de0d01 | 7600m | 185 | 62/220/47 (0 broken links) | pending int.jar |
| sfaassn2025-source/base | e11fe94 | 7600m | 362 | 0/3/2 (0 broken links) | pending int.jar |
