# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Profiles | Operations | Build Status |
|---|-----|------------|-----------|----------|------------|--------------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | Patient Administration | 7 | 0 | 5 | OK |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | Orders and Observations | 8 | 1 | 0 | OK |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/en/) | Clinical Genomics | 2 | 0 | 0 | OK |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/en/) | Public Health | 2 | 0 | 0 | OK |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/en/) | Clinical Decision Support | 1 | 0 | 0 | OK |
| 6 | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | FHIR Infrastructure | 1 | 0 | 8 (+ 2 planned but missing) | OK |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 3 (+ 1 extension, + 1 logical model) | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | FHIR Infrastructure | 3 | 1 | 0 | OK |
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | Security | 1 | 0 (+ 1 extension) | 0 | OK |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | Terminology Infrastructure | 0 | 0 | 2 | OK |
| 11 | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | Patient Care | 3 | 0 | 0 | OK |
| 12 | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | Pharmacy | 1 | 0 | 0 | OK |
| 13 | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | Financial Management | 3 (of ~8 planned) | 0 | 0 | OK |
| 14 | [mapping-language-ig](https://build.fhir.org/ig/HL7/mapping-language-ig/en/) **NEW** | FHIR Infrastructure | 1 | 0 | 1 | OK |

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| Citation \* | ebm-incubator | REMOVED — replaced by PublicationRecord |
| [ClinicalAssessment](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ClinicalAssessment.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [ConditionDefinition](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ConditionDefinition.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| Contract \*\* | fm-incubator | Not in repo, not in build (destination stated in R6 ballot-intro but never migrated) |
| Contributorship \*\*\* | ebm-incubator | REMOVED — was pending, deleted from repo 2026-05-19. No incubator destination assigned. |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [EnrollmentRequest](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentRequest.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [EnrollmentResponse](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentResponse.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [FormularyItem](https://build.fhir.org/ig/HL7/phx-incubator/StructureDefinition-FormularyItem.html) | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | OK |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [GraphDefinition](https://build.fhir.org/ig/HL7/api-incubator-ig/StructureDefinition-GraphDefinition.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) \*\*\*\* | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) (expected to move to fm-incubator) | OK |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) \*\*\*\* | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) (expected to move to fm-incubator) | OK |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [Invoice](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-Invoice.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [Linkage](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-Linkage.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [PublicationRecord](https://build.fhir.org/ig/HL7/ebm-incubator/en/StructureDefinition-PublicationRecord.html) \* **NEW** | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/en/) | OK |
| [StructureMap](https://build.fhir.org/ig/HL7/mapping-language-ig/en/StructureDefinition-StructureMap.html) **NEW** | [mapping-language-ig](https://build.fhir.org/ig/HL7/mapping-language-ig/en/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |

> \* **Citation → PublicationRecord**: The Citation resource previously in ebm-incubator has been superseded by the new **PublicationRecord** resource. PublicationRecord has broader scope covering identification, location, authorship, and contributorship of journal articles, reports, documents, and other knowledge artifacts. The old Citation.fsh has been removed from the repo.
>
> \*\* **Contract**: R6 ballot-intro lists Contract as moved to fm-incubator, but no Contract StructureDefinition exists in the fm-incubator repository or build. The migration was never completed.
>
> \*\*\* **Contributorship** was removed from ebm-incubator on 2026-05-19 by brianalperMD — the StructureDefinition and search-params bundle were deleted from the repo. No incubator destination has been assigned.
>
> \*\*\*\* **InsurancePlan** and **InsuranceProduct** are no longer present in the R6 core spec (as of 6.0.0-snapshot1). Both are hosted in admin-incubator, though R6 ballot-intro states they belong in fm-incubator. fm-incubator has been created but does not yet host them.

---

## All incubated profiles

| Profile | Incubator IG | Status |
|---------|-------------|--------|
| [medicalproductofhumanorigin](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-medicalproductofhumanorigin.html) (on BiologicallyDerivedProduct) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [shareabletestscript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-shareabletestscript.html) (on TestScript) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [FeatureCapabilityStatement](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) (on CapabilityStatement) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [feature](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) (extension on Extension) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [permission-from-consent](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-from-consent.html) (extension on Extension) **NEW** | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | OK |

Additionally, capstmt publishes **FeatureDefinition**, a logical model (kind=logical) — neither a resource nor a profile.

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
| [StructureMap $transform](https://build.fhir.org/ig/HL7/mapping-language-ig/en/OperationDefinition-StructureMap-transform.html) **NEW** | [mapping-language-ig](https://build.fhir.org/ig/HL7/mapping-language-ig/en/) | OK |

---

## All other incubated pages

| Page | Incubator IG | Status |
|------|-------------|--------|
| [Asynchronous Interaction Request Pattern](https://build.fhir.org/ig/HL7/api-incubator-ig/async-interaction.html) | api-incubator-ig | OK |
| [Asynchronous Bulk Data Request Pattern](https://build.fhir.org/ig/HL7/api-incubator-ig/async-bulk.html) | api-incubator-ig | OK |
| [Operations for Large Resources](https://build.fhir.org/ig/HL7/api-incubator-ig/operations-for-large-resources.html) | api-incubator-ig | OK |
| [FHIRPath Extensions for FHIR](https://build.fhir.org/ig/HL7/api-incubator-ig/fhirpath.html) | api-incubator-ig | OK |
| [FHIR Mapping Language (FML)](https://build.fhir.org/ig/HL7/mapping-language-ig/en/fml.html) **NEW** | mapping-language-ig | OK |
| [Mapping Language Tutorial](https://build.fhir.org/ig/HL7/mapping-language-ig/en/mapping-tutorial.html) **NEW** | mapping-language-ig | OK |

---

## Notes for the FHIR Management Group (R6 transition)

<details>
<summary>Show details</summary>

**FHIR R6 status:** The build is now at **6.0.0-snapshot1** (post-ballot5). Ballot 5 concluded and ballot-intro references still point at the incubator IGs.

**Still in R6 core, awaiting final incubator destination:**
- None detected. All resources listed in the R6 ballot-intro "moved to incubators" section have been removed from the R6 core resource list (verified against `build.fhir.org/resourcelist.html`).

**Removed from build with no incubator home:**
- **Contract**: R6 ballot-intro links Contract to `HL7/fm-incubator/StructureDefinition-Contract.html`, but no Contract StructureDefinition exists in the fm-incubator repo or build. Migration never completed; Contract was previously flagged as "in repo, not in build (partial migration)" — it is now gone from the repo entirely.
- **Contributorship**: Removed from ebm-incubator on 2026-05-19 by brianalperMD; StructureDefinition and search-params bundle deleted from repo. No incubator destination assigned. Not in R6 core either.
- **Citation**: The original Citation resource has been superseded by **PublicationRecord** in ebm-incubator. R6 ballot-intro still references Citation at `ig/HL7/ebm/StructureDefinition-Citation.html` (broken link — old path). Ballot-intro needs updating.

**Missing content:**
- **DocumentReference $generate** and **SpecimenDefinition $apply** operations: R6 ballot-intro lists both as moved to api-incubator, but neither OperationDefinition exists in the api-incubator-ig build (both return 404). Ballot-intro references broken paths (`build.fhir.org/HL7/api-incubator/...` missing `/ig/`).
- **Asynchronous Bundle Request** page (previously referenced in ballot-intro at `async-bundle.html`): api-incubator-ig now has `async-interaction.html` ("Asynchronous Interaction Request Pattern") instead; ballot-intro link is broken.

**Discrepancies between R6 ballot-intro and actual incubator locations:**
- **InsurancePlan** and **InsuranceProduct**: Ballot-intro shows them in fm-incubator; they are actually in admin-incubator. Both have been fully removed from R6 core (unchanged from prior report where they were still in core).
- **ChargeItemDefinition $apply**: Ballot-intro shows it in api-incubator; it is actually in admin-incubator.
- **GraphDefinition**, **FormularyItem**: Ballot-intro links are broken (missing `/ig/` in path).

**Untracked capstmt artifacts:**
- FeatureDefinition (logical model, kind=logical) — accepted per note that new artifacts in incubators are fine.
- The `feature` extension (complex-type on Extension) — accepted.

**New incubator this cycle:**
- **mapping-language-ig** (FHIR Infrastructure, created 2026-06-11): hosts StructureMap resource and the StructureMap $transform operation. Referenced from R6 ballot-intro.

</details>

---

Compiled 2026-09-18 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
