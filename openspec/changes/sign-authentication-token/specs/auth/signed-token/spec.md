# Spec Delta

## Purpose

Define the observable contract for issuing and validating authentication tokens with verifiable integrity, required identity and role claims, and expiration, while preserving the documented login response contract.

## ADDED Requirements

### Requirement: Authentication tokens have verifiable integrity
The system MUST issue authentication tokens whose authenticity and integrity can be validated, and MUST reject tokens whose protected content has been altered.

#### Scenario: An unaltered token is accepted
- **WHEN** a token issued for a successful login is validated before its expiration with its required claims intact
- **THEN** validation accepts the token and confirms the authenticated identity and role

#### Scenario: Altered token is rejected
- **WHEN** any protected part of an issued token is changed before validation
- **THEN** validation rejects the token

### Requirement: Authentication tokens contain required identity and role claims
The system MUST validate the identity and role claims required to represent the authenticated user, and MUST reject tokens with either claim missing or inconsistent with the authenticated user.

#### Scenario: Required identity claim is missing
- **WHEN** a token presented for validation has no identity claim
- **THEN** validation rejects the token

#### Scenario: Required role claim is missing or incorrect
- **WHEN** a token presented for validation has no role claim or a role claim that does not match the authenticated user's role
- **THEN** validation rejects the token

### Requirement: Authentication token expiration is enforced
The system MUST include a verifiable expiration for each issued token, MUST report its lifetime through the documented `expiresIn` response field, and MUST reject the token at or after its expiration.

#### Scenario: Token remains valid before expiration
- **WHEN** a token with valid integrity and required claims is validated before its expiration
- **THEN** validation accepts it

#### Scenario: Expired token is rejected
- **WHEN** a token is validated at or after its expiration
- **THEN** validation rejects it

#### Scenario: Login reports the issued token lifetime
- **WHEN** login succeeds
- **THEN** `expiresIn` reports the same lifetime used by the issued token and remains an integer in seconds

### Requirement: Successful login preserves the documented response contract
The system MUST preserve the documented successful `POST /auth/login` status, response fields, field types, and non-token values; the `token` value remains a string but no longer uses the unsigned Base64 legacy format.

#### Scenario: Successful login returns the documented fields
- **WHEN** a user submits valid login credentials
- **THEN** the response is HTTP 200 and contains `token` as a string, `expiresIn` as an integer, `user.username`, `user.displayName`, `user.role`, and `debug` with value `authenticated-with-legacy-flow`

#### Scenario: Invalid login behavior is preserved
- **WHEN** a user submits credentials rejected by the documented login contract
- **THEN** the response retains the documented HTTP status and JSON `message` for that error

### Requirement: Legacy token transition is decided before release
Before deployment, the system's behavior for previously issued legacy tokens MUST be explicitly decided, documented, and covered by tests; no compatibility behavior is assumed by this change.

#### Scenario: Legacy-token transition is not yet agreed
- **WHEN** the release is prepared and the legacy-token behavior has not been decided, documented, and tested
- **THEN** the change is not considered ready for deployment

#### Scenario: Legacy-token transition is agreed
- **WHEN** a release is prepared after the legacy-token behavior has been decided
- **THEN** the documented decision and tests establish whether legacy tokens are accepted during a defined transition or rejected from a defined cutover