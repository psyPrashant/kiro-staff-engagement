# Implementation Plan: Backend Authentication

## Overview

Implement session-based authentication for the Staff Engagement backend using Spring Security 6.x. The implementation adds a new `auth` package with security configuration, login/logout endpoints, and current-user resolution. The existing `User` entity is extended with a password hash column, and a Flyway V4 migration seeds existing users with a known password.

## Tasks

- [x] 1. Add Maven dependencies and Flyway migration
  - [x] 1.1 Add security and validation dependencies to pom.xml
    - Add `spring-boot-starter-security`, `spring-boot-starter-validation` as runtime dependencies
    - Add `spring-security-test` and `jqwik` (v1.9.2) as test-scoped dependencies
    - _Requirements: 2.1, 3.5, 3.6_

  - [x] 1.2 Create Flyway V4 migration to add password_hash column and seed data
    - Create `V4__add_password_hash.sql` in `src/main/resources/db/migration/`
    - Use `ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255)`
    - Update seed users (`alice.johnson@psybergate.com`, `bob.smith@psybergate.com`, `carol.williams@psybergate.com`) with BCrypt hash of `Password1` (cost 10)
    - _Requirements: 1.1, 1.2, 7.1, 7.4_

  - [x] 1.3 Extend User entity with passwordHash field
    - Add `@Column(name = "password_hash") @JsonIgnore private String passwordHash;` to the `User` entity
    - Ensure the field has a getter/setter via Lombok (`@Getter @Setter` already present on class)
    - _Requirements: 1.3, 1.4_

- [x] 2. Implement security configuration and user details service
  - [x] 2.1 Create SecurityConfig class
    - Create `com.psybergate.staff_engagement.auth.SecurityConfig`
    - Define `SecurityFilterChain` bean: disable CSRF, permit `/api/auth/login` (POST), `/api/auth/logout` (POST), `/actuator/health` (GET); require auth for `/api/**`; permit all other requests
    - Define `BCryptPasswordEncoder` bean with strength 10
    - Define `AuthenticationManager` bean using `AuthenticationConfiguration`
    - Implement custom `AuthenticationEntryPoint` returning JSON 401 `{"error":"Authentication required"}`
    - Implement custom `LogoutSuccessHandler` returning HTTP 200 with empty body
    - Configure session management with `SessionCreationPolicy.IF_REQUIRED`
    - Configure logout to invalidate session and delete `JSESSIONID` cookie
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 6.1, 6.2_

  - [x] 2.2 Create CustomUserDetailsService
    - Create `com.psybergate.staff_engagement.auth.CustomUserDetailsService`
    - Implement `UserDetailsService.loadUserByUsername(String email)`
    - Load user from `UserRepository.findByEmail()`, throw `UsernameNotFoundException` if not found
    - Build `UserDetails` with email as username, passwordHash as password, empty authorities
    - _Requirements: 3.3, 3.4, 5.2_

- [x] 3. Implement AuthController, AuthService, and DTOs
  - [x] 3.1 Create LoginRequest and LoginResponse DTOs
    - Create `com.psybergate.staff_engagement.auth.LoginRequest` as a Java record
    - Add `@NotBlank` and `@Size(max=255)` on `email` field
    - Add `@NotBlank` and `@Size(max=128)` on `password` field
    - Create `com.psybergate.staff_engagement.auth.LoginResponse` as a Java record with `id`, `name`, `email`
    - _Requirements: 3.2, 3.5_

  - [x] 3.2 Create AuthService
    - Create `com.psybergate.staff_engagement.auth.AuthService`
    - Implement `login(LoginRequest, HttpServletRequest)` method
    - Authenticate using `AuthenticationManager.authenticate()` with `UsernamePasswordAuthenticationToken`
    - Set `SecurityContext` and store it in the HTTP session via `HttpSessionSecurityContextRepository`
    - Look up User entity by email and return `LoginResponse`
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 3.3 Create AuthController
    - Create `com.psybergate.staff_engagement.auth.AuthController` with `@RestController @RequestMapping("/api/auth")`
    - Implement `POST /login` endpoint accepting `@Valid @RequestBody LoginRequest` and `HttpServletRequest`
    - Delegate to `AuthService.login()` and return `ResponseEntity.ok(loginResponse)`
    - _Requirements: 3.1, 3.2, 3.5, 3.6_

  - [x] 3.4 Create GlobalExceptionHandler
    - Create `com.psybergate.staff_engagement.auth.GlobalExceptionHandler` with `@RestControllerAdvice`
    - Handle `MethodArgumentNotValidException` → 400 with `error` + `fieldErrors` map
    - Handle `BadCredentialsException` → 401 with generic `{"error":"Invalid credentials"}`
    - Handle `UsernameNotFoundException` → 401 with generic `{"error":"Invalid credentials"}` (prevent enumeration)
    - _Requirements: 3.3, 3.4, 3.6_

- [x] 4. Implement CurrentUserResolver
  - [x] 4.1 Create CurrentUserResolver component
    - Create `com.psybergate.staff_engagement.auth.CurrentUserResolver` with `@Component`
    - Implement `resolve()` method that gets `Authentication` from `SecurityContextHolder`
    - If no authentication or not authenticated, throw `ResponseStatusException(HttpStatus.UNAUTHORIZED)`
    - Look up User by `auth.getName()` (email) via `UserRepository.findByEmail()`
    - If user not found, throw `ResponseStatusException(HttpStatus.UNAUTHORIZED)`
    - Return the resolved `User` entity
    - _Requirements: 5.1, 5.2, 5.3_

- [x] 5. Checkpoint - Verify core implementation compiles
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Unit tests
  - [x] 6.1 Write unit tests for AuthService
    - Create `com.psybergate.staff_engagement.auth.AuthServiceTest`
    - Test valid login returns correct LoginResponse (mock `AuthenticationManager`, `UserRepository`)
    - Test invalid email throws `BadCredentialsException` (authentication manager rejects)
    - Test wrong password throws `BadCredentialsException`
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 6.2 Write unit tests for CustomUserDetailsService
    - Create `com.psybergate.staff_engagement.auth.CustomUserDetailsServiceTest`
    - Test user found by email returns correct `UserDetails` with email as username and passwordHash as password
    - Test user not found throws `UsernameNotFoundException`
    - _Requirements: 3.3, 3.4, 5.2_

  - [x] 6.3 Write unit tests for CurrentUserResolver
    - Create `com.psybergate.staff_engagement.auth.CurrentUserResolverTest`
    - Test authenticated user resolved correctly
    - Test no authentication throws 401
    - Test authenticated principal with deleted user throws 401
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 6.4 Write unit tests for LoginRequest validation
    - Create `com.psybergate.staff_engagement.auth.LoginRequestValidationTest`
    - Test blank email rejected, blank password rejected, oversized email rejected, oversized password rejected
    - Use Jakarta Bean Validation `Validator` directly
    - _Requirements: 3.5, 3.6_

  - [x] 6.5 Write unit test for User entity JSON serialization
    - Create `com.psybergate.staff_engagement.auth.UserJsonSerializationTest`
    - Use `ObjectMapper` to serialize a User with a non-null `passwordHash`
    - Assert the resulting JSON does NOT contain `passwordHash` or `password_hash` key
    - _Requirements: 1.4_

- [x] 7. Property-based tests (jqwik)
  - [x] 7.1 Write property test: Password hash excluded from JSON serialization
    - **Property 1: Password hash excluded from JSON serialization**
    - **Validates: Requirements 1.4**
    - Create `com.psybergate.staff_engagement.auth.AuthPropertyTests` (or add to it)
    - Generate arbitrary non-empty strings for `passwordHash`, set on User, serialize to JSON
    - Assert output never contains `passwordHash` or `password_hash` key

  - [x] 7.2 Write property test: BCrypt output format compliance
    - **Property 2: BCrypt output format compliance**
    - **Validates: Requirements 2.1**
    - Generate random non-empty printable strings (1–128 chars)
    - Hash with `BCryptPasswordEncoder(10)`, assert result matches `^\$2[ab]\$10\$.{53}$` and is 60 chars

  - [x] 7.3 Write property test: Password verification round-trip
    - **Property 3: Password verification round-trip**
    - **Validates: Requirements 2.2**
    - Generate random non-empty plaintext, hash it, verify original → true
    - Mutate plaintext (append char), verify mutated → false

  - [x] 7.4 Write property test: BCrypt salt uniqueness
    - **Property 4: BCrypt salt uniqueness**
    - **Validates: Requirements 2.3**
    - Generate random non-empty plaintext, hash twice, assert the two hashes differ

- [x] 8. Integration tests
  - [x] 8.1 Write AuthLoginIntegrationTest
    - Create `com.psybergate.staff_engagement.auth.AuthLoginIntegrationTest` extending `BaseIntegrationTest`
    - Test valid login returns 200 + session cookie + correct JSON body (id, name, email)
    - Test invalid email returns 401 + generic error
    - Test wrong password returns 401 + generic error
    - Test missing fields returns 400 + field errors
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [x] 8.2 Write AuthLogoutIntegrationTest
    - Create `com.psybergate.staff_engagement.auth.AuthLogoutIntegrationTest` extending `BaseIntegrationTest`
    - Test logout with valid session returns 200 + empty body + session invalidated
    - Test logout without session returns 401
    - Test accessing protected endpoint after logout returns 401
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 8.3 Write SecurityFilterChainIntegrationTest
    - Create `com.psybergate.staff_engagement.auth.SecurityFilterChainIntegrationTest` extending `BaseIntegrationTest`
    - Test unauthenticated access to `/api/**` returns 401 JSON
    - Test authenticated access to protected endpoint succeeds
    - Test unauthenticated access to `/actuator/health` succeeds
    - Test unauthenticated access to login/logout endpoints succeeds (not blocked)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 8.4 Write FlywayMigrationV4IntegrationTest
    - Create `com.psybergate.staff_engagement.auth.FlywayMigrationV4IntegrationTest` extending `BaseIntegrationTest`
    - Test `password_hash` column exists on `users` table
    - Test seed users have non-null `password_hash` values
    - Test seed user hashes verify against `Password1` using `BCryptPasswordEncoder`
    - _Requirements: 7.1, 7.2, 7.4_

- [x] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Integration tests use the existing `BaseIntegrationTest` pattern with Testcontainers
- `UserRepository.findByEmail()` already exists — no changes needed to the repository
- The Flyway V4 migration uses `IF NOT EXISTS` for idempotency (Requirement 7.4)
- Cucumber acceptance tests are NOT included — handled separately

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3", "3.4", "4.1"] },
    { "id": 4, "tasks": ["6.1", "6.2", "6.3", "6.4", "6.5"] },
    { "id": 5, "tasks": ["7.1", "7.2", "7.3", "7.4"] },
    { "id": 6, "tasks": ["8.1", "8.2", "8.3", "8.4"] }
  ]
}
```
