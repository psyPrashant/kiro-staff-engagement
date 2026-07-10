# Implementation Plan: Employee 360 View

## Overview

This plan implements the Employee 360 View as a full-stack vertical slice. The backend exposes a single aggregate read endpoint (`GET /api/employees/{id}/360`) assembling profile, interaction history, and open tasks into one response. The Angular frontend renders the consolidated view with profile header, interaction timeline, and task list. Acceptance tests verify end-to-end correctness through the UI.

## Tasks

- [x] 1. Backend: Create employee360 package with DTOs and exception handling
  - [x] 1.1 Create response DTOs and exception class
    - Create package `com.psybergate.staff_engagement.employee360`
    - Create Java records: `Employee360Response`, `ProfileDto`, `InteractionDto`, `ProjectContextDto`, `TaskDto`
    - Create `Employee360NotFoundException` with `@ResponseStatus(HttpStatus.NOT_FOUND)`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Backend: Add repository query methods
  - [x] 2.1 Add query methods to InteractionRepository and TaskRepository
    - Add `findByEmployeeIdOrderByOccurredAtDesc(Long employeeId)` to `InteractionRepository`
    - Add `findByInteractionIdInAndStatus(List<Long> interactionIds, TaskStatus status)` to `TaskRepository`
    - _Requirements: 1.2, 1.3_

- [x] 3. Backend: Implement Employee360Service
  - [x] 3.1 Create Employee360Service with DTO assembly logic
    - Implement `getEmployee360(Long employeeId)` method
    - Fetch employee or throw `Employee360NotFoundException`
    - Fetch interactions ordered by occurredAt descending
    - Fetch open tasks filtered by OPEN status for the employee's interaction IDs
    - Assemble `Employee360Response` from entities, mapping profile fields, interaction fields with project context, and task fields
    - Use `@Transactional(readOnly = true)` for the read operation
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6_

  - [x]* 3.2 Write property test: Profile mapping preserves all employee fields
    - **Property 1: Profile mapping preserves all employee fields**
    - Use jqwik to generate arbitrary Employee entities (with/without manager)
    - Assert ProfileDto contains same id, name, email, jobTitle, and manager name (or null)
    - **Validates: Requirements 1.1**

  - [x]* 3.3 Write property test: Interaction ordering is descending by occurredAt
    - **Property 2: Interaction ordering is descending by occurredAt**
    - Use jqwik to generate lists of Interactions with random occurredAt values
    - Assert returned InteractionDto list satisfies `interactions[i].occurredAt >= interactions[i+1].occurredAt` for all consecutive pairs
    - **Validates: Requirements 1.2, 2.2**

  - [x]* 3.4 Write property test: Only OPEN tasks are included
    - **Property 3: Only OPEN tasks are included**
    - Use jqwik to generate tasks with mixed statuses (OPEN, DONE)
    - Assert response openTasks list contains only tasks where status == OPEN
    - **Validates: Requirements 1.3**

  - [x]* 3.5 Write property test: Project context is present iff interaction has a project
    - **Property 4: Project context is present if and only if the interaction has a project**
    - Use jqwik to generate interactions with/without project references
    - Assert projectContext is non-null iff source interaction has a non-null project, and names match
    - **Validates: Requirements 1.4, 2.3**

- [x] 4. Backend: Implement Employee360Controller
  - [x] 4.1 Create Employee360Controller with GET endpoint
    - Create `@RestController` with `@GetMapping("/api/employees/{id}/360")`
    - Delegate to `Employee360Service.getEmployee360(id)`
    - Return `ResponseEntity.ok(response)`
    - Rely on Spring's type conversion for invalid ID format (HTTP 400)
    - _Requirements: 1.1, 1.5, 1.7, 7.1, 7.2_

  - [x]* 4.2 Write unit tests for Employee360Controller
    - Use `@WebMvcTest` with mocked `Employee360Service`
    - Test HTTP 200 for valid employee ID
    - Test HTTP 404 when `Employee360NotFoundException` is thrown
    - Test HTTP 400 for non-numeric ID path variable
    - Test HTTP 401 for unauthenticated requests
    - _Requirements: 1.1, 1.5, 1.7, 7.1, 7.2_

  - [x]* 4.3 Write unit tests for Employee360Service
    - Use Mockito to mock repositories
    - Test DTO assembly with full data (employee + manager + interactions + tasks + projects)
    - Test empty interactions case returns empty lists
    - Test null manager maps to null managerName
    - Test null project maps to null projectContext
    - Test null due dates on tasks
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6_

- [x] 5. Checkpoint - Backend complete
  - Ensure all backend tests pass, ask the user if questions arise.

- [x] 6. Frontend: Create TypeScript models and service
  - [x] 6.1 Create Employee 360 TypeScript interfaces and service
    - Create `src/app/employee/models/employee-360.model.ts` with interfaces: `Employee360Response`, `ProfileDto`, `InteractionDto`, `ProjectContextDto`, `TaskDto`
    - Create `src/app/employee/services/employee-360.service.ts` with `getEmployee360(id: number): Observable<Employee360Response>` calling `GET /api/employees/${id}/360`
    - _Requirements: 1.1, 5.1_

- [x] 7. Frontend: Implement Employee360Component
  - [x] 7.1 Create Employee360Component with signal-based state
    - Create standalone component at `src/app/employee/employee-360/`
    - Implement signals: `loading`, `error`, `data`
    - Extract employee ID from route params on init
    - Call `Employee360Service.getEmployee360(id)` and manage loading/error states
    - Implement `retry()` method for re-fetching
    - Implement `isOverdue(dueDate)` helper returning true if dueDate is non-null and before today
    - Implement `truncateNotes(notes, maxLength=200)` helper
    - Sort open tasks by dueDate ascending (nulls last) for display
    - Apply RxJS `timeout(30000)` to the HTTP request
    - Map error responses to user-friendly messages (404 → "Employee not found", 5xx → generic error, timeout → timeout message)
    - _Requirements: 2.1, 2.2, 3.1, 3.3, 3.4, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 7.2 Create Employee360Component template and styles
    - Render profile summary section with `data-testid="profile-summary"` and `data-testid="employee-name"`
    - Render interaction history section with `data-testid="interaction-history"`
    - Render open tasks section with `data-testid="open-tasks"`
    - Render empty-state messages with `data-testid="empty-interactions"` and `data-testid="empty-tasks"`
    - Apply `.overdue` CSS class on task rows where `isOverdue(task.dueDate)` is true (with `data-testid="task-row"`)
    - Show loading indicator while fetching
    - Show error message with retry button on failure
    - Display project context alongside interactions when present
    - Omit manager field if null (no blank/null display)
    - Display placeholder for missing required profile fields
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 5.2, 5.3_

  - [x]* 7.3 Write property test: Notes truncation preserves short strings and clips long ones
    - **Property 5: Notes truncation preserves short strings and clips long ones**
    - Use fast-check to generate arbitrary strings
    - Assert: strings ≤ 200 chars returned unchanged; strings > 200 chars return exactly 201 chars (200 + ellipsis) matching original prefix
    - **Validates: Requirements 2.1**

  - [x]* 7.4 Write property test: Tasks ordered by due date ascending with nulls last
    - **Property 6: Tasks are ordered by due date ascending with nulls last**
    - Use fast-check to generate lists of tasks with mixed null/non-null due dates
    - Assert: all non-null dueDate tasks appear before null dueDate tasks, and non-null tasks are in ascending order
    - **Validates: Requirements 3.1**

  - [x]* 7.5 Write property test: Overdue classification correctness
    - **Property 7: Overdue classification correctness**
    - Use fast-check to generate arbitrary dates (past, today, future, null)
    - Assert: `isOverdue(dueDate)` returns true iff dueDate is non-null AND dueDate < today; returns false if dueDate is null
    - **Validates: Requirements 3.3, 3.4**

- [x] 8. Frontend: Update routing to include Employee 360 view
  - [x] 8.1 Register Employee360Component route
    - Update `src/app/employee/employee.routes.ts` to add `{ path: ':id', component: Employee360Component }` route
    - Ensure route is within the `authGuard`-protected shell (already handled by parent route in `app.routes.ts`)
    - _Requirements: 5.1, 7.3_

  - [x]* 8.2 Write Vitest unit tests for Employee360Component
    - Test component renders profile summary when data loads
    - Test loading indicator shown while fetching
    - Test error message displayed on API failure
    - Test retry button re-fetches data
    - Test overdue tasks have `.overdue` class applied
    - Test notes truncation in template
    - Test empty-state messages for no interactions and no tasks
    - _Requirements: 2.1, 2.4, 3.2, 3.3, 4.1, 5.2, 5.3_

- [x] 9. Checkpoint - Frontend complete
  - Ensure all frontend tests pass, ask the user if questions arise.

- [x] 10. Acceptance Tests: Create Cucumber feature file and seed data
  - [x] 10.1 Create Gherkin feature file for Employee 360 View
    - Create `acceptance-tests/src/test/resources/features/employee360/employee_360_view.feature`
    - Write scenario: authenticated user navigates to employee 360 and sees profile summary, interaction history, and open tasks
    - Write scenario: employee with no interactions shows empty-state messages for interaction history and open tasks
    - Write scenario: overdue tasks are visually distinguished from non-overdue tasks
    - Write scenario: unauthenticated user is redirected to login
    - Use `@employee360` tag on feature
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.7_

  - [x] 10.2 Create seed data SQL script for Employee 360 acceptance tests
    - Create SQL fixture at `acceptance-tests/src/test/resources/fixtures/employee360_seed.sql`
    - Seed test employee, manager, users, company, project, interactions (various types, different dates), tasks (OPEN with past/future due dates, DONE)
    - Ensure data supports all scenarios (full data, empty interactions, overdue tasks)
    - _Requirements: 6.6_

- [x] 11. Acceptance Tests: Create page object and domain layer
  - [x] 11.1 Create Employee360Page page object
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/drivers/ui/pages/Employee360Page.java`
    - Extend `BasePage`
    - Add `open(Long employeeId)` navigating to `/employee/{id}`
    - Add `isProfileSummaryVisible()`, `getEmployeeName()`, `isInteractionHistoryVisible()`, `isOpenTasksVisible()`
    - Add `isEmptyInteractionsMessageVisible()`, `isEmptyTasksMessageVisible()`
    - Add `hasOverdueTaskStyling()` checking for `.overdue` class on task rows
    - Add `waitForProfileLoaded()` for waiting until data renders
    - Use `data-testid` selectors for stability
    - _Requirements: 6.5_

  - [x] 11.2 Create Employee360Actor domain actor
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/employee360/Employee360Actor.java`
    - Inject `Employee360Page` and `TestWorld`
    - Add `navigateToEmployee360(Long employeeId)` that opens the page and stores the employee ID in TestWorld
    - Use `@Component` and `@ScenarioScope` annotations
    - _Requirements: 6.5_

  - [x] 11.3 Create Employee360Assertions domain assertions
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/employee360/Employee360Assertions.java`
    - Inject `Employee360Page`
    - Add assertion methods: `assertProfileSummaryIsVisible()`, `assertInteractionHistoryIsVisible()`, `assertOpenTasksAreVisible()`
    - Add `assertEmptyInteractionsMessageShown()`, `assertEmptyTasksMessageShown()`
    - Add `assertOverdueTasksAreDistinguished()`
    - Use `@Component` and `@ScenarioScope` annotations
    - _Requirements: 6.5_

- [x] 12. Acceptance Tests: Create step definitions and wire everything together
  - [x] 12.1 Create Employee360StepDefinitions
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/stepdefs/employee360/Employee360StepDefinitions.java`
    - Inject `LoginActor`, `Employee360Actor`, `Employee360Assertions`, `SeedDataApiDriver`, `TestWorld`
    - Implement Given steps for seeding test data and authenticating
    - Implement When steps for navigating to the employee 360 view
    - Implement Then steps for asserting profile, interactions, tasks, empty states, and overdue styling
    - Use `LoginActor` to authenticate before navigation where scenarios require it
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [x] 13. Final checkpoint - Ensure all tests pass
  - Ensure all backend tests, frontend tests, and acceptance tests pass. Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- Backend uses Java 21 + Spring Boot 3.5 + jqwik for PBT
- Frontend uses TypeScript + Angular 21 + Vitest + fast-check for PBT
- Acceptance tests use Cucumber + Playwright + Spring four-layer architecture
- The `data-testid` attributes in the frontend template are critical for acceptance test stability

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "6.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["3.1"] },
    { "id": 3, "tasks": ["3.2", "3.3", "3.4", "3.5", "4.1"] },
    { "id": 4, "tasks": ["4.2", "4.3"] },
    { "id": 5, "tasks": ["7.1"] },
    { "id": 6, "tasks": ["7.2", "7.3", "7.4", "7.5"] },
    { "id": 7, "tasks": ["8.1"] },
    { "id": 8, "tasks": ["8.2"] },
    { "id": 9, "tasks": ["10.1", "10.2"] },
    { "id": 10, "tasks": ["11.1", "11.2", "11.3"] },
    { "id": 11, "tasks": ["12.1"] }
  ]
}
```
