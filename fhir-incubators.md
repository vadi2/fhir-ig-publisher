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
| 6 | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | FHIR Infrastructure | 1 | 12 | BUILD MISSING |
| 7 | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | FHIR Infrastructure | 0 | 4 | OK |
| 8 | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | FHIR Infrastructure | 3 | 0 | OK |
| 9 | [data-access-policies](https://build.fhir.org/ig/HL7/data-access-policies/) | Security | 1 | 0 | OK |
| 10 | [txmodule-incubator](https://build.fhir.org/ig/HL7/txmodule-incubator/) | Terminology Infrastructure | 0 | 2 | OK |
| 11 | [pc-incubator](https://github.com/HL7/pc-incubator) | Patient Care | 3 (planned) | 0 | REPO CREATED (EMPTY), NO BUILD |
| 12 | [phx-incubator](https://github.com/HL7/phx-incubator) | Pharmacy | 1 (planned) | 0 | NOT CREATED |
| 13 | [sample-incubator-ig](https://build.fhir.org/ig/HL7/sample-incubator-ig/) † | (Template/Example) | 0 | 0 | OK (TEMPLATE ONLY) |

† **New finding** — `sample-incubator-ig` was discovered in the HL7 org with a working CI build, but it is the unmodified PHER template (no real resources or operations). Not listed on the current Confluence page.

---

## All incubated resources

| Resource | Incubator IG | Status |
|----------|--------------|--------|
| [BiologicallyDerivedProductDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-BiologicallyDerivedProductDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [ChargeItem](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItem.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [ChargeItemDefinition](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-ChargeItemDefinition.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| [Citation](https://build.fhir.org/ig/HL7/ebm-incubator/StructureDefinition-Citation.html) | [ebm-incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | OK |
| ClinicalAssessment | [pc-incubator](https://github.com/HL7/pc-incubator) | NOT BUILT (repo empty) |
| ConditionDefinition | [pc-incubator](https://github.com/HL7/pc-incubator) | NOT BUILT (repo empty) |
| ~~Contributorship~~ \* | ~~ebm-incubator~~ | **REMOVED** — was pending, deleted from repo 2026-05-19 |
| [DeviceDispense](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceDispense.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [DeviceUsage](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-DeviceUsage.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [EncounterHistory](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-EncounterHistory.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK |
| FormularyItem | [phx-incubator](https://github.com/HL7/phx-incubator) | NOT CREATED |
| [GenomicStudy](https://build.fhir.org/ig/HL7/cg-incubator/StructureDefinition-GenomicStudy.html) | [cg-incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | OK |
| GraphDefinition | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| [ImmunizationEvaluation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationEvaluation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [ImmunizationRecommendation](https://build.fhir.org/ig/HL7/immunization-incubator/StructureDefinition-ImmunizationRecommendation.html) | [immunization-incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | OK |
| [InsurancePlan](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsurancePlan.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK \*\*\* |
| [InsuranceProduct](https://build.fhir.org/ig/HL7/admin-incubator/StructureDefinition-InsuranceProduct.html) | [admin-incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | OK \*\*\* |
| [InventoryItem](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryItem.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [InventoryReport](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-InventoryReport.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| Invoice \*\* | TBD | FLAGGED FOR REMOVAL |
| Linkage | [pc-incubator](https://github.com/HL7/pc-incubator) | NOT BUILT (repo empty) |
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

> \* **Contributorship**: Was listed as pending on the previous Confluence page (added to the ebm-incubator repo on 2026-04-02). On 2026-05-19 the repo received a `removal of Contributorship Additional Resource` commit by brianalperMD — the StructureDefinition and search-params bundle were deleted. No incubator destination has been assigned. **Status change since the Confluence page.**
>
> \*\* **Invoice** is flagged in the R6 ballot4 ballot-intro as "likely to be removed" from core post-ballot, alongside InsurancePlan and InsuranceProduct. No incubator destination has been assigned yet.
>
> \*\*\* **InsurancePlan** and **InsuranceProduct** are also still present in the R6 core spec for now; they have been added to admin-incubator preemptively.

---

## All incubated profiles

| Profile | Incubator IG | Status |
|---------|--------------|--------|
| [FeatureCapabilityStatement (on CapabilityStatement)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureCapabilityStatement.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryInputParameters (on Parameters)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryInputParameters.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [FeatureQueryOutputParameters (on Parameters)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureQueryOutputParameters.html) | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [feature (extension on Extension)](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-feature.html) † | [capstmt](https://build.fhir.org/ig/HL7/capstmt/) | OK |
| [medicalproductofhumanorigin (on BiologicallyDerivedProduct)](https://build.fhir.org/ig/HL7/oo-incubator/StructureDefinition-medicalproductofhumanorigin.html) | [oo-incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | OK |
| [shareabletestscript (on TestScript)](https://build.fhir.org/ig/HL7/fhir-testing-ig/StructureDefinition-shareabletestscript.html) | [fhir-testing-ig](https://build.fhir.org/ig/HL7/fhir-testing-ig/) | OK |

† **New finding** — the `feature` extension in capstmt is not listed on the Confluence page.

Additionally, capstmt also publishes [FeatureDefinition](https://build.fhir.org/ig/HL7/capstmt/StructureDefinition-FeatureDefinition.html), a **logical model** (`kind=logical`, derivation `specialization` from DomainResource) — neither a resource nor a profile. It is new since the Confluence page and was not previously listed.

---

## All incubated operations

| Operation | Incubator IG | Status |
|-----------|--------------|--------|
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
| DocumentReference $generate | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| SpecimenDefinition $apply | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Resource $graph | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Resource $meta | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Resource $meta-add | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Resource $meta-delete | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Resource $large-resource-add | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Resource $large-resource-filter | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Resource $large-resource-remove | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| List $find | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Asynchronous Bulk Data Request | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |
| Asynchronous Bundle Request | [api-incubator-ig](https://github.com/HL7/api-incubator-ig) | BUILD MISSING |

---

## Discrepancies vs. the current Confluence page (compiled 2026-05-19)

1. **Contributorship removed from ebm-incubator.** Confluence shows it as "PENDING — in repo, not yet in build". On 2026-05-19 the ebm-incubator repo received a `removal of Contributorship Additional Resource` commit by brianalperMD; the StructureDefinition and search-params bundle were deleted. The "(+1 pending)" annotation on ebm-incubator's resource count should be dropped.
2. **api-incubator-ig build is missing, not just failed.** Confluence reports "BUILD FAILED". In fact `https://build.fhir.org/ig/HL7/api-incubator-ig/` returns no qa.html, no IG output, and no branches under `/branches/` — the CI build is no longer producing any artifacts at all. (Last repo commit: 2025-11-05.) The repo still contains the GraphDefinition resource and four OperationDefinition XMLs at `input/resources/` (List-find, Resource-large-resource-{add,filter,remove}). The other eight operations claimed on Confluence (DocumentReference $generate, SpecimenDefinition $apply, Resource $graph/$meta/$meta-add/$meta-delete, Asynchronous Bulk Data, Asynchronous Bundle) are not visible as standalone files in the repo top level — they may be defined as FSH or in nested folders.
3. **capstmt has additional artifacts not listed on Confluence.**
   - `StructureDefinition-feature` — an extension on `Extension` (derivation=constraint, kind=complex-type). Should be added to the profiles section.
   - `StructureDefinition-FeatureDefinition` — a logical model (kind=logical). Neither a resource nor a profile.
4. **New repo: `sample-incubator-ig`.** Exists in the HL7 org with a working CI build but is the unmodified PHER template — no resources, profiles, or operations. Should be tracked as an example/template, not as a real incubator.
5. **All other listings match.** Resource, profile, and operation counts for admin-incubator (7/5), oo-incubator (8 + 1 profile), cg-incubator (2), immunization-incubator (2), fhir-testing-ig (3 + 1 profile), data-access-policies (1), txmodule-incubator (2 ops), and capstmt (4 ops + 3 documented profiles) all match the live CI build outputs exactly.

---

## Notes for the FHIR Management Group (R6 transition)

- **Still in R6 core, awaiting final incubator destination:**
  - **Invoice** — flagged in the R6 ballot4 ballot-intro as "likely to be removed" from core post-ballot. No incubator owner identified. FMG action: assign owner WG (most plausibly Patient Administration, alongside InsurancePlan/InsuranceProduct).
  - **InsurancePlan**, **InsuranceProduct** — present in both R6 core and admin-incubator. The dual-listing should be resolved at R6 ballot4 disposition: either remove from core (preferred per the ballot intro) or remove from admin-incubator.
- **Removed from build with no incubator home:**
  - **Contributorship** — removed from ebm-incubator on 2026-05-19. If Clinical Decision Support / EBMonFHIR still wants this resource, FMG should confirm the deprioritisation or identify a new owner; otherwise drop it from any "incubated" tracking entirely.
- **Repo created but build never run:**
  - **pc-incubator** — empty repo (description updated 2026-05-18 but still 0 commits on `main`). FMG should chase Patient Care to seed it with ClinicalAssessment, ConditionDefinition, and Linkage before R6 publication, or these will fall through the cracks.
- **No repo yet:**
  - **phx-incubator** — does not exist (HTTP 404). FMG should ask Pharmacy WG to create the repo so FormularyItem has a published home before it is removed from core.
- **Build broken for an entire IG:**
  - **api-incubator-ig** — last commit 2025-11-05; CI build produces no output. The IG is intended to hold FHIR-I's R6 operations (`$meta`, `$graph`, large-resource ops, Async Bulk/Bundle Requests, etc.) plus the GraphDefinition resource. FMG should escalate to FHIR-I to fix the build so the operations can be reviewed before R6 ballot4 disposition; otherwise these 12 operations have no published home.
- **Sample/template repo discovered:**
  - **sample-incubator-ig** — appears in the HL7 org and builds successfully but is the unmodified PHER template (0 resources, 0 operations). FMG should mark it clearly as a template so it is not mistaken for a real incubator going forward, or rename it (e.g. `incubator-template`) to make its purpose obvious.
- **Untracked capstmt artifacts:**
  - `feature` (extension) and `FeatureDefinition` (logical model) are now published by capstmt but were not listed on Confluence. FMG should confirm these belong to the incubator scope and update tracking.

---

Compiled 2026-05-26 from [build.fhir.org](https://build.fhir.org) CI builds and [HL7 GitHub](https://github.com/HL7).
