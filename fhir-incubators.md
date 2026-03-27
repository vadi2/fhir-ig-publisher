# FHIR Incubators Overview

**Report Date:** 2026-03-27

> **Note:** The Confluence page (pageId 441128732) could not be accessed due to AWS WAF CAPTCHA
> protection. This report is built entirely from live GitHub and build.fhir.org data.

## Summary Table

| IG Name | Work Group | Resources | Operations | Build Status | Last Built | GitHub |
|---------|-----------|-----------|------------|-------------|------------|--------|
| [EBM Incubator](https://build.fhir.org/ig/HL7/ebm-incubator/) | Clinical Decision Support | 1 | 0 | Built (47 errors, 113 warnings) | 2026-03-27 | [HL7/ebm-incubator](https://github.com/HL7/ebm-incubator) |
| [CG Incubator](https://build.fhir.org/ig/HL7/cg-incubator/) | Clinical Genomics | 2 | 0 | Built (7 errors, 31 warnings) | 2026-03-24 | [HL7/cg-incubator](https://github.com/HL7/cg-incubator) |
| [Immunization Incubator](https://build.fhir.org/ig/HL7/immunization-incubator/) | Public Health | 2 | 0 | Built (10 errors, 30 warnings) | 2026-02-24 | [HL7/immunization-incubator](https://github.com/HL7/immunization-incubator) |
| [OO Incubator](https://build.fhir.org/ig/HL7/oo-incubator/) | Orders and Observations | 8 | 0 | Built (111 errors, 179 warnings) | 2025-11-26 | [HL7/oo-incubator](https://github.com/HL7/oo-incubator) |
| [Admin Incubator](https://build.fhir.org/ig/HL7/admin-incubator/) | Patient Administration | 7 | 5 | Built (387 errors, 159 warnings) | 2025-11-08 | [HL7/admin-incubator](https://github.com/HL7/admin-incubator) |
| [API Incubator](https://build.fhir.org/ig/HL7/api-incubator-ig/) | FHIR Infrastructure | 0 | 0 | **FAILED** | 2025-11-05 | [HL7/api-incubator-ig](https://github.com/HL7/api-incubator-ig) |
| [Sample Incubator](https://build.fhir.org/ig/HL7/sample-incubator-ig/) | Public Health | 0 | 0 | Built (5 errors, 1 warning) | 2025-11-09 | [HL7/sample-incubator-ig](https://github.com/HL7/sample-incubator-ig) |

**Totals:** 7 incubator IGs, 20 incubated resources, 5 incubated operations

## Incubated Resources

| Resource | Incubator IG | Work Group |
|----------|-------------|-----------|
| Citation | EBM Incubator | Clinical Decision Support |
| GenomicStudy | CG Incubator | Clinical Genomics |
| MolecularDefinition | CG Incubator | Clinical Genomics |
| ImmunizationEvaluation | Immunization Incubator | Public Health |
| ImmunizationRecommendation | Immunization Incubator | Public Health |
| BiologicallyDerivedProductDispense | OO Incubator | Orders and Observations |
| DeviceDispense | OO Incubator | Orders and Observations |
| DeviceUsage | OO Incubator | Orders and Observations |
| InventoryItem | OO Incubator | Orders and Observations |
| InventoryReport | OO Incubator | Orders and Observations |
| SupplyDelivery | OO Incubator | Orders and Observations |
| SupplyRequest | OO Incubator | Orders and Observations |
| Transport | OO Incubator | Orders and Observations |
| ChargeItem | Admin Incubator | Patient Administration |
| ChargeItemDefinition | Admin Incubator | Patient Administration |
| EncounterHistory | Admin Incubator | Patient Administration |
| InsurancePlan | Admin Incubator | Patient Administration |
| InsuranceProduct | Admin Incubator | Patient Administration |
| PersonalRelationship | Admin Incubator | Patient Administration |
| VerificationResult | Admin Incubator | Patient Administration |

## Incubated Operations

| Operation | Incubator IG | Work Group |
|-----------|-------------|-----------|
| ChargeItemDefinition/$apply | Admin Incubator | Patient Administration |
| Encounter/$everything | Admin Incubator | Patient Administration |
| EpisodeOfCare/$everything | Admin Incubator | Patient Administration |
| Patient/$everything | Admin Incubator | Patient Administration |
| Patient/$merge | Admin Incubator | Patient Administration |

## Build Failure Details

### api-incubator-ig (FAILED)

The build failed with a `NoSuchFileException`:

```
java.lang.Exception: Error Parsing File ImplementationGuide-hl7.fhir.uv.symptoms.json
Caused by: java.nio.file.NoSuchFileException: /scratch/repo/fsh-generated/resources/ImplementationGuide-hl7.fhir.uv.symptoms.json
```

The IG references a generated file (`hl7.fhir.uv.symptoms`) that does not exist in the repo. This suggests a misconfigured `sushi-config.yaml` or missing FSH source.

## Discrepancies & Notes

1. **Confluence page inaccessible:** The Confluence REST API at `confluence.hl7.org` is behind an AWS WAF CAPTCHA challenge, preventing automated access. The Confluence search API is similarly blocked. Therefore, no direct comparison with the Confluence page content is possible.

2. **api-incubator-ig build broken:** The build has been failing since at least 2025-11-05. The IG references `hl7.fhir.uv.symptoms` but the repo is `api-incubator-ig`, suggesting it may have been repurposed or misconfigured.

3. **sample-incubator-ig:** This appears to be a template/example IG with no incubated resources or operations. It serves as documentation for how to set up an incubator IG.

4. **ebm-incubator active today:** The EBM incubator was rebuilt today (2026-03-27) with version 1.0.0-ballot, suggesting it is preparing for ballot. This is the most actively developed incubator currently.

5. **Stale IGs:** The OO, Admin, API, and Sample incubator IGs have not been updated since November 2025 (~4 months ago).
