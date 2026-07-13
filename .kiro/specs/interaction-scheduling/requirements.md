# Requirements Document

## Introduction

The Interaction Scheduling feature introduces the concept of planned future check-ins into the Staff Engagement platform. Managers can schedule upcoming interactions with employees, view them in a calendar/reminder list, and receive overdue notifications when a scheduled check-in passes its due date without being completed. The scheduling action is accessible directly from the interaction matrix, creating a natural workflow from identifying at-risk employees to planning engagement activities. The feature spans a full-stack vertical slice: a new `scheduled_interactions` entity and REST API on the backend, and an Angular calendar/reminder view with a schedule-next action on the frontend.

## Glossary

- **Scheduled_Interaction**: A persistence entity representing a planned future check-in between a user and an employee, with a scheduled date, interaction type, and completion status.
- **Scheduling_Service**: The backend service component responsible for creating, listing, updating, and managing scheduled interactions.
- **Scheduling_Controller**: The REST controller exposing endpoints for scheduled interaction CRUD operations.
- **Schedule_Calendar_Component**: The Angular standalone component that renders upcoming scheduled interactions in a calendar/reminder list view.
- **Schedule_Form_Component**: The Angular standalone component that provides the form for creating or editing a scheduled interaction.
- **Scheduling_Frontend_Service**: The Angular injectable service that communicates with the scheduling REST API.
- **Overdue_Scheduled_Interaction**: A Scheduled_Interaction whose scheduled date has passed without being marked as completed.
- **Completion_Status**: An enumeration representing the state of a scheduled interaction: PENDING, COMPLETED, or CANCELLED.

## Requirements

### Requirement 1: Scheduled Interaction Entity and Persistence

**User Story:** As a developer, I want a persistent entity for scheduled interactions, so that planned check-ins can be stored and queried reliably.

#### Acceptance Criteria

1. THE Scheduled_Interaction entity SHALL persist with the following required fields: id (auto-generated unique identifier using database identity strategy), employee reference, scheduled-by user reference, scheduled date (date only, no time component), interaction type, and completion status.
2. THE Scheduled_Interaction entity SHALL include an optional notes field (text, max 2000 characters) for context about the planned check-in.
3. THE Scheduled_Interaction entity SHALL default the Completion_Status to PENDING when created, where the allowed values for Completion_Status are PENDING, COMPLETED, and CANCELLED stored as a string column with a database CHECK constraint.
4. THE Scheduled_Interaction entity SHALL store a created_at timestamp that is automatically set on first persistence and is not updatable.
5. THE Scheduled_Interaction entity SHALL reference the Employee entity via a non-null foreign key relationship that prevents deletion of an Employee while associated Scheduled_Interaction records exist.
6. THE Scheduled_Interaction entity SHALL reference the User entity (the scheduler) via a non-null foreign key relationship that prevents deletion of a User while associated Scheduled_Interaction records exist.
7. THE Scheduled_Interaction entity SHALL store the interaction type using the existing InteractionType enumeration (CHECK_IN, MENTORING, CATCH_UP, OTHER) as a string column with a database CHECK constraint.
8. IF a Scheduled_Interaction is created with a scheduled date in the past (before the current date), THEN THE system SHALL reject the persistence and indicate that the scheduled date must be today or a future date.
9. IF a Scheduled_Interaction is created with a notes field exceeding 2000 characters, or with a null employee reference, null scheduled-by user reference, null scheduled date, or null interaction type, THEN THE system SHALL reject the persistence and indicate which field constraint was violated.

### Requirement 2: Create Scheduled Interaction API

**User Story:** As a manager, I want to schedule a future check-in for an employee via the API, so that I can plan my engagement activities in advance.

#### Acceptance Criteria

1. WHEN a POST request is made to `/api/scheduled-interactions` with a valid request body, THE Scheduling_Controller SHALL create a new Scheduled_Interaction and return a JSON response containing the fields id, employeeId, scheduledDate, interactionType, completionStatus, notes, scheduledByUserId, and createdAt with HTTP status 201.
2. THE Scheduling_Controller SHALL require the following fields in the request body: employeeId (valid existing employee), scheduledDate (ISO-8601 date format, yyyy-MM-dd), and interactionType (valid InteractionType value from: CHECK_IN, MENTORING, CATCH_UP, OTHER).
3. THE Scheduling_Controller SHALL accept an optional notes field in the request body with a maximum length of 2000 characters.
4. THE Scheduling_Controller SHALL set the scheduled-by user to the currently authenticated user.
5. IF the employeeId does not reference an existing employee, THEN THE Scheduling_Controller SHALL return HTTP status 404 with an error message indicating the employee was not found.
6. IF the scheduledDate is in the past (before the current system date), THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message indicating the scheduled date must be today or in the future.
7. IF a required field is missing or the interactionType is not a valid enumeration value, THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message identifying the invalid field.
8. IF the request is made without valid authentication, THEN THE Scheduling_Controller SHALL return HTTP status 401.
9. IF the notes field exceeds 2000 characters, THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message indicating the notes field exceeds the maximum length.

### Requirement 3: List Scheduled Interactions API

**User Story:** As a manager, I want to retrieve a list of my upcoming scheduled interactions, so that I can view my engagement plan.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/scheduled-interactions`, THE Scheduling_Controller SHALL return all Scheduled_Interaction entries for the authenticated user with HTTP status 200.
2. THE Scheduling_Controller SHALL return entries sorted by scheduledDate ascending (nearest date first).
3. WHEN the request includes an optional query parameter `status` with a valid Completion_Status value (PENDING, COMPLETED, or CANCELLED), THE Scheduling_Controller SHALL filter results to include only entries matching that status.
4. WHEN no `status` query parameter is provided, THE Scheduling_Controller SHALL return entries with all statuses.
5. WHEN the request includes an optional query parameter `employeeId` with a valid employee ID that references an existing employee, THE Scheduling_Controller SHALL filter results to include only entries for that specific employee.
6. WHEN no scheduled interactions exist for the authenticated user (or none match the applied filters), THE Scheduling_Controller SHALL return an empty JSON array with HTTP status 200.
7. THE Scheduling_Controller SHALL include in each response entry: id, employeeId, employeeName, scheduledDate (ISO-8601 date format, yyyy-MM-dd), interactionType, completionStatus, notes (null if not set), and an overdue boolean field.
8. IF the `status` query parameter is provided with a value that is not a valid Completion_Status (not one of PENDING, COMPLETED, or CANCELLED), THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message indicating the invalid status value.
9. IF the `employeeId` query parameter is provided with a value that does not reference an existing employee, THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message indicating the employee was not found.
10. THE Scheduling_Controller SHALL return a maximum of 200 entries per request; if the authenticated user has more than 200 matching entries, only the first 200 sorted by scheduledDate ascending SHALL be returned.

### Requirement 4: Update and Complete Scheduled Interaction API

**User Story:** As a manager, I want to mark a scheduled interaction as completed or cancelled, so that my schedule reflects actual engagement activities.

#### Acceptance Criteria

1. WHEN a PATCH request is made to `/api/scheduled-interactions/{id}` with a valid Completion_Status transition, THE Scheduling_Controller SHALL update the Scheduled_Interaction status and return the updated entry with HTTP status 200, including all fields: id, employeeId, employeeName, scheduledDate, interactionType, completionStatus, notes, and overdue status.
2. THE Scheduling_Controller SHALL allow only the following Completion_Status transitions: PENDING to COMPLETED, and PENDING to CANCELLED.
3. IF the requested transition is from COMPLETED or CANCELLED to any other status, THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message indicating that completed or cancelled interactions cannot be modified.
4. WHILE a Scheduled_Interaction has Completion_Status PENDING, THE Scheduling_Controller SHALL allow updating the scheduledDate and notes fields via the same PATCH endpoint, where scheduledDate must be in ISO-8601 date format (yyyy-MM-dd) and notes must not exceed 2000 characters.
5. IF the Scheduled_Interaction with the given id does not exist or does not belong to the authenticated user, THEN THE Scheduling_Controller SHALL return HTTP status 404.
6. IF the updated scheduledDate is in the past (before the current system date), THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message indicating the scheduled date must be today or in the future.
7. IF a PATCH request includes a Completion_Status transition together with scheduledDate or notes field updates, THEN THE Scheduling_Controller SHALL apply all changes atomically in a single operation, provided all validations pass.
8. IF the notes field in the PATCH request exceeds 2000 characters, THEN THE Scheduling_Controller SHALL return HTTP status 400 with an error message identifying that notes exceeds the maximum length.

### Requirement 5: Overdue Detection

**User Story:** As a manager, I want to know which of my scheduled check-ins are overdue, so that I can take immediate corrective action.

#### Acceptance Criteria

1. THE Scheduling_Service SHALL classify a Scheduled_Interaction as overdue when the scheduledDate is strictly before the current system date (scheduledDate < today, where a scheduledDate equal to today is NOT overdue) AND the Completion_Status is PENDING.
2. THE Scheduling_Service SHALL include an `overdue` boolean field in each list response entry, computed at query time against the current system date.
3. WHEN a GET request is made to `/api/scheduled-interactions` with query parameter `overdue=true`, THE Scheduling_Controller SHALL return only Scheduled_Interaction entries that are classified as overdue, applying any additional filter parameters (status, employeeId) as a logical AND conjunction.
4. IF a GET request is made to `/api/scheduled-interactions` with an `overdue` query parameter value other than `true`, THEN THE Scheduling_Controller SHALL ignore the overdue filter and return results as if the parameter were not provided.
5. THE Scheduling_Service SHALL expose a method that returns the count of overdue Scheduled_Interaction entries for a given authenticated user, as an integer value, so that the interaction matrix can consume it for engagement status enrichment.
6. THE Scheduling_Service SHALL compute overdue status identically for the same inputs regardless of invocation context, relying solely on the scheduledDate and current system date comparison.

### Requirement 6: Frontend Calendar/Reminder View

**User Story:** As a manager, I want to see my upcoming scheduled check-ins in a calendar or reminder list, so that I can plan my week and ensure no check-ins are missed.

#### Acceptance Criteria

1. THE Schedule_Calendar_Component SHALL display all PENDING scheduled interactions for the authenticated user grouped by date in ascending chronological order (earliest date first).
2. THE Schedule_Calendar_Component SHALL classify a scheduled interaction as overdue when its scheduled date is earlier than the current calendar date, and SHALL visually distinguish overdue items from upcoming items using a red/danger style for overdue and a neutral style for future-dated items.
3. THE Schedule_Calendar_Component SHALL display for each entry: employee name, interaction type, scheduled date, and notes (truncated to 100 characters with ellipsis if longer).
4. WHILE the scheduled interactions data is loading, THE Schedule_Calendar_Component SHALL display a loading indicator.
5. IF the Scheduling_Frontend_Service returns an error, THEN THE Schedule_Calendar_Component SHALL display an error message indicating the failure reason and a retry button that re-fetches the data when activated.
6. THE Schedule_Calendar_Component SHALL be accessible at the route `/schedule` within the Angular application, nested inside the authenticated shell layout.
7. WHEN a scheduled interaction entry is clicked, THE Schedule_Calendar_Component SHALL expand the entry inline to show the full notes text and action buttons for complete, cancel, and reschedule.
8. IF the authenticated user has zero PENDING scheduled interactions, THEN THE Schedule_Calendar_Component SHALL display an empty-state message indicating no upcoming check-ins are scheduled.
9. WHEN the complete, cancel, or reschedule action button is activated on an expanded entry, THE Schedule_Calendar_Component SHALL invoke the corresponding Scheduling_Frontend_Service operation and, upon success, remove or update the entry from the displayed list within 2 seconds.

### Requirement 7: Schedule-Next Action from Interaction Matrix

**User Story:** As a manager, I want to schedule a next check-in directly from the interaction matrix, so that I can take immediate action on employees flagged as overdue or at-risk.

#### Acceptance Criteria

1. THE Interaction_Matrix_Component SHALL render a "Schedule Next" action button on each employee row, identified by `data-testid="schedule-next-btn"`.
2. WHEN the user activates the "Schedule Next" action for a specific employee, THE Interaction_Matrix_Component SHALL navigate to the route `/schedule/new?employeeId={employeeId}` where `{employeeId}` is the numeric identifier of the selected employee.
3. THE Schedule_Form_Component SHALL read the `employeeId` query parameter from the route and pre-populate the employee field as a read-only display field so the user does not have to select the employee again.
4. THE Schedule_Form_Component SHALL require the user to select a scheduledDate (date input) and interactionType (dropdown with options: Check In, Mentoring, Catch Up, Other) before enabling the submit button.
5. THE Schedule_Form_Component SHALL accept an optional notes field (textarea, max 2000 characters).
6. THE Schedule_Form_Component SHALL validate that the scheduledDate is today or in the future on each change, displaying a visible validation error message adjacent to the date input if the date is in the past.
7. WHEN the form is submitted successfully (API returns HTTP 201), THE Schedule_Form_Component SHALL navigate back to the previous view using the browser history and display a toast/snackbar success notification visible for at least 3 seconds.
8. IF the API returns an error (HTTP 4xx or 5xx) during submission, THEN THE Schedule_Form_Component SHALL display the error message in a visible alert element adjacent to the form without navigating away, allowing the user to correct and resubmit.
9. WHILE a submission request is in flight, THE Schedule_Form_Component SHALL disable the submit button to prevent duplicate submissions.
10. IF the `employeeId` query parameter is missing or does not reference a valid employee, THEN THE Schedule_Form_Component SHALL display an error message and disable form submission.

### Requirement 8: Frontend Scheduling Service

**User Story:** As a frontend developer, I want a typed Angular service for the scheduling API, so that components can interact with scheduled interactions with compile-time type safety.

#### Acceptance Criteria

1. THE Scheduling_Frontend_Service SHALL be an Angular injectable service provided in root, using HttpClient to communicate with the `/api/scheduled-interactions` endpoints.
2. THE Scheduling_Frontend_Service SHALL expose a method to create a scheduled interaction, accepting employeeId (number), scheduledDate (string in ISO-8601 yyyy-MM-dd format), interactionType (InteractionType), and optional notes (string), and returning an Observable of ScheduledInteraction.
3. THE Scheduling_Frontend_Service SHALL expose a method to list scheduled interactions with optional status (CompletionStatus), employeeId (number), and overdue (boolean) filter parameters passed as HTTP query parameters, returning an Observable of ScheduledInteraction array.
4. THE Scheduling_Frontend_Service SHALL expose a method to update a scheduled interaction by id using HTTP PATCH, accepting an object with optional status (CompletionStatus), scheduledDate (string), and notes (string) fields, and returning an Observable of ScheduledInteraction.
5. THE Scheduling_Frontend_Service SHALL define a TypeScript interface ScheduledInteraction containing the fields: id (number), employeeId (number), employeeName (string), scheduledDate (string), interactionType (InteractionType), completionStatus (CompletionStatus), notes (string or null), overdue (boolean), and createdAt (string).
6. THE Scheduling_Frontend_Service SHALL define a TypeScript interface CreateScheduledInteractionRequest containing the fields: employeeId (number), scheduledDate (string), interactionType (InteractionType), and optional notes (string).
7. THE Scheduling_Frontend_Service SHALL define CompletionStatus as a TypeScript string union type with values `'PENDING' | 'COMPLETED' | 'CANCELLED'`.
8. THE Scheduling_Frontend_Service SHALL define InteractionType as a TypeScript string union type with values `'CHECK_IN' | 'MENTORING' | 'CATCH_UP' | 'OTHER'`.

### Requirement 9: Database Migration

**User Story:** As a developer, I want a Flyway migration for the scheduled_interactions table, so that the schema is versioned and reproducible across environments.

#### Acceptance Criteria

1. THE Flyway migration SHALL create a `scheduled_interactions` table with columns: id (BIGSERIAL PRIMARY KEY), employee_id (BIGINT NOT NULL, FK to employees(id)), scheduled_by_user_id (BIGINT NOT NULL, FK to users(id)), scheduled_date (DATE NOT NULL), interaction_type (VARCHAR(50) NOT NULL), notes (TEXT, nullable), completion_status (VARCHAR(50) NOT NULL DEFAULT 'PENDING'), and created_at (TIMESTAMP NOT NULL).
2. THE migration SHALL add a CHECK constraint on `interaction_type` accepting only values: 'CHECK_IN', 'MENTORING', 'CATCH_UP', 'OTHER'.
3. THE migration SHALL add a CHECK constraint on `completion_status` accepting only values: 'PENDING', 'COMPLETED', 'CANCELLED'.
4. THE migration SHALL create single-column indexes named `idx_scheduled_interactions_employee_id`, `idx_scheduled_interactions_scheduled_by_user_id`, and `idx_scheduled_interactions_scheduled_date` on the respective columns.
5. THE migration file SHALL be named `V5__create_scheduled_interactions_table.sql` following the project's existing Flyway naming convention of `V{N}__{description}.sql` with a double underscore separator.
6. IF the `employees` or `users` table does not exist when the migration runs, THEN the migration SHALL fail with a foreign key constraint error, preventing partial table creation.

### Requirement 10: Backend Unit and Property-Based Testing

**User Story:** As a developer, I want comprehensive unit and property-based tests for the scheduling logic, so that edge cases are verified and regressions are caught.

#### Acceptance Criteria

1. THE unit test suite SHALL verify that creating a Scheduled_Interaction with a date before the fixed reference date causes the Scheduling_Service to throw an IllegalArgumentException.
2. THE unit test suite SHALL verify that creating a Scheduled_Interaction with a date equal to the fixed reference date succeeds and the entity is persisted without exception.
3. THE unit test suite SHALL verify that status transitions from PENDING to COMPLETED and PENDING to CANCELLED succeed, with the entity reflecting the new Completion_Status after the operation.
4. THE unit test suite SHALL verify that status transitions from COMPLETED to each of PENDING, COMPLETED, and CANCELLED, and from CANCELLED to each of PENDING, COMPLETED, and CANCELLED, are rejected by the Scheduling_Service throwing an IllegalStateException.
5. THE unit test suite SHALL include a property-based test (minimum 100 trials) that generates random valid scheduled dates (fixed reference date through 365 days after the fixed reference date) and valid InteractionType values (CHECK_IN, MENTORING, CATCH_UP, OTHER), and verifies that the Scheduling_Service creates and persists the Scheduled_Interaction successfully for each combination.
6. THE unit test suite SHALL include a property-based test (minimum 100 trials) that generates random scheduled dates and current dates and verifies the overdue classification produces correct boolean results: true if and only if scheduledDate is strictly before currentDate AND status is PENDING.
7. THE unit test suite SHALL use a fixed reference date (injected via constructor or clock parameter) to ensure deterministic results independent of system clock.

### Requirement 11: Backend Integration Testing

**User Story:** As a developer, I want integration tests with a real PostgreSQL database verifying the full scheduling endpoint, so that I can confirm persistence, validation, and response serialization work end-to-end.

#### Acceptance Criteria

1. THE integration test SHALL use Testcontainers to provision a PostgreSQL database with the same Flyway migrations as the production schema, and SHALL seed prerequisite data (at least one valid Employee and one valid User) before executing scheduling endpoint tests.
2. THE integration test SHALL verify that POST `/api/scheduled-interactions` with a valid request body creates a record and returns HTTP 201 with a response body containing at minimum: id, employeeId, scheduledDate, interactionType, completionStatus set to PENDING, and createdAt.
3. THE integration test SHALL insert at least 2 scheduled interactions with distinct scheduledDate values and verify that GET `/api/scheduled-interactions` returns them sorted by scheduledDate ascending.
4. THE integration test SHALL verify that PATCH `/api/scheduled-interactions/{id}` with a status transition from PENDING to COMPLETED updates the completionStatus and returns HTTP 200 with the updated entity in the response body.
5. THE integration test SHALL use a controlled reference date (via a Clock bean or pre-seeded records with past dates) to verify that GET `/api/scheduled-interactions?overdue=true` returns only items with scheduledDate before the reference date and completionStatus PENDING, excluding items that are COMPLETED, CANCELLED, or future-dated.
6. THE integration test SHALL verify foreign key validation by attempting to create a scheduled interaction with a non-existent employeeId and confirming HTTP 404.
7. IF a PATCH request targets a Scheduled_Interaction with completionStatus COMPLETED or CANCELLED, THEN THE integration test SHALL verify the endpoint returns HTTP 400.

### Requirement 12: Frontend Vitest Unit Tests

**User Story:** As a developer, I want Vitest unit tests for the scheduling frontend components and service, so that regressions are caught early.

#### Acceptance Criteria

1. THE test suite SHALL include unit tests for the Scheduling_Frontend_Service verifying that create issues a POST request, list issues a GET request, and update issues a PATCH request, each to its respective URL with the expected request body or query parameters.
2. THE test suite SHALL include unit tests for the Schedule_Calendar_Component verifying that entries are rendered grouped by date and that items whose scheduled date is before today and whose status is PENDING have the CSS class `.overdue` applied.
3. THE test suite SHALL include a unit test verifying the loading indicator element (identified by `data-testid="loading-indicator"`) is present in the DOM while data is being fetched and absent once data arrives.
4. THE test suite SHALL include a unit test verifying that when the API returns an error, an error message element is rendered and a retry button is present, and that clicking the retry button re-invokes the service call and renders the data on success.
5. THE test suite SHALL include unit tests for the Schedule_Form_Component verifying date validation rejects past dates (dates before today) and accepts today or future dates.
6. THE test suite SHALL include a unit test verifying successful form submission triggers router navigation and renders a success notification element in the DOM.
7. THE test suite SHALL include a property-based test using fast-check that generates at least 100 random dates (between 2000-01-01 and 2099-12-31) combined with each schedule status value and verifies the overdue classification in the frontend model produces the correct result: overdue is true if and only if the scheduled date is before today and status is PENDING.

### Requirement 13: Acceptance Tests (Cucumber + Playwright)

**User Story:** As a developer, I want end-to-end acceptance tests covering the scheduling workflow, so that the full-stack feature is verified from UI through API to database.

#### Acceptance Criteria

1. THE acceptance test suite SHALL include a scenario where a manager schedules a next check-in from the interaction matrix, specifying an employee and a future date, and the scheduled check-in appears as a visible entry in the calendar view displaying the employee name and scheduled date.
2. THE acceptance test suite SHALL include a scenario where a manager marks a scheduled interaction as completed, and the interaction is no longer present in the PENDING interactions list while remaining visible in the completed interactions history.
3. THE acceptance test suite SHALL include a scenario where a scheduled interaction with a date in the past and a status of PENDING is rendered in the calendar view with a distinguishing visual indicator (verifiable via a dedicated data-testid attribute) that differentiates it from non-overdue entries.
4. THE acceptance test suite SHALL include a scenario where a manager attempts to schedule a check-in with a date earlier than today and a visible validation error message is displayed adjacent to the date input field, preventing submission.
5. THE acceptance test suite SHALL follow the four-layer architecture: Gherkin feature files, step definitions, domain actors/assertions, and page objects.
6. THE acceptance test suite SHALL establish prerequisite state for scheduling scenarios by driving the application through its real UI flows in Given/Background steps, resorting to SQL seed data only when the prerequisite feature is not yet implemented.
