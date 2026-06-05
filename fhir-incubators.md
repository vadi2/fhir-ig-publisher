# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Operations | Build Status |
|---|-----|------------|-----------|------------|--------------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | Patient Administration | 7 | 5 | OK |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 | 0 | OK |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | OK |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | OK |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 | 0 | OK |
| 6 | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | FHIR Infrastructure | 1 | 8 (+2 missing) | OK |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 0 | OK |
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | Security | 1 | 0 | OK |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | Terminology Infrastructure | 0 | 2 | OK |
| 11 | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | Patient Care | 3 | 0 | OK |
| 12 | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | Pharmacy | 1 | 0 | OK |
| 13 | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | Financial Management | 3 (of 8 planned) | 0 | OK |

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK |
| [ClinicalAssessment](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ClinicalAssessment.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [ConditionDefinition](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ConditionDefinition.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| Contributorship \* | ebm-incubator | REMOVED — was pending, deleted from repo 2026-05-19 |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [EnrollmentRequest](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentRequest.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [EnrollmentResponse](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentResponse.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [FormularyItem](https://build.fhir.org/ig/HL7/phx-incubator/StructureDefinition-FormularyItem.html) | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | OK |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [GraphDefinition](https://build.fhir.org/ig/HL7/api-incubator-ig/StructureDefinition-GraphDefinition.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [Invoice](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-Invoice.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK \*\* |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsurancePlan.html) | admin-incubator (moving to fm-incubator) | OK \*\*\* |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsuranceProduct.html) | admin-incubator (moving to fm-incubator) | OK \*\*\* |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [Linkage](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-Linkage.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |

> \* **Contributorship** was added to ebm-incubator on Apr 2, 2026, then removed on 2026-05-19 by brianalperMD — the StructureDefinition and search-params bundle were deleted from the repo. No incubator destination has been assigned.
>
> \*\* **Invoice** is flagged in the R6 ballot4 ballot-intro as "likely to be removed" from core post-ballot, alongside InsurancePlan and InsuranceProduct.
>
> \*\*\* **InsurancePlan** and **InsuranceProduct** are also still present in the R6 core spec for now; they have been added to admin-incubator preemptively.

---

## All incubated profiles

| Profile | Incubator IG | Status |
|---------|-------------|--------|
| [MedicalProductOfHumanOrigin](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-MedicalProductOfHumanOrigin.html) (on BiologicallyDerivedProduct) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [shareabletestscript](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-shareabletestscript.html) (on TestScript) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [FeatureCapabilityStatement](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) (on CapabilityStatement) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [feature](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) (extension on Extension) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |

Additionally, capstmt publishes [FeatureDefinition](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureDefinition.html), a logical model (kind=logical) — neither a resource nor a profile.

---

## All incubated operations

| Operation | Incubator IG | Status |
|-----------|-------------|--------|
| [CapabilityStatement $subset](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-subset.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $conforms](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-conforms.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $implements](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-implements.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [$feature-query](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-feature-query.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [ChargeItemDefinition $apply](https://build.fhir.org/ig/HL7/admin-incubator/OperationDefinition-ChargeItemDefinition-apply.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Encounter $everything](https://build.fhir.org/ig/HL7/admin-incubator/OperationDefinition-Encounter-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [EpisodeOfCare $everything](https://build.fhir.org/ig/HL7/admin-incubator/OperationDefinition-EpisodeOfCare-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Patient $everything](https://build.fhir.org/ig/HL7/admin-incubator/OperationDefinition-Patient-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Patient $merge](https://build.fhir.org/ig/HL7/admin-incubator/OperationDefinition-Patient-merge.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [ConceptMap $closure](https://build.fhir.org/ig/HL7/txmodule-incubator/OperationDefinition-ConceptMap-closure.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | OK |
| [CodeSystem $find-matches](https://build.fhir.org/ig/HL7/txmodule-incubator/OperationDefinition-CodeSystem-find-matches.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | OK |
| DocumentReference $generate | api-incubator-ig | Missing |
| SpecimenDefinition $apply | api-incubator-ig | Missing |
| [Resource $graph](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-graph.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $meta](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-meta.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $meta-add](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-meta-add.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $meta-delete](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-meta-delete.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $large-resource-add](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-large-resource-add.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $large-resource-filter](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-large-resource-filter.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $large-resource-remove](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-large-resource-remove.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [List $find](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-List-find.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |

---

## All other incubated pages

| Page | Incubator IG | Status |
|------|-------------|--------|
| Asynchronous Bulk Data Request | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| Asynchronous Bundle Request | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| FHIRPath Extensions for FHIR | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| Operations for Large Resources | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK (new entry — narrative grouping of the `$large-resource-*` operations) |

---

## Notes for the FHIR Management Group (R6 transition)

<details>
<summary>Click to expand R6 transition notes</summary>

**Still in R6 core, awaiting final incubator destination:**

- **InsurancePlan**, **InsuranceProduct** — present in both R6 core and admin-incubator. The dual-listing should be resolved at R6 ballot4 disposition: either remove from core (preferred per the ballot intro) or remove from admin-incubator.

**Removed from build with no incubator home:**

- **Contributorship** — removed from ebm-incubator on 2026-05-19. If Clinical Decision Support / EBMonFHIR still wants this resource, FMG should confirm the deprioritisation or identify a new owner; otherwise drop it from any "incubated" tracking entirely.

**Missing content:**

- **api-incubator-ig** — DocumentReference $generate and SpecimenDefinition $apply are noted as being here, but not there yet (no source files in `input/resources/`).

**Untracked capstmt artifacts (already in build, FMG should confirm scope):**

- **feature** (extension) and **FeatureDefinition** (logical model) are now published by capstmt but were not previously listed on the incubator overview. FMG should confirm these belong to the incubator scope.

**New since the prior compilation (2026-05-26):**

- **api-incubator-ig** — added a new top-level page **"Operations for Large Resources"** (`operations-for-large-resources.xml` in `input/pagecontent/`), grouping the `$large-resource-*` operations under a single narrative. Not previously listed in the "All other incubated pages" section.
- **fm-incubator** — repo gained a new `input/resources/contract/` folder containing `StructureDefinition-Contract.xml`, code systems, value sets, and several examples (`contract-example.xml`, `contract-example-42cfr-part2.xml`, `contract-example-ins-policy.xml`, etc.). The Contract resource is **not yet present in the IG build** (`StructureDefinition-Contract.html` returns 404 and the artifacts page does not list it), suggesting the IG definition has not yet been updated to include it. This is the first hint of an fm-incubator landing site for Contract.
- **oo-incubator** — repo activity dated 2026-06-05 (today); no new resources or profiles appear in the build relative to the prior report.

**fm-incubator planned scope (8 resources) — current state:**

- Built / in artifacts: EnrollmentRequest, EnrollmentResponse, Invoice (3)
- Staged in repo only (not in build): Contract (1)
- Still to come / unknown: 4 additional resources from the planned scope of 8.

**Summary table count adjustments versus prior page:**

- api-incubator-ig was previously summarised as "1 / 12"; the actual built artifacts are 1 resource + 8 operations (with 2 additional operations — DocumentReference $generate, SpecimenDefinition $apply — still missing from the source). The "+2 missing" annotation reflects what is tracked but not yet built.

</details>

---

Compiled 2026-06-05 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
