# Requirements Document

## Introduction

This feature implements the write API for interactions and tasks — the backend endpoints that the logging UI depends on. It enables creating interactions (with on-behalf-of logging support where one user logs an interaction another user conducted), creating tasks linked to interactions, and provides lookup endpoints the UI needs (employees list, projects filtered by company). The feature includes a proper service layer with business validation, request DTOs, and comprehensive test coverage.

## Glossary

- **Interaction_Service**: The service component responsible for validating and persisting interaction records.
- **Task_Service**: The service component responsible for validating and persisting task records.
- **Interaction_Controller**: The REST controller handling HTTP requests for interaction operations.
- **Task_Controller**: The REST controller handling HTTP requests for task operations.
- **Project_Controller**: The REST controller handling HTTP requests for project lookup operations.
- **Employee_Controller**: The REST controller handling HTTP requests for employee lookup operations.
- **Create_Interaction_Request**: The DTO representing the JSON payload for creating an interaction.
- **Create_Task_Request**: The DTO representing the JSON payload for creating a task.
- **Interaction**: A recorded engagement between a user and an employee, with distinct conducted-by and logged-by attribution.
- **Task**: An action item linked to an interaction, with a title, status, optional due date, and optional assignee.

## Requirements

### Requirement 1: Create Interaction

**User Story:** As a user, I want to create an interaction record via REST API, so that I can log engagements with employees including on-behalf-of attribution.

#### Acceptance Criteria

1. WHEN a valid Create_Interaction_Request is submitted to `POST /api/interactions`, THE Interaction_Controller SHALL return HTTP 201 with the persisted interaction including its generated ID.
2. THE Create_Interaction_Request SHALL require the following fields: employeeId (Long), conductedByUserId (Long), loggedByUserId (Long), type (InteractionType), notes (String non-blank), occurredAt (Instant).
3. THE Create_Interaction_Request SHALL accept the following optional field: projectId (Long, nullable).
4. WHEN the employeeId does not reference an existing Employee, THE Interaction_Service SHALL reject the request with HTTP 400 and a descriptive error message.
5. WHEN the conductedByUserId does not reference an existing User, THE Interaction_Service SHALL reject the request with HTTP 400 and a descriptive error message.
6. WHEN the loggedByUserId does not reference an existing User, THE Interaction_Service SHALL reject the request with HTTP 400 and a descriptive error message.
7. WHEN a non-null projectId does not reference an existing Project, THE Interaction_Service SHALL reject the request with HTTP 400 and a descriptive error message.
8. WHEN the request body fails Jakarta Bean Validation (missing required fields or invalid values), THE Interaction_Controller SHALL return HTTP 400 with validation error details.
9. WHEN the request is submitted without authentication, THE Interaction_Controller SHALL return HTTP 401.

### Requirement 2: Create Task

**User Story:** As a user, I want to create a task linked to an interaction via REST API, so that I can track follow-up actions arising from engagements.

#### Acceptance Criteria

1. WHEN a valid Create_Task_Request is submitted to `POST /api/tasks`, THE Task_Controller SHALL return HTTP 201 with the persisted task including its generated ID.
2. THE Create_Task_Request SHALL require the following fields: title (String, non-blank, max 255 characters).
3. THE Create_Task_Request SHALL accept the following optional fields: interactionId (Long, nullable), description (String, max 2000 characters), dueDate (LocalDate, nullable), assignedUserId (Long, nullable).
4. WHEN a non-null interactionId does not reference an existing Interaction, THE Task_Service SHALL reject the request with HTTP 400 and a descriptive error message.
5. WHEN a non-null assignedUserId does not reference an existing User, THE Task_Service SHALL reject the request with HTTP 400 and a descriptive error message.
6. WHEN a task is created without an explicit status, THE Task_Service SHALL default the status to OPEN.
7. WHEN the request body fails Jakarta Bean Validation, THE Task_Controller SHALL return HTTP 400 with validation error details.
8. WHEN the request is submitted without authentication, THE Task_Controller SHALL return HTTP 401.

### Requirement 3: Lookup Projects by Company

**User Story:** As a UI consumer, I want to retrieve projects filtered by company, so that I can populate project dropdowns scoped to the relevant company.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/projects` with query parameter `companyId`, THE Project_Controller SHALL return only projects belonging to that company.
2. WHEN a GET request is made to `/api/projects` without a `companyId` parameter, THE Project_Controller SHALL return all projects.
3. WHEN the companyId does not match any existing Company, THE Project_Controller SHALL return an empty list with HTTP 200.
4. WHEN the request is submitted without authentication, THE Project_Controller SHALL return HTTP 401.

### Requirement 4: Lookup Employees

**User Story:** As a UI consumer, I want to retrieve the list of employees, so that I can populate employee selection fields in the interaction form.

#### Acceptance Criteria

1. WHEN a GET request is made to `/api/employees`, THE Employee_Controller SHALL return all employees with HTTP 200.
2. WHEN the request is submitted without authentication, THE Employee_Controller SHALL return HTTP 401.

### Requirement 5: Service Layer Validation

**User Story:** As a developer, I want business validation encapsulated in service classes, so that controllers remain thin and validation logic is reusable and testable.

#### Acceptance Criteria

1. THE Interaction_Service SHALL validate all foreign-key references (employee, conductedBy, loggedBy, project) before persisting an interaction.
2. THE Task_Service SHALL validate all foreign-key references (interaction, assignedUser) before persisting a task.
3. WHEN a validation failure occurs in a service, THE service SHALL throw an exception that results in a structured JSON error response containing an error message field.

### Requirement 6: Error Response Structure

**User Story:** As a UI consumer, I want consistent error responses from the API, so that I can display meaningful messages to users.

#### Acceptance Criteria

1. WHEN a validation error occurs, THE system SHALL return a JSON response body containing at minimum a `message` field describing the error.
2. WHEN a Jakarta Bean Validation constraint is violated, THE system SHALL return a JSON response body containing a `message` field and a `fieldErrors` map keyed by field name with error descriptions as values.
