# Proposal

## Why

H-AUTH-07 is marked `Confirmado` as a need to evaluate dependency advisories. It identifies Quarkus 3.27.4 and Commons Text 1.9 as candidates for review, but explicitly does not confirm a CVE or its applicability. The change defines the evidence needed to make and record that determination.

## What Changes

- Specify an evidence-based advisory applicability assessment for the dependency candidates identified by H-AUTH-07.
- Distinguish an applicable advisory, no applicable advisory identified, and an inconclusive assessment.

## Scope

- Assess the Quarkus 3.27.4 and Commons Text 1.9 candidates named in H-AUTH-07.
- Record the dependency and version, advisory references considered, determination, and supporting evidence.

## Out of Scope

- Claiming that a CVE exists or applies before evidence supports that conclusion.
- Selecting assessment tools or prescribing dependency updates, mitigations, or other remediation.
- Changes to application behavior or dependency configuration.

## Verifiable Outcomes

- Each reviewed candidate has a recorded determination and supporting evidence.
- Version information alone is not represented as a confirmed vulnerability.
- An inconclusive assessment does not mark H-AUTH-07 resolved.
