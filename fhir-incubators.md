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
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | Security | 1 | 0 (+ 4 extensions) | 0 | OK |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | Terminology Infrastructure | 0 | 0 | 2 | OK |
| 11 | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | Patient Care | 3 | 0 | 0 | OK |
| 12 | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | Pharmacy | 1 | 0 | 0 | OK |
| 13 | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | Financial Management | 3 (+ Contract in repo, not in build) | 0 | 0 | OK |

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| ~~Citation~~ * | ~~ebm-incubator~~ | REMOVED/RENAMED — the ebm-incubator resource is now PublicationRecord |
| [ClinicalAssessment](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ClinicalAssessment.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [ConditionDefinition](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ConditionDefinition.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| Contract | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | In repo, not in build (partial migration) |
| ~~Contributorship~~ ** | ~~ebm-incubator~~ | REMOVED — deleted from repo 2026-05-19, no incubator destination assigned |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [EnrollmentRequest](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentRequest.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [EnrollmentResponse](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentResponse.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [FormularyItem](https://build.fhir.org/ig/HL7/phx-incubator/StructureDefinition-FormularyItem.html) | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | OK |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [GraphDefinition](https://build.fhir.org/ig/HL7/api-incubator-ig/StructureDefinition-GraphDefinition.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) *** | OK |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) *** | OK |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [Invoice](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-Invoice.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [Linkage](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-Linkage.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [PublicationRecord](https://build.fhir.org/ig/HL7/ebm-incubator/en/StructureDefinition-PublicationRecord.html) **** | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK — **NEW** (replaces Citation) |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |

\* Citation was previously tracked for ebm-incubator; the current build hosts **PublicationRecord** instead (same conceptual purpose — reference/attribution for knowledge artifacts). The SD was renamed/replaced between the 2026-07-17 snapshot and today.

\** Contributorship was removed from the ebm-incubator repo on 2026-05-19; no incubator destination has been assigned.

\*** InsurancePlan and InsuranceProduct were previously tracked as "still in R6 core, added to admin-incubator preemptively". They are **no longer present in R6 core** (removed from the R6 CI build's resourcelist) — they now live only in admin-incubator. fm-incubator still does not host them.

\**** PublicationRecord is new in ebm-incubator and was not on the prior Confluence page.

---

## All incubated profiles

| Profile | Base Resource | Incubator IG | Status |
|---------|---------------|--------------|--------|
| [FeatureCapabilityStatement](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) | CapabilityStatement | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) | Parameters | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) | Parameters | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [feature (extension)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) | Extension | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [MedicalProductOfHumanOrigin](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-medicalproductofhumanorigin.html) | BiologicallyDerivedProduct | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ShareableTestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-shareabletestscript.html) | TestScript | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [permission-rule-expression (extension)](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-rule-expression.html) | Extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK — **NEW** |
| [permission-from-consent (extension)](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-from-consent.html) | Extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK — **NEW** |
| [permission-K-anonymity (extension)](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-K-anonymity.html) | Extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK — **NEW** |
| [permission-imposed-on-bundle (extension)](https://build.fhir.org/ig/HL7/data-access-policies/en/StructureDefinition-permission-imposed-on-bundle.html) | Extension | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK — **NEW** |

Additionally, capstmt publishes **FeatureDefinition**, a logical model (kind=logical) — neither a resource nor a profile.

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
| [ConceptMap $closure](https://build.fhir.org/ig/HL7/txmodule-incubator/en/OperationDefinition-ConceptMap-closure.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | OK |
| [CodeSystem $find-matches](https://build.fhir.org/ig/HL7/txmodule-incubator/en/OperationDefinition-CodeSystem-find-matches.html) | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | OK |
| DocumentReference $generate | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | Missing (planned, not built) |
| SpecimenDefinition $apply | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | Missing (planned, not built) |
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

### Still in R6 core, awaiting final incubator destination

None identified in this run — intersecting the R6 core resourcelist (124 resources) with all incubator resources yields **zero overlap**. Every resource that has moved to an incubator is no longer present in the R6 CI build.

Previously flagged in Confluence as "still in R6 core" but now cleared:

- **InsurancePlan** — removed from R6 core; lives only in admin-incubator (change since 2026-07-17).
- **InsuranceProduct** — removed from R6 core; lives only in admin-incubator (change since 2026-07-17).
- **Invoice** — was flagged in R6 ballot4 as likely to be removed; now removed from R6 core and hosted only in fm-incubator.

### Removed from build with no incubator home

- **Contributorship** (ebm-incubator) — deleted from the repo on 2026-05-19; still has no assigned incubator destination. No action since the 2026-07-17 Confluence snapshot.
- **Citation** (ebm-incubator) — the StructureDefinition that was tracked on Confluence is no longer in the ebm-incubator build. See below (missing vs. renamed).

### Missing content

- **Contract** — StructureDefinition-Contract.xml is still present in the [fm-incubator repo](https://github.com/HL7/fm-incubator/tree/master/input/resources/contract) but is not included in the CI build output (partial migration, same state as the 2026-07-17 snapshot).
- **api-incubator-ig: DocumentReference $generate** — listed in the IG but no OperationDefinition published at build.fhir.org (same state as prior snapshot; 404 verified today).
- **api-incubator-ig: SpecimenDefinition $apply** — listed in the IG but no OperationDefinition published at build.fhir.org (same state as prior snapshot; 404 verified today).

### Renames / replacements

- **ebm-incubator Citation → PublicationRecord**: the ebm-incubator previously tracked a resource named "Citation"; the current build contains only PublicationRecord (same conceptual purpose: reference/attribution for knowledge artifacts). The resource count is unchanged (still 1) but the identity differs. The FMG should confirm whether this is an intended rename, or whether Citation should also be preserved/migrated separately.

### Previously-noted "untracked" items that are expected new artifacts

- **capstmt profiles, extensions, logical model, and operations** — all are additions by the capstmt IG itself; they are new artifacts created within the incubator (not resources migrated out of R6 core) and are therefore expected, not untracked discrepancies.

### New content since 2026-07-17 snapshot

- **ebm-incubator: PublicationRecord** (NEW resource, replacing Citation).
- **data-access-policies: 4 new extensions** — permission-rule-expression, permission-from-consent, permission-K-anonymity, permission-imposed-on-bundle. These were not on the prior Confluence table (which listed only the Permission resource).

### Build health

All 13 incubator IGs return HTTP 200 for both landing page and QA report. No build regressions detected.

</details>

---

Compiled 2026-11-06 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
