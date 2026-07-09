# Design Document: Backend Authentication

## Overview

This design adds session-based authentication to the Staff Engagement backend using Spring Security 6.x (bundled with Spring Boot 3.5). The implementation follows a minimal, POC-appropriate approach: a single `auth` package owns security configuration, login/logout endpoints, and current-user resolution. The existing `User` entity is extended with a password hash column, and Spring Security's `HttpSession`-based mechanism handles session lifecycle.

Key design decisions:
- **Session-based auth over JWT** — simpler server-side state management for a POC; avoids token refresh complexity.
- **BCrypt cost factor 10** — industry-standard default balancing security and speed for development.
- **CSRF disabled** — acceptable for a POC targeting non-browser API clients; revisit before production.
- **No roles/authorities** — single-user POC; only authenticated vs. anonymous distinction needed.
- **Flyway migration for schema changes** — consistent with existing migration strategy (V4 next).

## Architecture

```mermaid
graph TB
    subgraph "HTTP Layer"
        Client[HTTP Client]
    end

    subgraph "Spring Security Filter Chain"
        SF[SecurityFilterChain Bean]
        AF[AuthenticationFilter]
        EP[AuthenticationEntryPoint]
    end

    subgraph "auth package"
        AC[AuthController]
        AS[AuthService]
        SC[SecurityConfig]
        UDS[CustomUserDetailsService]
        CUR[CurrentUserResolver]
        DTO_REQ[LoginRequest DTO]
        DTO_RES[LoginResponse DTO]
    end

    subgraph "user package (existing)"
        UE[User Entity]
        UR[UserRepository]
    end

    subgraph "Database"
        DB[(PostgreSQL)]
    end

    Client -->|POST /api/auth/login| SF
    Client -->|POST /api/auth/logout| SF
    Client -->|GET /api/**| SF
    SF --> AF
    AF -->|unauthenticated protected| EP
    AF -->|authenticated| AC
    AC --> AS
    AS --> UDS
    UDS --> UR
    UR --> DB
    AS -->|BCryptPasswordEncoder| AS
    CUR -->|@AuthenticationPrincipal| UR
    UE --> DB
```

**Request flow:**
1. Client sends request → Security Filter Chain intercepts.
2. Permitted paths (login, logout, actuator/health) pass through without auth check.
3. Protected paths require valid `JSESSIONID` session cookie.
4. Login: `AuthController` delegates to `AuthService`, which uses `CustomUserDetailsService` + `BCryptPasswordEncoder` to authenticate, then creates a session.
5. Current user: Controllers use `@AuthenticationPrincipal` or a custom argument resolver to get the authenticated `User` entity.

## Components and Interfaces

### 1. SecurityConfig (`auth/SecurityConfig.java`)

Spring `@Configuration` class providing the `SecurityFilterChain` and `PasswordEncoder` beans.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedEntryPoint())
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
```

### 2. CustomUserDetailsService (`auth/CustomUserDetailsService.java`)

Implements `UserDetailsService` to load users from the database by email.

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(Collections.emptyList())
            .build();
    }
}
```

### 3. AuthController (`auth/AuthController.java`)

REST controller handling login. Logout is handled by Spring Security's built-in logout filter.

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }
}
```

### 4. AuthService (`auth/AuthService.java`)

Service encapsulating authentication logic — validates credentials, creates session.

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        return new LoginResponse(user.getId(), user.getName(), user.getEmail());
    }
}
```

### 5. DTOs

```java
// LoginRequest.java
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password must not exceed 128 characters")
    String password
) {}

// LoginResponse.java
public record LoginResponse(Long id, String name, String email) {}
```

### 6. CurrentUserResolver (`auth/CurrentUserResolver.java`)

Utility or argument resolver to obtain the authenticated `User` entity.

```java
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public User resolve() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String email = auth.getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
```

### 7. User Entity Extension

The existing `User` entity gains a `passwordHash` field:

```java
@Column(name = "password_hash")
@JsonIgnore
private String passwordHash;
```

### 8. AuthenticationEntryPoint and LogoutSuccessHandler

Custom entry point returns JSON 401 (not a redirect). Custom logout handler returns 200 with empty body.

```java
// In SecurityConfig
private AuthenticationEntryPoint unauthorizedEntryPoint() {
    return (request, response, authException) -> {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Authentication required\"}");
    };
}

private LogoutSuccessHandler logoutSuccessHandler() {
    return (request, response, authentication) -> {
        response.setStatus(HttpServletResponse.SC_OK);
    };
}
```

## Data Models

### User Entity (Extended)

| Field | Type | Column | Constraints | Notes |
|-------|------|--------|-------------|-------|
| id | Long | id | PK, auto-increment | Existing |
| name | String | name | NOT NULL, 255 chars | Existing |
| email | String | email | NOT NULL, UNIQUE, 255 chars | Existing |
| passwordHash | String | password_hash | NULLABLE, 255 chars | New — `@JsonIgnore` |
| createdAt | Instant | created_at | NOT NULL, non-updatable | Existing |

### Flyway Migration V4

```sql
-- V4__add_password_hash.sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Update seed users with BCrypt hash of "Password1" (cost factor 10)
UPDATE users SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE email IN (
    'alice.johnson@psybergate.com',
    'bob.smith@psybergate.com',
    'carol.williams@psybergate.com'
);
```

> Note: The hash value above is a pre-computed BCrypt hash of "Password1" with cost 10. The actual value will be generated during implementation.

### LoginRequest DTO

| Field | Type | Constraints |
|-------|------|-------------|
| email | String | @NotBlank, @Size(max=255) |
| password | String | @NotBlank, @Size(max=128) |

### LoginResponse DTO

| Field | Type | Description |
|-------|------|-------------|
| id | Long | User's database ID |
| name | String | User's display name |
| email | String | User's email address |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Password hash excluded from JSON serialization

*For any* User entity with a non-null passwordHash field, serializing the entity to JSON SHALL never include the `passwordHash` or `password_hash` key in the output.

**Validates: Requirements 1.4**

### Property 2: BCrypt output format compliance

*For any* non-null, non-empty plaintext password, hashing it with the Password_Encoder SHALL produce a string matching the BCrypt format (`$2a$10$...` or `$2b$10$...`) with exactly 60 characters.

**Validates: Requirements 2.1**

### Property 3: Password verification round-trip

*For any* non-null, non-empty plaintext password, hashing it and then verifying the original plaintext against the resulting hash SHALL return true, and verifying any different non-empty plaintext against that hash SHALL return false.

**Validates: Requirements 2.2**

### Property 4: BCrypt salt uniqueness

*For any* non-null, non-empty plaintext password, hashing it twice SHALL produce two distinct hash strings (due to random salt generation).

**Validates: Requirements 2.3**

## Error Handling

| Scenario | HTTP Status | Response Body | Notes |
|----------|-------------|---------------|-------|
| Valid login | 200 | `{"id":1,"name":"...","email":"..."}` | Session cookie set |
| Invalid credentials (bad email or password) | 401 | `{"error":"Invalid credentials"}` | Generic message — no email/password hint |
| Missing/empty email or password | 400 | `{"error":"...","fieldErrors":{...}}` | Per-field validation messages |
| Unauthenticated access to protected endpoint | 401 | `{"error":"Authentication required"}` | Custom entry point |
| Logout with valid session | 200 | Empty body | Session invalidated |
| Logout without valid session | 401 | `{"error":"Authentication required"}` | Caught by filter chain |
| Authenticated principal no longer in DB | 401 | `{"error":"Authentication required"}` | CurrentUserResolver throws |

**Error response structure:**
```json
{
  "error": "Human-readable error message",
  "fieldErrors": {
    "email": "Email is required",
    "password": "Password is required"
  }
}
```

The `fieldErrors` key is only present for validation (400) responses. All error responses use `application/json` content type.

**Global exception handling:** A `@RestControllerAdvice` in the auth package handles:
- `MethodArgumentNotValidException` → 400 with field errors
- `BadCredentialsException` → 401 with generic message
- `UsernameNotFoundException` → 401 with generic message (same as bad credentials to prevent enumeration)

## Testing Strategy

### Unit Tests (JUnit 5 + Mockito)

| Component | What to test | Approach |
|-----------|--------------|----------|
| `AuthService` | Login logic — valid credentials, invalid email, wrong password | Mock `AuthenticationManager`, `UserRepository` |
| `CustomUserDetailsService` | User lookup by email — found, not found | Mock `UserRepository` |
| `CurrentUserResolver` | Resolve authenticated user, handle missing user, handle no auth | Mock `SecurityContextHolder`, `UserRepository` |
| `LoginRequest` validation | Blank email, blank password, oversized fields | Jakarta validation unit tests |
| `User` entity | `@JsonIgnore` on passwordHash | Serialize with ObjectMapper, assert field absent |

### Property-Based Tests (jqwik)

The project will use **jqwik** (JUnit 5 compatible property-based testing library for Java) to validate universal correctness properties.

**Configuration:**
- Minimum 100 iterations per property (jqwik default: 1000, sufficient)
- Each test annotated with a comment referencing the design property

| Property | Test description | Generator strategy |
|----------|------------------|--------------------|
| Property 1: Hash excluded from JSON | Generate User entities with random passwordHash strings, serialize to JSON, assert key absent | Arbitrary strings for passwordHash (including special chars, long strings) |
| Property 2: BCrypt format | Generate random plaintext strings (1–128 chars), hash, verify format regex | Arbitrary non-empty strings with printable chars |
| Property 3: Verification round-trip | Generate random plaintext, hash, verify original → true, verify mutated → false | Arbitrary non-empty strings; mutation = append/prepend char |
| Property 4: Salt uniqueness | Generate random plaintext, hash twice, assert hashes differ | Arbitrary non-empty strings |

**Tag format:** `Feature: backend-auth, Property {N}: {property_text}`

### Integration Tests (Testcontainers + Spring Boot Test)

Extend `BaseIntegrationTest` to inherit PostgreSQL Testcontainers setup.

| Test class | Coverage |
|------------|----------|
| `AuthLoginIntegrationTest` | Full login flow — valid creds (200 + session cookie), invalid email (401), wrong password (401), missing fields (400) |
| `AuthLogoutIntegrationTest` | Logout with session (200, session invalidated), logout without session (401) |
| `SecurityFilterChainIntegrationTest` | Protected endpoints reject anonymous (401), permit authenticated, permit login/logout/health |
| `CurrentUserResolverIntegrationTest` | Resolver returns correct user after login, returns 401 if user deleted from DB |
| `FlywayMigrationV4IntegrationTest` | password_hash column exists, seed users have valid BCrypt hashes verifiable against "Password1" |

### Test Dependencies to Add

```xml
<!-- pom.xml additions (test scope) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.9.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

> Note: `spring-boot-starter-security` is a runtime dependency (not test-only). `spring-boot-starter-validation` is needed for `@Valid`/`@NotBlank` support. `spring-security-test` provides `MockMvc` security utilities. `jqwik` is test-only for property-based tests.
