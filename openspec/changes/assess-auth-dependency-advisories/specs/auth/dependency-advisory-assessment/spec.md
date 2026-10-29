# Spec Delta

## Purpose

Define verifiable expectations for assessing whether dependency advisories apply to the candidates identified by H-AUTH-07, without presuming a vulnerability from declared versions alone.

## ADDED Requirements

### Requirement: Dependency advisory assessments are evidence-based
The review MUST record, for each dependency candidate assessed under H-AUTH-07, the dependency and version, advisory references considered, the determination, and the evidence supporting that determination. The determination MUST distinguish an applicable advisory, no applicable advisory identified, and an inconclusive assessment. A version match alone MUST NOT be reported as confirmation of a vulnerability.

#### Scenario: A version candidate alone is insufficient
- **WHEN** a candidate dependency version is identified but evidence does not establish whether an advisory applies
- **THEN** the assessment records an inconclusive determination and does not report a confirmed vulnerability

#### Scenario: No applicable advisory is identified
- **WHEN** the assessment finds no applicable advisory for a reviewed candidate
- **THEN** the record identifies the dependency and version, the advisory references considered, and the evidence supporting the determination

#### Scenario: An advisory is determined to apply
- **WHEN** the assessment establishes that an advisory applies to a reviewed candidate
- **THEN** the record identifies the dependency and version, the advisory reference, and the evidence supporting applicability

#### Scenario: Assessment remains inconclusive
- **WHEN** the available evidence is insufficient to determine applicability
- **THEN** the result remains inconclusive and H-AUTH-07 is not marked resolved
