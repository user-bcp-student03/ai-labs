# Spec Delta

## Purpose

Defines the externally observable login behavior for authenticating users and rejecting invalid or crafted credentials without changing the existing login endpoint contract.

## ADDED Requirements

### Requirement: Successful login preserves the login response contract
The system MUST continue to authenticate valid credentials through `POST /auth/login` and return a successful response containing the authenticated username and a token.

#### Scenario: Login with valid workshop credentials
- **WHEN** a client submits username `student` and password `Workshop123!` to `POST /auth/login`
- **THEN** the system returns HTTP 200 with `user.username` equal to `student` and a non-empty `token`

### Requirement: Invalid credentials cannot authenticate a user
The system MUST reject invalid credentials and MUST NOT issue a token when submitted credential text attempts to change the meaning of authentication.

#### Scenario: Injection-like username is rejected
- **WHEN** a client submits `' OR '1'='1` as the username to `POST /auth/login`
- **THEN** the system returns HTTP 401 and the response does not contain a `token`

#### Scenario: Injection-like password is rejected
- **WHEN** a client submits `' OR '1'='1` as the password to `POST /auth/login`
- **THEN** the system returns HTTP 401 and the response does not contain a `token`

### Requirement: Missing or blank credentials are rejected
The system MUST reject login requests that omit a required credential or provide it as blank input.

#### Scenario: Username is blank
- **WHEN** a client submits a blank username to `POST /auth/login`
- **THEN** the system returns HTTP 400

#### Scenario: Password is blank
- **WHEN** a client submits a blank password to `POST /auth/login`
- **THEN** the system returns HTTP 400
