# Requirements Document

## Introduction

This feature closes out the data layer for the Staff Engagement platform. It introduces a seed-data loader that pre-populates the local and dev environments with sample employees, companies, and projects so the application is immediately usable and demo-able without manual data entry. It also provides a formal data-layer sign-off confirming that all five domain modules compile together, Flyway migrations apply cleanly on a fresh database, and the full test suite passes.

## Glossary

- **Seed_Data_Loader**: A Spring component that inserts sample data into the database on application startup, active only under specific Spring profiles.
- **Module**: One of the five domain packages in the backend: user, employee, client, interaction, task.
- **Flyway_Migration**: A versioned SQL script managed by Flyway that evolves the database schema.
- **Profile**: A Spring Boot profile (e.g., `local`, `dev`) that activates environment-specific configuration.
- **Fresh_Database**: A PostgreSQL database with no prior application tables, requiring all migrations to run from scratch.
- **Build_Verification**: Execution of `mvn verify` across the backend project confirming compilation, tests, and integration checks pass.

## Requirements

### Requirement 1: Profile-Scoped Seed Data Loading

**User Story:** As a developer, I want the application to automatically load sample data when running with the local or dev profile, so that the system is immediately usable without manual data entry.

#### Acceptance Criteria

1. WHEN the application starts with the `local` profile active, THE Seed_Data_Loader SHALL insert sample data into the database before the application signals readiness (health endpoint returns UP).
2. WHEN the application starts with the `dev` profile active, THE Seed_Data_Loader SHALL insert sample data into the database before the application signals readiness.
3. WHEN the application starts with a profile other than `local` or `dev` (e.g., `prod`, `staging`, default), THE Seed_Data_Loader SHALL NOT execute and no seed records SHALL be inserted.
4. WHEN the Seed_Data_Loader executes and at least one seed User record already exists in the database (determined by email match), THE Seed_Data_Loader SHALL skip all insertion and log an INFO-level message indicating seed data already present.
5. IF the Seed_Data_Loader encounters an unrecoverable error during insertion, THEN the application SHALL fail to start and log an ERROR-level message with the root cause.

### Requirement 2: Sample Employee Data

**User Story:** As a developer, I want sample employees pre-loaded in the system, so that I can test and demonstrate employee-related features without creating data manually.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL insert at least five sample Employee records, each with a unique name (1–255 characters), a unique email (1–255 characters containing exactly one "@" followed by a domain), and a non-null job title (1–255 characters).
2. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL assign at least one Employee a manager reference that points to another Employee created within the same seed execution.
3. THE Seed_Data_Loader SHALL produce Employee records that satisfy all database constraints defined in the employees table schema, including non-null name, non-null email, unique email, valid manager_id foreign key reference, and non-null created_at timestamp.
4. IF the Seed_Data_Loader executes and the sample Employee records already exist in the database, THEN THE Seed_Data_Loader SHALL skip insertion of those records without raising an error and without creating duplicates.

### Requirement 3: Sample Company and Project Data

**User Story:** As a developer, I want sample companies and projects pre-loaded, so that I can test and demonstrate client-related features.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL insert at least two sample Company records with distinct names (1–255 characters) and non-null created_at timestamps.
2. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL insert at least three sample Project records with distinct names, each linked to a seeded Company via a valid company_id foreign key, distributed across at least two different seeded Companies.
3. THE Seed_Data_Loader SHALL produce Company and Project records that satisfy all database constraints defined in the companies and projects table schemas, including non-null name, non-null company_id reference, and non-null created_at timestamps.

### Requirement 4: Sample User Data

**User Story:** As a developer, I want sample users pre-loaded, so that I can test and demonstrate user-related features such as interactions and task assignment.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL insert at least three sample User records, each with a name between 1 and 255 characters, and a unique email address in valid email format (containing exactly one "@" separating a local part and a domain part) with a maximum length of 255 characters.
2. THE Seed_Data_Loader SHALL produce User records that satisfy all database constraints defined in the users table schema, including: non-null name, non-null unique email, and a non-null created_at timestamp set to the time of insertion.
3. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL ensure that no two seeded User records share the same name value.

### Requirement 5: Sample Interaction and Task Data

**User Story:** As a developer, I want sample interactions and tasks pre-loaded, so that I can demonstrate the full engagement workflow end-to-end.

#### Acceptance Criteria

1. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL insert at least three sample Interaction records covering at least two distinct type values from the allowed set (CHECK_IN, MENTORING, CATCH_UP, OTHER).
2. WHEN the Seed_Data_Loader executes, THE Seed_Data_Loader SHALL insert at least three sample Task records, at least one with status OPEN and at least one with status DONE.
3. THE Seed_Data_Loader SHALL link each seeded Interaction to a seeded Employee (employee_id), a seeded conducted-by User (conducted_by_user_id), and a seeded logged-by User (logged_by_user_id).
4. THE Seed_Data_Loader SHALL link at least one seeded Interaction to a seeded Project (project_id) to demonstrate the full workflow including project context.
5. THE Seed_Data_Loader SHALL link at least one seeded Task to a seeded Interaction (interaction_id) and a seeded assigned User (assigned_user_id).
6. THE Seed_Data_Loader SHALL insert at least one Task with a non-null due_date value to cover due-date-based filtering and display.
7. THE Seed_Data_Loader SHALL produce Interaction and Task records that satisfy all database constraints defined in the interactions and tasks table schemas.

### Requirement 6: Migration Integrity on Fresh Database

**User Story:** As a developer, I want confidence that all Flyway migrations apply cleanly on a fresh database, so that new environments can be provisioned reliably.

#### Acceptance Criteria

1. WHEN `./mvnw verify` runs against a Fresh_Database, THE Flyway_Migration system SHALL apply all versioned migrations and each migration SHALL report a SUCCESS state.
2. WHEN all migrations complete, THE database schema SHALL pass Hibernate's `ddl-auto=validate` check without exceptions, confirming JPA entity mappings align with the physical schema for all five Modules (user, employee, client, interaction, task).
3. WHEN all migrations complete, THE Flyway schema history table SHALL report zero pending migrations remaining.
4. IF any Flyway migration fails, THEN the build SHALL exit with a non-zero exit code and log the failing migration version and error.

### Requirement 7: Cross-Module Compilation Integrity

**User Story:** As a developer, I want assurance that all five domain modules compile and integrate together, so that I can build on a stable data layer.

#### Acceptance Criteria

1. WHEN `./mvnw verify` executes from the `staff-engagement-backend/` directory, THE Build_Verification SHALL compile all five domain packages (user, employee, client, interaction, task) and exit with code 0, producing zero compilation errors including annotation processing.
2. WHEN `./mvnw verify` executes from the `staff-engagement-backend/` directory, THE Build_Verification SHALL execute all unit and integration tests and report zero test failures and zero test errors.
3. WHEN `./mvnw verify` completes successfully, THE Build_Verification SHALL confirm that each of the five domain packages (user, employee, client, interaction, task) contains at least one compiled class file in the build output.

### Requirement 8: Seed Data Insertion Order

**User Story:** As a developer, I want seed data inserted in the correct order respecting foreign key constraints, so that the loader never fails due to referential integrity violations.

#### Acceptance Criteria

1. THE Seed_Data_Loader SHALL insert records in dependency order: Users, then Companies, then Employees, then Projects, then Interactions, then Tasks.
2. THE Seed_Data_Loader SHALL insert Employee records that have no manager reference before Employee records that reference a manager, so that self-referential foreign keys are satisfied.
3. IF a foreign-key constraint violation occurs during seed data insertion, THEN THE Seed_Data_Loader SHALL roll back all inserted seed records, log an error message indicating the failing entity and constraint, and prevent the application from completing startup.
4. WHEN the Seed_Data_Loader completes successfully, THE Seed_Data_Loader SHALL have inserted every seed record within a single database transaction.

### Requirement 9: Acceptance Test Verification of Seed Data

**User Story:** As a developer, I want a Cucumber acceptance test that confirms the seeded data is accessible through the API, so that I have automated confidence the seed loader works end-to-end in a running environment.

#### Acceptance Criteria

1. WHEN the acceptance test suite runs against an application started with the `local` profile, THEN a Cucumber scenario SHALL verify that `GET /api/employees` returns at least five Employee records.
2. WHEN the acceptance test suite runs, THEN a Cucumber scenario SHALL verify that `GET /api/companies` returns at least two Company records.
3. WHEN the acceptance test suite runs, THEN a Cucumber scenario SHALL verify that `GET /api/projects` returns at least three Project records.
4. WHEN the acceptance test suite runs, THEN a Cucumber scenario SHALL verify that `GET /api/users` returns at least three User records.
5. WHEN the acceptance test suite runs, THEN a Cucumber scenario SHALL verify that `GET /api/interactions` returns at least three Interaction records covering at least two distinct type values.
6. WHEN the acceptance test suite runs, THEN a Cucumber scenario SHALL verify that `GET /api/tasks` returns at least three Task records with at least one OPEN and at least one DONE.
7. THE acceptance test SHALL be implemented using the existing four-layer Cucumber + Spring harness in the `acceptance-tests/` module, following the project's established API driver pattern.
