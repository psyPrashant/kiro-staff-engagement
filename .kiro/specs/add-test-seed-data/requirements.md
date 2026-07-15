# Requirements Document

## Introduction

Expand the existing test/seed data in the `SeedDataLoader` to provide a more realistic dataset for development and testing. The current seed script creates a minimal set of data (3 users, 5 employees, 4 interactions, 3 tasks). This feature adds additional users, employees, interactions, tasks, and scheduled interactions to better represent production-like conditions, enabling more meaningful UI development and testing scenarios.

## Glossary

- **Seed_Data_Loader**: The Spring Boot `ApplicationRunner` component (`SeedDataLoader.java`) that populates the database with test data on startup when running in "local" or "dev" profiles.
- **User**: A system user who logs interactions and is assigned tasks. Stored in the `users` table.
- **Employee**: A staff member who is the subject of interactions and engagement activities. Stored in the `employees` table.
- **Interaction**: A recorded engagement event between a User and an Employee (types: CHECK_IN, MENTORING, CATCH_UP, OTHER). Stored in the `interactions` table.
- **Task**: An action item arising from an interaction or assigned directly to an employee (statuses: OPEN, DONE). Stored in the `tasks` table.
- **Scheduled_Interaction**: A future planned engagement between a User and an Employee (completion statuses: PENDING, COMPLETED, CANCELLED). Stored in the `scheduled_interactions` table.
- **Idempotency_Check**: The existing mechanism that prevents duplicate seed data insertion by checking for the presence of the sentinel user email.

## Requirements

### Requirement 1: Add New Users

**User Story:** As a developer, I want additional seed users, so that I can test multi-user scenarios such as assignment distribution and interaction logging by different people.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader runs and seed data is not already present, THE Seed_Data_Loader SHALL create exactly 2 additional User records beyond the existing 3 users, resulting in a total of 5 users.
2. THE Seed_Data_Loader SHALL assign each new User a name that is unique among all 5 seeded users and a unique email address in the format `firstname.lastname@psybergate.com` that does not duplicate any existing seeded user email.
3. THE Seed_Data_Loader SHALL assign each new User a password hash derived from the same seed password used for the existing 3 users, verifiable by the application's configured PasswordEncoder.
4. IF the insertion of the new User records fails partway through, THEN THE Seed_Data_Loader SHALL roll back all seed data changes so that no partial seed state persists.

### Requirement 2: Add New Employees

**User Story:** As a developer, I want a larger pool of seed employees, so that I can test list views, filtering, and pagination with a realistic number of records.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader runs and seed data is not already present, THE Seed_Data_Loader SHALL create exactly 20 additional Employee records beyond the existing 5 employees, resulting in a total of 25 employees.
2. WHEN the Seed_Data_Loader creates the 20 new Employee records, THE Seed_Data_Loader SHALL assign each new Employee a name (maximum 255 characters), a unique email address (maximum 255 characters), and a job title (maximum 255 characters).
3. WHEN the Seed_Data_Loader creates the 20 new Employee records, THE Seed_Data_Loader SHALL assign at least 10 of the new Employees a valid manager reference pointing to an existing Employee record.
4. WHEN the Seed_Data_Loader creates the 20 new Employee records, THE Seed_Data_Loader SHALL use at least 8 distinct job titles across the 20 new employees.
5. IF a new Employee record fails to persist due to a duplicate email constraint violation, THEN THE Seed_Data_Loader SHALL skip that record without interrupting creation of the remaining Employee records.

### Requirement 3: Add Varied Interactions Per Employee

**User Story:** As a developer, I want each employee to have multiple interactions of different types, so that I can test the employee 360 view, interaction history, and timeline features with realistic data.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader runs and seed data is not already present, THE Seed_Data_Loader SHALL create exactly 20 Interaction records for each of the 20 new employees, resulting in a total of 400 new Interaction records.
2. THE Seed_Data_Loader SHALL distribute the interaction types (CHECK_IN, MENTORING, CATCH_UP, OTHER) across the 20 interactions per employee so that at least 3 of the 4 types are represented and no single type accounts for more than 10 of the 20 interactions for any employee.
3. THE Seed_Data_Loader SHALL assign `conducted_by_user_id` and `logged_by_user_id` values from the pool of all 5 users such that at least 3 distinct users appear as `conducted_by_user_id` across each employee's 20 interactions and at least 3 distinct users appear as `logged_by_user_id` across each employee's 20 interactions.
4. THE Seed_Data_Loader SHALL assign `occurred_at` timestamps within the past 12 months (relative to the current date at runtime) for each employee's interactions, such that at least 8 distinct calendar months are represented across the 20 interactions per employee.
5. THE Seed_Data_Loader SHALL assign a non-null project reference (from the pool of existing Project records) to at least 6 of the 20 interactions per employee (at least 30%).
6. THE Seed_Data_Loader SHALL provide a notes field for each interaction that is unique across all 400 new Interaction records and contains between 20 and 200 characters.

### Requirement 4: Add Varied Tasks Per User

**User Story:** As a developer, I want each user to have multiple tasks with different statuses and due dates, so that I can test the task dashboard, filtering by status, and overdue task indicators.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader runs and seed data is not already present, THE Seed_Data_Loader SHALL create exactly 5 Task records for each of the 5 seeded Employee records (linked via the `employee_id` field), resulting in a total of exactly 25 new Task records persisted in the database.
2. THE Seed_Data_Loader SHALL distribute task statuses across the 5 tasks per employee so that each employee has at least 2 tasks with status OPEN and at least 1 task with status DONE.
3. THE Seed_Data_Loader SHALL assign a non-null `due_date` to every task, including at least 1 task per employee with a due date in the past (before the current date) and at least 1 task per employee with a due date in the future (on or after the current date).
4. THE Seed_Data_Loader SHALL link each new Task to a valid Interaction record that already exists in the database at the time of task creation, referencing it via the `interaction_id` foreign key.
5. THE Seed_Data_Loader SHALL link each new Task to a valid Employee record via the `employee_id` field, where the referenced Employee already exists in the database at the time of task creation.
6. THE Seed_Data_Loader SHALL provide a title (1 to 255 characters) and a description (1 to 2000 characters) for each task, where no two tasks share the same title.
7. IF the Seed_Data_Loader runs and seed data is already present, THEN THE Seed_Data_Loader SHALL skip task creation and not insert any duplicate Task records.

### Requirement 5: Add Scheduled Interactions Per User

**User Story:** As a developer, I want each user to have scheduled interactions, so that I can test the scheduling features, upcoming interaction views, and completion status workflows.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader runs and seed data is not already present, THE Seed_Data_Loader SHALL create exactly 3 Scheduled_Interaction records for each of the 5 users (assigned via `scheduled_by_user_id`), resulting in a total of 15 new scheduled interactions.
2. THE Seed_Data_Loader SHALL distribute the interaction types (CHECK_IN, MENTORING, CATCH_UP, OTHER) across the 3 scheduled interactions per user so that at least 2 different types are represented per user.
3. THE Seed_Data_Loader SHALL distribute the completion statuses (PENDING, COMPLETED, CANCELLED) across the 3 scheduled interactions per user so that at least 2 different statuses are represented per user.
4. THE Seed_Data_Loader SHALL assign each Scheduled_Interaction a valid `employee_id` reference from the pool of all employees, with at least 5 distinct employees referenced across the 15 total scheduled interactions.
5. THE Seed_Data_Loader SHALL assign scheduled dates that are logically consistent with completion status: COMPLETED and CANCELLED records SHALL have a `scheduled_date` between 1 and 90 days before the current date, and PENDING records SHALL have a `scheduled_date` between 1 and 30 days after the current date.
6. THE Seed_Data_Loader SHALL provide a non-null notes value between 10 and 200 characters for each scheduled interaction, with no two scheduled interactions sharing identical notes text.
7. IF the Seed_Data_Loader creates a Scheduled_Interaction with a `scheduled_date` in the future and a completion status other than PENDING, THEN the record SHALL be considered invalid and the Seed_Data_Loader SHALL NOT insert it.

### Requirement 6: Preserve Existing Seed Data and Idempotency

**User Story:** As a developer, I want the expanded seed data to coexist with the existing seed data without breaking the idempotency mechanism, so that repeated application restarts do not cause duplicate data or errors.

#### Acceptance Criteria

1. THE Seed_Data_Loader SHALL retain all existing seed data records (3 original users, 5 original employees, 2 companies, 3 projects, 4 original interactions, 3 original tasks) with identical field values and foreign key relationships as before the expansion.
2. THE Seed_Data_Loader SHALL use the existing idempotency check (presence of a user with email `alice.johnson@psybergate.com`) to determine whether to run the seed insertion, inserting both original and new records in a single atomic operation.
3. WHEN the Seed_Data_Loader detects that seed data is already present, THE Seed_Data_Loader SHALL skip all seed data insertion including the new records, complete without throwing an exception, and log a message indicating that insertion was skipped.
4. IF the Seed_Data_Loader is executed multiple consecutive times on an already-seeded database, THEN THE Seed_Data_Loader SHALL produce no change in total row counts across all seeded tables (users, employees, companies, projects, interactions, tasks) after each subsequent execution.

### Requirement 7: Data Consistency and Referential Integrity

**User Story:** As a developer, I want all seed data to maintain valid foreign key relationships and logical date ordering, so that the seeded database accurately represents a valid application state.

#### Acceptance Criteria

1. THE Seed_Data_Loader SHALL create all new records with valid foreign key references that satisfy database constraints, including self-referencing relationships (e.g., `employees.manager_id` referencing an existing `employees.id`).
2. THE Seed_Data_Loader SHALL create Interaction records with `occurred_at` timestamps that precede the current date and with `created_at` timestamps equal to or later than the corresponding `occurred_at` value.
3. THE Seed_Data_Loader SHALL create Scheduled_Interaction records with `scheduled_date` values that are consistent with their `completion_status`: past dates (before today) for COMPLETED or CANCELLED, and future dates (today or later) for PENDING.
4. THE Seed_Data_Loader SHALL create Task records with `due_date` values that are consistent with their status: OPEN tasks may have a past due date, a future due date, or a NULL due date; DONE tasks shall have a past or NULL due_date.
5. THE Seed_Data_Loader SHALL insert records in dependency order: Users and Companies first, then Employees (with non-manager employees inserted before or updated after their managers to satisfy the self-referencing foreign key), then Projects, then Interactions, then Tasks and Scheduled_Interactions.
6. IF a database constraint violation occurs during seed data loading, THEN THE Seed_Data_Loader SHALL halt execution without committing partial data and report an error message indicating which record and constraint failed.
