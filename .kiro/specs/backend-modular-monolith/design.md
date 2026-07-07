# Design Document: Backend Modular Monolith

## Overview

This design establishes the foundational modular-monolith architecture for the Staff Engagement backend. It introduces five bounded-context modules (user, employee, client, interaction, task) as empty packages, integrates Flyway for database schema evolution, configures dual Spring profiles (local/dev) with environment-variable-driven secrets, enables structured JSON logging for non-local environments using Spring Boot 3.4+ native support, and exposes an Actuator health endpoint for CI/Docker readiness checks.

The architecture is intentionally skeletal — no domain entities are introduced yet. The goal is to prove the modular layout, migration pipeline, and environment configuration before domain logic lands, enabling parallel module development. Module boundaries are enforced by convention (package-per-module) rather than tooling. MVC sub-packages (controller, service, repository, model) will be created within each module during implementation.

## Architecture

### High-Level Module Layout

```
com.psybergate.staff_engagement/
├── StaffEngagementApplication.java
├── user/
│   └── .gitkeep
├── employee/
│   └── .gitkeep
├── client/
│   └── .gitkeep
├── interaction/
│   └── .gitkeep
└── task/
    └── .gitkeep
```

### Architectural Decisions

1. **Simple package-per-module convention without enforcement tooling** — Each bounded context is a top-level package under the application root. Module boundaries are enforced by developer discipline and code review rather than a framework like Spring Modulith. This keeps the dependency footprint minimal and avoids coupling to Modulith's package conventions (api/internal). MVC sub-packages (controller, service, repository, model) will be created per module as implementation begins.

2. **Empty packages with .gitkeep** — Git does not track empty directories. A `.gitkeep` file in each module package ensures the module skeleton is version-controlled from the start, making the intended architecture visible to all developers on clone.

3. **Spring Boot 3.4+ native structured logging** — Instead of adding Logstash encoder or custom Logback XML, we use the built-in `logging.structured.format.console` property introduced in Spring Boot 3.4. This simplifies configuration and reduces external dependencies.

4. **Flyway with baseline-only V1** — The first migration proves the pipeline works without creating domain tables. Future migrations will add schema objects per module as needed.

5. **Dual profile strategy (local/dev)** — The `local` profile uses hardcoded connection values for frictionless developer onboarding. The `dev` profile resolves all secrets from environment variables, making it suitable for shared/CI environments without secrets in source control.

### Dependency Flow

```mermaid
graph TD
    A[StaffEngagementApplication] --> C[Flyway]
    A --> D[Actuator]
    A --> E[Spring Data JPA]
    
    C --> G[V1__baseline.sql]
    D --> H[/actuator/health]
    E --> I[PostgreSQL]
    
    subgraph Modules
        M1[user]
        M2[employee]
        M3[client]
        M4[interaction]
        M5[task]
    end
    
    A --> Modules
```

## Components and Interfaces

### Maven Dependencies (additions to pom.xml)

| Artifact | Scope | Purpose |
|----------|-------|---------|
| `flyway-core` (via `spring-boot-starter-data-jpa` auto-config) | compile | Migration engine |
| `flyway-database-postgresql` | runtime | PostgreSQL dialect for Flyway |

> Note: Flyway core is auto-configured by Spring Boot when on the classpath. The `flyway-database-postgresql` artifact provides the PostgreSQL-specific dialect support required by Flyway 10+.

### Configuration Files

| File | Profile | Purpose |
|------|---------|---------|
| `application.properties` | (base) | App name, default profile, Flyway enabled, Actuator health config |
| `application-local.properties` | local | Local DB connection (localhost:5432/staff_engagement), plain-text logging |
| `application-dev.properties` | dev | Env-var-driven DB connection, structured JSON logging |

### Health Endpoint Configuration

The existing Actuator dependency already provides `/actuator/health`. Configuration ensures:
- Health endpoint is exposed (default behaviour in Spring Boot)
- Database health indicator is active (auto-configured with Spring Data JPA on classpath)
- No authentication required for health endpoint

### Flyway Migration

**Location**: `src/main/resources/db/migration/V1__baseline.sql`

```sql
-- Flyway baseline migration: proves the migration pipeline is operational.
-- No tables, sequences, or other database objects are created.
```

Flyway auto-configuration (provided by `spring-boot-starter-data-jpa` + `flyway-core` on classpath) runs migrations on startup before the application accepts requests.

### Spring Profiles

**local** (default):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/staff_engagement
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
```

**dev** (environment-variable-driven):
```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
logging.structured.format.console=ecs
```

### Structured Logging

Spring Boot 3.4+ provides native structured logging support. The configuration is profile-driven:

- **local profile**: No `logging.structured.format.console` property — defaults to standard plain-text Logback output
- **dev profile**: `logging.structured.format.console=ecs` — outputs each log line as a single JSON object in Elastic Common Schema format, including stack traces within the JSON body

The ECS format automatically includes: `@timestamp` (ISO-8601), `log.level`, `process.thread.name`, `service.name`, `log.logger`, and `message`.

## Data Models

This feature introduces no domain entities. The data model is limited to Flyway's schema history table (`flyway_schema_history`), which is auto-created by Flyway on first run.

### Flyway Schema History Table (auto-managed)

| Column | Type | Description |
|--------|------|-------------|
| installed_rank | INTEGER | Order of installation |
| version | VARCHAR | Migration version (e.g., "1") |
| description | VARCHAR | Migration description (e.g., "baseline") |
| type | VARCHAR | Migration type (SQL) |
| script | VARCHAR | Script filename |
| checksum | INTEGER | Script checksum for tamper detection |
| installed_by | VARCHAR | Database user that ran the migration |
| installed_on | TIMESTAMP | Execution timestamp |
| execution_time | INTEGER | Execution time in milliseconds |
| success | BOOLEAN | Whether migration succeeded |

### Configuration Property Model

| Property | Source | Required |
|----------|--------|----------|
| `DB_HOST` | Environment variable (dev) | Yes (dev profile only) |
| `DB_PORT` | Environment variable (dev) | Yes (dev profile only) |
| `DB_NAME` | Environment variable (dev) | Yes (dev profile only) |
| `DB_USERNAME` | Environment variable (dev) | Yes (dev profile only) |
| `DB_PASSWORD` | Environment variable (dev) | Yes (dev profile only) |

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Flyway migration idempotence

*For any* application startup sequence against a database where migrations have already been applied, Flyway shall not re-execute previously applied migrations and the application shall start successfully.

**Validates: Requirements 2.3, 2.4**

### Property 2: Health endpoint reflects database connectivity

*For any* application state, the `/actuator/health` endpoint shall return `"status": "UP"` if and only if the database is reachable, and `"status": "DOWN"` otherwise.

**Validates: Requirements 3.2, 3.3, 3.5**

### Property 3: Profile-driven logging format

*For any* log event emitted by the application, when the `dev` profile is active the output shall be a valid JSON object, and when the `local` profile is active the output shall be plain-text format.

**Validates: Requirements 5.1, 5.2, 5.4**

### Property 4: Environment variable resolution under dev profile

*For any* combination of valid database environment variable values (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`), the application under the `dev` profile shall resolve those variables into the datasource URL, username, and password and establish a database connection.

**Validates: Requirements 4.4, 4.7**

## Error Handling

### Database Connectivity Failures

| Scenario | Behaviour |
|----------|-----------|
| PostgreSQL unreachable at startup | Application fails to start. Spring Boot logs datasource connection error. Flyway does not attempt migrations. |
| PostgreSQL unreachable at runtime | `/actuator/health` returns 503 with `"status": "DOWN"`. Existing request processing may fail with `DataAccessException`. |
| Flyway migration script error | Application fails to start. Flyway logs the failed migration version and SQL error. |

### Environment Variable Failures (dev profile)

| Scenario | Behaviour |
|----------|-----------|
| Missing `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, or `DB_PASSWORD` | Application fails to start during property resolution with `IllegalArgumentException` indicating the unresolvable placeholder. |

### Profile Misconfiguration

| Scenario | Behaviour |
|----------|-----------|
| No profile explicitly activated | Application defaults to `local` profile (via `spring.profiles.default=local` in base `application.properties`). |
| Unknown profile activated | Application starts with base properties only; no profile-specific overrides apply. |

## Testing Strategy

### Integration Tests

- **Application context loads**: Standard Spring Boot test (`@SpringBootTest`) verifying the context starts under test profile with a PostgreSQL database (Testcontainers recommended for CI).
- **Flyway migration test**: Verify that the baseline migration applies cleanly against a real PostgreSQL instance (Testcontainers recommended for CI).
- **Health endpoint test**: Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` to verify `/actuator/health` returns 200 with `"status": "UP"` when the database is available.
- **Profile resolution test**: Verify that under the `dev` profile with environment variables set, the datasource is configured correctly.

### Property-Based Testing Assessment

Property-based testing is **not applicable** to this feature. The feature is primarily infrastructure and configuration:

- Module structure is validated by the presence of `.gitkeep` files and package directories
- Flyway migrations are SQL scripts validated by execution against a real database
- Profile configuration is declarative properties validated by integration tests
- Health endpoint behaviour is deterministic (UP/DOWN based on DB connectivity)
- Logging format is configuration-driven, not logic-driven

There are no pure functions with variable input spaces, no parsers/serializers, and no business logic algorithms to test with randomized inputs. The correctness properties above are validated through integration tests.

### Build Verification

The single quality gate is `mvn verify`, which:
1. Compiles all source code
2. Runs unit tests
3. Runs integration tests (Failsafe plugin if configured, or Surefire for `*Test` classes)
4. Packages the application

### Test Framework

- **JUnit 5** (via `spring-boot-starter-test`)
- **Spring Boot Test** for integration tests with application context
- **Testcontainers** (recommended addition for CI-safe PostgreSQL testing)
