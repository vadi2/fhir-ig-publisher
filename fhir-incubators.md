# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Operations | Build Status |
|---|-----|------------|-----------|------------|--------------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | Patient Administration | 7 | 5 | OK |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 (+1 profile) | 0 | OK |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | OK |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | OK |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 (+1 in dev) | 0 | OK |
| 6 | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | FHIR Infrastructure | 1 | 7+ | FAILED |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 0 | OK |
| 9 | pc-incubator | Patient Care | 3 (planned) | 0 | NOT CREATED |
| 10 | phx-incubator | Pharmacy | 1 (planned) | 0 | NOT CREATED |

### Changes since last report (2026-03-25)

- **ebm-incubator**: New **Contributorship** resource being developed (commits Mar 27 - Apr 2, 2026). Not yet in the CI build due to SUSHI errors being resolved. Resource count will increase from 1 to 2 once complete.
- **oo-incubator**: New **MedicalProductOfHumanOrigin** profile added (appears in CI build QA). Cleanup work in Apr 2026 fixed valuesets/codesystems to not reference R5 variants.
- **cg-incubator**: New CodeSystems and ValueSets for MolecularDefinition added (Mar 24). Spec metadata updated (Apr 10). No new resources.
- **capstmt**: Now at v1.0.0 STU1 -- formally published as "FHIR Application Feature Framework IG".
- **api-incubator-ig**: Build still failing; `failure/` directory visible on build server (last attempt 2026-02-17).
- **pc-incubator** and **phx-incubator**: Still not created on GitHub.

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK |
| ClinicalAssessment | pc-incubator | NOT CREATED |
| ConditionDefinition | pc-incubator | NOT CREATED |
| Contributorship | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | NEW -- in development, not yet in CI build |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| FormularyItem | phx-incubator | NOT CREATED |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| GraphDefinition | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK \* |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK \* |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| Linkage | pc-incubator | NOT CREATED |
| MedicalProductOfHumanOrigin | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | NEW -- profile added to CI build |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |

> \* InsurancePlan and InsuranceProduct are also still present in the [R6 core spec](https://build.fhir.org/insuranceplan.html) for now; they have been added to admin-incubator preemptively.

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

## Discrepancies between Confluence page and actual state

1. **ebm-incubator**: Confluence lists 1 resource (Citation). GitHub shows a new **Contributorship** resource being actively developed (commits Mar 27 - Apr 2, 2026) but not yet in the CI build due to SUSHI errors.
2. **oo-incubator**: Confluence lists 8 resources. CI build QA now shows **MedicalProductOfHumanOrigin** as an additional profile (9 StructureDefinitions total). Recent cleanup work (Apr 7-9) fixed valuesets/codesystems referencing R5.
3. **cg-incubator**: New CodeSystems and ValueSets for MolecularDefinition added (Mar 24). Spec metadata updated (Apr 10). Resource count unchanged at 2.
4. **capstmt**: Now published as v1.0.0 STU1 "FHIR Application Feature Framework IG" -- Confluence may still show draft status.
5. **Confluence REST API**: Returns 405 for both content and search endpoints; could not verify page content directly.
6. **No new incubator repos** found beyond the 7 known ones (including sample-incubator-ig template).
7. **pc-incubator** and **phx-incubator** repos still do not exist on GitHub.

---

## GitHub repository activity (since 2026-03-25)

| Repository | Last Updated | Recent Activity |
|------------|-------------|-----------------|
| [cg-incubator](https://github.com/HL7/cg-incubator) | Apr 10, 2026 | Metadata updates, new CodeSystems/ValueSets |
| [oo-incubator](https://github.com/HL7/oo-incubator) | Apr 9, 2026 | ValueSet/CodeSystem cleanup, intro text added |
| [ebm-incubator](https://github.com/HL7/ebm-incubator) | Apr 2, 2026 | **Contributorship resource added** |
| [immunization-incubator](https://github.com/HL7/immunization-incubator) | Feb 24, 2026 | No changes since last report |
| [admin-incubator](https://github.com/HL7/admin-incubator) | Nov 8, 2025 | No changes since last report |
| [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | Nov 5, 2025 | No changes; build still failing |

---

Compiled 2026-04-17 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
