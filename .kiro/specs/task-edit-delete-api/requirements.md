# Requirements Document

## Introduction

This feature adds update and delete capabilities for tasks, completing the CRUD surface that `Task_Controller` currently only partially exposes (list and create). It allows API consumers to modify an existing task's fields — including transitioning its status between OPEN and DONE — and to permanently remove a task. The status transition covers the frontend's task completion and reopening workflow: completing or reopening a task is performed by updating the task's `status` field via the update endpoint, not through a separate completion endpoint. Update requests are validated using the same rules as task creation, and both the update and delete endpoints return HTTP 404 when the referenced task does not exist.

## Glossary

- **Task_Service**: The service component responsible for validating, persisting, updating, and deleting task records.
- **Task_Controller**: The REST controller handling HTTP requests for task operations at `/api/tasks`.
- **Task_Entity**: The JPA entity mapped to the `tasks` database table.
- **Update_Task_Request**: The DTO representing the JSON payload for updating an existing task.
- **Task_Response**: The DTO representing the JSON payload returned when reading or updating a task, including employee and assigned user details.
- **Task_Status**: The enumerated lifecycle state of a task, with values OPEN and DONE.
- **Employee**: The JPA entity representing a staff member, mapped to the `employees` table.
- **User**: The JPA entity representing an application user who may be assigned to a task.
- **Interaction**: The JPA entity representing a recorded engagement that a task may optionally be linked to.

## Requirements

### Requirement 1: Update Task

**User Story:** As an API consumer, I want to update an existing task's fields via REST API, so that I can edit task details, reassign it, relink it, or change its status from a single endpoint.

#### Acceptance Criteria

1. WHEN a valid Update_Task_Request is submitted to `PUT /api/tasks/{id}` for an existing Task_Entity, THE Task_Controller SHALL return HTTP 200 with the updated task.
2. THE Update_Task_Request SHALL require the following field: title (String, non-blank, max 255 characters).
3. THE Update_Task_Request SHALL accept the following optional fields: description (String, max 2000 characters), dueDate (LocalDate, nullable), assignedUserId (Long, nullable), employeeId (Long, nullable), interactionId (Long, nullable), status (Task_Status).
4. WHEN an Update_Task_Request is submitted with a non-null assignedUserId, THE Task_Service SHALL resolve the referenced User and associate it with the Task_Entity.
5. WHEN an Update_Task_Request is submitted with a non-null employeeId, THE Task_Service SHALL resolve the referenced Employee and associate it with the Task_Entity.
6. WHEN an Update_Task_Request is submitted with a non-null interactionId, THE Task_Service SHALL resolve the referenced Interaction and associate it with the Task_Entity.
7. WHEN an Update_Task_Request is submitted with a null assignedUserId, employeeId, or interactionId, THE Task_Service SHALL clear the corresponding association on the Task_Entity.
8. WHEN an Update_Task_Request updates a Task_Entity, THE Task_Service SHALL persist all provided field values, replacing the previous values.

### Requirement 2: Task Status Transition via Update

**User Story:** As a frontend consumer, I want to mark a task as complete or reopen it by updating its status field, so that task completion uses the same endpoint as other task edits rather than a dedicated completion endpoint.

#### Acceptance Criteria

1. THE Update_Task_Request SHALL accept a status field constrained to the values OPEN and DONE.
2. WHEN an Update_Task_Request sets status to DONE for a Task_Entity currently OPEN, THE Task_Service SHALL persist the task with status DONE.
3. WHEN an Update_Task_Request sets status to OPEN for a Task_Entity currently DONE, THE Task_Service SHALL persist the task with status OPEN.
4. WHEN an Update_Task_Request omits the status field, THE Task_Service SHALL retain the Task_Entity's current status.
5. IF an Update_Task_Request specifies a status value other than OPEN or DONE, THEN THE Task_Controller SHALL reject the request with HTTP 400 and a descriptive error message.

### Requirement 3: Delete Task

**User Story:** As an API consumer, I want to delete an existing task via REST API, so that I can permanently remove tasks that are no longer needed.

#### Acceptance Criteria

1. WHEN `DELETE /api/tasks/{id}` is called for an existing Task_Entity, THE Task_Service SHALL remove the task from the tasks table.
2. WHEN a Task_Entity is successfully deleted, THE Task_Controller SHALL return an HTTP 2xx success status code.

### Requirement 4: Not Found Handling

**User Story:** As an API consumer, I want a clear 404 response when I reference a task that does not exist, so that I can distinguish missing resources from validation errors.

#### Acceptance Criteria

1. WHEN `PUT /api/tasks/{id}` is called with an id that does not reference an existing Task_Entity, THE Task_Controller SHALL return HTTP 404 with a descriptive error message.
2. WHEN `DELETE /api/tasks/{id}` is called with an id that does not reference an existing Task_Entity, THE Task_Controller SHALL return HTTP 404 with a descriptive error message.

### Requirement 5: Update Validation Reuse

**User Story:** As a developer, I want the update endpoint to enforce the same field validation as task creation, so that data integrity rules are consistent regardless of which operation modifies a task.

#### Acceptance Criteria

1. THE Update_Task_Request SHALL apply the same Jakarta Bean Validation constraints on title and description as Create_Task_Request.
2. WHEN an Update_Task_Request fails Jakarta Bean Validation, THE Task_Controller SHALL return HTTP 400 with a descriptive error message identifying the invalid field.
3. WHEN an Update_Task_Request references a non-null assignedUserId, employeeId, or interactionId that does not resolve to an existing record, THE Task_Service SHALL reject the request with HTTP 400 and a descriptive error message.
4. WHEN an Update_Task_Request has both a Jakarta Bean Validation failure and a reference validation failure, THE system SHALL return the error detected first in the existing validation order, with Jakarta Bean Validation constraints evaluated before service-layer foreign-key resolution.

### Requirement 6: Update Response Shape

**User Story:** As a UI consumer, I want the response from a successful update to include full task details, so that I can refresh the task list or detail view without an additional request.

#### Acceptance Criteria

1. WHEN a Task_Entity is successfully updated, THE Task_Controller SHALL return a Task_Response containing id, title, description, status, dueDate, assignedUserId, assignedUserName, interactionId, employeeId, employeeName, and createdAt.
2. WHEN an updated Task_Entity has no associated Employee, THE Task_Response SHALL return null for employeeId and employeeName.
3. WHEN an updated Task_Entity has no associated User, THE Task_Response SHALL return null for assignedUserId and assignedUserName.
4. WHEN an updated Task_Entity has an associated Employee with incomplete data, THE Task_Response SHALL return the available employee fields without failing the request.

### Requirement 7: Regression Safety

**User Story:** As a developer, I want existing task read and create behavior to remain unaffected by this feature, so that no regressions are introduced to already-working functionality.

#### Acceptance Criteria

1. THE existing `GET /api/tasks` and `POST /api/tasks` endpoints SHALL continue to behave exactly as before this feature is implemented.
2. THE existing task integration tests SHALL continue to pass without modification.
