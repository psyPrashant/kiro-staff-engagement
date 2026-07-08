# Implementation Plan: Backend Modular Monolith

## Overview

This plan establishes the modular-monolith skeleton for the Staff Engagement backend. Tasks are ordered to prove infrastructure first (dependencies, migration pipeline, profiles, logging), then scaffold modules, and finally wire up integration tests as the quality gate.

## Tasks

- [x] 1. Add Flyway dependencies and configure base application properties
  - [x] 1.1 Add Flyway Maven dependencies to pom.xml
    - Add `flyway-core` and `flyway-database-postgresql` (runtime scope) dependencies
    - _Requirements: 2.1_

  - [x] 1.2 Configure base application.properties
    - Set `spring.application.name=staff-engagement`
    - Set `spring.profiles.default=local`
    - Set `spring.flyway.enabled=true`
    - Set `management.endpoints.web.exposure.include=health`
    - Set `management.endpoint.health.show-details=always` (to include DB indicator)
    - _Requirements: 3.1, 3.4, 3.5, 4.5_

- [x] 2. Configure Spring profiles for local and dev environments
  - [x] 2.1 Create application-local.properties
    - Set `spring.datasource.url=jdbc:postgresql://localhost:5432/staff_engagement`
    - Set `spring.datasource.username=postgres`
    - Set `spring.datasource.password=postgres`
    - Set `spring.jpa.hibernate.ddl-auto=validate`
    - No structured logging property (defaults to plain-text)
    - _Requirements: 4.1, 4.3, 5.2, 5.4_

  - [x] 2.2 Create application-dev.properties
    - Set `spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}`
    - Set `spring.datasource.username=${DB_USERNAME}`
    - Set `spring.datasource.password=${DB_PASSWORD}`
    - Set `spring.jpa.hibernate.ddl-auto=validate`
    - Set `logging.structured.format.console=ecs`
    - _Requirements: 4.2, 4.4, 4.7, 5.1, 5.3, 5.5_

- [x] 3. Create Flyway baseline migration and module package structure
  - [x] 3.1 Create V1__baseline.sql migration script
    - Create `src/main/resources/db/migration/V1__baseline.sql`
    - Content: a SQL comment asserting the pipeline is operational, no DDL statements
    - _Requirements: 2.2, 2.3, 2.4_

  - [x] 3.2 Create module package directories with .gitkeep files
    - Create `src/main/java/com/psybergate/staff_engagement/user/.gitkeep`
    - Create `src/main/java/com/psybergate/staff_engagement/employee/.gitkeep`
    - Create `src/main/java/com/psybergate/staff_engagement/client/.gitkeep`
    - Create `src/main/java/com/psybergate/staff_engagement/interaction/.gitkeep`
    - Create `src/main/java/com/psybergate/staff_engagement/task/.gitkeep`
    - _Requirements: 1.1, 1.2_

- [x] 4. Checkpoint - Verify build compiles and configuration is correct
  - Ensure `mvn compile` succeeds with the new dependencies and configuration files. Ask the user if questions arise.

- [x] 5. Write integration tests
  - [x] 5.1 Add Testcontainers dependency to pom.xml
    - Add `org.testcontainers:postgresql` and `org.testcontainers:junit-jupiter` with test scope
    - Add `spring-boot-testcontainers` dependency for auto-configuration support
    - _Requirements: 6.1_

  - [x] 5.2 Create base test configuration with Testcontainers
    - Create a test configuration class that provisions a PostgreSQL Testcontainer
    - Use `@ServiceConnection` annotation for automatic datasource wiring
    - Set test profile to `local` to avoid env-var requirements
    - _Requirements: 6.1, 6.2_

  - [x] 5.3 Write application context load test
    - Create `StaffEngagementApplicationTests.java` (update existing)
    - Use `@SpringBootTest` with Testcontainers to verify context starts cleanly
    - Verifies Flyway migration applies and JPA validation passes
    - _Requirements: 6.1, 6.2, 6.4_

  - [x] 5.4 Write health endpoint integration test
    - Create `HealthEndpointIntegrationTest.java`
    - Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`
    - Assert GET `/actuator/health` returns 200 with `"status": "UP"`
    - Assert response includes database health indicator (db component present)
    - _Requirements: 3.1, 3.2, 3.4, 3.5_

  - [x] 5.5 Write Flyway migration integration test
    - Create `FlywayMigrationIntegrationTest.java`
    - Inject `Flyway` bean and verify `info().applied()` contains the V1 baseline
    - Assert migration version is "1", description is "baseline", and success is true
    - _Requirements: 2.3, 2.4, 2.5_

- [x] 6. Final checkpoint - Ensure all tests pass with `mvn verify`
  - Run `mvn verify` and ensure BUILD SUCCESS. Ask the user if questions arise.

## Notes

- All tasks use Java 21 with Spring Boot 3.5 conventions
- Testcontainers provides a real PostgreSQL instance for integration tests, avoiding H2 compatibility issues with Flyway's PostgreSQL dialect
- `ddl-auto=validate` ensures Flyway is the sole schema authority — Hibernate only validates, never modifies
- The `ecs` structured logging format is Spring Boot 3.4+ native and requires no additional dependencies
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "3.2"] },
    { "id": 1, "tasks": ["2.1", "2.2", "3.1"] },
    { "id": 2, "tasks": ["5.1"] },
    { "id": 3, "tasks": ["5.2"] },
    { "id": 4, "tasks": ["5.3", "5.4", "5.5"] }
  ]
}
```
