# Implementation Plan: Log Interaction Frontend

## Overview

Implement the "Log Interaction" page as an Angular 21 standalone component that replaces the existing placeholder. The work is broken into incremental steps: models/enums first, then services, then the main component with form logic, then validation and submission handling, inline task section, and finally tests. Each step builds on the previous and ends with integrated, working code.

## Tasks

- [x] 1. Create models, enums, and validators
  - [x] 1.1 Create interaction models and InteractionType enum
    - Create `src/app/interaction/models/interaction.model.ts` with `CreateInteractionRequest`, `InteractionResponse`, and `ApiErrorResponse` interfaces
    - Create `src/app/interaction/models/interaction-type.enum.ts` with the `InteractionType` enum, `INTERACTION_TYPES` array, and `formatInteractionTypeLabel` utility function
    - _Requirements: 8.1, 8.2_

  - [x] 1.2 Create task models
    - Create `src/app/task/models/task.model.ts` with `CreateTaskRequest` and `TaskResponse` interfaces
    - _Requirements: 4.1, 4.3_

  - [x] 1.3 Create shared models (Employee, Project)
    - Create `src/app/shared/models/employee.model.ts` with `Employee` interface (id, name, email, jobTitle)
    - Create `src/app/shared/models/project.model.ts` with `Project` interface (id, name)
    - _Requirements: 6.1, 6.3_

  - [x] 1.4 Create custom validators
    - Create `src/app/interaction/validators/not-blank.validator.ts` with `notBlankValidator` (rejects empty/whitespace-only strings) and `futureDateValidator` (rejects dates before today)
    - _Requirements: 2.3, 4.8_

- [x] 2. Create HTTP services
  - [x] 2.1 Create InteractionService
    - Create `src/app/interaction/services/interaction.service.ts` with `create(request)` method that POSTs to `/api/interactions`
    - _Requirements: 3.1_

  - [x] 2.2 Create TaskService
    - Create `src/app/task/services/task.service.ts` with `create(request)` method that POSTs to `/api/tasks`
    - _Requirements: 4.3_

  - [x] 2.3 Create EmployeeService
    - Create `src/app/shared/services/employee.service.ts` with `getAll()` method that GETs `/api/employees`
    - _Requirements: 6.1_

  - [x] 2.4 Create UserService
    - Create `src/app/shared/services/user.service.ts` with `getAll()` method that GETs `/api/users`
    - _Requirements: 6.2_

  - [x] 2.5 Create ProjectService
    - Create `src/app/shared/services/project.service.ts` with `getAll()` method that GETs `/api/projects`
    - _Requirements: 6.3_

- [x] 3. Implement the LogInteractionComponent (core form)
  - [x] 3.1 Create LogInteractionComponent with reactive form
    - Create `src/app/interaction/log-interaction.component.ts` as a standalone component
    - Inject all services (AuthService, InteractionService, TaskService, EmployeeService, UserService, ProjectService)
    - Build the reactive form in `ngOnInit` with controls: employeeId, conductedByUserId (defaulting to currentUser.id), type, notes, occurredAt (defaulting to now), projectId, and task sub-form fields (taskTitle, taskDescription, taskDueDate, taskAssignedUserId)
    - Define signals for picker data (employees, users, projects), loading states, error states, submission state, success/error messages, and taskSectionExpanded
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.7_

  - [x] 3.2 Create the component template
    - Create `src/app/interaction/log-interaction.component.html` with:
      - Employee picker (select with loading/error states)
      - Conducted-by picker (select, defaults to current user)
      - Type selector (dropdown with INTERACTION_TYPES options showing Title Case labels)
      - Notes textarea
      - Occurred-at datetime-local input
      - Project picker (optional select)
      - Inline task section (collapsible: title, description, dueDate, assignedUserId)
      - Submit button (disabled when form invalid or submitting)
      - Validation error messages adjacent to each field
      - Success/error notification areas
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 4.1, 5.1, 8.1, 8.2, 8.3, 8.4_

  - [x] 3.3 Create the component styles
    - Create `src/app/interaction/log-interaction.component.css` with form layout, field groups, validation error styles, task section toggle, loading/error indicators, and responsive layout
    - _Requirements: 1.1_

- [x] 4. Implement data loading and picker population
  - [x] 4.1 Implement picker data fetching in ngOnInit
    - In `LogInteractionComponent.ngOnInit`, call `EmployeeService.getAll()`, `UserService.getAll()`, and `ProjectService.getAll()` to populate picker signals
    - Set loading signals while requests are in-flight, set error signals on failure
    - Disable picker controls while loading, show retry buttons on failure
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [x] 5. Implement form submission and error handling
  - [x] 5.1 Implement interaction submission logic
    - On submit: mark all controls as touched, validate form, set `submitting=true`, disable button
    - Build `CreateInteractionRequest` payload with `loggedByUserId` always set to `currentUser.id`
    - Call `InteractionService.create()` and handle HTTP 201 (success notification, reset form, collapse task section)
    - Handle HTTP 400 (parse `ApiErrorResponse`, display message, highlight field errors)
    - Handle network/5xx errors (generic error message, re-enable button)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 5.2, 5.3, 5.4, 7.1, 7.2_

  - [x] 5.2 Implement inline task submission logic
    - After successful interaction creation (201 with response.id), if taskSectionExpanded and taskTitle is filled:
      - Build `CreateTaskRequest` with `interactionId` from interaction response
      - Call `TaskService.create()`, handle success (combined notification) and error (task error + interaction success message)
    - If task section collapsed or title empty, skip task POST entirely
    - On interaction failure, do NOT attempt task POST
    - _Requirements: 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 7.3_

  - [x] 5.3 Implement task section toggle with dynamic validators
    - When `taskSectionExpanded` changes to `true`: add `Validators.required`, `Validators.maxLength(255)`, and `notBlankValidator` to taskTitle; add `futureDateValidator` to taskDueDate
    - When collapsed: clear validators on task fields and reset their values
    - _Requirements: 4.1, 4.2, 4.8_

- [x] 6. Update routing and remove old placeholder
  - [x] 6.1 Update interaction.routes.ts and delete old files
    - Modify `src/app/interaction/interaction.routes.ts` to import and route to `LogInteractionComponent` instead of the old `Interaction` placeholder
    - Delete old files: `interaction.ts`, `interaction.html`, `interaction.css`
    - _Requirements: 1.1, 1.5_

- [x] 7. Checkpoint - Verify compilation and manual smoke test
  - Ensure the project compiles without errors (`npm run build`). Ask the user if questions arise.

- [x] 8. Unit tests
  - [x] 8.1 Write example-based unit tests for form validation
    - Create `src/app/interaction/log-interaction.component.spec.ts`
    - Test each required field produces validation error when empty: Employee_Picker, Conducted_By_Picker, type selector, notes, occurred_at (5 test cases)
    - Test submit button enabled when all required fields valid (1 test case)
    - Test form defaults: conductedByUserId = currentUser.id, occurredAt ≈ now
    - _Requirements: 9.1_

  - [x] 8.2 Write example-based unit tests for inline task section
    - Test toggling task section renders/hides sub-form
    - Test expanded task section with empty title produces validation error
    - Test collapsed task section produces no task validation errors
    - _Requirements: 9.2_

  - [x] 8.3 Write example-based unit tests for submission flows
    - Test successful submission: success notification, form reset, task section collapse
    - Test 400 error: field errors highlighted, error message displayed
    - Test network error: generic error message, button re-enabled
    - Test partial failure: interaction succeeds, task fails → both messages shown
    - Test interaction failure prevents task POST
    - _Requirements: 3.2, 3.3, 3.4, 4.4, 4.5, 4.7_

  - [x] 8.4 Write property-based tests for validators and form logic
    - **Property 2: Whitespace-only notes are rejected**
    - **Property 3: Valid required fields enable submission**
    - **Property 6: Task title required when section expanded**
    - **Property 9: Past due dates are rejected**
    - **Property 10: Interaction type label formatting**
    - **Validates: Requirements 2.3, 2.6, 4.2, 4.8, 8.2**

  - [x] 8.5 Write property-based tests for submission payload and component behaviour
    - **Property 1: Form defaults match current user**
    - **Property 4: Submission payload correctness**
    - **Property 5: Server field errors are displayed**
    - **Property 7: Task POST uses interaction response ID**
    - **Property 8: Collapsed task section produces no task request**
    - **Property 11: User picker displays full names**
    - **Validates: Requirements 1.2, 3.1, 3.3, 4.3, 4.6, 5.1, 5.2, 5.3, 5.4**

- [x] 9. End-to-end test
  - [x] 9.1 Write Playwright e2e test for the full interaction + task flow
    - Create `e2e/log-interaction.spec.ts`
    - Navigate to `/interaction` (authenticated), fill all required fields, expand task section, fill task title, submit
    - Assert: success notification visible, form reset to defaults, task section collapsed
    - Uses seeded reference data (employees, users) from backend
    - _Requirements: 9.3_

- [x] 10. Final checkpoint - Ensure all tests pass
  - Run `npx vitest --run` and verify all unit/property tests pass. Ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The backend API is already implemented — this plan is frontend-only
- The existing `AuthService`, interceptors, and route guards are reused as-is

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3", "2.4", "2.5"] },
    { "id": 2, "tasks": ["3.1", "3.3"] },
    { "id": 3, "tasks": ["3.2", "4.1"] },
    { "id": 4, "tasks": ["5.1", "5.3"] },
    { "id": 5, "tasks": ["5.2", "6.1"] },
    { "id": 6, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5"] },
    { "id": 7, "tasks": ["9.1"] }
  ]
}
```
