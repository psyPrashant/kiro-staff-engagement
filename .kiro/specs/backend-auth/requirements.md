# Requirements Document

## Introduction

Backend authentication for the Staff Engagement POC. This feature adds minimal session-based authentication using Spring Security so that a single seeded user can log in, all protected API endpoints reject anonymous access, and the currently authenticated user is resolvable server-side for attributing interactions. No roles or complex authorization are needed at this stage.

## Glossary

- **Auth_Module**: The Spring Security authentication module within the `com.psybergate.staff_engagement.auth` package responsible for login, session management, and current-user resolution.
- **User_Entity**: The JPA entity mapped to the `users` table, extended with a `password_hash` column for credential storage.
- **Login_Endpoint**: The HTTP POST endpoint at `/api/auth/login` that accepts credentials and initiates an authenticated session.
- **Security_Filter_Chain**: The Spring Security filter chain configuration that enforces access rules on HTTP requests.
- **Password_Encoder**: The BCrypt-based password encoder used for hashing and verifying passwords.
- **Current_User_Resolver**: The mechanism (e.g., `SecurityContextHolder` or custom argument resolver) that provides the authenticated User_Entity to application code.
- **Protected_Endpoint**: Any endpoint matching `/api/**` that requires an authenticated session, excluding the Login_Endpoint and actuator health endpoint.
- **Session**: The server-side HTTP session used to maintain authentication state between requests.

## Requirements

### Requirement 1: Extend User Entity with Password Hash

**User Story:** As a developer, I want the User entity to include a hashed password field, so that credentials can be stored securely in the database.

#### Acceptance Criteria

1. THE Flyway_Migration (V4) SHALL add a `password_hash` column of type `VARCHAR(255)` to the existing `users` table using an `ALTER TABLE` statement.
2. THE `password_hash` column SHALL be nullable to allow backward compatibility with existing seed data during migration.
3. THE User_Entity SHALL include a `passwordHash` field of type `String` mapped to the `password_hash` column.
4. THE User_Entity SHALL exclude the `passwordHash` field from JSON serialization so that password hashes are never returned in API responses.

### Requirement 2: BCrypt Password Hashing

**User Story:** As a developer, I want passwords to be hashed with BCrypt, so that stored credentials are resistant to brute-force attacks.

#### Acceptance Criteria

1. THE Password_Encoder SHALL be exposed as a Spring bean that uses BCrypt with a strength (cost factor) of 10 to hash plaintext passwords, producing output in standard BCrypt format (starting with `$2a$` or `$2b$` prefix).
2. WHEN a plaintext password is verified against a stored hash, THE Password_Encoder SHALL return true only when the plaintext matches the original password.
3. WHEN the same plaintext password is hashed twice, THE Password_Encoder SHALL produce two distinct hash values (due to random salt).
4. IF a null or empty plaintext password is provided for hashing or verification, THEN THE Password_Encoder SHALL reject the input by throwing an IllegalArgumentException.

### Requirement 3: Login Endpoint

**User Story:** As a user, I want to log in with my email and password, so that I can access the protected features of the application.

#### Acceptance Criteria

1. WHEN a POST request with valid email and password is sent to the Login_Endpoint, THE Auth_Module SHALL authenticate the user, create a Session, and return a session cookie in the response headers.
2. WHEN a POST request with valid credentials is sent to the Login_Endpoint, THE Auth_Module SHALL return an HTTP 200 response containing a JSON body with the authenticated user's id, name, and email fields only.
3. WHEN a POST request with an email that does not match any User_Entity is sent to the Login_Endpoint, THE Auth_Module SHALL return an HTTP 401 response with an error message indicating invalid credentials, without revealing whether the email or password was incorrect.
4. WHEN a POST request with a registered email but incorrect password is sent to the Login_Endpoint, THE Auth_Module SHALL return an HTTP 401 response with an error message indicating invalid credentials, without revealing whether the email or password was incorrect.
5. THE Login_Endpoint SHALL accept a JSON request body with `email` (string, maximum 255 characters) and `password` (string, maximum 128 characters) fields.
6. IF a POST request is sent to the Login_Endpoint with a missing or empty `email` field or a missing or empty `password` field, THEN THE Auth_Module SHALL return an HTTP 400 response with an error message indicating which fields are missing or invalid.

### Requirement 4: Security Filter Chain Configuration

**User Story:** As a developer, I want all API endpoints protected by default with explicit permit rules for login and health, so that unauthenticated users cannot access application data.

#### Acceptance Criteria

1. THE Security_Filter_Chain SHALL permit unauthenticated access to the Login_Endpoint (`POST /api/auth/login`) and the Logout_Endpoint (`POST /api/auth/logout`).
2. THE Security_Filter_Chain SHALL permit unauthenticated access to the actuator health endpoint (`GET /actuator/health`).
3. THE Security_Filter_Chain SHALL require authentication for all other endpoints matching `/api/**`.
4. WHEN an unauthenticated request is made to a Protected_Endpoint, THE Security_Filter_Chain SHALL return an HTTP 401 response with a JSON body containing an error message indicating authentication is required.
5. WHEN an authenticated request is made to a Protected_Endpoint, THE Security_Filter_Chain SHALL delegate the request to the matched controller handler.
6. THE Security_Filter_Chain SHALL use session-based authentication (not stateless JWT).
7. THE Security_Filter_Chain SHALL disable CSRF protection for API endpoints to support non-browser clients during the POC phase.

### Requirement 5: Current User Resolution

**User Story:** As a developer, I want to resolve the currently authenticated user in any controller or service, so that features can attribute interactions to the logged-in user.

#### Acceptance Criteria

1. WHEN a request is authenticated, THE Current_User_Resolver SHALL make the corresponding User_Entity available to controllers and services, including the user's id, name, and email fields as stored in the database.
2. WHEN the Current_User_Resolver resolves the authenticated principal, THE Current_User_Resolver SHALL look up the User_Entity from the database by the email address present in the authenticated principal.
3. IF the authenticated principal's email does not correspond to a User_Entity in the database, THEN THE Current_User_Resolver SHALL return an HTTP 401 response.

### Requirement 6: Logout

**User Story:** As a user, I want to log out, so that my session is invalidated and subsequent requests are treated as anonymous.

#### Acceptance Criteria

1. WHEN a POST request is sent to `/api/auth/logout` with a valid session, THE Auth_Module SHALL invalidate the Session.
2. WHEN a POST request is sent to `/api/auth/logout` with a valid session, THE Auth_Module SHALL return an HTTP 200 response with an empty body.
3. WHEN the Session has been invalidated, THE Security_Filter_Chain SHALL return an HTTP 401 response to any subsequent request that presents the invalidated session identifier on a Protected_Endpoint.
4. IF a POST request is sent to `/api/auth/logout` without a valid session, THEN THE Security_Filter_Chain SHALL return an HTTP 401 response.

### Requirement 7: Seed User Password

**User Story:** As a developer, I want the existing seed data mechanism to set a known password hash for the seeded user, so that integration tests and local development can authenticate.

#### Acceptance Criteria

1. THE Flyway migration (V4) SHALL add a `password_hash` column of type VARCHAR(255) to the `users` table and update all seed users to store a BCrypt hash (cost factor 10) corresponding to the plaintext password `Password1`.
2. WHEN a developer or integration test authenticates using the plaintext password `Password1` against any seeded user's email, THE system SHALL successfully verify the credential via Spring Security's BCryptPasswordEncoder.
3. THE default seed password value (`Password1`) and the list of seeded user emails SHALL be documented in the project README under a "Local Development" section.
4. IF the `password_hash` column already exists on the `users` table, THEN THE Flyway migration (V4) SHALL skip the column addition and only perform the UPDATE of seed user password hashes.
