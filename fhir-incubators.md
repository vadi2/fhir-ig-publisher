# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Profiles | Operations | Build Status |
|---|-----|------------|-----------|----------|------------|--------------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | Patient Administration | 7 | 0 | 5 | OK |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 | 1 | 0 | OK |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | 0 | OK |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | 0 | OK |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 | 0 | 0 | OK |
| 6 | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | FHIR Infrastructure | 1 | 0 | 8 (+ 2 planned but missing) | OK |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 3 (+ 1 extension, + 1 logical model) | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 1 | 0 | OK |
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | Security | 1 | 0 | 0 | OK |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | Terminology Infrastructure | 0 | 0 | 2 | OK |
| 11 | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | Patient Care | 3 | 0 | 0 | OK |
| 12 | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | Pharmacy | 1 | 0 | 0 | OK |
| 13 | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | Financial Management | 3 (of ~8 planned) | 0 | 0 | OK |

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/en/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK |
| [ClinicalAssessment](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ClinicalAssessment.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [ConditionDefinition](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ConditionDefinition.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| Contract | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | In repo, not in build (partial migration) |
| Contributorship \* | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | REMOVED - was pending, deleted from repo 2026-05-19 |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [EnrollmentRequest](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentRequest.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [EnrollmentResponse](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentResponse.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [FormularyItem](https://build.fhir.org/ig/HL7/phx-incubator/StructureDefinition-FormularyItem.html) | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | OK |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [GraphDefinition](https://build.fhir.org/ig/HL7/api-incubator-ig/StructureDefinition-GraphDefinition.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) \*\*\* | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) (expected to move to fm-incubator) | OK |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) \*\*\* | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) (expected to move to fm-incubator) | OK |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [Invoice](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-Invoice.html) \*\* | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [Linkage](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-Linkage.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |

> \* **Contributorship** was added to ebm-incubator on Apr 2, 2026, then removed on 2026-05-19 (by brianalperMD) - the StructureDefinition and search-params bundle were deleted from the repo. No incubator destination has been assigned.
>
> \*\* **Invoice** is flagged in the R6 ballot4 ballot-intro as "likely to be removed" from core post-ballot, alongside InsurancePlan and InsuranceProduct. Invoice has been added to fm-incubator (2026-05-27+); InsurancePlan/InsuranceProduct remain in admin-incubator only.
>
> \*\*\* **InsurancePlan** and **InsuranceProduct** are also still present in the R6 core spec for now; they have been added to admin-incubator preemptively. fm-incubator exists but does not yet host them.

---

## All incubated profiles

| Profile | Incubator IG | Status |
|---------|-------------|--------|
| [medicalproductofhumanorigin](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-medicalproductofhumanorigin.html) (on BiologicallyDerivedProduct) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [shareabletestscript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-shareabletestscript.html) (on TestScript) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [FeatureCapabilityStatement](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) (on CapabilityStatement) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [feature](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) (extension on Extension) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |

Additionally, capstmt publishes [FeatureDefinition](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureDefinition.html), a logical model (kind=logical) - neither a resource nor a profile.

---

## All incubated operations

| Operation | Incubator IG | Status |
|-----------|-------------|--------|
| [CapabilityStatement $subset](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-subset.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $conforms](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-conforms.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $implements](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-implements.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [$feature-query](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-feature-query.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [ChargeItemDefinition $apply](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-ChargeItemDefinition-apply.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Encounter $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Encounter-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [EpisodeOfCare $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-EpisodeOfCare-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Patient $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Patient $merge](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-merge.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
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
|------|-------------|--------|
| [Asynchronous Interaction Request Pattern](https://build.fhir.org/ig/HL7/api-incubator-ig/async-interaction.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Asynchronous Bulk Data Request Pattern](https://build.fhir.org/ig/HL7/api-incubator-ig/async-bulk.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Operations for Large Resources](https://build.fhir.org/ig/HL7/api-incubator-ig/operations-for-large-resources.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [FHIRPath Extensions for FHIR](https://build.fhir.org/ig/HL7/api-incubator-ig/fhirpath.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |

---

## Notes for the FHIR Management Group (R6 transition)

<details>
<summary>Show details</summary>

**Still in R6 core, awaiting final incubator destination:**

- **InsurancePlan** and **InsuranceProduct** - still present in R6 core; flagged in R6 ballot4 ballot-intro as "likely to be removed" post-ballot. Preemptively added to admin-incubator; fm-incubator (their expected final home) exists but does not yet host them.
- **Invoice** - still present in R6 core; also flagged for removal. Already added to fm-incubator, so post-ballot core deletion should be clean.

**Removed from build with no incubator home:**

- **Contributorship** - added to ebm-incubator on 2026-04-02 and removed on 2026-05-19 (by brianalperMD). The StructureDefinition and search-params bundle were deleted from the ebm-incubator repo. No incubator destination has been assigned. FMG action needed to decide destination (or confirm withdrawal).

**Missing content (advertised or planned but not yet published):**

- **api-incubator-ig / DocumentReference $generate** - listed as an intended operation but no OperationDefinition file exists in the build (or in `input/resources/` on the repo).
- **api-incubator-ig / SpecimenDefinition $apply** - listed as an intended operation but no OperationDefinition file exists in the build (or in `input/resources/` on the repo).
- **fm-incubator / Contract** - present in the repo (`input/resources/contract/`) but not compiled into the build. Only EnrollmentRequest, EnrollmentResponse, and Invoice currently publish. Partial migration.
- **fm-incubator** - ~5 additional resources planned (of ~8 target) not yet added to the build. FMG should confirm the exact target list with the FM WG (candidates typically include Coverage, ExplanationOfBenefit, PaymentNotice, PaymentReconciliation, etc.).

**Untracked capstmt artifacts:**

- These exist in the build now and are expected — the incubator process is meant to produce new artifacts. Currently: FeatureDefinition (logical model, `kind=logical`), FeatureCapabilityStatement, FeatureQueryInputParameters, FeatureQueryOutputParameters (profiles), feature (extension), and 4 operations. No untracked artifacts appearing that are absent from the build.

**Duplicated hosting to be cleaned up post-ballot:**

- InsurancePlan, InsuranceProduct, and Invoice currently live in both R6 core AND their respective incubators (admin-incubator for the first two, fm-incubator for Invoice). Once R6 ballot resolves their removal from core, the incubator becomes the sole home. FMG to schedule the post-ballot cleanup.

</details>

---

Compiled 2026-07-31 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
