# Implementation Plan: Next Scheduled Interaction

## Overview

This plan implements the next-scheduled-interaction feature by building up from the data layer (repository queries), through the service layer (NextScheduledInteractionService), to the API layer (enriching Employee360Response and EmployeeController). Testing is layered: property-based tests validate correctness properties, integration tests verify against a real database, and Cucumber acceptance tests confirm end-to-end behaviour.

## Tasks

- [x] 1. Add repository query methods and NextScheduledDto
  - [x] 1.1 Create NextScheduledDto record in the scheduling package
    - Create `NextScheduledDto.java` in `com.psybergate.staff_engagement.scheduling`
    - Simple record with fields: `String scheduledAt`, `String type`
    - No Jackson annotations needed — default serialization is sufficient
    - _Requirements: 2.1_

  - [x] 1.2 Add single-employee JPQL query to ScheduledInteractionRepository
    - Add `findNextPendingByEmployeeId(Long employeeId, LocalDate referenceDate)` returning `Optional<ScheduledInteraction>`
    - JPQL filters: `scheduledDate >= referenceDate`, `completionStatus = PENDING`
    - Order by `scheduledDate ASC, id ASC`, limit 1
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 1.3 Add batch JPQL query to ScheduledInteractionRepository
    - Add `findNextPendingByEmployeeIds(List<Long> employeeIds, LocalDate referenceDate)` returning `List<ScheduledInteraction>`
    - Uses correlated subquery to find MIN scheduledDate per employee
    - Order by `employee.id ASC, id ASC` for deterministic tiebreaker
    - _Requirements: 2.6, 4.4_

- [x] 2. Implement NextScheduledInteractionService
  - [x] 2.1 Create NextScheduledInteractionService with single-employee method
    - Create `NextScheduledInteractionService.java` in `com.psybergate.staff_engagement.scheduling`
    - Inject `ScheduledInteractionRepository` and `Clock`
    - Implement `getNextScheduled(Long employeeId)` returning `NextScheduledDto` or null
    - Validate null employeeId → throw `IllegalArgumentException`
    - Use `LocalDate.now(clock)` as reference date
    - Map entity to DTO: `scheduledDate.toString()` for scheduledAt, `interactionType.name()` for type
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 1.5_

  - [x] 2.2 Add batch method to NextScheduledInteractionService
    - Implement `getNextScheduledBatch(List<Long> employeeIds)` returning `Map<Long, NextScheduledDto>`
    - Validate: null list → `IllegalArgumentException`, empty list → return `Map.of()`, size > 200 → `IllegalArgumentException`
    - Call batch repository query, group results by employee, take first per employee (ordering guarantees lowest ID tiebreaker)
    - _Requirements: 2.6, 2.7_

  - [x] 2.3 Write property test: Repository returns earliest qualifying interaction (Property 1)
    - **Property 1: Repository returns the earliest qualifying interaction**
    - Use jqwik to generate random sets of ScheduledInteraction data with varying dates, statuses, and IDs
    - Verify the repository query selects the correct interaction (earliest PENDING date >= referenceDate, lowest ID tiebreaker)
    - Configure `@Property(tries = 100)`
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4**

  - [x] 2.4 Write property test: Service DTO mapping round-trip (Property 2)
    - **Property 2: Service DTO mapping round-trip**
    - Use jqwik to generate random ScheduledInteraction entities with valid dates and InteractionTypes
    - Verify `toDto()` produces `scheduledAt == scheduledDate.toString()` and `type == interactionType.name()`
    - Verify null is returned when repository returns empty
    - Configure `@Property(tries = 100)`
    - **Validates: Requirements 2.1, 2.4, 2.5**

  - [x] 2.5 Write property test: Batch consistency with single method (Property 3)
    - **Property 3: Batch method consistency with single method**
    - Use jqwik to generate random lists of employee IDs (1–200) and verify `getNextScheduledBatch(ids).get(id)` equals `getNextScheduled(id)` for each ID
    - Configure `@Property(tries = 100)`
    - **Validates: Requirements 2.6, 2.7**

- [x] 3. Enrich Employee360Response with nextScheduled field
  - [x] 3.1 Modify Employee360Response record to include nextScheduled
    - Add `NextScheduledDto nextScheduled` as the fourth field in the record (nullable)
    - Import `NextScheduledDto` from scheduling package
    - _Requirements: 3.1_

  - [x] 3.2 Update Employee360Service to inject and call NextScheduledInteractionService
    - Add `NextScheduledInteractionService` as a dependency
    - In `getEmployee360()`, call `nextScheduledInteractionService.getNextScheduled(employeeId)`
    - Pass the result to `buildResponse()` as a fourth parameter
    - Update `buildResponse()` signature to accept `NextScheduledDto nextScheduled`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 4. Checkpoint - Ensure 360 enrichment compiles and basic tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Refactor EmployeeController to return EmployeeListDto
  - [x] 5.1 Create EmployeeListDto record in the employee package
    - Create `EmployeeListDto.java` in `com.psybergate.staff_engagement.employee`
    - Fields: `Long id`, `String name`, `String email`, `String jobTitle`, `String managerName`, `NextScheduledDto nextScheduled`
    - Import `NextScheduledDto` from scheduling package
    - _Requirements: 4.5_

  - [x] 5.2 Refactor EmployeeController to use NextScheduledInteractionService and return List<EmployeeListDto>
    - Inject `NextScheduledInteractionService`
    - In `getAllEmployees()`: fetch all employees, extract IDs, call `getNextScheduledBatch(ids)`
    - Map each Employee + its NextScheduledDto into an `EmployeeListDto`
    - Return `List<EmployeeListDto>` instead of `List<Employee>`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 6. Checkpoint - Ensure employees list endpoint compiles and all existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Integration tests with Testcontainers
  - [x] 7.1 Create NextScheduledInteractionIntegrationTest class
    - Extend the existing `BaseIntegrationTest` (Testcontainers PostgreSQL + Flyway)
    - Seed test data: at least one Employee, one User, and multiple ScheduledInteractions with varying dates and statuses
    - Test: employee with multiple future PENDING interactions → 360 response returns soonest one with correct `scheduledAt` and `type`
    - Test: employee with only past interactions or only COMPLETED/CANCELLED → `nextScheduled` is null
    - Test: employee with no scheduled interactions at all → `nextScheduled` is null
    - Test: employees list endpoint includes correct `nextScheduled` per employee
    - Test: creating a new PENDING interaction with earlier date updates the 360 response
    - Use `@AutoConfigureMockMvc` and authenticate via test helpers
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 8. Cucumber acceptance tests (four-layer architecture)
  - [x] 8.1 Create NextScheduledApiDriver extending BaseApiDriver
    - Create `NextScheduledApiDriver.java` in `acceptance/drivers/api/`
    - Methods: schedule a future interaction (POST), get employee 360 (GET), get employees list (GET)
    - Reuse authentication patterns from existing API drivers
    - _Requirements: 6.6, 6.8_

  - [x] 8.2 Create domain actors and assertions for next-scheduled
    - Create `NextScheduledActor.java` in `acceptance/domain/scheduling/`
    - Create `NextScheduledAssertions.java` in `acceptance/domain/scheduling/`
    - Actor wraps API driver calls with domain-level language
    - Assertions verify `nextScheduled` field values in responses
    - _Requirements: 6.6_

  - [x] 8.3 Create next_scheduled_interaction.feature file with Gherkin scenarios
    - Create feature file in `acceptance-tests/src/test/resources/features/scheduling/`
    - Tag with `@next-scheduled`
    - Scenario Outline: for interaction types CHECK_IN, MENTORING, CATCH_UP, OTHER — schedule future interaction, verify 360 response
    - Scenario: no pending interactions → `nextScheduled` is null
    - Scenario: only past-dated pending interactions → `nextScheduled` is null
    - Scenario: employees list returns correct `nextScheduled` per employee
    - Scenario: scheduling a nearer future interaction updates `nextScheduled`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.7_

  - [x] 8.4 Create NextScheduledStepDefinitions binding Gherkin to domain actors
    - Create `NextScheduledStepDefinitions.java` in `acceptance/stepdefs/scheduling/`
    - Inject `TestWorld` for scenario state sharing
    - Wire Given/When/Then steps to `NextScheduledActor` and `NextScheduledAssertions`
    - Establish prerequisite data via API calls (POST scheduled interactions)
    - _Requirements: 6.6, 6.8_

- [x] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- The design already specifies that `ClockConfig.java` exists with a `Clock` bean — no need to create it
- Integration tests extend `BaseIntegrationTest` which provides Testcontainers PostgreSQL + Flyway setup
- Cucumber tests follow the four-layer architecture established by existing acceptance tests

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "5.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "3.1"] },
    { "id": 3, "tasks": ["2.5", "3.2", "5.2"] },
    { "id": 4, "tasks": ["7.1", "8.1"] },
    { "id": 5, "tasks": ["8.2"] },
    { "id": 6, "tasks": ["8.3", "8.4"] }
  ]
}
```
