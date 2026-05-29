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
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 | 0 | OK |
| 6 | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | FHIR Infrastructure | 1 | 8 (+2 missing) | OK |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 0 | OK |
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | Security | 1 | 0 | OK |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | Terminology Infrastructure | 0 | 2 | OK |
| 11 | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | Patient Care | 3 | 0 | OK |
| 12 | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | Pharmacy | 1 | 0 | OK |
| 13 | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | Financial Management | 3 (+Contract pending) | 0 | OK **(new build)** |

> oo-incubator, capstmt and fhir-testing-ig also publish profiles/extensions/logical models — see [All incubated profiles](#all-incubated-profiles). The Resources column counts new resource types only.

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/en/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK |
| [ClinicalAssessment](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ClinicalAssessment.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [ConditionDefinition](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ConditionDefinition.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| Contract \* | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | In repo, not in build (partial migration) |
| Contributorship \*\* | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | REMOVED — deleted from repo 2026-05-19, no incubator home |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [EnrollmentRequest](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentRequest.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK \*\*\*\* |
| [EnrollmentResponse](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentResponse.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK \*\*\*\* |
| [FormularyItem](https://build.fhir.org/ig/HL7/phx-incubator/StructureDefinition-FormularyItem.html) | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | OK |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [GraphDefinition](https://build.fhir.org/ig/HL7/api-incubator-ig/StructureDefinition-GraphDefinition.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) \*\*\* | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) (move to fm-incubator planned) | OK \*\*\*\* |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) \*\*\* | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) (move to fm-incubator planned) | OK \*\*\*\* |
| [Invoice](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-Invoice.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK \*\*\*\* |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [Linkage](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-Linkage.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |

> \* **Contract** was partially migrated to fm-incubator on 2026-05-27 (commit *"Partial migration of Contract, source is here, but not included in sushi-config"*). The StructureDefinition and its terminology are in the repo but deliberately excluded from `sushi-config.yaml`, so Contract does **not** appear in the CI build yet. Contract is still present in [R6 core](https://build.fhir.org/contract.html).
>
> \*\* **Contributorship** was added to ebm-incubator on Apr 2, 2026, then removed on 2026-05-19 (commit *"removal of Contributorship Additional Resource"*) by brianalperMD — the StructureDefinition and search-params bundle were deleted from the repo. It was never part of R6 core. No incubator destination has been assigned.
>
> \*\*\* **InsurancePlan** and **InsuranceProduct** are present in admin-incubator and are flagged on the page as "moving to fm-incubator". fm-incubator now exists, but the move has **not** happened yet — the fm-incubator build does not contain them; they remain in admin-incubator and in [R6 core](https://build.fhir.org/insuranceplan.html).
>
> \*\*\*\* **EnrollmentRequest, EnrollmentResponse, Invoice, InsurancePlan, InsuranceProduct** are all still present in the R6 core spec as well as in their incubators (dual-listed). The R6 ballot4 ballot-intro flags Invoice/InsurancePlan/InsuranceProduct as "likely to be removed" from core post-ballot; the dual-listing should be resolved at ballot disposition.

---

## All incubated profiles

| Profile | Incubator IG | Status |
|-----------|-------------|--------|
| [medicalproductofhumanorigin](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-medicalproductofhumanorigin.html) (on BiologicallyDerivedProduct) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [shareabletestscript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-shareabletestscript.html) (on TestScript) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [FeatureCapabilityStatement](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) (on CapabilityStatement) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [feature](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) (extension on Extension) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |

Additionally, capstmt publishes **FeatureDefinition**, a logical model (`kind=logical`) — neither a resource nor a profile.

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
| [ConceptMap $closure](https://build.fhir.org/ig/HL7/txmodule-incubator/OperationDefinition-ConceptMap-closure.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | OK |
| [CodeSystem $find-matches](https://build.fhir.org/ig/HL7/txmodule-incubator/OperationDefinition-CodeSystem-find-matches.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | OK |
| DocumentReference $generate | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | Missing |
| SpecimenDefinition $apply | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | Missing |
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
|-----------|-------------|--------|
| [Asynchronous Bulk Data Request](https://build.fhir.org/ig/HL7/api-incubator-ig/async-bulk.html) | api-incubator-ig | OK |
| [Asynchronous Bundle Request](https://build.fhir.org/ig/HL7/api-incubator-ig/async-bundle.html) | api-incubator-ig | OK |
| [FHIRPath Extensions for FHIR](https://build.fhir.org/ig/HL7/api-incubator-ig/fhirpath.html) | api-incubator-ig | OK |

---

## Changes since the 2026-05-26 page

- **fm-incubator is now a live build.** The repo was created 2026-05-27 (after the previous page) and now renders at [build.fhir.org](https://build.fhir.org/ig/HL7/fm-incubator/). The summary row changes from "8 (planned) / 0 / OK" to **3 resources built** — EnrollmentRequest, EnrollmentResponse, Invoice.
- **Invoice now has a confirmed incubator home (fm-incubator).** The previous page's footnote said "no incubator destination has been assigned yet"; the live fm-incubator build resolves this.
- **Contract** is newly present in the fm-incubator repo as a *partial migration* (StructureDefinition in the repo but excluded from `sushi-config.yaml`), so it is **not** yet in the build. New finding.
- **No regressions:** all 14 incubator IGs build and render (HTTP 200). No OK→FAILED status changes.
- **No new incubator repos** beyond the tracked set. The HL7 org also contains `sample-incubator-ig` — the boilerplate template (0 resources/operations) — which is not a working-group incubator and is excluded.
- **Count correction:** api-incubator-ig's operation count is **8 built (+2 missing)**; the previous page summary listed 12.
- Profiles, the capstmt logical model, txmodule operations, and all other resources/operations are unchanged from the 2026-05-26 page.

---

## Notes for the FHIR Management Group (R6 transition)

<details>
<summary>R6 spec → incubator transition status</summary>

**Still in R6 core, awaiting final incubator destination (dual-listed):**

- **EnrollmentRequest, EnrollmentResponse, Invoice** — now live in fm-incubator (new build) but still present in R6 core. The dual-listing should be resolved at R6 ballot4 disposition.
- **InsurancePlan, InsuranceProduct** — present in R6 core *and* admin-incubator. The page records a planned move to fm-incubator, which has **not** happened yet (fm-incubator build does not contain them). Resolve at ballot4: either remove from core (preferred per the ballot intro) or from admin-incubator, and complete the move to fm-incubator.
- **Contract** — present in R6 core and *partially* migrated into fm-incubator (StructureDefinition in the repo but excluded from sushi-config, so not in the build). FMG should confirm Contract's target incubator and complete the migration so it actually renders.

**Removed from build with no incubator home:**

- **Contributorship** — removed from ebm-incubator on 2026-05-19. It was never in R6 core. If Clinical Decision Support / EBMonFHIR still wants this resource, FMG should confirm the deprioritisation or identify a new owner; otherwise drop it from any "incubated" tracking entirely.

**Missing content:**

- **api-incubator-ig** — DocumentReference $generate and SpecimenDefinition $apply are noted as belonging here, but are not present in the build.

**Untracked capstmt artifacts:**

- **feature** (extension) and **FeatureDefinition** (logical model) are published by capstmt and are now tracked above (profiles section + note). These already exist in the build — they are legitimate incubator artifacts, not new content to publish. FMG should confirm they belong to the incubator scope.

**Other observations:**

- All builds render (HTTP 200) despite QA validation errors (mostly R5/R6 dependency-version mismatches and ballot config); errors do not block rendering.
- `sample-incubator-ig` (HL7 org) is the boilerplate template repo and is intentionally excluded from the tables.

</details>

---

Compiled 2026-05-29 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
