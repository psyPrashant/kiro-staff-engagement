# Requirements Document

## Introduction

This feature surfaces each employee's next upcoming scheduled interaction so the frontend can display a "Next check-in" column on the Employees list and on the Employee 360 view. It builds on the existing `scheduled_interactions` entity and scheduling endpoints introduced by KSE-54 (the interaction-scheduling feature). The feature adds a repository query to find the soonest future pending interaction per employee, a lightweight DTO to carry the result, and enrichment of both the employees list endpoint and the employee 360 response. Acceptance tests verify the end-to-end behaviour through Cucumber scenarios using the four-layer harness.

## Glossary

- **Next_Scheduled_Interaction_Service**: The backend service component responsible for resolving the next upcoming scheduled interaction for a given employee.
- **NextScheduledDto**: A data transfer object containing `scheduledAt` (date-time) and `type` (InteractionType) representing the soonest future scheduled interaction for an employee.
- **Employee_List_Controller**: The REST controller serving the `GET /api/employees` endpoint, enriched to include next-scheduled data.
- **Employee360_Service**: The existing service that assembles the Employee 360 response, extended to include next-scheduled data.
- **ScheduledInteraction_Repository**: The existing Spring Data JPA repository for the `scheduled_interactions` table, extended with a new query method.
- **Employees_List_Response**: The DTO returned by the employees list endpoint, wrapping employee data with the associated `nextScheduled` field.
- **Acceptance_Test_Suite**: The Cucumber + Playwright four-layer test harness that verifies next-scheduled-interaction behaviour end-to-end.

## Requirements

### Requirement 1: Repository Query for Next Scheduled Interaction

**User Story:** As a developer, I want a repository query that finds the soonest future pending scheduled interaction for a given employee, so that services can resolve next-check-in data efficiently.

#### Acceptance Criteria

1. THE ScheduledInteraction_Repository SHALL expose a method that accepts an employee ID (Long) and a reference date (LocalDate), and returns an Optional containing the single ScheduledInteraction with the earliest scheduledDate that is on or after the reference date and has CompletionStatus PENDING, or returns an empty Optional if none exists.
2. WHEN multiple pending scheduled interactions exist for the same employee with scheduledDate on or after the reference date, THE ScheduledInteraction_Repository SHALL return only the one with the earliest scheduledDate, using the lowest entity ID as a tiebreaker when multiple interactions share the same earliest scheduledDate.
3. WHEN no pending scheduled interactions exist for the employee with scheduledDate on or after the reference date, THE ScheduledInteraction_Repository SHALL return an empty Optional.
4. THE ScheduledInteraction_Repository SHALL exclude scheduled interactions with CompletionStatus COMPLETED or CANCELLED from the query results regardless of their scheduledDate.
5. IF the employee ID parameter or reference date parameter is null, THEN THE ScheduledInteraction_Repository SHALL throw an IllegalArgumentException.

### Requirement 2: Next Scheduled Interaction Service

**User Story:** As a developer, I want a service method that resolves the next scheduled interaction for an employee into a lightweight DTO, so that controllers can include it in API responses without coupling to entity internals.

#### Acceptance Criteria

1. THE Next_Scheduled_Interaction_Service SHALL expose a method that accepts an employee ID and returns a NextScheduledDto containing `scheduledAt` (the scheduledDate formatted as an ISO-8601 date string, e.g. "2024-03-15") and `type` (the InteractionType enum value as its name string, e.g. "CHECK_IN"), or returns null if no qualifying interaction exists. A qualifying interaction is one whose scheduledDate is strictly after the current system date and whose completionStatus is PENDING.
2. WHEN multiple qualifying interactions exist for the given employee, THE Next_Scheduled_Interaction_Service SHALL return the one with the earliest scheduledDate. IF two or more qualifying interactions share the same earliest scheduledDate, THE Next_Scheduled_Interaction_Service SHALL return any one of them (selection is non-deterministic).
3. THE Next_Scheduled_Interaction_Service SHALL use the current system date as the reference date when querying the ScheduledInteraction_Repository.
4. WHEN the ScheduledInteraction_Repository returns an empty result for the given employee, THE Next_Scheduled_Interaction_Service SHALL return null.
5. IF the provided employee ID does not correspond to any scheduled interactions, THEN THE Next_Scheduled_Interaction_Service SHALL return null (identical behavior to an employee with no qualifying interactions).
6. THE Next_Scheduled_Interaction_Service SHALL expose a batch method that accepts a list of employee IDs (maximum 200 entries) and returns a Map from employee ID to NextScheduledDto (or null for employees with no qualifying interaction), so that the employees list endpoint can resolve all next-scheduled data in a single operation without N+1 queries.
7. WHEN the batch method receives an empty list of employee IDs, THE Next_Scheduled_Interaction_Service SHALL return an empty Map.

### Requirement 3: Employee 360 Response Enrichment

**User Story:** As a frontend developer, I want the Employee 360 API response to include the next scheduled interaction, so that the Employee 360 view can display "Next check-in" information.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/employees/{id}/360`, THE Employee360_Service SHALL include a `nextScheduled` field in the response body, where `nextScheduled` is either an object containing `scheduledAt` and `type` fields, or null.
2. IF the employee has one or more scheduled interactions with `completionStatus` equal to PENDING and `scheduledDate` strictly after today's date, THEN THE Employee360_Service SHALL set `nextScheduled` to an object containing `scheduledAt` (ISO-8601 date string, e.g. "2025-02-15") and `type` (InteractionType string, one of: "CHECK_IN", "MENTORING", "CATCH_UP", "OTHER") representing the scheduled interaction with the earliest `scheduledDate` among those qualifying records.
3. IF the employee has multiple qualifying future pending scheduled interactions sharing the same earliest `scheduledDate`, THEN THE Employee360_Service SHALL select the one with the lowest `id` value as the `nextScheduled` entry.
4. IF the employee has no scheduled interactions with `completionStatus` equal to PENDING and `scheduledDate` strictly after today's date, THEN THE Employee360_Service SHALL set `nextScheduled` to null in the response body.
5. WHEN a GET request is made to `/api/employees/{id}/360`, THE Employee360_Service SHALL determine "future" scheduled interactions by comparing the interaction's `scheduledDate` against the server's current date at the time the request is processed.

### Requirement 4: Employees List Response Enrichment

**User Story:** As a frontend developer, I want the employees list API response to include each employee's next scheduled interaction, so that the Employees list page can display a "Next check-in" column.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/employees`, THE Employee_List_Controller SHALL return a JSON array where each element includes the employee data and a `nextScheduled` field.
2. WHEN an employee has one or more future pending scheduled interactions (scheduledDate strictly after the server's current date with completionStatus PENDING), THE Employee_List_Controller SHALL set that employee's `nextScheduled` to an object containing `scheduledAt` (ISO-8601 date string) and `type` (InteractionType string).
3. WHEN an employee has no future pending scheduled interactions, THE Employee_List_Controller SHALL set that employee's `nextScheduled` to null.
4. THE Employee_List_Controller SHALL resolve all `nextScheduled` values using the batch method from the Next_Scheduled_Interaction_Service, avoiding N+1 database queries.
5. THE Employee_List_Controller SHALL return a DTO (Employees_List_Response) instead of raw Employee entities, containing the fields: id, name, email, jobTitle, managerName (null if the employee has no manager), and nextScheduled.

### Requirement 5: Backend Integration Tests

**User Story:** As a developer, I want Testcontainers integration tests verifying the next-scheduled query logic, so that edge cases around date selection and null handling are covered with a real database.

#### Acceptance Criteria

1. THE integration test SHALL use Testcontainers to provision a PostgreSQL database with the production Flyway migrations applied, seeding at least one Employee and one User before test execution.
2. THE integration test SHALL verify that when an employee has at least two future PENDING scheduled interactions, the GET `/api/employees/{id}/360` response returns the soonest one (earliest scheduledDate strictly after today) in the `nextScheduled` field with correct `scheduledAt` and `type` values.
3. THE integration test SHALL verify that when an employee has only past scheduled interactions (scheduledDate before today) or only COMPLETED/CANCELLED interactions, the `nextScheduled` field in the response is null.
4. THE integration test SHALL verify that when an employee has no scheduled interactions at all, the `nextScheduled` field in the response is null.
5. THE integration test SHALL verify that the GET `/api/employees` response includes `nextScheduled` for each employee with correct soonest-future values and null where applicable.
6. THE integration test SHALL verify that after creating a new PENDING scheduled interaction for an employee (via POST `/api/scheduled-interactions`) with a scheduledDate earlier than an existing future PENDING interaction, the subsequent GET to `/api/employees/{id}/360` reflects the newly created interaction in the `nextScheduled` field.

### Requirement 6: Cucumber Acceptance Tests

**User Story:** As a developer, I want end-to-end Cucumber acceptance tests verifying that the next-scheduled-interaction data is surfaced correctly through the API, so that the full-stack integration is validated from request through persistence.

#### Acceptance Criteria

1. THE Acceptance_Test_Suite SHALL include a Scenario Outline that verifies for multiple interaction type and date combinations, when a manager schedules a future pending interaction for an employee, the employee's 360 response returns HTTP 200 and contains the correct `nextScheduled` object with `scheduledAt` matching the scheduled ISO-8601 date and `type` matching the interaction type from the Examples table.
2. THE Acceptance_Test_Suite SHALL include a scenario verifying that when an employee has no pending interactions with a `scheduledDate` value after the current server date, the 360 response returns `nextScheduled` as null.
3. THE Acceptance_Test_Suite SHALL include a scenario verifying that when an employee has only past-dated pending interactions (all `scheduledDate` values before the current server date), the 360 response returns `nextScheduled` as null.
4. THE Acceptance_Test_Suite SHALL include a scenario verifying that the employees list endpoint returns HTTP 200 and includes the soonest future pending interaction (earliest `scheduledDate` after the current server date) for each employee, including null for employees with no future pending interactions.
5. THE Acceptance_Test_Suite SHALL include a scenario verifying that when a new pending interaction is scheduled with a `scheduledDate` earlier than an existing future pending interaction for the same employee, a subsequent GET request to the employee's 360 endpoint returns the `nextScheduled` field reflecting the newly soonest interaction.
6. THE Acceptance_Test_Suite SHALL follow the four-layer architecture: Gherkin feature files (tagged @next-scheduled), step definitions, domain actors/assertions, and API drivers extending BaseApiDriver.
7. THE Acceptance_Test_Suite SHALL use Scenario Outline with Examples tables to test at minimum the interaction types CHECK_IN, MENTORING, CATCH_UP, and OTHER without duplicating scenario bodies.
8. THE Acceptance_Test_Suite SHALL establish prerequisite data (employees, users, scheduled interactions) using direct API calls through the existing BaseApiDriver or SQL seed scripts, consistent with the patterns used in existing feature modules such as the scheduling and employee360 acceptance tests.
