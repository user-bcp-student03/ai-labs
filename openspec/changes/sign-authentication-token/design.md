# Design

## Context

See [proposal.md](proposal.md) for the motivation and [specs/auth/signed-token/spec.md](specs/auth/signed-token/spec.md) for the normative behavior.

In `auth/`, `LoginService` creates the successful response and delegates token creation to `LegacyTokenUtil`. The current token is Base64 of `username|role|expiry|secret`; it is not signed and there is no token validation path or bearer-authenticated route in this component. `LoginResponse` exposes `token`, `expiresIn`, `user`, and `debug`. The configured production secret is provided through `AUTH_TOKEN_SECRET`; development and test profiles use separate local values. The project is Quarkus on Java 21 and currently has no JWT dependency.

The documented login response contract is treated as protected except for the opaque `token` value, whose unsigned Base64 format is intentionally replaced by Issue #24. The exact legacy-token transition remains a release gate because no consumer inventory is in scope here.

## Goals / Non-Goals

**Goals:**

- Issue signed tokens with verifiable integrity and explicit `sub` (username), `role`, and `exp` claims.
- Validate signature, expected algorithm, required claims, and expiration at a deterministic validation boundary.
- Keep token validation internal to `auth/`; do not add a route or change authorization behavior of other endpoints.
- Preserve the documented successful login response fields, types, existing non-token values, and error responses.
- Make the legacy-token decision explicit and tested before deployment.

**Non-Goals:**

- Adding bearer authentication to `/auth/login`, `/q/health`, or other endpoints.
- Changing the login request, user persistence, password verification, CORS, or response DTO shape.
- Updating `HALLAZGOS.md` to mark H-AUTH-03 resolved before the tests and compatibility decision are complete.
- Changing `api/`, `web/`, or any consumer outside `auth/`.

## Contratos Protegidos

- `POST /auth/login` remains unauthenticated, consumes the documented JSON request, and returns HTTP 200 on valid credentials.
- Success continues to include `token` as a string, `expiresIn` as an integer in seconds, `user.username`, `user.displayName`, `user.role`, and `debug` with value `authenticated-with-legacy-flow`. The current lifetime remains 3600 seconds unless a separate contract change is approved; the token's `exp` must agree with `expiresIn`.
- Documented login error status codes and JSON `message` bodies remain unchanged.
- The content of `token` is opaque to the response contract. Its legacy Base64 encoding is the intentional contract change; clients must not rely on decoding that representation.

## Componentes Afectados

- `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java`: replace the unsigned token creation responsibility with signed issuance and validation behind a focused auth-owned boundary.
- `auth/src/main/java/com/bcp/workshop/auth/LoginService.java`: continue to create the same response DTO while using the signed issuer and keeping `expiresIn` consistent with token expiration.
- `auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java`: no shape change is expected.
- `auth/src/main/resources/application.properties`: retain external secret configuration; strengthen validation of key suitability and keep profile keys isolated. No real secret is committed.
- `auth/pom.xml`: add the selected JOSE/JWT library and subject it to the existing dependency and security checks.
- Auth tests only: characterization and token issuance/validation coverage described below.

## Decisions

### Signed token format and cryptographic library

Use a compact signed JWT (JWS) with HS256, implemented with Nimbus JOSE+JWT rather than custom cryptographic framing. The existing externalized secret is a natural boundary for a single auth service that issues and validates its own tokens; the key must contain at least 256 bits of unpredictable material. Production obtains it through `AUTH_TOKEN_SECRET`; dev/test use separate synthetic keys that meet the same length requirement.

Alternatives considered:

- Hand-built HMAC and Base64 using JCA: avoids a dependency, but requires custom parsing, canonicalization, algorithm handling, and claim validation. The security-sensitive parsing risk outweighs the dependency cost.
- Quarkus SmallRye JWT bearer integration: provides framework-level request authentication, but this change has no in-scope bearer-protected route. Integrating it now would widen the change and risk altering endpoint behavior.
- Asymmetric signing: separates signing and verification keys and is preferable for independent services, but the inspected `auth/` scope has no independent verifier or key-distribution requirement. Revisit if that deployment boundary appears.

### Claims and validation boundary

Use `sub` for the authenticated username, a `role` claim for the user's role, and standard `exp` for expiration. The validator must pin HS256 rather than trust an algorithm named by an untrusted token, reject missing or mismatched identity/role claims, reject invalid signatures, and reject tokens at or after `exp`. Keep the validator internal; do not add an HTTP endpoint or security filter in this change. Use an injectable clock or equivalent deterministic time source so expiration tests do not depend on sleeps or wall-clock races.

### Legacy-token transition

Do not silently treat an unsigned legacy token as valid. Before implementation is considered deployable, the owner must select and document one policy after confirming consumer expectations: (a) a coordinated cutover that rejects all legacy tokens, or (b) a bounded transition that accepts legacy tokens only until an explicit end time and tests both formats. No consumer inventory outside `auth/` is assumed or performed under this component boundary. This choice is a release blocker, not implicit backward compatibility.

## Estrategia Incremental

1. **Caracterizar antes del cambio:** strengthen the auth HTTP test for a successful login to assert the complete response shape, field types, `expiresIn`, user values, and `debug`, while treating `token` as opaque. Retain characterization of the documented invalid-request and invalid-credential status/body behavior. Do not pin the old token bytes or mistake Base64 decoding for a security guarantee.
2. **Specify validator behavior in unit tests:** cover a valid token, altered signature/content, wrong key or algorithm, missing identity/role, mismatched role, and expiration immediately before and at/after the boundary using deterministic time.
3. **Introduce signed issuance and validation:** add the vetted library, validate key strength/configuration, and pass the same lifetime source to token generation and `expiresIn`. Keep DTO and route contracts unchanged.
4. **Verify the HTTP boundary:** test that login's token validates to the response user's identity and role, its expiration agrees with `expiresIn`, and the existing response/error contract remains intact.
5. **Resolve and verify migration policy:** record the legacy-token policy, add tests for the selected cutover or bounded transition, and confirm consumers are prepared before release. Only then may H-AUTH-03 be marked resolved, after all required tests pass.

## Pruebas de Caracterización

Before changing token issuance or response behavior, auth tests must establish:

- Valid login returns HTTP 200 with exactly the documented field names and types, including `user.displayName` and the existing `debug` value; token content is asserted only to be a non-empty string.
- Missing/blank request fields, invalid credentials, and the documented generic server error retain their HTTP status and JSON message behavior.
- The current lifetime exposed by `expiresIn` is 3600 seconds. The characterization does not assert that the current unsigned token is secure or that any external consumer accepts it.

After the change, add focused token tests for valid signature, tampering, expiration, required claims, key/algorithm rejection, and the agreed legacy policy. Keep all tests and coverage scope within `auth/`.

## Risks / Trade-offs

- **Shared-secret compromise or weak key** → require a high-entropy key of at least 256 bits, keep production values external, isolate test/dev values, and never log or commit keys or complete tokens.
- **Legacy consumers decode the old value** → make consumer confirmation and a tested cutover/finite transition a release blocker; do not add indefinite fallback.
- **HS256 requires every verifier to hold the signing secret** → acceptable for the inspected single auth boundary; move to asymmetric keys if verification is later distributed.
- **JWT dependency adds supply-chain and maintenance surface** → pin a compatible version, run existing dependency/security checks, and do not suppress findings.
- **Clock differences can cause boundary surprises** → define `exp` semantics as reject at or after expiry and use deterministic time in tests; any production clock-skew allowance requires a separately specified security decision.
- **The service currently has no protected route using the validator** → validation can be unit/integration tested inside `auth/`, but adding authorization to consumers remains out of scope and must be planned separately.

## Plan de Migración y Rollback

Deploy only after the legacy policy and consumer readiness are documented and tested. Keep the existing response contract and production secret source; provision a sufficiently strong signing key through the existing environment-based configuration before enabling signed issuance. The configured key must be recoverable for rollback operations without storing it in this repository.

If the release is rolled back after signed tokens have been issued, the previous version cannot validate or interpret the new format as a legacy token. Stop issuing the new format, restore the previous application/configuration, and require affected sessions to authenticate again; with the current 3600-second lifetime, outstanding signed tokens expire within at most one hour. Do not restore an unsigned-token fallback as an emergency workaround. If a bounded legacy transition is selected, rollback must also honor its documented end time and tests.

## Open Questions

- Which legacy-token policy do consumers require: coordinated rejection at cutover or a bounded dual-format transition? This must be answered before implementation planning is considered deployable; the proposal/spec already require the decision and tests before release.