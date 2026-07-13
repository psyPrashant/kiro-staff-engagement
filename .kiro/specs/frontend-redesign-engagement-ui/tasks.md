# Implementation Plan: Frontend Redesign & Engagement Workflow UI

## Overview

This plan implements the frontend redesign in three phases: Foundation (design tokens, shared classes, label helpers), Restyle existing screens (shell, login, dashboard, log interaction, employee 360), and New pages (employees list, task list, task form, task create dialog). The frontend uses Angular 21 standalone components with signals, plain CSS, and Vitest.

## Tasks

### Phase 1 — Foundation

- [x] 1. Implement design tokens, CSS reset, and base element styles
  - [x] 1.1 Add CSS reset, `:root` custom properties (color, typography, spacing, radius, shadow), `color-scheme: light`, and body base styles to `staff-engagement-frontend/src/styles.css`
    - Define all tokens from the design: `--color-bg`, `--color-surface`, `--color-text`, `--color-muted`, `--color-border`, `--color-primary`, `--color-primary-hover`, `--color-danger`, `--color-warning`, `--color-success`, `--color-danger-soft`, `--color-warning-soft`, `--color-success-soft`
    - Typography tokens: `--font-sans`, `--fs-xs` through `--fs-2xl`, `--fw-normal`, `--fw-medium`
    - Spacing tokens: `--space-1` (4px) through `--space-8` (64px)
    - Radius and shadow tokens
    - CSS reset: `*, *::before, *::after { box-sizing: border-box }`, body margin removal
    - _Requirements: 1.1, 1.2, 1.3_
  - [x] 1.2 Add base element styles for `h1`–`h3`, `a`, `button`, `input`, `select`, `textarea`, `table` consuming tokens, plus `:focus-visible` outline rule
    - All interactive elements get a visible `:focus-visible` outline using `--color-primary`
    - _Requirements: 1.4, 1.5_

- [x] 2. Implement shared global CSS classes
  - [x] 2.1 Add `.page` class (max-width ~1100px, centered, horizontal padding with spacing tokens) to `staff-engagement-frontend/src/styles.css`
    - _Requirements: 1.6, 2.1_
  - [x] 2.2 Add `.card` class (surface background, border, card radius, padding) to `staff-engagement-frontend/src/styles.css`
    - _Requirements: 2.2_
  - [x] 2.3 Add `.btn`, `.btn-primary`, `.btn-secondary` classes to `staff-engagement-frontend/src/styles.css`
    - _Requirements: 2.3_
  - [x] 2.4 Add `.form-field` class (label, input spacing, consistent sizing) to `staff-engagement-frontend/src/styles.css`
    - _Requirements: 2.4_
  - [x] 2.5 Add `.badge`, `.badge-danger`, `.badge-warning`, `.badge-success` classes to `staff-engagement-frontend/src/styles.css`
    - _Requirements: 2.5, 2.6_
  - [x] 2.6 Add responsive `@media` breakpoints (768px stacked cards, 375px single column) to `staff-engagement-frontend/src/styles.css`
    - _Requirements: 12.5, 12.6_

- [x] 3. Create shared `formatEnumLabel` utility and label helper functions
  - [x] 3.1 Create `staff-engagement-frontend/src/app/shared/utils/format-enum-label.ts` with `formatEnumLabel` function (splits on `_`, title-cases each word, joins with spaces)
    - _Requirements: 5.5_
  - [x] 3.2 Add `formatEngagementStatusLabel` function to `staff-engagement-frontend/src/app/dashboard/models/engagement.model.ts` that maps OVERDUE→"Overdue", AT_RISK→"At risk", ON_TRACK→"On track", with fallback to `formatEnumLabel`
    - _Requirements: 5.1_
  - [x] 3.3 Add `formatTaskStatusLabel` function to `staff-engagement-frontend/src/app/task/models/task.model.ts` that maps OPEN→"Open", DONE→"Done", with fallback to `formatEnumLabel`
    - _Requirements: 5.4_
  - [x] 3.4 Write property tests for label helpers using fast-check in `staff-engagement-frontend/src/app/shared/utils/format-enum-label.spec.ts`
    - **Property 1: Engagement status label round-trip consistency** — for any valid EngagementStatus, formatEngagementStatusLabel returns a non-empty transformed string
    - **Property 2: Task status label round-trip consistency** — for any valid task status, formatTaskStatusLabel returns a non-empty transformed string
    - **Property 3: Unknown enum fallback produces title case** — for any UPPER_SNAKE_CASE string, formatEnumLabel returns title-cased output without underscores
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

- [x] 4. Checkpoint — Phase 1 complete
  - Ensure all tests pass, ask the user if questions arise.
  - Verify tokens render correctly in the browser by checking body styles

### Phase 2 — Restyle Existing Screens

- [x] 5. Redesign shell navigation
  - [x] 5.1 Update `staff-engagement-frontend/src/app/shell/shell.component.html` to render: brand label ("Staff Engagement" linked to `/dashboard`), links for Dashboard, Employees, Log interaction, Tasks, and right-aligned Logout button
    - Remove User and Client links from visible nav (keep routes in `app.routes.ts`)
    - Preserve all existing `data-testid` attributes
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.7_
  - [x] 5.2 Update `staff-engagement-frontend/src/app/shell/shell.component.css` to consume design tokens, add responsive rules for ≤768px and ≤375px (no horizontal overflow)
    - _Requirements: 3.5, 3.6, 12.2_
  - _Depends on: Tasks 1, 2_

- [x] 6. Style login page
  - [x] 6.1 Update `staff-engagement-frontend/src/app/auth/login/login.component.html` and its CSS to render a centered `.card` with branded header, `.form-field` styled inputs, `.btn-primary` submit button, and styled inline error messages
    - Add visible focus states on form inputs
    - Ensure usability at 375px viewport without horizontal scroll
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  - _Depends on: Tasks 1, 2_

- [x] 7. Style dashboard with triage stats and matrix
  - [x] 7.1 Add a `triageStats` computed signal to `staff-engagement-frontend/src/app/dashboard/` (in the matrix or dashboard component) that derives Overdue/At risk/On track counts from matrix entries
    - Render as a row of three stat cards above the matrix table
    - _Requirements: 6.1_
  - [x] 7.2 Style the Interaction Matrix table in its component CSS: header styling, row hover, cell padding with spacing tokens, status badges using `.badge-danger`/`.badge-warning`/`.badge-success`
    - Integrate `formatEngagementStatusLabel` in the template where status text is rendered
    - _Requirements: 5.2, 6.2, 6.3_
  - [x] 7.3 Style the Follow-Up Section with card styling, spacing, and status badges consuming design tokens; integrate `formatEngagementStatusLabel`
    - _Requirements: 5.3, 6.4_
  - [x] 7.4 Add responsive stacked-card layout for viewports below 768px and loading/error/empty states styled with tokens
    - _Requirements: 6.5, 6.6_
  - _Depends on: Tasks 1, 2, 3_

- [x] 8. Style Log Interaction form
  - [x] 8.1 Apply `.form-field` class to all fields in `staff-engagement-frontend/src/app/interaction/` log interaction component template
    - Style Employee, Conducted by, Type, Notes, Occurred at, Project fields
    - _Requirements: 7.1_
  - [x] 8.2 Implement `<optgroup>`-based project picker: add `companyName` to Project interface, create a `projectsByCompany` computed signal grouping projects by company, render `<optgroup label="CompanyName">` in template
    - _Requirements: 7.2_
  - [x] 8.3 Style the "Add follow-up task" toggle and nested fields, plus loading/error/success states
    - Disable submit button while submitting and show "Submitting..." label
    - Style inline validation error messages
    - _Requirements: 7.3, 7.4, 7.5, 7.6_
  - [x] 8.4 Write property test for project grouping using fast-check
    - **Property 4: Project grouping preserves all items** — for any list of projects, grouping by company and flattening preserves count and IDs
    - **Validates: Requirements 7.2**
  - _Depends on: Tasks 1, 2_

- [x] 9. Style Employee 360 page
  - [x] 9.1 Style the profile header with employee name, details, and engagement status Badge in `staff-engagement-frontend/src/app/employee/employee-360/`
    - Add header action buttons: "Log interaction" (links to interaction form with employee pre-selected) and "New task" (will open dialog in Phase 3)
    - _Requirements: 8.1, 8.5_
  - [x] 9.2 Integrate `formatInteractionTypeLabel` in the interaction history list; show `projectContext` (project name + company name) or "No project" when null
    - _Requirements: 8.2, 8.3_
  - [x] 9.3 Apply danger color token to overdue tasks (open + due date in the past) in the tasks section
    - Preserve all existing `data-testid` attributes
    - _Requirements: 8.4, 8.6_
  - _Depends on: Tasks 1, 2, 3_

- [x] 10. Checkpoint — Phase 2 complete
  - Ensure all tests pass, ask the user if questions arise.
  - Verify all restyled pages render correctly with tokens

### Phase 3 — New Pages

- [x] 11. Build Employees List page
  - [x] 11.1 Replace stub content in `staff-engagement-frontend/src/app/employee/employee.ts` and `employee.html` with a standalone `EmployeesListComponent` that fetches from `GET /api/employees` and joins engagement data from `GET /api/engagement/matrix` by employee ID
    - Define `EmployeeListEntry` interface in `staff-engagement-frontend/src/app/employee/models/employee-list.model.ts`
    - _Requirements: 9.1, 9.2_
  - [x] 11.2 Implement search input (filters by name or job title) and status filter control (All, Overdue, At risk, On track) as signals with a `filteredEmployees` computed signal
    - _Requirements: 9.3, 9.4_
  - [x] 11.3 Render table with columns: initials avatar, name, job title, manager, engagement status badge (using `formatEngagementStatusLabel`), last seen date; row click navigates to `/employee/:id`
    - Style loading, empty, and error states with design tokens
    - Ensure usability at 375px without horizontal page scroll
    - _Requirements: 9.5, 9.6, 9.7, 9.8_
  - [x] 11.4 Write property test for employee list filtering
    - **Property 5: Employee list filtering is monotonic** — for any list and search term, filtered result length ≤ unfiltered length
    - **Validates: Requirements 9.3, 9.4**
  - _Depends on: Tasks 1, 2, 3_

- [x] 12. Extend TaskService and update task models
  - [x] 12.1 Add `getAll(params?: { status?: string }): Observable<TaskResponse[]>` and `updateStatus(taskId: number, status: string): Observable<TaskResponse>` methods to `staff-engagement-frontend/src/app/task/services/task.service.ts`
    - _Requirements: 10.1, 10.5_
  - [x] 12.2 Add `employeeId: number` (required) to `CreateTaskRequest` and `employeeId: number | null`, `employeeName: string | null` to `TaskResponse` in `staff-engagement-frontend/src/app/task/models/task.model.ts`
    - _Requirements: 10.7_
  - [x] 12.3 Add an `isOverdue` utility function in the task models file: returns `true` when status is `'OPEN'` and dueDate is before today
    - _Requirements: 10.4_
  - [x] 12.4 Write property tests for task status toggle and overdue detection
    - **Property 6: Task status toggle is involutory** — toggling OPEN↔DONE twice returns to original status
    - **Property 7: Overdue detection correctness** — for any task, isOverdue returns correct boolean based on status and dueDate
    - **Validates: Requirements 10.4, 10.5**
  - _Depends on: Tasks 1, 3_

- [x] 13. Build Task List page
  - [x] 13.1 Replace stub content in `staff-engagement-frontend/src/app/task/task.ts` and `task.html` with a standalone `TaskListComponent` that fetches tasks via `TaskService.getAll()`, renders a table with columns: title, employee, assignee, due date, status (using `formatTaskStatusLabel`)
    - Apply danger styling to overdue rows using `isOverdue` utility
    - _Requirements: 10.1, 10.3, 10.4_
  - [x] 13.2 Add status filter control (Open | Done | All, default: Open) and completion toggle (checkbox/button calling `TaskService.updateStatus` to flip OPEN↔DONE)
    - _Requirements: 10.2, 10.5_
  - [x] 13.3 Style loading, empty, and error states; add responsive handling
    - _Requirements: 10.8_
  - _Depends on: Tasks 1, 2, 12_

- [x] 14. Build shared TaskFormComponent
  - [x] 14.1 Create `staff-engagement-frontend/src/app/task/components/task-form/task-form.component.ts` as a standalone component with:
    - Inputs: `employeeId` (InputSignal, when non-null pre-fills and disables employee picker), `defaultAssigneeId` (InputSignal), `interactions` (InputSignal of `{ id: number; label: string }[]`)
    - Outputs: `submitted` (OutputEmitterRef emitting `CreateTaskRequest`), `cancelled` (OutputEmitterRef)
    - Fields: Employee (required select), Assignee (select), Title (required, max 255), Description (optional, max 2000), Due date, Link to interaction (optional select)
    - Client-side validation on blur and submit; styled inline error messages
    - _Requirements: 10.6, 10.9, 11.4, 11.8_
  - _Depends on: Tasks 1, 2, 12_

- [x] 15. Build Task Create Dialog
  - [x] 15.1 Create `staff-engagement-frontend/src/app/task/components/task-create-dialog/task-create-dialog.component.ts` as an accessible modal wrapping `TaskFormComponent`
    - `role="dialog"`, `aria-modal="true"`, `aria-labelledby` pointing to dialog title
    - Focus trap: on open focus moves to first focusable element; Tab cycles within dialog; Shift+Tab wraps
    - Escape key closes without saving
    - On close, focus returns to trigger element
    - _Requirements: 11.1, 11.6, 11.7, 11.8, 12.3_
  - [x] 15.2 Integrate dialog into Employee 360: wire "New task" button to open dialog with employee pre-filled and `defaultAssigneeId` set to current user; on submit POST to `/api/tasks` and refresh open-tasks list
    - Populate "Link to interaction" dropdown from employee's recent interactions
    - _Requirements: 11.2, 11.3, 11.4, 11.5_
  - [x] 15.3 Wire the Task List "New task" form section to use the same `TaskFormComponent`; on submit POST to `/api/tasks` with `employeeId` and refresh the task list
    - _Requirements: 10.6, 10.7_
  - _Depends on: Tasks 14, 9, 13_

- [x] 16. Update existing unit tests to use new readable labels
  - [x] 16.1 Find all Vitest specs asserting on raw enum strings (`"OVERDUE"`, `"AT_RISK"`, `"ON_TRACK"`, `"CHECK_IN"`, `"MENTORING"`, `"CATCH_UP"`, `"OTHER"`, `"OPEN"`, `"DONE"`) in rendered output and update assertions to use the formatted label values ("Overdue", "At risk", "On track", "Check In", "Mentoring", "Catch Up", "Other", "Open", "Done")
    - _Requirements: 13.2, 13.3_
  - _Depends on: Tasks 3, 5, 7, 8, 9, 11, 13_

- [x] 17. Final checkpoint — All phases complete
  - Ensure all tests pass (`npx vitest --run`), ask the user if questions arise.
  - Verify all `data-testid` attributes are preserved
  - Verify responsive behavior at 375px and 768px breakpoints
  - _Requirements: 13.1, 13.3, 12.5_

## Notes

- Tasks marked with `*` are optional property-based test tasks and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation between phases
- Property tests use `fast-check` with minimum 100 iterations per property
- The frontend project root is `staff-engagement-frontend/`
- All component CSS should consume tokens via `var(--token-name)` — no hardcoded color or spacing values in component files
