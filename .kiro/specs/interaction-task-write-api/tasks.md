# Implementation Plan: Interaction & Task Write API

## Overview

Introduces service-layer architecture, DTOs with Jakarta Bean Validation, and global exception handling to support POST endpoints for interactions and tasks, plus a company filter on projects. Implementation proceeds bottom-up: error infrastructure first, then services, then controller updates, then tests.

## Tasks

- [x] 1. Create error handling infrastructure
  - [x] 1.1 Create `ErrorResponse` record in `common.exception` package
    - Path: `src/main/java/com/psybergate/staff_engagement/common/exception/ErrorResponse.java`
    - Record with `String message` and `Map<String, String> fieldErrors`
    - _Requirements: 6.1, 6.2_
  - [x] 1.2 Create `GlobalExceptionHandler` with `@RestControllerAdvice`
    - Path: `src/main/java/com/psybergate/staff_engagement/common/exception/GlobalExceptionHandler.java`
    - Handle `MethodArgumentNotValidException` → 400 with fieldErrors map
    - Handle `IllegalArgumentException` → 400 with message only
    - _Requirements: 5.3, 6.1, 6.2_

- [x] 2. Implement InteractionService and DTO
  - [x] 2.1 Create `CreateInteractionRequest` record in `interaction.dto` package
    - Path: `src/main/java/com/psybergate/staff_engagement/interaction/dto/CreateInteractionRequest.java`
    - Fields: `@NotNull Long employeeId`, `@NotNull Long conductedByUserId`, `@NotNull Long loggedByUserId`, `@NotNull InteractionType type`, `@NotBlank String notes`, `@NotNull Instant occurredAt`, `Long projectId` (nullable)
    - _Requirements: 1.2, 1.3_
  - [x] 2.2 Create `InteractionService` with `@Service` annotation
    - Path: `src/main/java/com/psybergate/staff_engagement/interaction/InteractionService.java`
    - Inject `InteractionRepository`, `EmployeeRepository`, `UserRepository`, `ProjectRepository`
    - `create(CreateInteractionRequest)` method: validate FK references, map DTO to entity, save and return
    - Throw `IllegalArgumentException` with descriptive message for missing FK references
    - _Requirements: 1.1, 1.4, 1.5, 1.6, 1.7, 5.1_

- [x] 3. Update InteractionController with POST endpoint
  - Inject `InteractionService` (replace direct repository usage for create)
  - Keep existing `GET /api/interactions` (still uses repository directly)
  - Add `POST /api/interactions` with `@RequestBody @Valid CreateInteractionRequest`
  - Return `ResponseEntity.status(201).body(savedInteraction)`
  - _Requirements: 1.1, 1.8, 1.9_

- [x] 4. Implement TaskService and DTO
  - [x] 4.1 Create `CreateTaskRequest` record in `task.dto` package
    - Path: `src/main/java/com/psybergate/staff_engagement/task/dto/CreateTaskRequest.java`
    - Fields: `@NotBlank @Size(max=255) String title`, `@Size(max=2000) String description`, `Long interactionId`, `LocalDate dueDate`, `Long assignedUserId`
    - _Requirements: 2.2, 2.3_
  - [x] 4.2 Create `TaskService` with `@Service` annotation
    - Path: `src/main/java/com/psybergate/staff_engagement/task/TaskService.java`
    - Inject `TaskRepository`, `InteractionRepository`, `UserRepository`
    - `create(CreateTaskRequest)` method: validate optional FK references, set status=OPEN, save and return
    - Throw `IllegalArgumentException` for missing FK references
    - _Requirements: 2.1, 2.4, 2.5, 2.6, 5.2_

- [x] 5. Update TaskController with POST endpoint
  - Inject `TaskService` (replace direct repository usage for create)
  - Keep existing `GET /api/tasks`
  - Add `POST /api/tasks` with `@RequestBody @Valid CreateTaskRequest`
  - Return `ResponseEntity.status(201).body(savedTask)`
  - _Requirements: 2.1, 2.7, 2.8_

- [x] 6. Update ProjectController and ProjectRepository for company filter
  - [x] 6.1 Add `findByCompanyId(Long companyId)` to `ProjectRepository`
    - Spring Data derived query method returning `List<Project>`
    - _Requirements: 3.1_
  - [x] 6.2 Update `ProjectController.getProjects()` to accept optional `@RequestParam companyId`
    - If companyId is present, use `projectRepository.findByCompanyId(companyId)`
    - If absent, use `projectRepository.findAll()`
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 7. Checkpoint
  - Ensure all code compiles (`mvnw compile` passes), ask the user if questions arise.

- [x] 8. Unit tests — Controller layer
  - [x] 8.1 Write `InteractionController` POST tests (`@WebMvcTest`)
    - Mock `InteractionService`
    - Test: valid request → 201 with body
    - Test: bean validation failure (null fields) → 400 with fieldErrors
    - Test: service throws `IllegalArgumentException` → 400 with message
    - _Requirements: 1.1, 1.8_
  - [x] 8.2 Write `TaskController` POST tests (`@WebMvcTest`)
    - Mock `TaskService`
    - Test: valid request → 201 with body
    - Test: blank title → 400 with fieldErrors
    - Test: title >255 chars → 400 with fieldErrors
    - Test: service throws `IllegalArgumentException` → 400 with message
    - _Requirements: 2.1, 2.7_
  - [x] 8.3 Write `ProjectController` filter tests (`@WebMvcTest`)
    - Mock `ProjectRepository`
    - Test: no companyId → returns all (calls findAll)
    - Test: with companyId → returns filtered (calls findByCompanyId)
    - _Requirements: 3.1, 3.2_

- [x] 9. Unit tests — Service layer
  - [x] 9.1 Write `InteractionService` tests (JUnit + Mockito)
    - Mock all repositories
    - Test: valid request → entity saved with correct field mapping
    - Test: non-existent employeeId → throws IllegalArgumentException
    - Test: non-existent conductedByUserId → throws IllegalArgumentException
    - Test: non-existent loggedByUserId → throws IllegalArgumentException
    - Test: non-existent projectId → throws IllegalArgumentException
    - Test: null projectId → project is null, save succeeds
    - _Requirements: 1.1, 1.4, 1.5, 1.6, 1.7, 5.1_
  - [x] 9.2 Write `TaskService` tests (JUnit + Mockito)
    - Mock all repositories
    - Test: valid request → entity saved with status=OPEN
    - Test: non-existent interactionId → throws IllegalArgumentException
    - Test: non-existent assignedUserId → throws IllegalArgumentException
    - Test: null interactionId and assignedUserId → save succeeds
    - _Requirements: 2.1, 2.4, 2.5, 2.6, 5.2_

- [x] 10. Checkpoint
  - Ensure all tests pass (`mvnw test`), ask the user if questions arise.

- [x]* 11. Integration tests (Testcontainers)
  - [x]* 11.1 Write `InteractionIntegrationTest` extending `BaseIntegrationTest`
    - Use `TestRestTemplate` to POST valid interaction, verify 201 and persisted data
    - POST with non-existent employee, verify 400
    - _Requirements: 1.1, 1.4_
  - [x]* 11.2 Write `TaskIntegrationTest` extending `BaseIntegrationTest`
    - POST valid task, verify 201 with status=OPEN
    - POST with non-existent interactionId, verify 400
    - _Requirements: 2.1, 2.4, 2.6_
  - [x]* 11.3 Write `ProjectFilterIntegrationTest` extending `BaseIntegrationTest`
    - Seed projects for multiple companies, GET with companyId filter, verify only matching returned
    - GET without companyId, verify all returned
    - _Requirements: 3.1, 3.2, 3.3_

- [ ]* 12. Property-based tests (jqwik)
  - [ ]* 12.1 Write property test: Interaction bean validation rejects invalid requests
    - **Property 2: Interaction bean validation rejects invalid requests**
    - Generate requests with random null/blank required fields, verify 400 + fieldErrors contains the invalid field
    - **Validates: Requirements 1.2, 1.8**
  - [ ]* 12.2 Write property test: Task bean validation rejects invalid requests
    - **Property 4: Task bean validation rejects invalid requests**
    - Generate blank/oversized titles (whitespace strings, strings >255 chars), verify 400 + fieldErrors contains "title"
    - **Validates: Requirements 2.2, 2.7**
  - [ ]* 12.3 Write property test: Company filter returns only matching projects
    - **Property 5: Company filter returns only matching projects**
    - Seed random companies and projects, filter by companyId, verify all returned projects have matching company.id
    - **Validates: Requirements 3.1**

- [x] 13. Final checkpoint
  - Ensure all tests pass (`mvnw test`), ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP.
- The existing `GET` endpoints remain unchanged (still backed by direct repository calls).
- No Flyway migrations are needed — the schema already supports all entities and relationships.
- jqwik (already in pom.xml) is used for property-based tests.
- Each task references specific requirements for traceability.
- Checkpoints ensure incremental validation.
