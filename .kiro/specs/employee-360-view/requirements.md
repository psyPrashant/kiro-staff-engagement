# Requirements Document

## Introduction

The Employee 360 View feature provides a single screen that presents a consolidated view of an employee, aggregating their interaction history, open tasks, and associated project/company context. This is a full-stack vertical slice: the backend exposes an aggregate read endpoint, and the frontend renders the consolidated data on one screen.

## Glossary

- **Employee_360_API**: The backend REST endpoint that aggregates and returns the complete 360-degree view data for a given employee.
- **Employee_360_Screen**: The frontend Angular component that displays the consolidated employee view.
- **Interaction_History**: The chronologically ordered list of interactions (CHECK_IN, MENTORING, CATCH_UP, OTHER) associated with an employee.
- **Open_Tasks**: Tasks with status OPEN that are linked to an employee's interactions.
- **Project_Context**: The project and company information derived from an employee's interactions.
- **Aggregate_Response**: The JSON payload returned by the Employee_360_API containing employee details, interaction history, open tasks, and project/company context.

## Requirements

### Requirement 1: Aggregate Read Endpoint

**User Story:** As a frontend client, I want a single API endpoint that returns the full 360-degree view of an employee, so that the UI can render the complete picture in one request.

#### Acceptance Criteria

1. WHEN a GET request is made to the Employee_360_API with a valid employee ID, THE Employee_360_API SHALL return HTTP 200 with an Aggregate_Response containing the employee's profile details (id, name, email, jobTitle, manager name).
2. WHEN a GET request is made to the Employee_360_API with a valid employee ID, THE Employee_360_API SHALL include the Interaction_History ordered by occurredAt descending, where each interaction entry contains: id, type, occurredAt, conductedBy user name, notes, and associated Project_Context if present.
3. WHEN a GET request is made to the Employee_360_API with a valid employee ID, THE Employee_360_API SHALL include all Open_Tasks linked to the employee's interactions, where each task entry contains: id, title, dueDate, and assigned user name.
4. WHEN a GET request is made to the Employee_360_API with a valid employee ID, THE Employee_360_API SHALL include the Project_Context (project name and company name) for each interaction that has an associated project.
5. IF a GET request is made to the Employee_360_API with a non-existent employee ID, THEN THE Employee_360_API SHALL return HTTP 404 with an error message indicating the employee was not found.
6. WHEN a GET request is made to the Employee_360_API with a valid employee ID that has no interactions, THE Employee_360_API SHALL return an Aggregate_Response with an empty Interaction_History list and an empty Open_Tasks list.
7. IF a GET request is made to the Employee_360_API with an employee ID that is not a valid numeric identifier, THEN THE Employee_360_API SHALL return HTTP 400 with an error message indicating the ID format is invalid.
8. THE Employee_360_API SHALL return the Aggregate_Response within 2000 milliseconds under normal operating conditions.

### Requirement 2: Interaction History Display

**User Story:** As a user, I want to see an employee's interaction history on the Employee 360 screen, so that I can understand engagement patterns at a glance.

#### Acceptance Criteria

1. WHEN the Employee_360_Screen loads for a given employee, THE Employee_360_Screen SHALL display each interaction showing its type as a human-readable label, date of occurrence in localized date format, conducting user name, and notes truncated to a maximum of 200 characters with an expand option.
2. THE Employee_360_Screen SHALL display interactions in reverse chronological order (most recent first).
3. WHEN an interaction has an associated project, THE Employee_360_Screen SHALL display the project name and company name alongside the interaction.
4. WHEN an employee has no interactions, THE Employee_360_Screen SHALL display an empty-state message indicating no interaction history exists.

### Requirement 3: Open Tasks Display

**User Story:** As a user, I want to see open tasks related to an employee on the same screen, so that I can track outstanding action items.

#### Acceptance Criteria

1. WHEN the Employee_360_Screen loads for a given employee, THE Employee_360_Screen SHALL display all Open_Tasks showing title, due date, and assigned user name, ordered by due date ascending with tasks that have no due date listed last.
2. IF an employee has no Open_Tasks, THEN THE Employee_360_Screen SHALL display an empty-state message indicating no open tasks exist.
3. THE Employee_360_Screen SHALL visually distinguish overdue tasks (tasks with a due date before the current calendar date) from tasks that are not yet due by applying a distinct visual style (e.g., color or icon) to the overdue task row.
4. IF an Open_Task has no due date assigned, THEN THE Employee_360_Screen SHALL display the task without a due date value and SHALL NOT mark it as overdue.

### Requirement 4: Employee Profile Summary

**User Story:** As a user, I want to see core employee details at the top of the 360 view, so that I can confirm the identity and role of the employee.

#### Acceptance Criteria

1. WHEN the Employee_360_Screen loads for a given employee, THE Employee_360_Screen SHALL display the employee's name, email, job title, and manager name in a profile summary section positioned above the Interaction_History and Open_Tasks sections.
2. IF the employee has no manager assigned, THEN THE Employee_360_Screen SHALL omit the manager field from the profile summary without displaying a blank or null value.
3. IF any required profile field (name, email, or job title) is empty or unavailable in the Aggregate_Response, THEN THE Employee_360_Screen SHALL display a placeholder label indicating the information is unavailable for that field.

### Requirement 5: Navigation and Loading

**User Story:** As a user, I want to navigate to the Employee 360 screen from the existing employee route, so that I can access the consolidated view seamlessly.

#### Acceptance Criteria

1. WHEN a user navigates to the employee detail route with an employee ID that exists in the system, THE Employee_360_Screen SHALL load and display the aggregate data for that employee within 3 seconds of navigation.
2. WHILE the Employee_360_Screen is fetching data from the Employee_360_API, THE Employee_360_Screen SHALL display a loading indicator that remains visible until the data is fully loaded or an error occurs.
3. IF the Employee_360_API returns an error response, THEN THE Employee_360_Screen SHALL display an error message indicating the nature of the failure and provide a retry button that re-initiates the data fetch from the Employee_360_API when activated.
4. IF a user navigates to the employee detail route with an employee ID that does not exist in the system, THEN THE Employee_360_Screen SHALL display a message indicating that no employee was found for the given ID.
5. IF the Employee_360_API does not respond within 30 seconds, THEN THE Employee_360_Screen SHALL treat the request as failed, hide the loading indicator, and display an error message indicating a timeout along with a retry button.

### Requirement 6: Acceptance Tests

**User Story:** As a developer, I want Cucumber acceptance tests covering the Employee 360 view, so that end-to-end correctness is verified through the UI against a real backend.

#### Acceptance Criteria

1. WHEN the acceptance-test suite is executed, THE suite SHALL include a Gherkin feature file describing the Employee 360 view scenarios under the acceptance-tests module.
2. THE feature file SHALL contain scenarios that verify: an authenticated user can navigate to an employee's 360 view and see the profile summary, interaction history, and open tasks.
3. THE feature file SHALL contain a scenario that verifies: when an employee has no interactions, the 360 view displays appropriate empty-state messages for both interaction history and open tasks.
4. THE feature file SHALL contain a scenario that verifies: overdue tasks are visually distinguished from tasks that are not yet due.
5. THE acceptance tests SHALL use the four-layer Cucumber + Playwright + Spring architecture established in the acceptance-tests module, including domain actors, page objects, and the TestWorld for scenario-scoped state.
6. THE acceptance tests SHALL seed required test data (employees, interactions, tasks, projects, companies) using the SeedDataApiDriver or SQL scripts before each scenario.
7. IF a scenario requires an authenticated user, THEN the step definitions SHALL use the LoginActor to authenticate before navigating to the Employee 360 screen.

### Requirement 7: Authentication and Authorization

**User Story:** As a system administrator, I want the Employee 360 endpoint and screen to be protected by authentication, so that only authorized users can access employee data.

#### Acceptance Criteria

1. THE Employee_360_API SHALL require a valid authenticated session for access.
2. IF a request to the Employee_360_API does not include a valid authenticated session, THEN THE Employee_360_API SHALL return HTTP 401 with a response body containing an error message indicating that authentication is required.
3. IF an unauthenticated user navigates to the Employee_360_Screen, THEN THE Employee_360_Screen SHALL redirect the user to the login page with the originally requested URL preserved as a returnUrl parameter.
4. IF the Employee_360_API returns HTTP 401 while the Employee_360_Screen is displayed, THEN THE Employee_360_Screen SHALL redirect the user to the login page with the current URL preserved as a returnUrl parameter.
