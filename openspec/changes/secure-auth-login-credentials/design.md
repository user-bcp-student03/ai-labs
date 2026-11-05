# Design

## Context

See `proposal.md` for motivation and `specs/auth-login/spec.md` for the required observable behavior. In `auth/`, `LegacyDatabase.findByCredentials` currently combines submitted credentials into a SQL string and executes it with a `Statement`. `AuthResource` maps a missing user to HTTP 401, returns HTTP 200 with `user.username` and a token for a match, and rejects blank credentials with HTTP 400. The existing `AuthResourceTest` covers only a successful login. The requested `docs/ENDPOINTS.md` baseline is not present, so the Issue #23 contract and current `auth/` behavior are the available references.

### Contracts protected

- Keep `POST /auth/login` and its successful response contract: HTTP 200, the authenticated username, and a token.
- Keep blank required credentials rejected with HTTP 400.
- Ensure injection-like input in either credential is rejected with HTTP 401 and no token.
- Keep all changes and tests within `auth/`.

### Components affected

- `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`: credential lookup.
- `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`: behavior is protected; no endpoint or response redesign is intended.
- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: characterization and security regression coverage.

## Goals / Non-Goals

**Goals:**

- Prevent credential input from changing the meaning of authentication.
- Preserve the documented-by-Issue login outcomes and current blank-input behavior.
- Establish characterization coverage before changing the credential lookup behavior.

**Non-Goals:**

- Changes to `api/`, `web/`, other endpoints, account provisioning, database schema, token format or lifetime.
- A broader authentication-layer redesign or unrelated error-handling changes.

## Decisions

1. **Treat both credentials strictly as values in the lookup.** Use a fixed credential lookup with bound parameters via JDBC `PreparedStatement`. This keeps the existing exact-credential matching behavior while preventing input from becoming part of the query structure. Escaping or filtering input was rejected because it is fragile and changes which legitimate values can be accepted. Replacing JDBC or introducing an ORM was rejected as unnecessary scope and dependency churn.
2. **Preserve the endpoint boundary and response mapping.** Keep the existing route and status/response behavior; the security correction belongs in credential verification. This avoids changing client-visible login behavior beyond rejecting the bypass that Issue #23 identifies.
3. **Separate characterization from the security regression.** Before changing lookup behavior, extend `AuthResourceTest` to characterize successful login, ordinary invalid credentials, and blank username/password responses. Then add assertions for the injection-like value in username and password: each request must return 401 and omit `token`. The security assertions may fail against the vulnerable baseline; they become the regression checks for the correction. Do not treat the current bypass behavior as behavior to preserve.
4. **Validate only the selected component.** Run the auth tests with `mvn test` from `auth/`; do not modify or test `api/` or `web/` for this change.

## Risks / Trade-offs

- **A test may pass against a shared or unexpected database state** → use the existing isolated Quarkus test setup and assert response status and token presence/absence rather than relying on row ordering.
- **Changing lookup semantics could reject legitimate values** → bind the original username and password as supplied; do not normalize, escape, or filter them as part of this change.
- **Rollback would restore the authentication bypass** → there is no data migration to reverse. If a release must be rolled back for a separate regression, treat the security exposure as an incident and deploy a corrected replacement promptly rather than considering the vulnerable version an acceptable steady state.

## Migration Plan

1. Add and run characterization tests for the existing successful-login response, ordinary invalid credentials, and blank inputs.
2. Add the injection regression cases for both username and password and confirm they express the Issue #23 outcome.
3. Correct credential lookup in `auth/` while keeping the route and response behavior unchanged.
4. Run `mvn test` from `auth/` and review that no files outside `auth/` were changed.

No schema or data migration is required. Rollback is a code deployment rollback only and must account for the security risk described above.
