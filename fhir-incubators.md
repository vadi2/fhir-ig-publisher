# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Operations | Build Status | QA Errors |
|---|-----|------------|-----------|------------|--------------|-----------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | Patient Administration | 7 | 5 | FAILING | 387 errors, 1379 broken links |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 | 0 | FAILING | 109 errors |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | FAILING | 7 errors |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | FAILING | 10 errors |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 2 | 0 | FAILING | 47 errors |
| 6 | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | FHIR Infrastructure | 1 | 7+ | FAILED | No output produced |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK | 0 errors |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 0 | FAILING | 12 errors |
| 9 | pc-incubator | Patient Care | 3 (planned) | 0 | NOT CREATED | — |
| 10 | phx-incubator | Pharmacy | 1 (planned) | 0 | NOT CREATED | — |

> **Build status key:** OK = builds without errors; FAILING = build completes but has validation errors; FAILED = build does not produce output; NOT CREATED = repo does not exist yet.

### Changes since last report (2026-04-17)

- **ebm-incubator**: Contributorship resource (added Mar 27–Apr 2) remains in development. Resource count updated from 1 → 2.
- **oo-incubator**: Documentation cleanup commits Apr 9 (README title update, PR #4 merged). No new resources.
- **Build QA verified**: All R6-targeting IGs have build errors due to FHIR 6.0.0-ballot3 depending on R5 packages (hl7.terminology.r5, hl7.fhir.uv.extensions.r5, hl7.fhir.uv.tools.r5). Only capstmt (targeting R5) builds clean.
- **capstmt**: Published as v1.0.0 STU1 "FHIR Application Feature Framework IG". 0 errors, 0 warnings.
- **pc-incubator** and **phx-incubator**: Still not created on GitHub.
- **No new incubator repos** discovered (sample-incubator-ig is a template).

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | FAILING |
| ClinicalAssessment | pc-incubator | NOT CREATED |
| ConditionDefinition | pc-incubator | NOT CREATED |
| **Contributorship** *(NEW — added 2026-03-27)* | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | FAILING |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| FormularyItem | phx-incubator | NOT CREATED |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | FAILING |
| GraphDefinition | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | FAILING |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | FAILING |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING \* |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING \* |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| Linkage | pc-incubator | NOT CREATED |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | FAILING |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FAILING |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FAILING |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FAILING |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |

> \* InsurancePlan and InsuranceProduct are also still present in the [R6 core spec](https://build.fhir.org/insuranceplan.html) for now; they have been added to admin-incubator preemptively.

---

## All incubated operations

| Operation | Incubator IG | Status |
|-----------|-------------|--------|
| [CapabilityStatement $subset](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-subset.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $conforms](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-conforms.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $implements](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-implements.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [$feature-query](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-feature-query.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [ChargeItemDefinition $apply](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-ChargeItemDefinition-apply.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| [Encounter $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Encounter-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| [EpisodeOfCare $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-EpisodeOfCare-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| [Patient $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| [Patient $merge](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-merge.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | FAILING |
| DocumentReference $generate | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| SpecimenDefinition $apply | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Resource $graph | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Resource $meta | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Resource $meta-add | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Resource $meta-delete | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Resource $large-resource-add | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Resource $large-resource-filter | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Resource $large-resource-remove | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| List $find | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Asynchronous Bulk Data Request | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| Asynchronous Bundle Request | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |

---

## Discrepancies between Confluence page and actual state

1. **NEW RESOURCE — Contributorship**: Added to ebm-incubator on 2026-03-27 by brianalperMD. Increases ebm-incubator resource count from 1 → 2. Not on Confluence page.
2. **Build status degradation**: 7 of 8 existing IGs now have build errors. All R6-targeting IGs fail due to FHIR 6.0.0-ballot3 depending on R5 packages. Only capstmt (FHIR R5) builds clean.
3. **capstmt published**: Now v1.0.0 STU1 "FHIR Application Feature Framework IG" — Confluence may still show draft/ballot status.
4. **pc-incubator and phx-incubator**: Still not created on GitHub as of 2026-04-24.
5. **No new incubator repos**: 7 repos found under HL7 matching "incubator" (including sample-incubator-ig template). No new incubators beyond those on the Confluence page.

---

## GitHub repository activity

| Repository | Last Updated | Recent Activity |
|------------|-------------|-----------------|
| [oo-incubator](https://github.com/HL7/oo-incubator) | Apr 23, 2026 | README title update, PR #4 cleanup merged (Apr 9) |
| [cg-incubator](https://github.com/HL7/cg-incubator) | Apr 10, 2026 | Metadata updates, new CodeSystems/ValueSets for MolecularDefinition |
| [ebm-incubator](https://github.com/HL7/ebm-incubator) | Apr 2, 2026 | **Contributorship resource added** (Mar 27), search params bundle (Apr 2) |
| [immunization-incubator](https://github.com/HL7/immunization-incubator) | Feb 24, 2026 | No significant changes |
| [admin-incubator](https://github.com/HL7/admin-incubator) | Nov 8, 2025 | Inactive |
| [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | Nov 5, 2025 | Inactive; build still failing |

---

## Build QA details

| IG | FHIR Version | Errors | Warnings | Root Cause |
|----|-------------|--------|----------|------------|
| admin-incubator | 6.0.0-ballot3 | 387 | 159 | R5/R6 dependency mismatches, 1379 broken links, duplicate titles |
| oo-incubator | 6.0.0-ballot3 | 109 | 178 | R5 package dependencies, broken resource references |
| cg-incubator | 6.0.0-ballot3 | 7 | 31 | GenomicStudy type not recognized, R5/R6 version conflict |
| immunization-incubator | 6.0.0-ballot3 | 10 | 30 | ImmunizationEvaluation/Recommendation types not recognized |
| ebm-incubator | 6.0.0-ballot3 | 47 | 113 | Citation canonical unresolvable, 169 broken links |
| fhir-testing-ig | 6.0.0-ballot3 | 12 | — | Missing Jira spec, broken valueset links |
| capstmt | R5 | 0 | 0 | Clean build — targets R5, not affected by R6 transition |
| api-incubator-ig | — | — | — | Build does not produce output (last attempt 2026-02-17) |

> **Common root cause:** Most failures stem from IGs targeting FHIR 6.0.0-ballot3 while depending on R5 packages. R6-compatible dependency packages are not yet available.

---

Compiled 2026-04-24 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
