# Tasks

## 1. Characterize the auth contract

- [ ] 1.1 Extend the successful-login HTTP characterization to assert HTTP 200, the documented response fields and types, `expiresIn` of 3600 seconds, all `user` values, and `debug`; assert only that `token` is a non-empty string, not its encoding. Verify with `mvn -Dtest=AuthResourceTest test` from `auth/` before changing token behavior.
- [ ] 1.2 Preserve characterization of documented 400, 401, and generic 500 login errors, adding assertions only for uncovered contract details. Verify those cases with `mvn -Dtest=AuthResourceTest test` from `auth/` while the legacy implementation is still active.

## 2. Prepare and implement signed token behavior

- [ ] 2.1 Record the user-confirmed coordinated cutover policy in `design.md`: after consumer readiness is confirmed, legacy Base64 tokens are rejected; remove the now-resolved open question. Verify the decision is explicit and `openspec validate sign-authentication-token --strict` passes from the repository root.
- [ ] 2.2 Add Nimbus JOSE+JWT to `auth/` and enforce suitable signing-key configuration while keeping production secrets external and dev/test values isolated. Verify dependency resolution, configuration tests, and existing auth tests from `auth/` without committing real secrets.
- [ ] 2.3 Implement signed HS256 issuance with `sub`, `role`, and `exp` claims. Add focused tests that a newly issued token verifies and carries the authenticated username and role; verify the focused token test suite from `auth/`.
- [ ] 2.4 Implement validation with the expected algorithm pinned, required identity/role claims checked, and expiration rejected at or after `exp`, using deterministic time in tests. Cover tampering, wrong key/algorithm, missing or mismatched claims, and the expiration boundary; verify the focused token test suite from `auth/`.

## 3. Integrate and complete the coordinated cutover

- [ ] 3.1 Integrate signed issuance into the login service without changing the response DTO or documented error behavior. Verify through auth tests that the issued token matches the response user's identity and role and that its `exp` agrees with `expiresIn`; run `mvn -Dtest=LoginServiceTest,AuthResourceTest test` from `auth/`.
- [ ] 3.2 Add a regression test that a legacy unsigned Base64 token is rejected after the coordinated cutover; verify the test passes without introducing an indefinite legacy fallback, using the auth token test suite.
- [ ] 3.3 Update only the auth section of `docs/ENDPOINTS.md` to describe the signed token's opaque response value, required claims, and enforced expiration; preserve the API and web sections. Verify the documentation matches the implemented behavior and inspect the diff to confirm those other sections are unchanged.
- [ ] 3.4 Confirm consumer readiness and the coordinated cutover before deployment; verify the cutover decision and its rejection test are recorded, and do not change consumer code outside `auth/` in this Issue.

## 4. Verify and close H-AUTH-03

- [ ] 4.1 Run `mvn verify` from `auth/`; confirm the modified-code JaCoCo coverage target is at least 85% and existing security/dependency checks remain enabled with no suppressed findings.
- [ ] 4.2 After all token, response-contract, coverage, and cutover checks pass, update only H-AUTH-03 in `HALLAZGOS.md` to Resuelto. Verify its evidence supports resolution and the API/web finding sections remain unchanged.