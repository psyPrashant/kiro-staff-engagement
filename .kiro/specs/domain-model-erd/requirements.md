# Requirements Document

## Introduction

This feature establishes the domain model and database schema for the Staff Engagement platform. It defines JPA entity classes for six core entities (User, Employee, Company, Project, Interaction, Task), their relationships, enumerations, Flyway migration scripts, and Spring Data JPA repositories. The cross-module reference strategy uses direct JPA relationships within a shared PostgreSQL schema, with entities residing in their respective module packages.

## Glossary

- **Entity_Layer**: The set of JPA-annotated classes representing database tables
- **Migration_Script**: A Flyway versioned SQL file that creates or alters the database schema
- **Repository_Layer**: Spring Data JPA interfaces providing CRUD operations for entities
- **Interaction_Type**: An enumeration with values CHECK_IN, MENTORING, CATCH_UP, OTHER representing the kind of interaction
- **Task_Status**: An enumeration with values OPEN, DONE representing the lifecycle state of a task
- **Cross_Module_Reference**: The strategy by which entity classes in different packages reference each other via direct JPA associations (@ManyToOne, @OneToMany)
- **Schema**: The PostgreSQL database schema containing all application tables
- **Module_Package**: A Java package under `com.psybergate.staff_engagement` representing a bounded context (user, employee, client, interaction, task)

## Requirements

### Requirement 1: User Entity

**User Story:** As a developer, I want a User JPA entity mapped to the database, so that the system can persist and retrieve user records.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define a User entity in the `user` package mapped to a table named `users` with fields: id (bigint, primary key, auto-generated), name (string, not null, maximum 255 characters), email (string, not null, unique, maximum 255 characters), and created_at (timestamp, not null).
2. THE Entity_Layer SHALL annotate the User entity with JPA annotations (@Entity, @Table, @Id, @GeneratedValue, @Column) and Lombok annotations (@Getter, @Setter, @NoArgsConstructor).
3. WHEN a User record is persisted, THE Entity_Layer SHALL auto-generate the id using the IDENTITY generation strategy.
4. WHEN a User record is persisted without an explicit created_at value, THE Entity_Layer SHALL automatically set created_at to the current timestamp using a @PrePersist callback.
5. IF a User record is persisted with a name or email that exceeds 255 characters, THEN THE Entity_Layer SHALL reject the persist operation with a validation error.

### Requirement 2: Employee Entity

**User Story:** As a developer, I want an Employee JPA entity mapped to the database, so that the system can persist and retrieve employee records with optional self-referencing manager relationships.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define an Employee entity in the `employee` package with fields: id (bigint, primary key, auto-generated using sequence or identity strategy), name (string, not null, maximum 255 characters), email (string, not null, unique, maximum 255 characters), manager_id (bigint, nullable, self-referencing foreign key), job_title (string, nullable, maximum 255 characters), and created_at (timestamp, not null).
2. THE Entity_Layer SHALL annotate the Employee entity with JPA annotations (@Entity, @Table, @Id, @GeneratedValue, @Column) and Lombok annotations (@Getter, @Setter, @NoArgsConstructor).
3. THE Entity_Layer SHALL model the manager relationship as a @ManyToOne self-referencing association on the Employee entity with FetchType.LAZY.
4. WHEN an Employee record is persisted without a manager, THE Entity_Layer SHALL store null for the manager_id column.
5. IF an Employee record is persisted with a manager_id that does not reference an existing Employee, THEN THE Entity_Layer SHALL reject the persistence operation with a constraint violation.

### Requirement 3: Company Entity

**User Story:** As a developer, I want a Company JPA entity mapped to the database, so that the system can persist and retrieve company (client) records.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define a Company entity in the `client` package mapped to the `companies` table with fields: id (bigint, primary key, auto-generated), name (string, not null, maximum 255 characters), and created_at (timestamp, not null).
2. THE Entity_Layer SHALL annotate the Company entity with JPA annotations (@Entity, @Table, @Id, @GeneratedValue, @Column) and Lombok annotations (@Getter, @Setter, @NoArgsConstructor).
3. WHEN a Company record is persisted, THE Entity_Layer SHALL auto-generate the id using a sequence or identity strategy.

### Requirement 4: Project Entity

**User Story:** As a developer, I want a Project JPA entity mapped to the database, so that the system can persist and retrieve projects that belong to a company.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define a Project entity in the `client` package mapped to the `projects` table with fields: id (bigint, primary key, auto-generated), name (string, not null, maximum 255 characters), company_id (bigint, not null, foreign key referencing Company), and created_at (timestamp, not null).
2. THE Entity_Layer SHALL annotate the Project entity with JPA annotations (@Entity, @Table, @Id, @GeneratedValue, @Column) and Lombok annotations (@Getter, @Setter, @NoArgsConstructor).
3. THE Entity_Layer SHALL model the company relationship as a @ManyToOne association from Project to Company with FetchType.LAZY.
4. WHEN a Project record is persisted, THE Entity_Layer SHALL enforce a non-null reference to an existing Company.
5. WHEN a Project record is persisted without an explicit created_at value, THE Entity_Layer SHALL automatically set created_at to the current timestamp using a @PrePersist callback.

### Requirement 5: Interaction Entity

**User Story:** As a developer, I want an Interaction JPA entity mapped to the database, so that the system can persist and retrieve interaction records between users and employees.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define an Interaction entity in the `interaction` package mapped to the `interactions` table with fields: id (bigint, primary key, auto-generated), employee_id (bigint, not null, foreign key referencing Employee), conducted_by_user_id (bigint, not null, foreign key referencing User), logged_by_user_id (bigint, not null, foreign key referencing User), project_id (bigint, nullable, foreign key referencing Project), type (Interaction_Type enum, not null), notes (text, not null), occurred_at (timestamp, not null), and created_at (timestamp, not null).
2. THE Entity_Layer SHALL annotate the Interaction entity with JPA annotations (@Entity, @Table, @Id, @GeneratedValue, @Column) and Lombok annotations (@Getter, @Setter, @NoArgsConstructor).
3. THE Entity_Layer SHALL model conducted_by_user_id and logged_by_user_id as separate @ManyToOne associations to the User entity with FetchType.LAZY.
4. THE Entity_Layer SHALL model the project association as an optional @ManyToOne from Interaction to Project with FetchType.LAZY.
5. THE Entity_Layer SHALL persist the type field as a string using @Enumerated(EnumType.STRING).
6. WHEN an Interaction record is persisted without an explicit created_at value, THE Entity_Layer SHALL automatically set created_at to the current timestamp using a @PrePersist callback.

### Requirement 6: Task Entity

**User Story:** As a developer, I want a Task JPA entity mapped to the database, so that the system can persist and retrieve tasks that optionally originate from an interaction.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define a Task entity in the `task` package mapped to the `tasks` table with fields: id (bigint, primary key, auto-generated), interaction_id (bigint, nullable, foreign key referencing Interaction), title (string, not null, maximum 255 characters), description (text, nullable, maximum 2000 characters), status (Task_Status enum, not null, default OPEN for new entities), due_date (date, nullable), assigned_user_id (bigint, nullable, foreign key referencing User), and created_at (timestamp, not null, auto-populated on first persist).
2. THE Entity_Layer SHALL annotate the Task entity with JPA annotations (@Entity, @Table, @Id, @GeneratedValue, @Column) and Lombok annotations (@Getter, @Setter, @NoArgsConstructor).
3. THE Entity_Layer SHALL model the interaction association as an optional @ManyToOne from Task to Interaction with FetchType.LAZY.
4. THE Entity_Layer SHALL model the assigned_user association as an optional @ManyToOne from Task to User with FetchType.LAZY.
5. THE Entity_Layer SHALL persist the status field as a string using @Enumerated(EnumType.STRING).
6. WHEN a Task entity is persisted for the first time, THE Entity_Layer SHALL auto-populate the created_at field with the current timestamp via a @PrePersist callback.

### Requirement 7: Interaction Type Enumeration

**User Story:** As a developer, I want a well-defined enumeration for interaction types, so that the system constrains interaction records to valid types.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define an InteractionType enum in the `interaction` package with exactly four values in this order: CHECK_IN, MENTORING, CATCH_UP, OTHER.
2. THE Migration_Script SHALL enforce the interaction type constraint using a CHECK constraint on the type column that permits only the values 'CHECK_IN', 'MENTORING', 'CATCH_UP', 'OTHER', matching the Java enum names exactly.
3. IF a record is inserted or updated with a type value not in the set ('CHECK_IN', 'MENTORING', 'CATCH_UP', 'OTHER'), THEN THE Migration_Script SHALL cause the database to reject the operation via the CHECK constraint violation.

### Requirement 8: Task Status Enumeration

**User Story:** As a developer, I want a well-defined enumeration for task statuses, so that the system constrains task records to valid lifecycle states.

#### Acceptance Criteria

1. THE Entity_Layer SHALL define a TaskStatus enum in the `task` package with exactly two values in this order: OPEN, DONE.
2. THE Migration_Script SHALL enforce the task status constraint using a CHECK constraint on the tasks.status column that permits only the values 'OPEN' and 'DONE'.
3. IF an INSERT or UPDATE statement attempts to set the tasks.status column to a value not in ('OPEN', 'DONE'), THEN THE Migration_Script's CHECK constraint SHALL cause the database to reject the operation.
4. THE Entity_Layer SHALL ensure the TaskStatus enum value names match exactly the string literals used in the Migration_Script CHECK constraint, so that @Enumerated(EnumType.STRING) persistence produces only values accepted by the constraint.

### Requirement 9: Flyway Migration Script

**User Story:** As a developer, I want a Flyway migration script that creates all domain tables, so that the database schema is versioned and reproducible.

#### Acceptance Criteria

1. THE Migration_Script SHALL create tables in the following dependency order: users, employees, companies, projects, interactions, tasks — with all columns, data types, NOT NULL constraints, and default values as defined in Requirements 1–6.
2. THE Migration_Script SHALL create foreign key constraints for: employees.manager_id → employees.id, projects.company_id → companies.id, interactions.employee_id → employees.id, interactions.conducted_by_user_id → users.id, interactions.logged_by_user_id → users.id, interactions.project_id → projects.id, tasks.interaction_id → interactions.id, and tasks.assigned_user_id → users.id.
3. THE Migration_Script SHALL create unique constraints on users.email and employees.email.
4. THE Migration_Script SHALL create CHECK constraints for interactions.type (CHECK_IN, MENTORING, CATCH_UP, OTHER) and tasks.status (OPEN, DONE).
5. THE Migration_Script SHALL be stored as `V3__create_domain_tables.sql` in `src/main/resources/db/migration/`, following the existing V1__baseline.sql and V2__create_greeting_table.sql.
6. THE Migration_Script SHALL create indexes on all foreign key columns: employees.manager_id, projects.company_id, interactions.employee_id, interactions.conducted_by_user_id, interactions.logged_by_user_id, interactions.project_id, tasks.interaction_id, and tasks.assigned_user_id.
7. THE Migration_Script SHALL define string fields as VARCHAR(255) for name, email, title, and job_title columns, and as TEXT for notes and description columns.
8. IF the Migration_Script is applied to a database where V1__baseline.sql and V2__create_greeting_table.sql have already run, THEN the Migration_Script SHALL execute successfully without errors.

### Requirement 10: Spring Data JPA Repositories

**User Story:** As a developer, I want Spring Data JPA repository interfaces for each entity, so that the application can perform standard CRUD operations.

#### Acceptance Criteria

1. THE Repository_Layer SHALL define a JpaRepository interface for each entity (UserRepository, EmployeeRepository, CompanyRepository, ProjectRepository, InteractionRepository, TaskRepository) in their respective module packages (UserRepository in `user`, EmployeeRepository in `employee`, CompanyRepository and ProjectRepository in `client`, InteractionRepository in `interaction`, TaskRepository in `task`).
2. THE Repository_Layer SHALL parameterize each repository with the entity type and Long as the ID type (e.g., `JpaRepository<User, Long>`).
3. THE Repository_Layer SHALL define each repository as a public interface extending JpaRepository with no custom query methods, so that only standard inherited CRUD operations are exposed at this stage.
4. WHEN the Spring application context loads, THE Repository_Layer SHALL ensure all six repository interfaces are discoverable by Spring Boot component scanning without requiring explicit @Repository annotations or additional configuration.

### Requirement 11: Cross-Module JPA References

**User Story:** As a developer, I want entities in different packages to reference each other via direct JPA associations, so that the modular monolith maintains relational integrity without indirection.

#### Acceptance Criteria

1. THE Entity_Layer SHALL use direct @ManyToOne annotations for cross-module references: Interaction referencing User (conducted_by, logged_by), Employee, and Project; Task referencing User (assigned_user) and Interaction.
2. THE Entity_Layer SHALL use FetchType.LAZY for all @ManyToOne cross-module associations to prevent eager loading of referenced entities from other module packages.
3. THE Entity_Layer SHALL define all cross-module associations as unidirectional @ManyToOne from the owning entity to the referenced entity, without @OneToMany inverse collections on the referenced entity.
4. THE Entity_Layer SHALL not define cascade operations on cross-module @ManyToOne associations, so that deletion or modification of a referenced entity does not automatically propagate to the referencing entity.
5. THE Entity_Layer SHALL import entity classes across module package boundaries directly using standard Java imports without intermediate abstractions or interfaces.

### Requirement 12: Entity Mapping Integration Tests

**User Story:** As a developer, I want integration tests that verify entity mappings and schema consistency, so that I have confidence the JPA model and Flyway schema are aligned.

#### Acceptance Criteria

1. WHEN the application context loads with the migration applied, THE Integration_Tests SHALL verify that all six repository interfaces (UserRepository, EmployeeRepository, CompanyRepository, ProjectRepository, InteractionRepository, TaskRepository) are injectable and each can execute a save followed by a findById that returns the persisted entity.
2. WHEN a complete entity graph is persisted (User, Employee, Company, Project, Interaction, Task) and the persistence context is flushed and cleared, THE Integration_Tests SHALL verify that reloading each entity by ID resolves all @ManyToOne associations to the originally persisted related entities.
3. WHEN an Interaction is persisted with a null project_id and the persistence context is flushed and cleared, THE Integration_Tests SHALL verify that reloading the Interaction returns a null project association.
4. WHEN a Task is persisted without an interaction or assigned user and the persistence context is flushed and cleared, THE Integration_Tests SHALL verify that reloading the Task returns null for both the interaction and assignedUser associations.
5. WHEN an Interaction is persisted with a type value and a Task is persisted with a status value, THE Integration_Tests SHALL verify that reloading each entity returns the original enum value, confirming @Enumerated(EnumType.STRING) mapping is correct.
