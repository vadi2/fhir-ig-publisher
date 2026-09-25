# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Profiles | Operations | Build Status |
|---|-----|------------|-----------|----------|------------|--------------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | Patient Administration | 7 | 0 | 5 | OK (169 errors) |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 | 1 | 0 | OK (37 errors) |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | 0 | OK (131 errors) |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | 0 | OK (12 errors) |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 | 0 | 0 | OK |
| 6 | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | FHIR Infrastructure | 1 | 0 | 8 (+2 planned but missing) | OK (16 errors) |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 3 (+ 1 extension, + 1 logical model) | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 1 | 0 | OK (47 errors) |
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | Security | 1 | 0 (+ 1 extension) | 0 | OK (59 errors) |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | Terminology Infrastructure | 0 | 0 | 2 | OK |
| 11 | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | Patient Care | 3 | 0 | 0 | OK (33 errors) |
| 12 | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | Pharmacy | 1 | 0 | 0 | OK (3 errors) |
| 13 | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | Financial Management | 3 (of ~8 planned) | 0 | 0 | OK (20 errors) |

_"OK (N errors)" means the IG built successfully and artifacts are produced, but the QA report shows N validation errors that should be resolved before ballot._

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| ~~Citation~~ | ~~ebm-incubator~~ | **REMOVED / REPLACED** — no longer in ebm-incubator (see change note below) |
| [ClinicalAssessment](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ClinicalAssessment.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [ConditionDefinition](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-ConditionDefinition.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| Contract | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | In repo, not in build (partial migration — sushi-config does not yet include it) |
| ~~Contributorship *~~ | ~~ebm-incubator~~ | REMOVED — deleted from repo 2026-05-19 by brianalperMD |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [EnrollmentRequest](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentRequest.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [EnrollmentResponse](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-EnrollmentResponse.html) | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [FormularyItem](https://build.fhir.org/ig/HL7/phx-incubator/StructureDefinition-FormularyItem.html) | [phx-incubator](https://build.fhir.org/ig/HL7/phx-incubator/) | OK |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [GraphDefinition](https://build.fhir.org/ig/HL7/api-incubator-ig/StructureDefinition-GraphDefinition.html) | [api-incubator-ig](https://build.fhir.org/ig/HL7/api-incubator-ig/) | OK (but misfiled under "Other" in artifacts.html — see FMG notes) |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) expected to move to fm-incubator | OK *** |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) expected to move to fm-incubator | OK *** |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [Invoice](https://build.fhir.org/ig/HL7/fm-incubator/StructureDefinition-Invoice.html) ** | [fm-incubator](https://build.fhir.org/ig/HL7/fm-incubator/) | OK |
| [Linkage](https://build.fhir.org/ig/HL7/pc-incubator/StructureDefinition-Linkage.html) | [pc-incubator](https://build.fhir.org/ig/HL7/pc-incubator/) | OK |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| **[PublicationRecord](https://build.fhir.org/ig/HL7/ebm-incubator/StructureDefinition-PublicationRecord.html)** *NEW* | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |

\* Contributorship was added to ebm-incubator on Apr 2, 2026, then removed on 2026-05-19 by brianalperMD — the StructureDefinition, search-params bundle and operations list were deleted from the repo (commit b34f6d0, 3,521 lines deleted). No incubator destination has been assigned.

\** Invoice is flagged in the R6 ballot4 ballot-intro as "likely to be removed" from core post-ballot, alongside InsurancePlan and InsuranceProduct. Invoice has been added to fm-incubator (2026-05-27+); InsurancePlan/InsuranceProduct remain in admin-incubator only.

\*** InsurancePlan and InsuranceProduct have been removed from the R6 core spec (verified against v6.0.0-snapshot1 resourcelist.html on 2026-09-25); they have been added to admin-incubator. fm-incubator exists but does not yet host them.

**⚠️ New this cycle: PublicationRecord (ebm-incubator) — appears to have replaced Citation as the incubator's evidence-based-medicine resource. Citation is no longer present in the ebm-incubator repo (`input/resources/` contains only `publicationrecord/`), and PublicationRecord commits by brianalperMD dominate the recent history (Aug 20 → Sep 24, 2026).**

---

## All incubated profiles

| Profile | Incubator IG | Status |
|---------|-------------|--------|
| [medicalproductofhumanorigin](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-medicalproductofhumanorigin.html) (on BiologicallyDerivedProduct) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [shareabletestscript](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-shareabletestscript.html) (on TestScript) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |
| [FeatureCapabilityStatement](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) (on CapabilityStatement) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) (on Parameters) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [feature](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) (extension on Extension) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| **[permission-from-consent](https://build.fhir.org/ig/HL7/data-access-policies/StructureDefinition-permission-from-consent.html)** *NEW* (extension on Consent → Permission) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK |

Additionally, capstmt publishes [FeatureDefinition](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureDefinition.html), a logical model (kind=logical) — neither a resource nor a profile.

**⚠️ New this cycle: `permission-from-consent` extension in data-access-policies (added 2026-09-11 alongside FHIR-56877 Permission.rule.limit clarification).**

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
| [Asynchronous Interaction Request Pattern](https://build.fhir.org/ig/HL7/api-incubator-ig/async-interaction.html) | api-incubator-ig | OK |
| [Asynchronous Bulk Data Request Pattern](https://build.fhir.org/ig/HL7/api-incubator-ig/async-bulk.html) | api-incubator-ig | OK |
| [Operations for Large Resources](https://build.fhir.org/ig/HL7/api-incubator-ig/operations-for-large-resources.html) | api-incubator-ig | OK |
| [FHIRPath Extensions for FHIR](https://build.fhir.org/ig/HL7/api-incubator-ig/fhirpath.html) | api-incubator-ig | OK |

---

## Notes for the FHIR Management Group (R6 transition)

<details>
<summary>Show details</summary>

### Still in R6 core (v6.0.0-snapshot1), awaiting final incubator destination:
- No resources from the incubator target list were found to still be present in the R6 core `resourcelist.html`. Verified 2026-09-25 that all of the following are OUT of core: Contract, InsurancePlan, InsuranceProduct, Invoice, EnrollmentRequest, EnrollmentResponse, ChargeItem, ChargeItemDefinition.
- **InsurancePlan / InsuranceProduct** are hosted in **admin-incubator** but are expected to move to **fm-incubator** — the fm-incubator sushi-config does not yet include them. FMG action: assign a destination and schedule the move.

### Removed from build with no incubator home:
- **Contributorship** (evidence-based medicine) — removed from `ebm-incubator` on **2026-05-19** (commit `b34f6d0`, 3,521 lines deleted, brianalperMD). No incubator destination has been assigned. FMG action: confirm this is intentionally dropped, or route to a new home.
- **Citation** — no longer present in `ebm-incubator` (repo `input/resources/` contains only `publicationrecord/`). It appears that **PublicationRecord** (new resource, active development Aug 20 → Sep 24, 2026) has taken its place. FMG action: confirm intent — is PublicationRecord a rename of / successor to Citation, or was Citation always meant to be elsewhere?

### Contract migration is partial:
- **Contract** source files are present in `fm-incubator/input/resources/contract/` but `sushi-config.yaml` does not yet include Contract, so it is not in the build. FMG action: complete the migration or move Contract to a different home.

### Missing operations (declared but not published):
- `DocumentReference $generate` and `SpecimenDefinition $apply` are still listed as planned operations for **api-incubator-ig** but the OperationDefinition artifacts are not yet published. FMG action: either author these OperationDefinitions or remove them from the plan.

### GraphDefinition misfiled:
- **GraphDefinition** (in api-incubator-ig) appears under the artifacts page's "Other" section rather than "Structures: Resources". This is causing the FAILED-QA state for that IG (16 errors including "not legal because it is not defined in the FHIR specification" and 5 unresolved-canonical errors on its search parameters). FMG action: fix the resource categorisation in the IG.

### QA-errors backlog (all IGs build successfully but have QA report errors):
| IG | Errors | Notes |
|---|---|---|
| admin-incubator | 169 | R6/R5 dependency mismatch; missing FHIR-admin-incubator Jira spec; duplicate ToC titles; 38 broken links |
| cg-incubator | 131 | R5 dependency versions (terminology.r5, extensions.r5, tools.r5); registry metadata gaps |
| data-access-policies | 59 | Content and QA issues (verified via qa.html) |
| fhir-testing-ig | 47 | Content and QA issues |
| oo-incubator | 37 | R6/R5 mismatch; broken cross-refs to workflow-episodeOfCare / MedicationKnowledge / supplydelivery-stage |
| pc-incubator | 33 | Custom resources not recognized by R6 tooling; 9 broken links |
| fm-incubator | 20 | 16 broken links; custom resources not recognized by R6 tooling |
| api-incubator-ig | 16 | GraphDefinition mis-categorised; 5 unresolved canonicals on search params |
| immunization-incubator | 12 | Immunization* not recognized by R6 tooling; 2 broken links |
| phx-incubator | 3 | Minor cleanup only |
| txmodule-incubator | 1 | Only missing FHIR-txmodule-incubator Jira spec |
| ebm-incubator | 0 | Only 1 JIRA-out-of-date warning; cleanest of the set |
| capstmt | 0 | "No Messages found - all good" |

Most repos received a coordinated **2026-09-08 "get building"** commit from grahamegrieve to force a rebuild — this appears to have surfaced QA errors that were previously latent. FMG action: prioritise resolving errors before ballot 5 closes.

### Not on the Confluence page but tracked:
- **sample-incubator-ig** — HL7/sample-incubator-ig is a template/skeleton repo (5 commits, all Nov 2025), not a live incubator. Excluded from the report.
- **capstmt**, **fhir-testing-ig**, **data-access-policies** — not named `*-incubator` but are R6 incubator IGs by function. Included above (as in the current Confluence page).

### Untracked capstmt artifacts:
None found — the capstmt incubator publishes exactly the artifacts listed in the profiles/operations tables above (3 profiles, 1 extension, 1 logical model, 4 operations, plus supporting ValueSets/CodeSystems/examples). New artifacts in incubators are fine — the purpose of them — this section only flags things that would be published in a NEW state without matching the build; no such items found.

</details>

---

Compiled 2026-09-25 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
