# Requirements Document

## Introduction

This feature adds a direct employee link to the Task entity, allowing tasks to be created and listed per-employee independently of an Interaction. Currently, a Task references an employee only indirectly through an optional Interaction. By adding a first-class `employee_id` foreign key to the tasks table and exposing employee information on task read responses, the backend supports standalone employee tasks, the Tasks list UI, and the Employee 360 view without requiring an interaction to exist.

## Glossary

- **Task_Service**: The service component responsible for validating and persisting task records.
- **Task_Controller**: The REST controller handling HTTP requests for task operations at `/api/tasks`.
- **Task_Entity**: The JPA entity mapped to the `tasks` database table.
- **Create_Task_Request**: The DTO representing the JSON payload for creating a task.
- **Task_Response**: The DTO representing the JSON payload returned when reading tasks, including employee details.
- **Employee**: The JPA entity representing a staff member, mapped to the `employees` table.
- **Migration**: A Flyway versioned SQL script that alters the database schema.

## Requirements

### Requirement 1: Database Migration for Employee Link

**User Story:** As a developer, I want a Flyway migration that adds an employee_id column to the tasks table, so that tasks can reference an employee directly at the database level.

#### Acceptance Criteria

1. THE Migration SHALL create a file named `V5__add_task_employee.sql` following the existing Flyway naming convention.
2. THE Migration SHALL add a nullable column `employee_id` of type `BIGINT` to the `tasks` table.
3. THE Migration SHALL define a foreign key constraint from `tasks.employee_id` referencing `employees(id)`.
4. THE Migration SHALL NOT modify any existing migration files.
5. WHEN the migration is applied to a database containing existing tasks, THE Migration SHALL succeed without data loss (existing rows receive NULL for employee_id).

### Requirement 2: Task Entity Employee Mapping

**User Story:** As a developer, I want the Task entity to have a direct ManyToOne relationship to Employee, so that the ORM can resolve and persist the employee link.

#### Acceptance Criteria

1. THE Task_Entity SHALL include a `ManyToOne` relationship to Employee mapped to the `employee_id` column.
2. THE Task_Entity SHALL allow the employee field to be null (for backward compatibility with existing interaction-linked tasks).
3. WHEN a Task is persisted with an employee reference, THE Task_Entity SHALL store the employee_id foreign key in the `tasks` table.

### Requirement 3: Create Task with Employee

**User Story:** As an API consumer, I want to provide an employeeId when creating a task, so that the task is directly linked to a specific employee without requiring an interaction.

#### Acceptance Criteria

1. THE Create_Task_Request SHALL accept an optional field `employeeId` of type Long.
2. WHEN a valid employeeId is provided in the request, THE Task_Service SHALL resolve the Employee and associate it with the new Task.
3. WHEN the provided employeeId does not reference an existing Employee, THE Task_Service SHALL reject the request with HTTP 400 and a descriptive error message.
4. WHEN employeeId is null and interactionId is also null, THE Task_Service SHALL create the task without an employee or interaction link (standalone task).
5. WHEN both employeeId and interactionId are provided, THE Task_Service SHALL associate both the employee and the interaction with the task.
6. WHEN the request body fails Jakarta Bean Validation, THE Task_Controller SHALL return HTTP 400 with validation error details.

### Requirement 4: Task Read Response with Employee Details

**User Story:** As an API consumer, I want task read responses to include the employee's id and name, so that the Tasks list and Employee 360 view can display the linked employee without additional lookups.

#### Acceptance Criteria

1. WHEN `GET /api/tasks` is called, THE Task_Controller SHALL return each task with `employeeId` and `employeeName` fields populated from the associated Employee.
2. WHEN a task has no associated employee, THE Task_Controller SHALL return null for `employeeId` and `employeeName` fields.
3. THE Task_Response SHALL include all existing task fields (id, title, description, status, dueDate, assignedUser, interaction, createdAt) in addition to the new employee fields.

### Requirement 5: Backward Compatibility

**User Story:** As a developer, I want existing task creation (interaction-linked tasks) to continue working unchanged, so that no regressions are introduced.

#### Acceptance Criteria

1. WHEN a Create_Task_Request is submitted with an interactionId and without an employeeId, THE Task_Service SHALL create the task linked to the interaction (existing behavior preserved).
2. WHEN a Create_Task_Request is submitted with only a title, THE Task_Service SHALL create a standalone task with status OPEN (existing behavior preserved).
3. THE existing task integration tests SHALL continue to pass without modification.
