# Implementation Plan: Task Edit & Delete API

## Overview

Completes the CRUD surface for tasks by adding `PUT /api/tasks/{id}` and `DELETE /api/tasks/{id}` to the existing `Task_Controller`. Implementation proceeds bottom-up: new DTO/exception types → exception handler wiring → service methods → controller endpoints → tests (unit → property → integration).

## Tasks

- [x] 1. Create supporting types
  - [x] 1.1 Create `UpdateTaskRequest` DTO
    - Path: `staff-engagement-backend/src/main/java/com/psybergate/staff_engagement/task/dto/UpdateTaskRequest.java`
    - Record with `@NotBlank @Size(max = 255) String title`, `@Size(max = 2000) String description`, `Long interactionId`, `Long employeeId`, `LocalDate dueDate`, `Long assignedUserId`, `TaskStatus status`
    - `title`/`description` constraints mirror `CreateTaskRequest` exactly
    - _Requirements: 1.2, 1.3, 5.1_

  - [x] 1.2 Create `TaskNotFoundException`
    - Path: `staff-engagement-backend/src/main/java/com/psybergate/staff_engagement/task/TaskNotFoundException.java`
    - Unchecked `RuntimeException` with constructor `TaskNotFoundException(Long id)` producing message `"Task not found with id: " + id`
    - _Requirements: 4.1, 4.2_

- [x] 2. Update `GlobalExceptionHandler`
  - [x] 2.1 Add `TaskNotFoundException` → 404 handler
    - Add `@ExceptionHandler(TaskNotFoundException.class)` `@ResponseStatus(HttpStatus.NOT_FOUND)` method returning `new ErrorResponse(ex.getMessage(), null)`
    - Existing `IllegalArgumentException` → 400 and `MethodArgumentNotValidException` → 400 handlers remain untouched
    - _Requirements: 4.1, 4.2_

  - [x] 2.2 Add `HttpMessageNotReadableException` → 400 handler
    - Add `@ExceptionHandler(HttpMessageNotReadableException.class)` `@ResponseStatus(HttpStatus.BAD_REQUEST)` method returning `new ErrorResponse("Malformed request body", null)`
    - Import `org.springframework.http.converter.HttpMessageNotReadableException`
    - Covers malformed JSON and invalid `status` enum values submitted on `PUT`
    - _Requirements: 2.5_

- [x] 3. Implement `TaskService.update()` and `TaskService.delete()`
  - [x] 3.1 Implement `update(Long id, UpdateTaskRequest request)`
    - Fetch the task via `taskRepository.findById(id)`, throwing `TaskNotFoundException` if absent, before any FK resolution
    - Resolve `interactionId`/`assignedUserId`/`employeeId` when non-null, throwing `IllegalArgumentException` with a descriptive message on an unresolved id (mirrors `create()`'s pattern)
    - Set `title`, `description`, `dueDate`, and unconditionally reassign `interaction`/`assignedUser`/`employee` to the resolved value or `null` (full-replacement/clear semantics)
    - Set `status` to `request.status() != null ? request.status() : task.getStatus()`
    - Save and return the updated task
    - _Requirements: 1.1, 1.4, 1.5, 1.6, 1.7, 1.8, 2.2, 2.3, 2.4, 5.3_

  - [x] 3.2 Implement `delete(Long id)`
    - Fetch the task via `taskRepository.findById(id)`, throwing `TaskNotFoundException` if absent
    - Delete via `taskRepository.delete(task)`
    - _Requirements: 3.1, 4.2_

- [x] 4. Wire `TaskController` endpoints
  - [x] 4.1 Add `PUT /api/tasks/{id}`
    - `@PutMapping("/api/tasks/{id}")` method `updateTask(@PathVariable Long id, @RequestBody @Valid UpdateTaskRequest request)`
    - Calls `taskService.update(id, request)` and returns `ResponseEntity.ok(TaskResponse.from(updatedTask))`
    - _Requirements: 1.1, 6.1, 6.2, 6.3, 6.4_

  - [x] 4.2 Add `DELETE /api/tasks/{id}`
    - `@DeleteMapping("/api/tasks/{id}")` method `deleteTask(@PathVariable Long id)`
    - Calls `taskService.delete(id)` and returns `ResponseEntity.noContent().build()`
    - _Requirements: 3.1, 3.2_

- [x] 5. Checkpoint - Verify compilation and no regressions
  - Run `mvnw clean compile` to confirm the project builds
  - Run `mvnw test` to confirm existing tests (including `GET`/`POST /api/tasks`) still pass unmodified
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Unit tests for `TaskService`
  - [x] 6.1 Write `TaskServiceTest` additions for `update()`
    - Task not found → `TaskNotFoundException`, no repository interaction beyond `findById`
    - Invalid `assignedUserId`/`employeeId`/`interactionId` each throw `IllegalArgumentException` with the correct entity type and id in the message
    - Valid update persists `title`/`description`/`dueDate` and replaces prior associations (including clearing a previously-set association when the request id is null)
    - Omitted `status` retains the task's prior status; explicit `OPEN`/`DONE` overwrites it
    - _Requirements: 1.4, 1.5, 1.6, 1.7, 1.8, 2.2, 2.3, 2.4, 4.1, 5.3_

  - [x] 6.2 Write `TaskServiceTest` additions for `delete()`
    - Task not found → `TaskNotFoundException`
    - Existing task → `taskRepository.delete(task)` is called
    - _Requirements: 3.1, 4.2_

- [x] 7. Unit tests for `TaskController`
  - [x] 7.1 Write `TaskControllerTest` additions for `PUT /api/tasks/{id}`
    - Valid request → 200 with `TaskResponse` body (mirrors existing `createTask_validRequest`)
    - Blank title → 400 with `fieldErrors.title`
    - Service throws `TaskNotFoundException` → 404 with a non-null message
    - Invalid `status` JSON value (e.g. `"CANCELLED"`) → 400
    - _Requirements: 1.1, 2.5, 4.1, 5.2_

  - [x] 7.2 Write `TaskControllerTest` additions for `DELETE /api/tasks/{id}`
    - Existing task → 204 with no body
    - Service throws `TaskNotFoundException` → 404 with a non-null message
    - _Requirements: 3.2, 4.2_

- [x] 8. Checkpoint - Ensure unit tests pass
  - Run `mvnw test` and confirm all `TaskServiceTest` / `TaskControllerTest` cases pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Property-based tests (jqwik) for `TaskService`
  - [x] 9.1 Write property test for update round-trip
    - **Property 1: Update round-trip persists submitted fields**
    - **Validates: Requirements 1.1, 1.8, 6.1**
    - Generate a random existing `Task` (mocked `taskRepository.findById`) and a random valid `UpdateTaskRequest` (title 1-255 chars, description 0-2000 chars, resolvable FK ids where non-null); assert the returned `Task`'s `title`/`description`/`dueDate` match the request and `TaskResponse.from(result)`'s fields match the updated entity

  - [x] 9.2 Write property test for association set/clear
    - **Property 2: Association fields are set or cleared based on request nullability**
    - **Validates: Requirements 1.4, 1.5, 1.6, 1.7, 6.2, 6.3**
    - Generate a random `Task` with each of `assignedUser`/`employee`/`interaction` independently null or present, and a random `UpdateTaskRequest` with each FK id independently null or resolvable; assert each association is non-null and matches the resolved entity when the request id is non-null, and null otherwise, regardless of prior state

  - [x] 9.3 Write property test for status set/retain
    - **Property 3: Status is set from the request or retained when omitted**
    - **Validates: Requirements 2.2, 2.3, 2.4**
    - Generate a random `Task` with a random current `TaskStatus` and a random `UpdateTaskRequest.status` in `{OPEN, DONE, null}`; assert the resulting status equals the request status when non-null, else the prior status

  - [x] 9.4 Write property test for invalid FK rejection
    - **Property 4: Invalid foreign key references are rejected without persisting**
    - **Validates: Requirements 5.3**
    - Generate a random existing `Task` and a random `UpdateTaskRequest` where exactly one of `assignedUserId`/`employeeId`/`interactionId` does not resolve; assert `IllegalArgumentException` is thrown and `taskRepository.save` is never called

  - [x] 9.5 Write property test for not-found → 404 on update and delete
    - **Property 5: Update and delete of a non-existent task return 404**
    - **Validates: Requirements 4.1, 4.2**
    - Generate a random `Long` id absent from a mocked repository; assert both `update` and `delete` throw `TaskNotFoundException`

  - [x] 9.6 Write property test for delete removal
    - **Property 6: Delete removes the task from the repository**
    - **Validates: Requirements 3.1**
    - Generate a random existing `Task` id; call `delete` then assert `taskRepository.findById` returns empty for that id afterward

- [x] 10. Property-based test (jqwik) for controller-level validation precedence
  - [x] 10.1 Write property test for bean validation precedence over FK resolution
    - **Property 7: Bean validation failures take precedence over foreign-key resolution failures**
    - **Validates: Requirements 5.4**
    - Generate random `UpdateTaskRequest` JSON bodies that are simultaneously bean-invalid (blank/256+ char title, or 2001+ char description) and FK-invalid (an unresolvable id for at least one of the three FK fields); `PUT` via MockMvc against `@WebMvcTest` with a mocked `TaskService`; assert 400 with a `fieldErrors` body and that `taskService.update` is never invoked

- [x] 11. Checkpoint - Ensure all property tests pass
  - Run `mvnw test` and confirm all jqwik property tests (tries = 100) pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Integration tests (Testcontainers) — code written; see note on execution below
  - [x] 12.1 Write `TaskIntegrationTest` additions for update happy path
    - `POST` to create a task, `PUT` to update title/description/dueDate/status, `GET` to verify the persisted state
    - _Requirements: 1.1, 1.8, 2.2, 2.3_

  - [x] 12.2 Write `TaskIntegrationTest` additions for association clearing
    - Create a task with an employee, `PUT` with `employeeId: null`, verify the response and a subsequent `GET` show `employeeId: null`
    - _Requirements: 1.7, 6.2_

  - [x] 12.3 Write `TaskIntegrationTest` additions for delete happy path
    - Create a task, `DELETE` it, verify a subsequent `GET /api/tasks` no longer includes it
    - _Requirements: 3.1, 3.2_

  - [x] 12.4 Write `TaskIntegrationTest` additions for 404 handling
    - `PUT`/`DELETE` on a non-existent id against the real database return 404 with a descriptive message
    - _Requirements: 4.1, 4.2_

  - [x] 12.5 Verify regression safety of existing `GET`/`POST` behavior
    - Confirm the existing task list/create integration tests continue to pass unmodified alongside the new update/delete tests
    - _Requirements: 7.1, 7.2_

- [~] 13. Final checkpoint - Full verification
  - Production code compiles (`mvnw clean compile`) ✅
  - Unit + property tests pass: 146/146 project-wide, incl. new `TaskServiceTest` (19), `TaskControllerTest` (14), `TaskEditDeletePropertyTest` (6), `TaskUpdateValidationPropertyTest` (1) ✅
  - Integration tests (`TaskIntegrationTest`) written but **could not be executed in this environment**: the JDK `HttpClient` used by Spring's `TestRestTemplate` fails with `java.io.IOException: Unable to establish loopback connection`, which fails the entire test class at ApplicationContext load — including pre-existing tests untouched by this change. This is a local machine networking restriction (loopback socket blocked, e.g. firewall/AV), not a code issue. Docker is available. Needs a re-run on an environment that allows loopback connections (e.g. CI).
  - Note: JaCoCo instrumentation errors on this JDK (`Unsupported class file major version 70`) are non-fatal noise; runs used `-Djacoco.skip=true`.

## Notes

- Each task references specific requirements for traceability.
- Checkpoints ensure incremental validation before moving to the next layer of tests.
- Property tests validate the seven universal correctness properties from `design.md` using jqwik (already a test dependency at version 1.9.2, no `pom.xml` changes needed).
- Unit tests validate specific examples, edge cases, and error conditions using Mockito / `@WebMvcTest`.
- Integration tests use Testcontainers with PostgreSQL for full round-trip verification against a real database.
- No schema, entity, or `CreateTaskRequest`/`TaskResponse` changes are required — see design.md's Data Models section.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["4.1", "4.2"] },
    { "id": 4, "tasks": ["5"] },
    { "id": 5, "tasks": ["6.1", "6.2", "7.1", "7.2"] },
    { "id": 6, "tasks": ["8"] },
    { "id": 7, "tasks": ["9.1", "9.2", "9.3", "9.4", "9.5", "9.6", "10.1"] },
    { "id": 8, "tasks": ["11"] },
    { "id": 9, "tasks": ["12.1", "12.2", "12.3", "12.4", "12.5"] },
    { "id": 10, "tasks": ["13"] }
  ]
}
```
