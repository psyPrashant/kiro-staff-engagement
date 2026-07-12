# Requirements Document

## Introduction

This feature surfaces the engagement interaction matrix on the Angular dashboard. The backend API (`GET /api/engagement/matrix`) is already implemented and returns employee engagement data including recency, frequency, engagement status, and follow-up flags. This spec covers the frontend components needed to render that data: an engagement matrix table with status indicators, drill-through links to the Employee 360 view and log-interaction form, a suggested follow-ups section, and status filtering/sorting controls. The implementation uses Angular 21 standalone components with signals, and must include Vitest unit tests.

## Glossary

- **Dashboard**: The main landing page component at route `/dashboard` that aggregates engagement information for managers.
- **Interaction_Matrix_Component**: The Angular standalone component responsible for fetching and rendering the engagement matrix data on the Dashboard.
- **Engagement_Service**: The Angular injectable service that calls `GET /api/engagement/matrix` and returns typed response data to consuming components.
- **Matrix_Entry**: A TypeScript interface representing a single row in the engagement matrix, corresponding to the `EngagementMatrixEntry` DTO from the backend.
- **Engagement_Status**: A TypeScript union type with values `'OVERDUE'`, `'AT_RISK'`, or `'ON_TRACK'` representing an employee's engagement classification.
- **Follow_Up_Section**: A UI section within the Interaction_Matrix_Component that highlights employees where `followUpRequired` is true.
- **Status_Filter**: A UI control allowing the user to filter the matrix by Engagement_Status values.
- **Sort_Control**: A UI control allowing the user to toggle between alphabetical (default) and recency sort orders.

## Requirements

### Requirement 1: Engagement Service (API Client)

**User Story:** As a frontend developer, I want a typed Angular service that calls the engagement matrix API, so that components can consume engagement data with compile-time type safety.

#### Acceptance Criteria

1. THE Engagement_Service SHALL be an Angular injectable service provided in root, using `HttpClient` to call `GET /api/engagement/matrix`.
2. WHEN called with no parameters, THE Engagement_Service SHALL request the full unfiltered matrix from the backend.
3. WHEN called with an optional `status` parameter, THE Engagement_Service SHALL append `?status={value}` as a query parameter to the request URL.
4. WHEN called with an optional `sort` parameter, THE Engagement_Service SHALL append `sort={value}` as a query parameter to the request URL.
5. THE Engagement_Service SHALL return an Observable of `MatrixEntry[]` where `MatrixEntry` is a TypeScript interface matching the backend `EngagementMatrixEntry` DTO fields: `employeeId` (number), `employeeName` (string), `employeeEmail` (string), `recency` (number | null), `frequency` (number), `lastInteractionDate` (string | null), `engagementStatus` (EngagementStatus), `followUpRequired` (boolean).
6. THE Engagement_Service SHALL define `EngagementStatus` as a TypeScript string union type with values `'OVERDUE' | 'AT_RISK' | 'ON_TRACK'`.

### Requirement 2: Interaction Matrix Component Rendering

**User Story:** As a manager, I want to see the engagement matrix rendered on the dashboard, so that I can view all employees' engagement status at a glance.

#### Acceptance Criteria

1. WHEN the Dashboard loads, THE Interaction_Matrix_Component SHALL fetch the engagement matrix from the Engagement_Service and render one row per Matrix_Entry.
2. THE Interaction_Matrix_Component SHALL display for each employee row: employee name, engagement status badge, recency (days since last interaction or "No interactions" when null), frequency count, and last interaction date (formatted as a readable date or "Never" when null).
3. THE Interaction_Matrix_Component SHALL apply distinct visual styling to each Engagement_Status value: a red/danger style for OVERDUE, an amber/warning style for AT_RISK, and a green/success style for ON_TRACK.
4. WHEN `followUpRequired` is true for a Matrix_Entry, THE Interaction_Matrix_Component SHALL render a visible follow-up indicator (icon or badge) on that row.
5. WHILE the matrix data is loading, THE Interaction_Matrix_Component SHALL display a loading indicator to inform the user that data is being fetched.
6. IF the Engagement_Service returns an HTTP error, THEN THE Interaction_Matrix_Component SHALL display an error message informing the user that the matrix could not be loaded, and SHALL offer a retry action.

### Requirement 3: Drill-Through Navigation Links

**User Story:** As a manager, I want to click on a matrix row to navigate to the Employee 360 view or log an interaction, so that I can take action on engagement issues directly from the dashboard.

#### Acceptance Criteria

1. THE Interaction_Matrix_Component SHALL render a link or button on each row that navigates to the Employee 360 view at route `/employee/{employeeId}`.
2. THE Interaction_Matrix_Component SHALL render a link or button on each row that navigates to the log-interaction form at route `/interaction` with the employee pre-identified (via query parameter or route state).
3. WHEN the user activates the Employee 360 link, THE Interaction_Matrix_Component SHALL navigate using the Angular Router to `/employee/{employeeId}` where `{employeeId}` is the `employeeId` from the Matrix_Entry.
4. WHEN the user activates the log-interaction link, THE Interaction_Matrix_Component SHALL navigate using the Angular Router to `/interaction` passing the employee's ID so the form can pre-select that employee.

### Requirement 4: Suggested Follow-Ups Section

**User Story:** As a manager, I want to see a dedicated section showing employees who need follow-up, so that I can quickly prioritise my engagement activities without scanning the full matrix.

#### Acceptance Criteria

1. THE Interaction_Matrix_Component SHALL render a Follow_Up_Section that displays only Matrix_Entry items where `followUpRequired` is true.
2. THE Follow_Up_Section SHALL display each follow-up employee with their name, engagement status badge, and recency value.
3. THE Follow_Up_Section SHALL render drill-through links to the Employee 360 view and log-interaction form for each listed employee, following the same navigation logic as Requirement 3.
4. WHEN no employees have `followUpRequired` set to true, THE Follow_Up_Section SHALL display a message indicating no follow-ups are needed at this time.
5. THE Follow_Up_Section SHALL be visually distinguished from the full matrix (as a separate card or panel) and positioned prominently on the dashboard.

### Requirement 5: Status Filtering

**User Story:** As a manager, I want to filter the matrix by engagement status, so that I can focus on overdue or at-risk employees without visual clutter.

#### Acceptance Criteria

1. THE Interaction_Matrix_Component SHALL render a Status_Filter control with options: All (default), OVERDUE, AT_RISK, and ON_TRACK.
2. WHEN the user selects a status filter value, THE Interaction_Matrix_Component SHALL re-fetch the matrix from the Engagement_Service with the selected `status` query parameter.
3. WHEN the user selects "All", THE Interaction_Matrix_Component SHALL fetch the matrix without a `status` query parameter, displaying all employees.
4. WHILE a filter is active, THE Status_Filter control SHALL visually indicate which filter is currently selected.
5. WHEN the filter changes, THE Interaction_Matrix_Component SHALL display the loading indicator until new data arrives.

### Requirement 6: Sort Control

**User Story:** As a manager, I want to sort the matrix by recency, so that I can see who has the longest gap since their last interaction at the top.

#### Acceptance Criteria

1. THE Interaction_Matrix_Component SHALL render a Sort_Control with options: "Name (A-Z)" (default) and "Recency (longest gap first)".
2. WHEN the user selects "Recency (longest gap first)", THE Interaction_Matrix_Component SHALL re-fetch the matrix from the Engagement_Service with query parameter `sort=recency`.
3. WHEN the user selects "Name (A-Z)", THE Interaction_Matrix_Component SHALL re-fetch the matrix without a `sort` query parameter.
4. WHILE a sort option is active, THE Sort_Control SHALL visually indicate the current sort order.
5. WHEN both a Status_Filter and Sort_Control are active, THE Interaction_Matrix_Component SHALL include both query parameters in the API request.

### Requirement 7: Accessibility and Responsive Design

**User Story:** As a user with accessibility needs, I want the interaction matrix to be navigable with a keyboard and readable by screen readers, so that I can use the feature regardless of ability.

#### Acceptance Criteria

1. THE Interaction_Matrix_Component SHALL use semantic HTML elements: a `<table>` with `<thead>` and `<tbody>` for the matrix data, and appropriate heading elements for section titles.
2. THE Interaction_Matrix_Component SHALL ensure all interactive elements (links, buttons, filter controls) are keyboard-focusable and operable using Enter or Space keys.
3. THE Interaction_Matrix_Component SHALL provide `aria-label` attributes on status badges so screen readers announce the engagement status (e.g., "Status: Overdue").
4. THE Interaction_Matrix_Component SHALL use sufficient colour contrast (minimum WCAG AA ratio of 4.5:1 for text) for all status indicators against their backgrounds.
5. WHILE the matrix is loading, THE Interaction_Matrix_Component SHALL use an `aria-live` region or `aria-busy` attribute to announce the loading state to assistive technologies.
6. THE Interaction_Matrix_Component SHALL render responsively, adapting the table layout for viewports narrower than 768px (e.g., stacking rows as cards or enabling horizontal scroll).

### Requirement 8: Vitest Unit Tests

**User Story:** As a developer, I want comprehensive Vitest unit tests for the engagement service and matrix component, so that regressions are caught early and the component behaviour is documented.

#### Acceptance Criteria

1. THE test suite SHALL include unit tests for the Engagement_Service verifying that HTTP GET requests are made to the correct URL with correct query parameters for each combination of filter and sort options.
2. THE test suite SHALL include unit tests for the Interaction_Matrix_Component verifying that matrix rows are rendered correctly when the service returns data.
3. THE test suite SHALL include a unit test verifying that the loading indicator is displayed while data is being fetched and hidden once data arrives.
4. THE test suite SHALL include a unit test verifying that an error message and retry button are displayed when the service returns an error.
5. THE test suite SHALL include unit tests verifying that the correct CSS class or attribute is applied for each Engagement_Status value (OVERDUE, AT_RISK, ON_TRACK).
6. THE test suite SHALL include unit tests verifying that drill-through links contain the correct router paths with the appropriate employee ID.
7. THE test suite SHALL include a unit test verifying the Follow_Up_Section renders only entries where `followUpRequired` is true.
8. THE test suite SHALL include unit tests verifying that selecting a status filter triggers a new API call with the correct query parameter.
9. THE test suite SHALL include a unit test verifying the "no follow-ups needed" message when all entries have `followUpRequired` set to false.
