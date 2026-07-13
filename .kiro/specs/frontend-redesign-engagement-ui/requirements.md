# Requirements Document

## Introduction

This specification covers the complete frontend redesign of the Staff Engagement Angular 21 application. The work replaces the unstyled, cramped default rendering with a custom CSS design-token system, fixes raw enum values leaking into the UI, reworks navigation to match the manager workflow, and completes two stub pages (Employees list, Tasks) including per-employee task creation. The redesign is structured across three phases: Foundation (tokens and shared classes), Restyle existing screens and label fixes, and Complete stub pages with new task features.

## Glossary

- **Design_Token_System**: A set of CSS custom properties defined in `src/styles.css` providing consistent color, typography, spacing, radius, and shadow values consumed by all components.
- **Shell**: The Angular `ShellComponent` that wraps authenticated routes with top-level navigation and layout.
- **Dashboard**: The landing page showing the engagement interaction matrix, triage stats, and suggested follow-ups.
- **Interaction_Matrix**: A table component displaying per-employee engagement status, recency, frequency, and action links.
- **Follow_Up_Section**: A sub-component of the dashboard listing employees requiring follow-up with status badges.
- **Employee_360**: The single-employee profile view showing summary, interaction history, and open tasks.
- **Employees_List**: The roster table page listing all employees with search, status filter, and drill-through to Employee 360.
- **Log_Interaction_Form**: The form for recording a new interaction with an employee including optional project context and follow-up task.
- **Task_List**: The page listing all tasks with status filter, completion toggle, and a new-task form.
- **Task_Create_Dialog**: An accessible modal opened from Employee 360 for creating a task with the employee pre-filled.
- **Label_Helper**: A pure function that converts a backend enum string value to a human-readable display label.
- **Badge**: A styled inline element rendering a status label with a color-coded background indicating severity or state.
- **Page_Container**: A globally defined `.page` CSS class providing max-width, centering, and horizontal padding to page content.

## Requirements

### Requirement 1: Design Token Foundation

**User Story:** As a developer, I want a single source of truth for design tokens in `src/styles.css`, so that all components render with consistent visual styling.

#### Acceptance Criteria

1. THE Design_Token_System SHALL define CSS custom properties for color (background, surface, text, muted text, border, primary, danger, warning, success, and soft status backgrounds), typography (font family and scale from xs to 2xl with weights 400/500), spacing (space-1 through space-8 mapping to 4px–64px), border radius (controls and cards), and box shadow (sm and default).
2. THE Design_Token_System SHALL set `:root { color-scheme: light }` and style the `body` element with the background color token, text color token, and sans font-family token.
3. THE Design_Token_System SHALL include a CSS reset applying `box-sizing: border-box` to all elements and removing default margins from `body`.
4. THE Design_Token_System SHALL define base element styles for `h1`–`h3`, `a`, `button`, `input`, `select`, `textarea`, and `table` that consume the defined tokens.
5. THE Design_Token_System SHALL define a visible `:focus-visible` outline using the primary color token on all interactive elements.
6. WHEN any page loads, THE Page_Container SHALL center content within a maximum width of approximately 1100px with horizontal padding using spacing tokens.

### Requirement 2: Shared Global CSS Classes

**User Story:** As a developer, I want shared utility classes defined globally once, so that any component template can use them without duplicating styles.

#### Acceptance Criteria

1. THE Design_Token_System SHALL define a `.page` class providing max-width centering and horizontal padding.
2. THE Design_Token_System SHALL define a `.card` class providing surface background, border, card border-radius, and padding.
3. THE Design_Token_System SHALL define `.btn`, `.btn-primary`, and `.btn-secondary` classes with appropriate padding, radius, font sizing, and color tokens.
4. THE Design_Token_System SHALL define a `.form-field` class for consistent label, input, and spacing within forms.
5. THE Design_Token_System SHALL define `.badge`, `.badge-danger`, `.badge-warning`, and `.badge-success` classes using the soft status background and corresponding text color tokens.
6. WHEN a shared class is applied in any component template, THE Design_Token_System SHALL render consistent styling without requiring component-scoped overrides.

### Requirement 3: Navigation Redesign

**User Story:** As a manager, I want the navigation to reflect my workflow (Dashboard, Employees, Log interaction, Tasks), so that I can reach the screens I use most with minimal clicks.

#### Acceptance Criteria

1. THE Shell SHALL render a navigation bar containing a brand label on the left, links for Dashboard, Employees, Log interaction, and Tasks in the center, and a Logout button right-aligned.
2. THE Shell SHALL include a link to `/dashboard` in the primary navigation.
3. THE Shell SHALL remove User and Client from the primary navigation (routes remain accessible but are not shown in the nav bar).
4. WHEN the current route matches a navigation link, THE Shell SHALL visually highlight that link using `routerLinkActive`.
5. THE Shell SHALL be fully keyboard-navigable with visible focus indicators.
6. WHEN the viewport is 375px wide, THE Shell SHALL render navigation without horizontal overflow.
7. THE Shell SHALL preserve all existing `data-testid` attributes on navigation elements.

### Requirement 4: Login Page Styling

**User Story:** As a user, I want the login page to appear professional and centered, so that I have confidence in the application.

#### Acceptance Criteria

1. WHEN the login page loads, THE Login_Component SHALL render a centered card with a branded header, styled email and password fields, and a primary submit button.
2. WHEN invalid credentials are submitted, THE Login_Component SHALL display a styled inline error message.
3. THE Login_Component SHALL provide visible focus states on all form inputs.
4. WHEN the viewport is 375px wide, THE Login_Component SHALL remain usable without horizontal scroll.

### Requirement 5: Engagement Status Label Helpers

**User Story:** As a manager, I want engagement statuses displayed as readable labels (Overdue, At risk, On track), so that I do not see raw backend enum values.

#### Acceptance Criteria

1. THE Label_Helper SHALL provide a `formatEngagementStatusLabel` function in `dashboard/models/engagement.model.ts` that maps `OVERDUE` to "Overdue", `AT_RISK` to "At risk", and `ON_TRACK` to "On track".
2. WHEN an engagement status Badge is rendered in the Interaction_Matrix, THE Interaction_Matrix SHALL display the output of `formatEngagementStatusLabel` instead of the raw enum value.
3. WHEN an engagement status Badge is rendered in the Follow_Up_Section, THE Follow_Up_Section SHALL display the output of `formatEngagementStatusLabel` instead of the raw enum value.
4. THE Label_Helper SHALL provide a `formatTaskStatusLabel` function in the task models that maps `OPEN` to "Open" and `DONE` to "Done".
5. IF an unknown enum value is passed to a Label_Helper, THEN THE Label_Helper SHALL return the value formatted with title case and underscores replaced by spaces.

### Requirement 6: Dashboard Styling and Triage Stats

**User Story:** As a manager, I want the dashboard to show a triage summary and styled matrix, so that I can quickly assess team engagement at a glance.

#### Acceptance Criteria

1. THE Dashboard SHALL display a triage stat row above the Interaction_Matrix showing counts for Overdue, At risk, and On track derived from the matrix data.
2. THE Interaction_Matrix SHALL style the table with header styling, row hover effects, and consistent cell padding using spacing tokens.
3. THE Interaction_Matrix SHALL render status Badges with `.badge-danger` for Overdue, `.badge-warning` for At risk, and `.badge-success` for On track.
4. THE Follow_Up_Section SHALL be styled with appropriate card styling, spacing, and status badges consuming design tokens.
5. WHEN the viewport is below 768px, THE Dashboard SHALL switch to a responsive stacked-card layout.
6. THE Dashboard SHALL style loading, error, and empty states consistently with design tokens.

### Requirement 7: Log Interaction Form Styling

**User Story:** As a manager, I want the log interaction form to be clearly laid out with grouped project selection, so that recording interactions is intuitive.

#### Acceptance Criteria

1. THE Log_Interaction_Form SHALL style all form fields (Employee, Conducted by, Type, Notes, Occurred at, Project) using the `.form-field` global class and design tokens.
2. THE Log_Interaction_Form SHALL render the project picker as an `<optgroup>`-based select where projects are grouped under their parent company name.
3. THE Log_Interaction_Form SHALL style the inline "Add follow-up task" toggle and its nested fields consistently with the rest of the form.
4. WHEN a validation error occurs, THE Log_Interaction_Form SHALL display styled inline error messages below the relevant field.
5. WHEN a submission succeeds, THE Log_Interaction_Form SHALL display a styled confirmation message.
6. WHILE the form is submitting, THE Log_Interaction_Form SHALL disable the submit button and show a "Submitting..." label.

### Requirement 8: Employee 360 Styling and Label Fixes

**User Story:** As a manager, I want the Employee 360 view to display interaction types as readable labels and show project context, so that I have full visibility into each employee's engagement history.

#### Acceptance Criteria

1. THE Employee_360 SHALL style the profile header section with the employee name, details, and an engagement status Badge.
2. THE Employee_360 SHALL display each interaction's type using `formatInteractionTypeLabel` instead of the raw enum value.
3. WHEN an interaction has a `projectContext`, THE Employee_360 SHALL display the project name and company name; WHEN `projectContext` is null, THE Employee_360 SHALL display "No project".
4. THE Employee_360 SHALL visually distinguish overdue tasks (due date in the past) using the danger color token.
5. THE Employee_360 SHALL render header actions for "Log interaction" (linking to the interaction form with employee pre-selected) and "New task" (opening the Task_Create_Dialog).
6. THE Employee_360 SHALL preserve all existing `data-testid` attributes.

### Requirement 9: Employees List Page

**User Story:** As a manager, I want to see a roster of all employees with search and status filtering, so that I can quickly find and navigate to any team member's 360 view.

#### Acceptance Criteria

1. THE Employees_List SHALL replace the stub content and display a table with columns: avatar/initials, name, job title, manager, engagement status badge, and last seen date.
2. THE Employees_List SHALL fetch data from `GET /api/employees` and join engagement status and last-seen from `GET /api/engagement/matrix` by employee ID.
3. THE Employees_List SHALL provide a search input filtering employees client-side by name or job title.
4. THE Employees_List SHALL provide a status filter control to filter by engagement status (All, Overdue, At risk, On track).
5. WHEN a row is clicked, THE Employees_List SHALL navigate to `/employee/{id}` (the Employee 360 view).
6. THE Employees_List SHALL render engagement status badges using the shared Label_Helper and Badge classes.
7. THE Employees_List SHALL display loading, empty, and error states styled with design tokens.
8. WHEN the viewport is 375px wide, THE Employees_List SHALL remain usable without horizontal page scroll.

### Requirement 10: Task List Page

**User Story:** As a manager, I want a dedicated tasks page listing all tasks with filtering and completion toggling, so that I can manage follow-up actions across my team.

#### Acceptance Criteria

1. THE Task_List SHALL replace the stub content and display tasks from `GET /api/tasks` in a table with columns: title, employee, assignee, due date, and status.
2. THE Task_List SHALL provide a status filter control allowing filtering by Open, Done, or All.
3. THE Task_List SHALL display task status using the `formatTaskStatusLabel` Label_Helper.
4. WHEN a task is overdue (due date in the past and status is Open), THE Task_List SHALL highlight the row using the danger color token.
5. THE Task_List SHALL provide a completion toggle allowing a task's status to be changed between Open and Done.
6. THE Task_List SHALL include a "New task" form with fields: Employee (required select), Assignee (select), Title (required, max 255 characters), Description (optional, max 2000 characters), Due date, Status, and optional "Link to interaction" select.
7. WHEN the new-task form is submitted, THE Task_List SHALL POST to `/api/tasks` including the `employeeId` field and refresh the task list on success.
8. THE Task_List SHALL display loading, empty, and error states styled with design tokens.
9. WHEN a validation error occurs on the new-task form, THE Task_List SHALL display styled inline error messages.

### Requirement 11: Create Task from Employee 360 Dialog

**User Story:** As a manager viewing an employee's 360 page, I want to create a task for that employee via a modal dialog, so that I can quickly assign follow-ups without leaving the context.

#### Acceptance Criteria

1. WHEN the "New task" button is clicked on Employee_360, THE Task_Create_Dialog SHALL open an accessible modal overlay.
2. THE Task_Create_Dialog SHALL pre-fill the Employee field with the current employee and render it as non-editable.
3. THE Task_Create_Dialog SHALL default the Assignee field to the current logged-in user.
4. THE Task_Create_Dialog SHALL provide fields for Title (required, max 255), Description (optional, max 2000), Due date, and an optional "Link to interaction" select populated from the employee's recent interactions.
5. WHEN the form is submitted successfully, THE Task_Create_Dialog SHALL close and the Employee_360 open-tasks list SHALL refresh to include the new task.
6. THE Task_Create_Dialog SHALL be focus-trapped (focus cannot leave the modal while open).
7. WHEN the Escape key is pressed, THE Task_Create_Dialog SHALL close without saving.
8. THE Task_Create_Dialog SHALL reuse the same form component used by the Task_List new-task form.

### Requirement 12: Accessibility and Responsiveness

**User Story:** As a user with assistive technology, I want the application to meet accessibility standards, so that I can operate it effectively with a keyboard or screen reader.

#### Acceptance Criteria

1. THE Design_Token_System SHALL ensure all text and Badge elements meet WCAG AA contrast ratios (4.5:1 for normal text, 3:1 for large text).
2. THE Shell SHALL be fully operable via keyboard alone with visible focus indicators.
3. THE Task_Create_Dialog SHALL implement focus trapping and appropriate `aria-*` attributes (role="dialog", aria-modal="true", aria-labelledby).
4. WHEN interactive or status elements are rendered, THE components SHALL include appropriate `aria-label` or `aria-live` attributes.
5. WHEN the viewport is 375px wide, THE application SHALL render all pages without horizontal page scroll.
6. WHEN the viewport is 1280px wide, THE application SHALL render all pages within the Page_Container max-width.

### Requirement 13: Preserve Existing Test Infrastructure

**User Story:** As a developer, I want existing test hooks and data-testid attributes preserved, so that acceptance tests and unit tests continue to pass after the redesign.

#### Acceptance Criteria

1. THE redesign SHALL preserve all existing `data-testid` attributes in their current location within component templates.
2. WHEN unit tests assert on previously raw enum text (e.g., "OVERDUE"), THE unit tests SHALL be updated to assert on the new human-readable label (e.g., "Overdue").
3. THE redesign SHALL ensure all Vitest unit tests pass after modifications.
