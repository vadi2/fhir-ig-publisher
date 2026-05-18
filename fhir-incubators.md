# FHIR Incubator IGs Overview

As part of FHIR R6 (v6.0.0), immature resources and operations were moved out of the core specification into "incubator" Implementation Guides for further development. Incubators and their resources are listed below.

---

## Summary

| # | IG | Work Group | Resources | Operations | Build Status |
|---|-----|------------|-----------|------------|--------------|
| 1 | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | Patient Administration | 7 | 5 | OK |
| 2 | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | Orders and Observations | 8 | 0 | OK |
| 3 | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/en/) | Clinical Genomics | 2 | 0 | OK |
| 4 | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/en/) | Public Health | 2 | 0 | OK |
| 5 | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/en/) | Clinical Decision Support | 1 (+1 pending) | 0 | OK |
| 6 | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | FHIR Infrastructure | 1 | 12 | FAILED |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | FHIR Infrastructure | 3 | 0 | OK |
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | Security | 1 | 0 | OK |
| 10 | [txmodule-incubator](https://github.com/HL7/txmodule-incubator) | Terminology Infrastructure | TBD (planned) | 0 | REPO CREATED (EMPTY), NO BUILD |
| 11 | [pc-incubator](https://github.com/HL7/pc-incubator) | Patient Care | 3 (planned) | 0 | REPO CREATED (EMPTY), NO BUILD |
| 12 | phx-incubator | Pharmacy | 1 (planned) | 0 | NOT CREATED |

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|-------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/en/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/en/) | OK |
| ClinicalAssessment | [pc-incubator](https://github.com/HL7/pc-incubator) | NOT BUILT (repo empty) |
| ConditionDefinition | [pc-incubator](https://github.com/HL7/pc-incubator) | NOT BUILT (repo empty) |
| Contributorship * | [ebm-incubator](https://github.com/HL7/ebm-incubator) | PENDING — in repo, not yet in build |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| FormularyItem | phx-incubator | NOT CREATED |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/en/) | OK |
| GraphDefinition | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD FAILED |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/en/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/en/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/en/) | OK |
| Invoice ** | TBD | FLAGGED FOR REMOVAL |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK *** |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK *** |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| Linkage | [pc-incubator](https://github.com/HL7/pc-incubator) | NOT BUILT (repo empty) |
| [MolecularDefinition](https://build.fhir.org/ig/HL7/cg-incubator/en/StructureDefinition-MolecularDefinition.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/en/) | OK |
| [Permission](https://build.fhir.org/ig/HL7/data-access-policies/StructureDefinition-Permission.html) | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | OK |
| [PersonalRelationship](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-PersonalRelationship.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |
| [SupplyDelivery](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyDelivery.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [SupplyRequest](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-SupplyRequest.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [TestPlan](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestPlan.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [TestReport](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestReport.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [TestScript](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-TestScript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [Transport](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-Transport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [VerificationResult](https://build.fhir.org/ig/HL7/admin-incubator/en/StructureDefinition-VerificationResult.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/en/) | OK |

\* Contributorship was added to ebm-incubator on Apr 2, 2026. The StructureDefinition and search parameters are in the repo but the resource still does not appear in the CI build output (verified still pending as of 2026-05-18 — the ebm-incubator build only exposes Citation under "Additional Resources").

\*\* Invoice is flagged in the R6 ballot4 ballot-intro as "likely to be removed" from core post-ballot, alongside InsurancePlan and InsuranceProduct. No incubator destination has been assigned yet.

\*\*\* InsurancePlan and InsuranceProduct are also still present in the R6 core spec for now; they have been added to admin-incubator preemptively.

---

## All incubated profiles

| Profile | Incubator IG | Status |
|-----------|-------------|--------|
| [medicalproductofhumanorigin (on BiologicallyDerivedProduct)](https://build.fhir.org/ig/HL7/oo-incubator/en/StructureDefinition-medicalproductofhumanorigin.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/en/) | OK |
| [shareabletestscript (on TestScript)](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/StructureDefinition-shareabletestscript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/en/) | OK |
| [FeatureCapabilityStatement (on CapabilityStatement)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters (on Parameters)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters (on Parameters)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |

Profile lists were re-verified against every working CI build's `artifacts.html` ("Structures: Resource Profiles" section). No additional profiles were found in any other incubator IG; the five above are the complete set. (capstmt also publishes a `FeatureDefinition` logical model, which is not a profile and is not tracked here.)

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

## Discrepancies vs. the Confluence page

1. **NEW incubator discovered — `txmodule-incubator` (Terminology Infrastructure).** The repository [HL7/txmodule-incubator](https://github.com/HL7/txmodule-incubator) (internally named "tmi-ig", the Terminology Module Incubator) exists in the HL7 GitHub org (last push 2026-04-27) but is **not listed on the Confluence page**. It is currently an empty scaffold (only a `README.md`; no `ig.ini`/`sushi-config.yaml`) and has **no CI build** (build.fhir.org returns 404; absent from the CI build registry). Added as row 10 in the summary above. The Confluence page should be updated to include it.
2. **api-incubator-ig** — still **FAILED**. No build output at all (`artifacts.html` empty, no `qa.json` in the CI registry). The operation list is carried over unchanged from the Confluence page and cannot be independently re-verified until the build is fixed.
3. **ebm-incubator / Contributorship** — confirmed **still pending**: the working CI build exposes only `Citation` under "Additional Resources"; `Contributorship` is in the repo but not yet in the build. No change since the page note.
4. **pc-incubator** — still empty / **no build** (404). The GitHub repo shows a recent push (2026-05-18) but no `README`/`ig.ini`/`sushi-config.yaml` and no build output, so the "REPO CREATED (EMPTY), NO BUILD" status is unchanged. (Renumbered to row 11.)
5. **phx-incubator** — still **NOT CREATED** (no GitHub repo, no build). (Renumbered to row 12.)
6. All resource, profile and operation counts/statuses for the nine built incubators (admin, oo, cg, immunization, ebm, api, capstmt, fhir-testing, data-access-policies) **match** the Confluence page exactly. The full profile list was re-verified across every IG and is complete (5 profiles).

---

Compiled 2026-05-18 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
