# QA round 2 results, shard s5

Status: Phase A done (all 8 R1 reference builds pass). Phase B: waiting for refs/heads/qa/page-only-ready (not pushed yet). Master `-rapido -watch` sequences (WM) running meanwhile for timings and spec checks.

- master.jar: HL7 master 2ccca179460d9bb9f4185a59a6fd66d4d9330951
- int.jar: (pending)
- VM: 4 CPU, 15 GB, swap file 6 GB (not 16: about 30 GB writable disk). All runs with LC_ALL=C.UTF-8 (nw-gmsa needs a UTF-8 locale, see round 1).
- eu-lab: SUSHI can't fetch hl7.fhir.eu.extensions.r4#current (as in round 1); seeded ~/.fhir/packages from the publisher's copy (date 20261029172704), same package for every build.
- Templates: every IG's template chain ends in fhir2.base.template, whose config.json declares ant script targets onLoad/onGenerate/onCheck (scripts/ant.xml); hl7.base.template overrides the script with ant-hl7.xml, hl7.davinci.template with ant-davinci.xml. So all 8 IGs have ant script steps.
- Times in seconds (wall clock of the java run).

| IG | commit | heap | R1 | R1 qa e/w/h/links | I1 | I2 | W1..W6 (int) | W1..W6 (master) | F1 | F2 | F6 | verdict |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| HL7/fhir-tx-ecosystem-ig | 6d88f37e | 7600m | 77 | 0/6/15/0 | | | | | | | | |
| HL7/cds-hooks-library | 19a388ba | 7600m | 88 | 0/0/0/0 | | | | | | | | |
| HL7/uv-pocd | 383969ea | 7600m | 60 | 0/0/4/0 | | | | | | | | |
| hl7-eu/laboratory | 4571ab84 | 7600m | 187 | 5/11/62/0 | | | | | | | | |
| HL7/fhir-bfdr | 1f981246 | 7600m | 362 | 291/365/791/7 | | | | | | | | |
| HL7/davinci-pdex-plan-net | 8d9037d0 | 9g | 234 | 11/865/8/0 | | | | | | | | |
| aehrc/smart-forms-ig | 95651ff7 | 9g | 283 | 146/294/119/0 | | | | | | | | |
| nw-gmsa/nw-gmsa.github.com | 3aff5f8f | 9g | 562 | 93/3638/1865/10 | | | | | | | | |
