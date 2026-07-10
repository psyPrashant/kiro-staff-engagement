# Implementation Plan: Interaction Matrix & Follow-Up Logic

## Overview

Implement a read-only engagement analytics endpoint (`GET /api/engagement/matrix`) in the existing Spring Boot backend. The implementation creates a new `engagement` package with pure classification logic, a service orchestrating aggregate queries, and a REST controller. Existing files (`InteractionRepository`, `application.properties`, `GlobalExceptionHandler`) receive targeted additions. Comprehensive testing covers boundary unit tests, jqwik property-based tests, controller slice tests, and a Testcontainers integration test.

## Tasks

- [x] 1. Create engagement package foundation
  - [x] 1.1 Create `EngagementStatus` enum and `EngagementThresholdProperties` configuration class
    - Create `src/main/java/com/psybergate/staff_engagement/engagement/EngagementStatus.java` with values OVERDUE, AT_RISK, ON_TRACK
    - Create `src/main/java/com/psybergate/staff_engagement/engagement/EngagementThresholdProperties.java` with `@ConfigurationProperties(prefix = "engagement.thresholds")`, `@Validated`, `@Min(1) @Max(365)` on both fields, defaults 30 and 14
    - Add a custom validation method (e.g. `@PostConstruct`) that fails startup if `atRiskDays >= overdueDays`
    - Add `engagement.thresholds.overdue-days=30` and `engagement.thresholds.at-risk-days=14` to `application.properties`
    - Enable `@ConfigurationPropertiesScan` or register the properties bean in the application class
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [x] 1.2 Create `EngagementClassifier` pure classification utility
    - Create `src/main/java/com/psybergate/staff_engagement/engagement/EngagementClassifier.java`
    - Implement `static EngagementStatus classify(Integer recency, int atRiskThreshold, int overdueThreshold)` — returns OVERDUE if recency is null or >= overdueThreshold, AT_RISK if >= atRiskThreshold, else ON_TRACK
    - Implement `static boolean needsFollowUp(EngagementStatus status)` — returns `status != ON_TRACK`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3_

  - [x] 1.3 Create `EngagementMatrixEntry` response DTO record
    - Create `src/main/java/com/psybergate/staff_engagement/engagement/dto/EngagementMatrixEntry.java`
    - Define record with fields: `Long employeeId`, `String employeeName`, `String employeeEmail`, `Integer recency`, `int frequency`, `LocalDate lastInteractionDate`, `EngagementStatus engagementStatus`, `boolean followUpRequired`
    - _Requirements: 1.5, 3.4, 6.1_

- [x] 2. Implement data layer and service
  - [x] 2.1 Add aggregate query to `InteractionRepository`
    - Add JPQL method `findInteractionAggregatesByEmployee()` returning `List<Object[]>` with SELECT `i.employee.id, MAX(i.occurredAt), COUNT(i)` grouped by employee id
    - _Requirements: 1.2, 1.3, 6.3_

  - [x] 2.2 Implement `EngagementService`
    - Create `src/main/java/com/psybergate/staff_engagement/engagement/EngagementService.java`
    - Inject `EmployeeRepository`, `InteractionRepository`, `EngagementThresholdProperties`, `Clock`
    - Implement `computeMatrix(LocalDate referenceDate, EngagementStatus statusFilter, String sortOrder)`:
      - Default referenceDate to `LocalDate.now(clock)` if null
      - Load all employees, execute aggregate query, build lookup map
      - For each employee compute recency (days between lastInteractionDate and referenceDate), frequency, lastInteractionDate; handle null (no interactions → recency null, frequency 0, lastInteractionDate null)
      - Classify using `EngagementClassifier.classify()` and derive followUp flag
      - Apply optional status filter
      - Sort by recency descending (nulls first) when sort="recency", else by name case-insensitive ascending
    - Register a `Clock` bean (e.g. `Clock.systemDefaultZone()`) in a config class for production use
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 5.1, 5.2, 5.3, 5.6, 6.1, 6.2, 6.3_

  - [x] 2.3 Write unit tests for `EngagementClassifier` (boundary cases)
    - Create `src/test/java/com/psybergate/staff_engagement/engagement/EngagementClassifierTest.java`
    - Test: `classify(null, 14, 30)` → OVERDUE
    - Test: `classify(30, 14, 30)` → OVERDUE (exactly at overdue threshold)
    - Test: `classify(31, 14, 30)` → OVERDUE (above overdue threshold)
    - Test: `classify(14, 14, 30)` → AT_RISK (exactly at at-risk threshold)
    - Test: `classify(15, 14, 30)` → AT_RISK (between thresholds)
    - Test: `classify(13, 14, 30)` → ON_TRACK (one day below at-risk)
    - Test: `classify(0, 14, 30)` → ON_TRACK (interaction today)
    - Test: `needsFollowUp()` returns true for OVERDUE/AT_RISK, false for ON_TRACK
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.9_

  - [x] 2.4 Write property-based test for `EngagementClassifier`
    - **Property 4: Engagement classification is deterministic and correct**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.6, 7.8**
    - Create `src/test/java/com/psybergate/staff_engagement/engagement/EngagementClassifierPropertyTest.java`
    - Use jqwik `@Property(tries = 200)` with `@ForAll @IntRange` for recency [0,365], atRiskThreshold [1,364], overdueThreshold [2,365]; `Assume.that(atRiskThreshold < overdueThreshold)`
    - Assert: recency >= overdueThreshold → OVERDUE; atRiskThreshold <= recency < overdueThreshold → AT_RISK; recency < atRiskThreshold → ON_TRACK
    - Include null recency test generating random valid thresholds and verifying OVERDUE

  - [x] 2.5 Write property-based test for follow-up flag derivation
    - **Property 5: Follow-up flag is derived from engagement status**
    - **Validates: Requirements 3.1, 3.2, 3.3**
    - Add to `EngagementClassifierPropertyTest.java`
    - Use jqwik to generate random recency + valid thresholds, classify, then verify `needsFollowUp(status) == (status != ON_TRACK)`

- [x] 3. Implement controller and error handling
  - [x] 3.1 Implement `EngagementController`
    - Create `src/main/java/com/psybergate/staff_engagement/engagement/EngagementController.java`
    - `@RestController` with `GET /api/engagement/matrix`
    - Parse optional `status` query param (case-insensitive enum match); throw `IllegalArgumentException` with message listing valid options if invalid
    - Parse optional `sort` query param; throw `IllegalArgumentException` with message listing supported options if value is not "recency"
    - Delegate to `EngagementService.computeMatrix(null, statusFilter, sort)`
    - Return `ResponseEntity.ok(matrix)`
    - _Requirements: 1.1, 1.6, 5.1, 5.4, 5.5_

  - [x] 3.2 Add `DataAccessException` handler to `GlobalExceptionHandler`
    - Add `@ExceptionHandler(DataAccessException.class)` method returning HTTP 500 with message "Unable to compute engagement matrix due to a data access failure"
    - Import `org.springframework.dao.DataAccessException`
    - _Requirements: 1.7_

- [x] 4. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Service and controller tests
  - [x] 5.1 Write unit tests for `EngagementService`
    - Create `src/test/java/com/psybergate/staff_engagement/engagement/EngagementServiceTest.java`
    - Use Mockito to mock `EmployeeRepository`, `InteractionRepository`, `EngagementThresholdProperties`, `Clock`
    - Test: empty employee list → empty result
    - Test: employee with no interactions → recency null, frequency 0, lastInteractionDate null, status OVERDUE, followUpRequired true
    - Test: employee with interactions → correct recency/frequency/lastInteractionDate computation
    - Test: status filter correctly includes/excludes entries
    - Test: sort by recency (nulls first, descending)
    - Test: default sort by name (case-insensitive ascending)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 5.1, 5.2, 5.3_

  - [x] 5.2 Write property-based tests for `EngagementService` logic
    - **Property 1: Matrix contains exactly one entry per employee**
    - **Validates: Requirements 1.1**
    - Create `src/test/java/com/psybergate/staff_engagement/engagement/EngagementServicePropertyTest.java`
    - Generate random employee lists (0–50), random interaction aggregates; verify `result.size() == employees.size()` and each employeeId appears exactly once

  - [x] 5.3 Write property-based tests for sorting invariants
    - **Property 7: Recency sort ordering**
    - **Property 8: Default name sort ordering**
    - **Validates: Requirements 5.2, 5.3**
    - Add to `EngagementServicePropertyTest.java`
    - Property 7: generate random matrix entries with varying recency (including nulls), sort by recency, verify nulls-first then descending order
    - Property 8: generate random employee names, compute matrix with default sort, verify case-insensitive ascending name order

  - [x] 5.4 Write controller slice tests (`@WebMvcTest`)
    - Create `src/test/java/com/psybergate/staff_engagement/engagement/EngagementControllerTest.java`
    - Use `@WebMvcTest(EngagementController.class)` with `@MockBean EngagementService`
    - Use `@WithMockUser` for authenticated context
    - Test: valid request → 200 with JSON array
    - Test: invalid `status` param → 400 with error message listing valid options
    - Test: invalid `sort` param → 400 with error message listing supported options
    - Test: service throws `DataAccessException` → 500 with generic error message
    - _Requirements: 1.6, 5.4, 5.5, 1.7_

- [x] 6. Threshold validation tests
  - [x] 6.1 Write tests for `EngagementThresholdProperties` validation
    - Create `src/test/java/com/psybergate/staff_engagement/engagement/EngagementThresholdPropertiesTest.java`
    - Test: defaults load correctly (overdueDays=30, atRiskDays=14)
    - Test: atRiskDays >= overdueDays causes validation/startup failure
    - Test: out-of-range values (0, -1, 366) cause validation failure
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 7. Integration test
  - [x] 7.1 Write integration test with Testcontainers
    - Create `src/test/java/com/psybergate/staff_engagement/engagement/EngagementIntegrationTest.java`
    - Use `@SpringBootTest` with Testcontainers PostgreSQL and Flyway migrations
    - Override `Clock` bean in test config with a fixed clock for deterministic reference date
    - Seed 3+ employees: one with no interactions, one with recent interaction (within 14 days), one with old interaction (beyond 30 days)
    - Test: `GET /api/engagement/matrix` → 200, verify recency, frequency, engagementStatus, followUpRequired, lastInteractionDate for each seeded employee
    - Test: `GET /api/engagement/matrix?status=OVERDUE` → returns only OVERDUE employees
    - Test: verify response is valid JSON array structure
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 8. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases (especially the boundary conditions from Requirement 7)
- The `Clock` bean injection pattern enables deterministic testing without coupling to system time
- The `EngagementClassifier` is deliberately a pure static utility to maximize testability with jqwik

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.3", "2.4", "2.5"] },
    { "id": 2, "tasks": ["2.2"] },
    { "id": 3, "tasks": ["3.1", "3.2", "5.1", "5.2", "5.3"] },
    { "id": 4, "tasks": ["5.4", "6.1"] },
    { "id": 5, "tasks": ["7.1"] }
  ]
}
```
