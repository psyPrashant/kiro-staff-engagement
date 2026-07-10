# Requirements Document

## Introduction

This feature implements the "Log Interaction" page in the Angular frontend, consuming the already-built backend write API. The page allows a logged-in user to record a staff engagement interaction (e.g. a check-in with an employee) and optionally create a follow-up task linked to that interaction — all in a single flow. The "conducted by" field defaults to the current user but supports on-behalf-of logging.

## Glossary

- **Log_Interaction_Page**: The Angular page/component where users fill in interaction details and submit them to the backend.
- **Interaction_Form**: The reactive form within the Log_Interaction_Page that captures all interaction fields.
- **Inline_Task_Section**: An expandable section within the Log_Interaction_Page where users can optionally define a follow-up task.
- **Employee_Picker**: A form control for selecting the employee the interaction is about.
- **Project_Picker**: A form control for optionally selecting the project associated with the interaction.
- **Conducted_By_Picker**: A form control for selecting who conducted the interaction, defaulting to the current user.
- **Interaction_Service**: The Angular service responsible for HTTP calls to the interaction backend API.
- **Task_Service**: The Angular service responsible for HTTP calls to the task backend API.
- **AuthService**: The existing Angular service providing the current authenticated user via a signal.
- **Current_User**: The authenticated user provided by AuthService.currentUser, always used as the loggedByUserId.

## Requirements

### Requirement 1: Log Interaction Page Rendering

**User Story:** As a logged-in user, I want to access a log interaction page, so that I can record staff engagement interactions.

#### Acceptance Criteria

1. WHEN the user navigates to the `/interaction` route, THE Log_Interaction_Page SHALL render the Interaction_Form with all required fields: Employee_Picker, Conducted_By_Picker, type selector, notes text area, and occurred_at date-time picker.
2. WHEN the Log_Interaction_Page loads, THE Conducted_By_Picker SHALL default its value to the Current_User and THE occurred_at date-time picker SHALL default to the current date and time.
3. WHEN the Log_Interaction_Page loads, THE Log_Interaction_Page SHALL render an optional Project_Picker control (empty by default, not required for form submission) for associating a project with the interaction.
4. WHEN the Log_Interaction_Page loads, THE Inline_Task_Section SHALL be collapsed by default and THE Log_Interaction_Page SHALL render a button to expand the Inline_Task_Section for creating a follow-up task.
5. IF the user is not authenticated WHEN navigating to the `/interaction` route, THEN THE System SHALL redirect the user to the login page without rendering the Log_Interaction_Page.

### Requirement 2: Interaction Form Validation

**User Story:** As a logged-in user, I want the form to prevent submission of incomplete data, so that only valid interactions are recorded.

#### Acceptance Criteria

1. WHEN the user attempts to submit the Interaction_Form with no employee selected in the Employee_Picker, THE Interaction_Form SHALL prevent submission and display a validation message adjacent to the Employee_Picker indicating that an employee selection is required.
2. WHEN the user attempts to submit the Interaction_Form with no type selected in the type selector, THE Interaction_Form SHALL prevent submission and display a validation message adjacent to the type selector indicating that a type selection is required.
3. WHEN the user attempts to submit the Interaction_Form with the notes field empty or containing only whitespace, THE Interaction_Form SHALL prevent submission and display a validation message adjacent to the notes field indicating that notes content is required.
4. WHEN the user attempts to submit the Interaction_Form with no date-time provided in the occurred_at field, THE Interaction_Form SHALL prevent submission and display a validation message adjacent to the occurred_at field indicating that a date and time is required.
5. WHEN the user attempts to submit the Interaction_Form with no user selected in the Conducted_By_Picker, THE Interaction_Form SHALL prevent submission and display a validation message adjacent to the Conducted_By_Picker indicating that a conducted-by selection is required.
6. WHEN all required fields (Employee_Picker, type selector, notes with at least one non-whitespace character, occurred_at, and Conducted_By_Picker) are populated, THE Interaction_Form SHALL enable the submit button.
7. WHEN the Interaction_Form initially renders, THE Interaction_Form SHALL display the submit button in a disabled state until all required fields are populated with valid values.

### Requirement 3: Interaction Submission

**User Story:** As a logged-in user, I want to submit the interaction form, so that the interaction is persisted via the backend API.

#### Acceptance Criteria

1. WHEN the user submits a valid Interaction_Form, THE Interaction_Service SHALL send a POST request to `/api/interactions` with the payload containing employeeId, conductedByUserId, loggedByUserId (set to Current_User id), type, notes, occurredAt, and projectId (if selected).
2. WHEN the POST to `/api/interactions` returns HTTP 201, THE Log_Interaction_Page SHALL display a success notification to the user and reset the Interaction_Form to its initial empty state within 1 second of receiving the response.
3. IF the POST to `/api/interactions` returns HTTP 400, THEN THE Log_Interaction_Page SHALL display the top-level error message from the response body and highlight each field listed in the fieldErrors map with its corresponding validation message.
4. IF the POST to `/api/interactions` returns a network error or an HTTP status code other than 201 or 400, THEN THE Log_Interaction_Page SHALL display an error message indicating the request failed and re-enable the submit button.
5. WHILE the interaction submission request is in progress, THE Log_Interaction_Page SHALL disable the submit button and display a loading indicator until a response is received or the request fails, at which point the submit button SHALL be re-enabled.

### Requirement 4: Inline Follow-Up Task Creation

**User Story:** As a logged-in user, I want to optionally add a follow-up task when logging an interaction, so that I can track next steps in a single flow.

#### Acceptance Criteria

1. WHEN the user expands the Inline_Task_Section, THE Log_Interaction_Page SHALL display a task sub-form with fields: title (required, maximum 255 characters), description (optional, maximum 2000 characters), dueDate (optional, date picker), and assignedUserId picker (optional).
2. IF the Inline_Task_Section is expanded and the user attempts to submit the Interaction_Form with the task title field empty, THEN THE Log_Interaction_Page SHALL prevent submission and display a validation message indicating the title is required.
3. WHEN the user submits the Interaction_Form with the Inline_Task_Section expanded and a valid title provided, THE Task_Service SHALL send a POST request to `/api/tasks` with the payload containing title, description, dueDate, assignedUserId, and interactionId set to the id returned from the interaction creation response.
4. WHEN both the interaction and task creation succeed, THE Log_Interaction_Page SHALL display a success notification indicating both were created.
5. IF the task POST to `/api/tasks` returns HTTP 400, THEN THE Log_Interaction_Page SHALL display the task error message while still indicating the interaction was created successfully.
6. WHEN the user submits the Interaction_Form with the Inline_Task_Section collapsed or without a title, THE Task_Service SHALL NOT send a POST request to `/api/tasks`.
7. IF the interaction creation fails, THEN THE Log_Interaction_Page SHALL NOT send the task POST request and SHALL display the interaction error without attempting task creation.
8. IF the user provides a dueDate value that is in the past, THEN THE Log_Interaction_Page SHALL prevent submission of the task sub-form and display a validation message indicating the due date must be today or a future date.

### Requirement 5: On-Behalf-Of Logging

**User Story:** As a logged-in user, I want to log an interaction on behalf of another user, so that I can record interactions conducted by someone else.

#### Acceptance Criteria

1. THE Conducted_By_Picker SHALL display each user option as their full name (matching the name field returned by GET /api/users) and allow selecting any user from the system as the person who conducted the interaction.
2. WHEN the user submits the Interaction_Form without changing the Conducted_By_Picker from its default, THE Interaction_Form SHALL use the Current_User id as the conductedByUserId in the submission payload.
3. WHEN the user changes the Conducted_By_Picker value to a different user, THE Interaction_Form SHALL use the selected user's id as the conductedByUserId in the submission payload.
4. THE Interaction_Form SHALL always set loggedByUserId to the Current_User id programmatically without exposing loggedByUserId as an editable field in the UI, regardless of the Conducted_By_Picker selection.

### Requirement 6: Data Loading

**User Story:** As a logged-in user, I want the pickers to be populated with available options, so that I can select employees, users, and projects.

#### Acceptance Criteria

1. WHEN the Log_Interaction_Page loads, THE Employee_Picker SHALL fetch the list of employees from `GET /api/employees` and display each option showing the employee's name.
2. WHEN the Log_Interaction_Page loads, THE Conducted_By_Picker SHALL fetch the list of users from `GET /api/users` and display each option showing the user's name.
3. WHEN the Log_Interaction_Page loads, THE Project_Picker SHALL fetch the list of projects from `GET /api/projects` and display each option showing the project's name.
4. WHILE any picker data-loading request is in progress, THE Log_Interaction_Page SHALL display a loading indicator for that picker and disable the picker control until the data arrives.
5. IF a data-loading request for one picker fails, THEN THE Log_Interaction_Page SHALL display an error message indicating which picker's options could not be loaded, while still allowing the other pickers that loaded successfully to remain functional.
6. IF all data-loading requests fail, THEN THE Log_Interaction_Page SHALL display an error message indicating the options could not be loaded and SHALL prevent form submission until at least the required pickers (Employee_Picker, Conducted_By_Picker) are populated.
7. IF a data-loading request fails, THEN THE Log_Interaction_Page SHALL provide a retry control that re-fetches the failed picker's data when activated.

### Requirement 7: Post-Submission Behaviour

**User Story:** As a logged-in user, I want a clear outcome after submission, so that I know my interaction was recorded and I can log another or navigate away.

#### Acceptance Criteria

1. WHEN the POST to `/api/interactions` returns HTTP 201, THE Log_Interaction_Page SHALL reset the Interaction_Form to its default state (Conducted_By_Picker defaulting to Current_User, Employee_Picker cleared, type selector cleared, notes cleared, occurred_at cleared, Project_Picker cleared).
2. WHEN the POST to `/api/interactions` returns HTTP 201, THE Inline_Task_Section SHALL collapse to its default hidden state with all task sub-form fields (title, description, dueDate, assignedUserId) cleared.
3. IF the interaction creation succeeds but the subsequent task creation fails, THEN THE Log_Interaction_Page SHALL still reset the Interaction_Form and collapse the Inline_Task_Section while keeping the task error notification visible to the user.

### Requirement 8: Interaction Type Selection

**User Story:** As a logged-in user, I want to select the type of interaction from a predefined list, so that interactions are categorized correctly.

#### Acceptance Criteria

1. THE type selector SHALL display exactly the following options: CHECK_IN, MENTORING, CATCH_UP, and OTHER, matching the backend InteractionType enum values.
2. THE type selector SHALL display each option as a Title Case label formed by replacing underscores with spaces and capitalizing the first letter of each word (e.g., CHECK_IN → "Check In", CATCH_UP → "Catch Up", MENTORING → "Mentoring", OTHER → "Other").
3. THE type selector SHALL allow exactly one type to be selected at a time.
4. WHEN the interaction form loads, THE type selector SHALL have no type pre-selected, requiring the user to make an explicit choice.
5. IF the user attempts to submit the interaction form without selecting a type, THEN THE system SHALL prevent submission and display a validation error indicating that interaction type is required.

### Requirement 9: Unit and End-to-End Testing

**User Story:** As a developer, I want automated tests covering the form logic and the end-to-end flow, so that regressions are caught early.

#### Acceptance Criteria

1. THE Log_Interaction_Page SHALL have Vitest unit tests that verify each required field (Employee_Picker, Conducted_By_Picker, type selector, notes, occurred_at) produces a validation error when left empty on submit attempt, and that the submit button is enabled when all required fields contain valid values — at minimum one test case per required field plus one valid-form test case (6 total minimum).
2. THE Log_Interaction_Page SHALL have Vitest unit tests that verify: (a) toggling the Inline_Task_Section renders and hides the task sub-form, (b) when the Inline_Task_Section is expanded, an empty title field produces a validation error on submit attempt, and (c) when the Inline_Task_Section is collapsed, no task validation errors are raised — at minimum one test case per scenario (3 total minimum).
3. THE Log_Interaction_Page SHALL have one Playwright e2e test that fills all required interaction fields, expands the Inline_Task_Section, fills the task title, submits the form, and asserts: (a) a success notification is visible, (b) the Interaction_Form is reset to its default state with Conducted_By_Picker set to Current_User and all other fields cleared, and (c) the Inline_Task_Section is collapsed — with the test running against a live backend with seeded reference data (employees, users).
