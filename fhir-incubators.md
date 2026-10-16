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
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | Security | 1 | 0 (+ 4 extensions) | 0 | OK |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/en/) | Terminology Infrastructure | 0 | 0 | 2 | OK |
| 11 | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | Patient Care | 3 | 0 | 0 | OK |
| 12 | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | Pharmacy | 1 | 0 | 0 | OK |
| 13 | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | Financial Management | 3 (of ~8 planned) | 0 | 0 | OK |

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [ClinicalAssessment](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ClinicalAssessment.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [ConditionDefinition](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ConditionDefinition.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [EnrollmentRequest](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentRequest.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [EnrollmentResponse](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentResponse.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [FormularyItem](https://build.fhir.org/ig/HL7/phx-incubator/StructureDefinition-FormularyItem.html) | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | OK |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/en/) | OK |
| [GraphDefinition](https://build.fhir.org/ig/HL7/api-incubator-ig/StructureDefinition-GraphDefinition.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/en/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/en/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/)<br/>_expected to move to fm-incubator_ | OK *** |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/)<br/>_expected to move to fm-incubator_ | OK *** |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [Invoice](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-Invoice.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [Linkage](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-Linkage.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/en/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [PublicationRecord](https://build.fhir.org/ig/HL7/ebm-incubator/en/StructureDefinition-PublicationRecord.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/en/) | OK (**NEW** - replaces former Citation) |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |

*Previously tracked but no longer present in builds:*
- **Contributorship** - was in ebm-incubator (added 2026-04-02), removed 2026-05-19 by @brianalperMD; no incubator destination assigned.
- **Citation** - previously listed under ebm-incubator; now effectively replaced by **PublicationRecord**, which generalises citation-record semantics. The index page describes citation-type profiles on PublicationRecord (JournalArticleCitation, BookCitation, BookPartCitation, DatabaseCitation, DatabaseEntryCitation, DatasetCitation, PreprintCitation, SoftwareCitation, WebPageCitation) but those profile StructureDefinitions are not yet in the published package.
- **Contract** - in fm-incubator repository but not yet in the build (partial migration).

*** InsurancePlan and InsuranceProduct are also still present in the R6 core spec for now; they have been added to admin-incubator preemptively. fm-incubator exists but does not yet host them.

---

## All incubated profiles

| Profile | Base | Incubator IG | Status |
|---------|------|-------------|--------|
| [FeatureCapabilityStatement](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) | CapabilityStatement | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) | Parameters | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) | Parameters | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [ShareableTestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-shareabletestscript.html) | TestScript | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [MedicalProductOfHumanOrigin](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-medicalproductofhumanorigin.html) | BiologicallyDerivedProduct | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |

### Related conformance assets (extensions, logical models)

| Artifact | Kind | Incubator IG | Status |
|----------|------|-------------|--------|
| [Feature](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) | extension | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureDefinition](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureDefinition.html) | logical model | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [PermissionFromConsent](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-from-consent.html) | extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | OK |
| [PermissionRuleExpression](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-rule-expression.html) | extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | OK |
| [PermissionImposedOnBundle](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-imposed-on-bundle.html) | extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | OK |
| [PermissionKanonymity](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-K-anonymity.html) | extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/en/) | OK |

---

## All incubated operations

| Operation | Incubator IG | Status |
|-----------|-------------|--------|
| [(system-level) $feature-query](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-feature-query.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $conforms](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-conforms.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $implements](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-implements.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [CapabilityStatement $subset](https://build.fhir.org/ig/HL7/capstmt/OperationDefinition-CapabilityStatement-subset.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [ChargeItemDefinition $apply](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-ChargeItemDefinition-apply.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [CodeSystem $find-matches](https://build.fhir.org/ig/HL7/txmodule-incubator/en/OperationDefinition-CodeSystem-find-matches.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/en/) | OK |
| [ConceptMap $closure](https://build.fhir.org/ig/HL7/txmodule-incubator/en/OperationDefinition-ConceptMap-closure.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/en/) | OK |
| [Encounter $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Encounter-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [EpisodeOfCare $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-EpisodeOfCare-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Group $add](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-large-resource-add.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Group $filter](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-large-resource-filter.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Group $remove](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-large-resource-remove.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [List $find](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-List-find.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Patient $everything](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-everything.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Patient $merge](https://build.fhir.org/ig/HL7/admin-incubator/en/OperationDefinition-Patient-merge.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Resource $graph](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-graph.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $meta](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-meta.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $meta-add](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-meta-add.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [Resource $meta-delete](https://build.fhir.org/ig/HL7/api-incubator-ig/OperationDefinition-Resource-meta-delete.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| DocumentReference $generate | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | **Missing** (listed but not built) |
| SpecimenDefinition $apply | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | **Missing** (listed but not built) |

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
<summary>Expand transition details</summary>

**Still in R6 core, awaiting final incubator destination:**
- `InsurancePlan` - added to admin-incubator preemptively; expected to move to fm-incubator but fm-incubator does not yet host it.
- `InsuranceProduct` - same as InsurancePlan.
- `Invoice` - flagged in R6 ballot4 ballot-intro as "likely to be removed" from core post-ballot; has been added to fm-incubator since 2026-05-27+. Dual-sited pending core removal.

**Removed from build with no incubator home:**
- `Contributorship` - was pending in ebm-incubator (added 2026-04-02), removed 2026-05-19 by @brianalperMD. StructureDefinition and search-params bundle were deleted from the repo. **No incubator destination has been assigned.**

**Resource identity changes since 2026-07-17:**
- **ebm-incubator**: `Citation` resource (previously tracked) is no longer in the build. The incubator now defines `PublicationRecord` as a new specialization resource. The narrative lists nine citation-type profiles (JournalArticleCitation, BookCitation, BookPartCitation, DatabaseCitation, DatabaseEntryCitation, DatasetCitation, PreprintCitation, SoftwareCitation, WebPageCitation) that are intended to replace the Citation surface, but the profile StructureDefinitions are not yet published in the package. **FHIR MG should confirm whether this is a rename/evolution of Citation or an entirely new resource.**

**Missing content (listed/planned but not yet in the build):**
- `api-incubator-ig`: `DocumentReference-generate` operation - listed but not yet published.
- `api-incubator-ig`: `SpecimenDefinition-apply` operation - listed but not yet published.
- `fm-incubator`: `Contract` resource - exists in the GitHub repo but is not in the current build.
- `ebm-incubator`: nine PublicationRecord citation-type profiles described in narrative but not yet published as StructureDefinitions.

**Untracked capstmt artifacts:**
- `FeatureDefinition` (logical model, `kind=logical`) - a logical model, neither a resource nor a profile. Now listed above under *Related conformance assets*.
- `feature` (extension on Extension) - likewise tracked above.

**New in this report vs. 2026-07-17 Confluence snapshot:**
- `data-access-policies` now ships four Permission-related extensions (permission-from-consent, permission-rule-expression, permission-imposed-on-bundle, permission-K-anonymity). These are tracked here under *Related conformance assets*.

**Build dates (as fetched 2026-10-16):**
- admin-incubator: 2026-09-08
- oo-incubator: 2026-10-15 (freshest)
- cg-incubator: 2026-09-08
- immunization-incubator: 2026-10-07
- ebm-incubator: 2026-10-09
- api-incubator-ig: 2026-09-08
- capstmt: 2026-06-03
- fhir-testing-ig: 2026-10-13
- data-access-policies: 2026-10-12
- txmodule-incubator: 2026-10-01
- pc-incubator: 2026-09-08
- phx-incubator: 2026-09-08
- fm-incubator: 2026-09-08

All incubator CI builds succeed (none are FAILED).

</details>

---

Compiled 2026-10-16 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
