# Implementation Plan: Task-Employee Link

## Overview

Add a direct `employee_id` foreign key to the `tasks` table so tasks can belong to an Employee independently of an Interaction. Implementation proceeds bottom-up: database migration → entity → DTO → service → controller → tests.

## Tasks

- [x] 1. Database migration and entity update
  - [x] 1.1 Create Flyway migration `V5__add_task_employee.sql`
    - Create file `staff-engagement-backend/src/main/resources/db/migration/V5__add_task_employee.sql`
    - Add nullable `employee_id BIGINT` column to `tasks` table
    - Add foreign key constraint `fk_tasks_employee` referencing `employees(id)`
    - Add index `idx_tasks_employee_id` on `tasks(employee_id)`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Add Employee relationship to Task entity
    - Add `@ManyToOne(fetch = FetchType.LAZY)` field `employee` of type `Employee` to `Task.java`
    - Map with `@JoinColumn(name = "employee_id")`
    - Add `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` for serialization safety
    - Field must be nullable (no `optional = false`)
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 2. Request/Response DTOs
  - [x] 2.1 Add `employeeId` field to `CreateTaskRequest`
    - Add optional `Long employeeId` parameter to the record in `CreateTaskRequest.java`
    - No validation annotation needed (field is optional)
    - _Requirements: 3.1_

  - [x] 2.2 Create `TaskResponse` record
    - Create `staff-engagement-backend/src/main/java/com/psybergate/staff_engagement/task/dto/TaskResponse.java`
    - Include fields: `id`, `title`, `description`, `status`, `dueDate`, `assignedUserId`, `assignedUserName`, `interactionId`, `employeeId`, `employeeName`, `createdAt`
    - Implement `public static TaskResponse from(Task task)` factory method
    - Handle null employee, assignedUser, and interaction gracefully (return null for their fields)
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 3. Service and controller updates
  - [x] 3.1 Update `TaskService.create()` with employee resolution
    - Inject `EmployeeRepository` into `TaskService`
    - Add employee resolution block: if `employeeId` is non-null, look up Employee or throw `IllegalArgumentException`
    - Set resolved employee on the Task entity before save
    - When `employeeId` is null, leave employee as null (backward compatible)
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 5.1, 5.2_

  - [x] 3.2 Update `TaskController` to return `TaskResponse`
    - Change `GET /api/tasks` return type from `List<Task>` to `List<TaskResponse>`
    - Map each Task to TaskResponse using `TaskResponse::from`
    - Change `POST /api/tasks` return type from `ResponseEntity<Task>` to `ResponseEntity<TaskResponse>`
    - Map saved Task to TaskResponse before returning
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 4. Checkpoint - Verify compilation and existing behavior
  - Ensure the project compiles cleanly with `mvnw clean package -DskipTests`
  - Ensure all existing tests pass with `mvnw test`
  - Ask the user if questions arise.

- [x] 5. Unit tests
  - [x] 5.1 Write unit tests for `TaskService` employee resolution
    - Create `staff-engagement-backend/src/test/java/com/psybergate/staff_engagement/task/TaskServiceTest.java`
    - Use Mockito to mock `TaskRepository`, `EmployeeRepository`, `InteractionRepository`, `UserRepository`
    - Test: valid employeeId resolves and sets employee on task
    - Test: invalid employeeId throws `IllegalArgumentException`
    - Test: both employeeId and interactionId provided resolves both
    - Test: null employeeId with valid interactionId preserves existing behavior
    - Test: null employeeId and null interactionId creates standalone task
    - _Requirements: 3.2, 3.3, 3.4, 3.5, 5.1, 5.2_

  - [x] 5.2 Write unit tests for `TaskController` endpoints
    - Create `staff-engagement-backend/src/test/java/com/psybergate/staff_engagement/task/TaskControllerTest.java`
    - Use `@WebMvcTest(TaskController.class)` with `@MockBean` for service and repository
    - Test: POST with employeeId returns 201 with TaskResponse containing employee details
    - Test: POST with invalid employeeId returns 400
    - Test: GET returns list of TaskResponse with employee fields populated/null
    - Test: POST with blank title returns 400 validation error
    - _Requirements: 3.2, 3.3, 3.6, 4.1, 4.2_

  - [x] 5.3 Write unit tests for `TaskResponse.from()` mapping
    - Test mapping with employee present
    - Test mapping with null employee
    - Test mapping with null assignedUser and null interaction
    - _Requirements: 4.1, 4.2, 4.3_

- [x] 6. Integration tests
  - [x] 6.1 Write integration test for task-employee round-trip
    - Create `staff-engagement-backend/src/test/java/com/psybergate/staff_engagement/task/TaskIntegrationTest.java`
    - Use `@SpringBootTest` with Testcontainers PostgreSQL
    - Test: create task with employeeId via POST, then GET tasks and verify employeeId/employeeName in response
    - Test: verify existing tasks (from seed data) have null employee_id after migration
    - Test: create task with interactionId only (backward compatibility)
    - _Requirements: 1.5, 3.2, 4.1, 5.1, 5.3_

- [x] 7. Property-based tests
  - [x] 7.1 Write property test for employee resolution round-trip
    - **Property 1: Employee resolution round-trip**
    - **Validates: Requirements 2.3, 3.2, 4.1**
    - Generate random valid Employee entities, create tasks referencing them, verify employeeId and employeeName in TaskResponse match

  - [x] 7.2 Write property test for invalid employeeId rejection
    - **Property 2: Invalid employeeId rejection**
    - **Validates: Requirements 3.3**
    - Generate random Long values not in the mock repository, verify IllegalArgumentException thrown and no save() call

  - [x] 7.3 Write property test for TaskResponse mapping correctness
    - **Property 3: TaskResponse mapping correctness**
    - **Validates: Requirements 4.1, 4.2, 4.3**
    - Generate random Task entities (some with employee, some without), map to TaskResponse, verify all fields mapped correctly

  - [x] 7.4 Write property test for backward compatibility (interaction-only tasks)
    - **Property 4: Backward compatibility — interaction-only tasks**
    - **Validates: Requirements 5.1**
    - Generate random valid Interaction entities without employeeId, create tasks, verify interaction linked and employee null

  - [x] 7.5 Write property test for standalone task creation
    - **Property 5: Standalone task creation**
    - **Validates: Requirements 3.4, 5.2**
    - Generate random valid title strings (1-255 chars, non-blank), create minimal tasks, verify OPEN status and null associations

  - [x] 7.6 Write property test for dual association
    - **Property 6: Dual association**
    - **Validates: Requirements 3.5**
    - Generate random valid Employee + Interaction pairs, create tasks with both, verify both associations present

- [x] 8. Final checkpoint - Ensure all tests pass
  - Run `mvnw test` and ensure all unit, integration, and property tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document using jqwik
- Unit tests validate specific examples and edge cases using Mockito / WebMvcTest
- Integration tests use Testcontainers with PostgreSQL for full round-trip verification
- The jqwik dependency must be added to `pom.xml` when implementing property tests (task 7.x)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2"] },
    { "id": 1, "tasks": ["1.2", "3.1"] },
    { "id": 2, "tasks": ["3.2"] },
    { "id": 3, "tasks": ["5.1", "5.2", "5.3"] },
    { "id": 4, "tasks": ["6.1", "7.1", "7.2", "7.3", "7.4", "7.5", "7.6"] }
  ]
}
```
