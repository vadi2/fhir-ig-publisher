# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Operations | Build Status |
|---|-----|------------|-----------|------------|--------------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | Patient Administration | 7 | 5 | OK |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 | 0 | OK |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | OK |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | OK |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 (+1 pending) | 0 | OK |
| 6 | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | FHIR Infrastructure | 1 | 12 | FAILED |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 0 | OK |
| 9 | pc-incubator | Patient Care | 3 (planned) | 0 | NOT CREATED |
| 10 | phx-incubator | Pharmacy | 1 (planned) | 0 | NOT CREATED |

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/en/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK |
| ClinicalAssessment | pc-incubator | NOT CREATED |
| ConditionDefinition | pc-incubator | NOT CREATED |
| Contributorship \* | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | NEW — in repo, not yet in build |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| FormularyItem | phx-incubator | NOT CREATED |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| GraphDefinition | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| Invoice \*\* | TBD | FLAGGED FOR REMOVAL |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK \*\*\* |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK \*\*\* |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| Linkage | pc-incubator | NOT CREATED |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |

> \* **Contributorship** was added to ebm-incubator on Apr 2, 2026 ([commit](https://github.com/HL7/ebm-incubator/commits/main)). The StructureDefinition and search parameters are in the repo but the resource does not yet appear in the CI build output.
>
> \*\* **Invoice** is flagged in the [R6 ballot4 ballot-intro](https://build.fhir.org/ballot-intro.html) as "likely to be removed" from core post-ballot, alongside InsurancePlan and InsuranceProduct. No incubator destination has been assigned yet.
>
> \*\*\* InsurancePlan and InsuranceProduct are also still present in the [R6 core spec](https://build.fhir.org/insuranceplan.html) for now; they have been added to admin-incubator preemptively.

---

## All incubated operations

| Operation | Incubator IG | Status |
|-----------|-------------|--------|
| [CapabilityStatement $subset](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-subset.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $conforms](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-conforms.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $implements](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-implements.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [$feature-query](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-feature-query.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [ChargeItemDefinition $apply](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-ChargeItemDefinition-apply.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Encounter $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Encounter-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [EpisodeOfCare $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-EpisodeOfCare-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Patient $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Patient $merge](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-merge.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
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

## Discrepancies and notes

- **Confluence page inaccessible**: The HL7 Confluence REST API is behind a WAF/CAPTCHA challenge and could not be fetched. This report is compiled independently from build.fhir.org CI builds and HL7 GitHub.
- **FHIR R6 ballot4**: The spec has progressed from ballot3 to ballot4 (May 2026). All incubator IG configs still reference ballot3.
- **New resource (Contributorship)**: Added to ebm-incubator repo on Apr 2, 2026. Not yet rendering in CI build.
- **Invoice flagged**: R6 ballot4 ballot-intro notes Invoice is "likely to be removed" from core post-ballot. No incubator assigned yet.
- **oo-incubator active**: 202 commits, last updated Apr 24. Added MedicalProductOfHumanOrigin profile (on BiologicallyDerivedProduct; not a new resource type). Ongoing cleanup of search params and examples.
- **api-incubator-ig misconfigured**: sushi-config.yaml ID is `hl7.fhir.uv.symptoms` (should be api-related), causing build failure since Nov 2025. No commits since creation.
- **QA validation errors**: All IGs except capstmt have QA validation errors, primarily R6/R5 dependency version mismatches. Builds complete and pages are accessible despite errors.
- **No new repos**: pc-incubator and phx-incubator remain uncreated. No new incubator repos found in HL7 org beyond those listed.

---

Compiled 2026-05-15 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
