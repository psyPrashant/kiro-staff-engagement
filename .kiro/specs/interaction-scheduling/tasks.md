# Implementation Plan: Interaction Scheduling

## Overview

Full-stack vertical slice implementing scheduled interactions: Flyway migration → JPA entity & repository → service with Clock injection → REST controller → backend property/integration tests → Angular models & service → Angular components (calendar, form) → frontend tests → interaction matrix integration → Cucumber acceptance tests. Each layer builds on the previous, ensuring no orphaned code.

## Tasks

- [x] 1. Database migration and backend entity layer
  - [x] 1.1 Create Flyway migration `V5__create_scheduled_interactions_table.sql`
    - Create `staff-engagement-backend/src/main/resources/db/migration/V5__create_scheduled_interactions_table.sql`
    - Define `scheduled_interactions` table: id (BIGSERIAL PK), employee_id (BIGINT NOT NULL FK→employees), scheduled_by_user_id (BIGINT NOT NULL FK→users), scheduled_date (DATE NOT NULL), interaction_type (VARCHAR(50) NOT NULL), notes (TEXT nullable), completion_status (VARCHAR(50) NOT NULL DEFAULT 'PENDING'), created_at (TIMESTAMP NOT NULL)
    - Add CHECK constraint `chk_si_interaction_type` for ('CHECK_IN','MENTORING','CATCH_UP','OTHER')
    - Add CHECK constraint `chk_si_completion_status` for ('PENDING','COMPLETED','CANCELLED')
    - Create indexes: `idx_scheduled_interactions_employee_id`, `idx_scheduled_interactions_scheduled_by_user_id`, `idx_scheduled_interactions_scheduled_date`
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [x] 1.2 Create `CompletionStatus` enum and `ScheduledInteraction` JPA entity
    - Create package `com.psybergate.staff_engagement.scheduling`
    - Create `CompletionStatus` enum (PENDING, COMPLETED, CANCELLED)
    - Create `ScheduledInteraction` entity with JPA annotations per design: @Entity, @Table, @Id with IDENTITY strategy, @ManyToOne relationships (LAZY, optional=false) to Employee and User, @Enumerated(STRING) for interactionType and completionStatus, @Column(length=2000) for notes, @PrePersist for createdAt
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

  - [x] 1.3 Create `ScheduledInteractionRepository` interface
    - Extend JpaRepository<ScheduledInteraction, Long>
    - Add query methods: findByScheduledByIdOrderByScheduledDateAsc, findByScheduledByIdAndCompletionStatusOrderByScheduledDateAsc, findByScheduledByIdAndEmployeeIdOrderByScheduledDateAsc, findByScheduledByIdAndCompletionStatusAndEmployeeIdOrderByScheduledDateAsc, countByScheduledByIdAndCompletionStatusAndScheduledDateBefore
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 5.5_

- [x] 2. Backend service and controller
  - [x] 2.1 Create `ClockConfig` configuration class
    - Create `ClockConfig` in `com.psybergate.staff_engagement.scheduling` (or a shared config package)
    - Provide a `Clock` bean (Clock.systemDefaultZone()) for injection
    - _Requirements: 10.7, 5.6_

  - [x] 2.2 Create request/response DTO records
    - Create `CreateScheduledInteractionRequest` record: @NotNull employeeId, @NotNull scheduledDate, @NotNull interactionType, @Size(max=2000) notes
    - Create `UpdateScheduledInteractionRequest` record: optional completionStatus, optional scheduledDate, @Size(max=2000) notes
    - Create `ScheduledInteractionResponse` record: id, employeeId, employeeName, scheduledDate, interactionType, completionStatus, notes, overdue, createdAt
    - _Requirements: 2.1, 2.2, 2.3, 3.7, 4.1_

  - [x] 2.3 Implement `SchedulingService`
    - Inject ScheduledInteractionRepository, EmployeeRepository, Clock
    - Implement `create(request, userId)`: validate scheduledDate ≥ today (throw IllegalArgumentException if past), validate employee exists (throw EmployeeNotFoundException if not), build entity, persist, return response DTO
    - Implement `list(userId, status, employeeId, overdue)`: query with filters, compute overdue boolean per entry (scheduledDate < today AND status == PENDING), sort ascending, cap at 200 entries
    - Implement `update(id, request, userId)`: validate ownership (throw 404 if not found/not owned), validate status transitions (only PENDING→COMPLETED, PENDING→CANCELLED allowed, throw IllegalStateException otherwise), validate scheduledDate if changed, apply atomically
    - Implement `countOverdue(userId)`: delegate to repository count method
    - Implement `isOverdue(scheduledDate, status)`: return status == PENDING && scheduledDate.isBefore(today)
    - _Requirements: 1.8, 1.9, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.10, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [x] 2.4 Implement `SchedulingController`
    - @RestController @RequestMapping("/api/scheduled-interactions")
    - POST endpoint: @ResponseStatus(CREATED), @Valid @RequestBody, extract userId from @AuthenticationPrincipal, delegate to service.create
    - GET endpoint: optional @RequestParam status, employeeId, overdue; validate status enum (return 400 for invalid), validate employeeId exists (return 400 for non-existent), delegate to service.list
    - PATCH /{id} endpoint: @Valid @RequestBody, delegate to service.update
    - Add @ControllerAdvice exception handler for IllegalArgumentException→400, IllegalStateException→400, EmployeeNotFoundException→404
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 3.1, 3.3, 3.7, 3.8, 3.9, 3.10, 4.1, 4.5, 5.3, 5.4_

- [x] 3. Backend property-based tests (jqwik)
  - [x] 3.1 Write property test: Date Validation Rejects Past Dates
    - Create `DateValidationPropertyTest` in test package `com.psybergate.staff_engagement.scheduling`
    - Use jqwik with @Tag("Feature: interaction-scheduling, Property 2: Date Validation Rejects Past Dates")
    - Generate random LocalDate values (past, today, future) with a fixed Clock
    - Assert: dates before today throw IllegalArgumentException; dates ≥ today pass
    - Minimum 100 trials
    - **Property 2: Date Validation Rejects Past Dates**
    - **Validates: Requirements 1.8, 2.6, 4.6, 10.1, 10.2**

  - [x] 3.2 Write property test: Completion Status Transition Correctness
    - Create `StatusTransitionPropertyTest`
    - Use jqwik with @Tag("Feature: interaction-scheduling, Property 3: Completion Status Transition Correctness")
    - Generate all (currentStatus, targetStatus) pairs from {PENDING, COMPLETED, CANCELLED} × {PENDING, COMPLETED, CANCELLED}
    - Assert: transition succeeds iff currentStatus == PENDING AND targetStatus ∈ {COMPLETED, CANCELLED}; else IllegalStateException
    - Minimum 100 trials
    - **Property 3: Completion Status Transition Correctness**
    - **Validates: Requirements 4.2, 4.3, 10.3, 10.4**

  - [x] 3.3 Write property test: Overdue Classification
    - Create `OverdueClassificationPropertyTest`
    - Use jqwik with @Tag("Feature: interaction-scheduling, Property 4: Overdue Classification")
    - Generate random (scheduledDate, referenceDate, completionStatus) triples
    - Assert: isOverdue returns true iff scheduledDate < referenceDate AND status == PENDING
    - Minimum 100 trials
    - **Property 4: Overdue Classification (Backend)**
    - **Validates: Requirements 5.1, 5.2, 5.6, 10.6**

  - [x] 3.4 Write property test: Scheduled Interaction Persistence Round-Trip
    - Create `ScheduledInteractionPersistencePropertyTest` (Spring Boot test with Testcontainers)
    - Use jqwik with @Tag("Feature: interaction-scheduling, Property 1: Persistence Round-Trip")
    - Generate valid entities with random scheduledDate (today to +365 days), random InteractionType, optional notes (null or 0–2000 chars)
    - Assert: persist and reload produces identical field values with completionStatus == PENDING and non-null createdAt
    - Minimum 100 trials
    - **Property 1: Scheduled Interaction Persistence Round-Trip**
    - **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.7**

  - [x] 3.5 Write property test: List Ordering Invariant
    - Create `ListOrderingPropertyTest`
    - Use jqwik with @Tag("Feature: interaction-scheduling, Property 5: List Ordering Invariant")
    - Seed multiple entries with random dates, call list endpoint
    - Assert: for every consecutive pair, entries[i].scheduledDate <= entries[i+1].scheduledDate
    - Minimum 100 trials
    - **Property 5: List Ordering Invariant**
    - **Validates: Requirements 3.2**

  - [x] 3.6 Write property test: Filter Correctness
    - Create `FilterCorrectnessPropertyTest`
    - Use jqwik with @Tag("Feature: interaction-scheduling, Property 6: Filter Correctness")
    - Generate random filter combinations (status, employeeId, overdue)
    - Assert: all returned entries satisfy all active filter predicates (logical AND)
    - Minimum 100 trials
    - **Property 6: Filter Correctness**
    - **Validates: Requirements 3.3, 3.5, 5.3**

- [x] 4. Checkpoint - Backend compilation and tests
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Backend integration tests (Testcontainers)
  - [x] 5.1 Write integration tests for scheduling endpoints
    - Create `SchedulingIntegrationTest` with @SpringBootTest and Testcontainers (PostgreSQL)
    - Seed prerequisite data: at least one Employee and one User via Flyway + programmatic setup
    - Test POST /api/scheduled-interactions: valid request → 201 with response containing id, employeeId, scheduledDate, interactionType, completionStatus=PENDING, createdAt
    - Test GET /api/scheduled-interactions: insert 2+ entries with distinct dates → verify ascending order
    - Test PATCH /api/scheduled-interactions/{id}: PENDING→COMPLETED transition → 200 with updated entity
    - Test GET with overdue=true: use controlled Clock/past-dated records → verify only overdue PENDING items returned
    - Test POST with non-existent employeeId → 404
    - Test PATCH on COMPLETED/CANCELLED entity → 400
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_

- [x] 6. Frontend models and service
  - [x] 6.1 Create TypeScript models for scheduling
    - Create `staff-engagement-frontend/src/app/schedule/models/scheduled-interaction.model.ts`
    - Define `CompletionStatus` type: `'PENDING' | 'COMPLETED' | 'CANCELLED'`
    - Define `InteractionType` type: `'CHECK_IN' | 'MENTORING' | 'CATCH_UP' | 'OTHER'`
    - Define `ScheduledInteraction` interface: id, employeeId, employeeName, scheduledDate, interactionType, completionStatus, notes, overdue, createdAt
    - Define `CreateScheduledInteractionRequest` interface: employeeId, scheduledDate, interactionType, optional notes
    - _Requirements: 8.5, 8.6, 8.7, 8.8_

  - [x] 6.2 Implement `SchedulingService` Angular service
    - Create `staff-engagement-frontend/src/app/schedule/services/scheduling.service.ts`
    - @Injectable({ providedIn: 'root' }), inject HttpClient
    - Implement `create(request)`: POST to `/api/scheduled-interactions`, return Observable<ScheduledInteraction>
    - Implement `list(params?)`: GET with optional status, employeeId, overdue query params, return Observable<ScheduledInteraction[]>
    - Implement `update(id, body)`: PATCH to `/api/scheduled-interactions/{id}`, return Observable<ScheduledInteraction>
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 7. Frontend components
  - [x] 7.1 Implement `ScheduleCalendarComponent`
    - Create `staff-engagement-frontend/src/app/schedule/schedule-calendar/` directory with component files
    - Standalone component using signals (loading, error, entries, expandedId, groupedEntries computed)
    - On init: call SchedulingService.list({ status: 'PENDING' })
    - Render entries grouped by date in ascending order
    - Apply `.overdue` CSS class and data-testid for overdue entries (scheduledDate < today AND status PENDING)
    - Display loading indicator with `data-testid="loading-indicator"`
    - Display error message with retry button on API failure
    - Display empty state with `data-testid="empty-schedule"` when no entries
    - Click entry → expand inline showing full notes + complete/cancel/reschedule buttons
    - Complete/cancel calls SchedulingService.update → remove entry from list
    - Truncate notes to 100 chars with ellipsis in collapsed view
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9_

  - [x] 7.2 Implement `ScheduleFormComponent`
    - Create `staff-engagement-frontend/src/app/schedule/schedule-form/` directory with component files
    - Standalone component with ReactiveFormsModule
    - Read `employeeId` from route query params; display error and disable form if missing/invalid
    - Pre-populate employee as read-only display field
    - Form fields: scheduledDate (date input, required), interactionType (select dropdown, required), notes (textarea, max 2000 chars)
    - Date validation: reject past dates on each change, show error via `data-testid="date-validation-error"`
    - Submit button disabled until valid + not submitting
    - On successful submit (201): navigate back via Location.back(), show success toast/snackbar (visible ≥3s)
    - On error: display error in alert element via `data-testid="api-error"`, no navigation
    - Use `data-testid` attributes: `employee-display`, `scheduled-date-input`, `interaction-type-select`, `notes-input`, `submit-btn`, `employee-error`
    - _Requirements: 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10_

  - [x] 7.3 Register schedule feature routes
    - Create `staff-engagement-frontend/src/app/schedule/schedule.routes.ts` with lazy-loaded routes: '' → ScheduleCalendarComponent, 'new' → ScheduleFormComponent
    - Update `app.routes.ts` to add `{ path: 'schedule', loadChildren: () => import('./schedule/schedule.routes').then(m => m.routes) }`
    - _Requirements: 6.6, 7.2_

- [x] 8. Frontend unit and property tests (Vitest + fast-check)
  - [x] 8.1 Write unit tests for `SchedulingService`
    - Create `staff-engagement-frontend/src/app/schedule/services/scheduling.service.spec.ts`
    - Verify create() issues POST, list() issues GET, update() issues PATCH with correct URLs and bodies/params
    - _Requirements: 12.1_

  - [x] 8.2 Write unit tests for `ScheduleCalendarComponent`
    - Create `staff-engagement-frontend/src/app/schedule/schedule-calendar/schedule-calendar.component.spec.ts`
    - Test: entries rendered grouped by date; overdue items have `.overdue` CSS class
    - Test: loading indicator present during fetch, absent after
    - Test: error message and retry button on API error; retry re-invokes service and renders data
    - _Requirements: 12.2, 12.3, 12.4_

  - [x] 8.3 Write unit tests for `ScheduleFormComponent`
    - Create `staff-engagement-frontend/src/app/schedule/schedule-form/schedule-form.component.spec.ts`
    - Test: date validation rejects past dates, accepts today/future
    - Test: successful submission triggers navigation and renders success notification
    - _Requirements: 12.5, 12.6_

  - [x] 8.4 Write property test: Frontend Overdue Classification
    - In `scheduling.service.spec.ts` (or separate property test file)
    - Use fast-check with // Feature: interaction-scheduling, Property 7: Frontend Overdue Classification
    - Generate random (scheduledDate, today, completionStatus) combinations
    - Assert: overdue === true iff scheduledDate < today AND status === 'PENDING'
    - Minimum 100 iterations
    - **Property 7: Frontend Overdue Classification**
    - **Validates: Requirements 6.2, 12.7**

  - [x] 8.5 Write property test: Frontend Notes Truncation
    - In `schedule-calendar.component.spec.ts`
    - Use fast-check with // Feature: interaction-scheduling, Property 8: Frontend Notes Truncation
    - Generate random strings; assert: length ≤ 100 → unchanged; length > 100 → 101 chars (first 100 + '…')
    - Minimum 100 iterations
    - **Property 8: Frontend Notes Truncation**
    - **Validates: Requirements 6.3**

  - [x] 8.6 Write property test: Frontend Date Validation
    - In `schedule-form.component.spec.ts`
    - Use fast-check with // Feature: interaction-scheduling, Property 9: Frontend Date Validation
    - Generate random dates; assert: before today → validation error; today/future → no error
    - Minimum 100 iterations
    - **Property 9: Frontend Date Validation**
    - **Validates: Requirements 7.6, 12.5**

  - [x] 8.7 Write property test: Frontend Service HTTP Method Correctness
    - In `scheduling.service.spec.ts`
    - Use fast-check with // Feature: interaction-scheduling, Property 10: Frontend Service HTTP Method Correctness
    - Generate valid request objects; assert: create→POST, list→GET, update→PATCH with correct URLs/bodies
    - Minimum 100 iterations
    - **Property 10: Frontend Service HTTP Method Correctness**
    - **Validates: Requirements 8.2, 8.3, 8.4**

- [x] 9. Checkpoint - Frontend compilation and tests
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Interaction matrix integration
  - [x] 10.1 Add "Schedule Next" button to interaction matrix
    - Modify `staff-engagement-frontend/src/app/dashboard/interaction-matrix/interaction-matrix.component.html`
    - Add a "Schedule Next" button on each employee row with `data-testid="schedule-next-btn"`
    - Add `navigateToSchedule(employeeId: number)` method in `interaction-matrix.component.ts` that navigates to `/schedule/new?employeeId={employeeId}`
    - _Requirements: 7.1, 7.2_

- [x] 11. Acceptance tests (Cucumber + Playwright)
  - [x] 11.1 Update `global-cleanup.sql` for scheduled_interactions
    - Add `TRUNCATE TABLE scheduled_interactions CASCADE;` **before** the `TRUNCATE TABLE interactions CASCADE;` line in `acceptance-tests/src/test/resources/fixtures/sql/global-cleanup.sql`
    - _Requirements: 13.6_

  - [x] 11.2 Create page objects: `ScheduleCalendarPage` and `ScheduleFormPage`
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/drivers/ui/pages/ScheduleCalendarPage.java`
    - Methods: open(), isEntryVisible(name), hasOverdueIndicator(name), isEmptyStateVisible(), expandEntry(name), clickComplete(), clickCancel(), isLoadingVisible()
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/drivers/ui/pages/ScheduleFormPage.java`
    - Methods: open(employeeId), getEmployeeDisplay(), setScheduledDate(date), selectInteractionType(type), setNotes(notes), submit(), isSubmitEnabled(), isDateValidationErrorVisible(), isApiErrorVisible(), isEmployeeErrorVisible()
    - Extend `BasePage`; use `data-testid` locators
    - _Requirements: 13.5_

  - [x] 11.3 Add `InteractionMatrixPage.clickScheduleNext(employeeId)` method
    - Update or create the interaction matrix page object to support clicking the "Schedule Next" button by employee ID using `data-testid="schedule-next-btn"`
    - _Requirements: 13.5_

  - [x] 11.4 Create domain actor `SchedulingActor` and assertions `SchedulingAssertions`
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/scheduling/SchedulingActor.java`
    - Methods: scheduleNextFromMatrix(employeeId), fillScheduleForm(date, type), submitScheduleForm(), navigateToCalendar(), completeEntry(employeeName)
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/scheduling/SchedulingAssertions.java`
    - Methods: assertEntryVisible(name), assertEntryNotVisible(name), assertOverdueIndicator(name), assertDateValidationError(), assertSubmitDisabled()
    - Both @Component @ScenarioScope, inject page objects and TestWorld
    - _Requirements: 13.5_

  - [x] 11.5 Create Gherkin feature file `interaction_scheduling.feature`
    - Create `acceptance-tests/src/test/resources/features/scheduling/interaction_scheduling.feature`
    - Tag: @scheduling
    - Background: login via real UI using LoginActor
    - Scenario 1: Manager schedules next check-in from matrix → appears in calendar view
    - Scenario 2: Manager marks scheduled interaction as completed → removed from pending list
    - Scenario 3: Past-dated pending interaction shows overdue indicator (use SQL seed for past date)
    - Scenario 4: Manager cannot schedule check-in with past date → validation error shown, submit disabled
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.6_

  - [x] 11.6 Create step definitions `SchedulingStepDefinitions.java`
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/steps/SchedulingStepDefinitions.java`
    - Wire SchedulingActor, SchedulingAssertions, LoginActor
    - Implement Given/When/Then steps matching the feature file scenarios
    - Use UI-driven prerequisite setup where possible; use seed SQL only for overdue (past-dated) scenario
    - _Requirements: 13.5, 13.6_

- [x] 12. Final checkpoint - Full stack verification
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (backend jqwik, frontend fast-check)
- Unit tests validate specific examples and edge cases
- Acceptance tests (task group 11) MUST be implemented last — they depend on the full stack being wired
- The `cucumber-acceptance-authoring` skill should be activated when executing acceptance test tasks (group 11)
- Backend uses Java 21 + Spring Boot 3.5 + Maven; Frontend uses TypeScript + Angular 21 + Vitest

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3", "2.1"] },
    { "id": 3, "tasks": ["2.2"] },
    { "id": 4, "tasks": ["2.3"] },
    { "id": 5, "tasks": ["2.4"] },
    { "id": 6, "tasks": ["3.1", "3.2", "3.3"] },
    { "id": 7, "tasks": ["3.4", "3.5", "3.6", "5.1"] },
    { "id": 8, "tasks": ["6.1"] },
    { "id": 9, "tasks": ["6.2"] },
    { "id": 10, "tasks": ["7.1", "7.2", "7.3"] },
    { "id": 11, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5", "8.6", "8.7"] },
    { "id": 12, "tasks": ["10.1"] },
    { "id": 13, "tasks": ["11.1", "11.2", "11.3"] },
    { "id": 14, "tasks": ["11.4"] },
    { "id": 15, "tasks": ["11.5"] },
    { "id": 16, "tasks": ["11.6"] }
  ]
}
```
