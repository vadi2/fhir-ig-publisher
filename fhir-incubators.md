# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Operations | Build Status | QA Errors |
|---|-----|------------|-----------|------------|--------------|-----------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | Patient Administration | 7 | 5 | FAILING | 387 errors |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 (+1 profile) | 0 | FAILING | 13 errors |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | FAILING | 7 errors |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | FAILING | 10 errors |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 (+1 pending) | 0 | FAILING | 47 errors |
| 6 | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | FHIR Infrastructure | 1 | 12 | FAILED | No output produced |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK | 0 errors |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 0 | FAILING | 12 errors |
| 9 | pc-incubator | Patient Care | 3 (planned) | 0 | NOT CREATED | — |
| 10 | phx-incubator | Pharmacy | 1 (planned) | 0 | NOT CREATED | — |

> **Build status key:** OK = builds without errors; FAILING = build completes but has validation errors; FAILED = build does not produce output; NOT CREATED = repo does not exist yet.

### Changes since last report (2026-05-01)

- **No changes detected.** All build dates, resource counts, and operation counts are identical to the previous report. No new builds have been triggered for any incubator IG.
- **pc-incubator and phx-incubator**: Still not created on GitHub.
- **Confluence REST API**: Blocked by WAF/CAPTCHA; could not verify new proposal pages.

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
| Contributorship *(in repo, not yet in build)* | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | PENDING |
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
| MedicalProductOfHumanOrigin *(profile on BiologicallyDerivedProduct)* | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | FAILING |
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

## Build details

| IG | Version | FHIR Version | Last Build | Errors | Warnings |
|----|---------|-------------|------------|--------|----------|
| admin-incubator | 0.1.0 | 6.0.0-ballot3 | 2025-11-08 | 387 | 159 |
| oo-incubator | 0.1.0 | 6.0.0-ballot3 | 2026-04-24 | 13 | 111 |
| cg-incubator | 0.1.0-ci-build | 6.0.0-ballot3 | 2026-04-10 | 7 | — |
| immunization-incubator | 0.1.0 | 6.0.0-ballot3 | 2026-02-24 | 10 | — |
| ebm-incubator | 1.0.0-ballot | 6.0.0-ballot3 | 2026-03-27 | 47 | — |
| capstmt | 1.0.0 | R5 | 2026-03-24 | 0 | 0 |
| fhir-testing-ig | 0.1.0-SNAPSHOT | 6.0.0-ballot3 | 2026-01-27 | 12 | — |
| api-incubator-ig | — | — | — | BUILD FAILED | — |

> **Common root cause for FAILING status:** Most IGs target FHIR 6.0.0-ballot3 while depending on R5 packages. R6-compatible dependency packages are not yet available. Only capstmt (targeting R5) builds clean.

---

## GitHub repository activity

| Repository | Created | Last Updated |
|------------|---------|-------------|
| [oo-incubator](https://github.com/HL7/oo-incubator) | 2025-11-05 | 2026-04-24 |
| [cg-incubator](https://github.com/HL7/cg-incubator) | 2026-02-05 | 2026-04-10 |
| [ebm-incubator](https://github.com/HL7/ebm-incubator) | 2026-02-08 | 2026-04-02 |
| [capstmt](https://github.com/HL7/capstmt) | 2023-09-28 | 2026-03-24 |
| [immunization-incubator](https://github.com/HL7/immunization-incubator) | 2025-11-05 | 2026-02-24 |
| [fhir-testing-ig](https://github.com/HL7/fhir-testing-ig) | 2025-09-26 | 2026-01-27 |
| [admin-incubator](https://github.com/HL7/admin-incubator) | 2025-11-06 | 2025-11-08 |
| [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | 2025-11-05 | 2025-11-05 |
| [sample-incubator-ig](https://github.com/HL7/sample-incubator-ig) | 2025-11-06 | 2025-11-09 |

> **sample-incubator-ig** is a template/example IG, not a real incubator. It contains no incubated resources.

---

Compiled 2026-05-08 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
