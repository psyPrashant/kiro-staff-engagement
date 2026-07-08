# Implementation Plan: Domain Model ERD

## Overview

This plan implements the persistence layer for the Staff Engagement platform: a Flyway V2 migration script, six JPA entities with two enumerations, six Spring Data JPA repositories, and integration tests (standard + property-based). Tasks are ordered so that foundational schema and entities are built first, then repositories, then tests wire everything together.

## Tasks

- [x] 1. Create Flyway V2 migration script and enum classes
  - [x] 1.1 Create V2__create_domain_tables.sql migration script
    - Create file `src/main/resources/db/migration/V2__create_domain_tables.sql`
    - Define tables in dependency order: users, employees, companies, projects, interactions, tasks
    - Include all columns with correct data types (BIGSERIAL, VARCHAR(255), TEXT, TIMESTAMP, DATE)
    - Add NOT NULL constraints, UNIQUE constraints on users.email and employees.email
    - Add foreign key constraints for all relationships (employees.manager_id, projects.company_id, interactions FK columns, tasks FK columns)
    - Add CHECK constraints for interactions.type (CHECK_IN, MENTORING, CATCH_UP, OTHER) and tasks.status (OPEN, DONE)
    - Add DEFAULT 'OPEN' on tasks.status column
    - Create indexes on all foreign key columns
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 7.2, 7.3, 8.2, 8.3_

  - [x] 1.2 Create InteractionType enum
    - Create file `src/main/java/com/psybergate/staff_engagement/interaction/InteractionType.java`
    - Define enum with values: CHECK_IN, MENTORING, CATCH_UP, OTHER (in this order)
    - _Requirements: 7.1, 7.2_

  - [x] 1.3 Create TaskStatus enum
    - Create file `src/main/java/com/psybergate/staff_engagement/task/TaskStatus.java`
    - Define enum with values: OPEN, DONE (in this order)
    - _Requirements: 8.1, 8.4_

- [x] 2. Implement entity classes (independent entities)
  - [x] 2.1 Create User entity
    - Create file `src/main/java/com/psybergate/staff_engagement/user/User.java`
    - Annotate with @Entity, @Table(name = "users"), @Getter, @Setter, @NoArgsConstructor
    - Fields: id (Long, @Id, @GeneratedValue IDENTITY), name (String, not null, length 255), email (String, not null, unique, length 255), createdAt (Instant, not null, updatable = false)
    - Add @PrePersist callback to auto-set createdAt if null
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

  - [x] 2.2 Create Employee entity
    - Create file `src/main/java/com/psybergate/staff_engagement/employee/Employee.java`
    - Annotate with @Entity, @Table(name = "employees"), @Getter, @Setter, @NoArgsConstructor
    - Fields: id, name, email, manager (@ManyToOne LAZY self-referencing), jobTitle, createdAt
    - Add @PrePersist callback for createdAt
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 11.2_

  - [x] 2.3 Create Company entity
    - Create file `src/main/java/com/psybergate/staff_engagement/client/Company.java`
    - Annotate with @Entity, @Table(name = "companies"), @Getter, @Setter, @NoArgsConstructor
    - Fields: id, name, createdAt with @PrePersist callback
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 2.4 Create Project entity
    - Create file `src/main/java/com/psybergate/staff_engagement/client/Project.java`
    - Annotate with @Entity, @Table(name = "projects"), @Getter, @Setter, @NoArgsConstructor
    - Fields: id, name, company (@ManyToOne LAZY, optional = false, not null), createdAt
    - Add @PrePersist callback for createdAt
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 11.1, 11.2_

- [x] 3. Implement entity classes (dependent entities)
  - [x] 3.1 Create Interaction entity
    - Create file `src/main/java/com/psybergate/staff_engagement/interaction/Interaction.java`
    - Annotate with @Entity, @Table(name = "interactions"), @Getter, @Setter, @NoArgsConstructor
    - Fields: id, employee (@ManyToOne LAZY, not null), conductedBy (@ManyToOne LAZY User, not null), loggedBy (@ManyToOne LAZY User, not null), project (@ManyToOne LAZY, nullable), type (@Enumerated STRING InteractionType, not null), notes (TEXT, not null), occurredAt (Instant, not null), createdAt
    - Add @PrePersist callback for createdAt
    - Cross-module imports from user, employee, client packages
    - No cascade operations on cross-module associations
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 11.1, 11.2, 11.3, 11.4, 11.5_

  - [x] 3.2 Create Task entity
    - Create file `src/main/java/com/psybergate/staff_engagement/task/Task.java`
    - Annotate with @Entity, @Table(name = "tasks"), @Getter, @Setter, @NoArgsConstructor
    - Fields: id, interaction (@ManyToOne LAZY, nullable), title (String, not null, length 255), description (String, nullable, length 2000), status (@Enumerated STRING TaskStatus, not null, default OPEN), dueDate (LocalDate, nullable), assignedUser (@ManyToOne LAZY User, nullable), createdAt
    - Add @PrePersist callback for createdAt
    - Cross-module imports from interaction and user packages
    - No cascade operations on cross-module associations
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 11.1, 11.2, 11.3, 11.4, 11.5_

- [x] 4. Implement repository interfaces
  - [x] 4.1 Create UserRepository interface
    - Create file `src/main/java/com/psybergate/staff_engagement/user/UserRepository.java`
    - Extend JpaRepository<User, Long> with no custom methods
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 4.2 Create EmployeeRepository interface
    - Create file `src/main/java/com/psybergate/staff_engagement/employee/EmployeeRepository.java`
    - Extend JpaRepository<Employee, Long> with no custom methods
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 4.3 Create CompanyRepository and ProjectRepository interfaces
    - Create file `src/main/java/com/psybergate/staff_engagement/client/CompanyRepository.java`
    - Create file `src/main/java/com/psybergate/staff_engagement/client/ProjectRepository.java`
    - Both extend JpaRepository with appropriate entity type and Long ID
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 4.4 Create InteractionRepository interface
    - Create file `src/main/java/com/psybergate/staff_engagement/interaction/InteractionRepository.java`
    - Extend JpaRepository<Interaction, Long> with no custom methods
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 4.5 Create TaskRepository interface
    - Create file `src/main/java/com/psybergate/staff_engagement/task/TaskRepository.java`
    - Extend JpaRepository<Task, Long> with no custom methods
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

- [x] 5. Checkpoint - Verify compilation and migration
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Add jqwik dependency and write integration tests
  - [x] 6.1 Add jqwik dependency to pom.xml
    - Add net.jqwik:jqwik:1.9.2 with scope test to `pom.xml`
    - _Requirements: (supports testing strategy for Property tests)_

  - [x] 6.2 Write DomainModelIntegrationTest
    - Create file `src/test/java/com/psybergate/staff_engagement/DomainModelIntegrationTest.java`
    - Use @SpringBootTest with existing TestcontainersConfiguration
    - Test: all six repositories are injectable and can save/findById
    - Test: full entity graph persistence and retrieval with associations resolved
    - Test: nullable FK handling (null project on Interaction, null interaction/assignedUser on Task)
    - Test: enum mapping correctness for all InteractionType and TaskStatus values
    - Test: self-referencing Employee manager relationship
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_

  - [ ] 6.3 Write EntityPersistencePropertyTest (Properties 1 & 2)
    - **Property 1: Entity Persistence Round-Trip**
    - **Property 2: @PrePersist Auto-Populates created_at**
    - Create file `src/test/java/com/psybergate/staff_engagement/EntityPersistencePropertyTest.java`
    - Use jqwik @Property with minSuccess = 100
    - Generate random valid entities, persist, flush/clear, retrieve and verify field equality
    - Verify created_at is auto-populated and approximately current time
    - **Validates: Requirements 12.1, 1.3, 1.4, 3.3, 4.5, 5.6, 6.6, 10.1, 10.2**

  - [ ] 6.4 Write EnumMappingPropertyTest (Property 3)
    - **Property 3: Enum Field Serialization Round-Trip**
    - Create file `src/test/java/com/psybergate/staff_engagement/EnumMappingPropertyTest.java`
    - Use jqwik @Property with minSuccess = 100
    - Generate random InteractionType and TaskStatus values, persist entities, flush/clear, reload and verify enum value matches
    - **Validates: Requirements 5.5, 6.5, 7.2, 8.2, 8.4, 12.5**

  - [ ] 6.5 Write AssociationResolutionPropertyTest (Property 4)
    - **Property 4: Association Resolution After Flush and Clear**
    - Create file `src/test/java/com/psybergate/staff_engagement/AssociationResolutionPropertyTest.java`
    - Use jqwik @Property with minSuccess = 100
    - Generate entities with various association combinations (required + optional, null + non-null)
    - Persist, flush, clear, reload and verify all associations resolve correctly
    - **Validates: Requirements 2.3, 4.3, 5.3, 5.4, 6.3, 6.4, 11.2, 12.2, 12.3, 12.4**

  - [ ] 6.6 Write NoCascadePropertyTest (Property 5)
    - **Property 5: No Cascade on Cross-Module Delete**
    - Create file `src/test/java/com/psybergate/staff_engagement/NoCascadePropertyTest.java`
    - Use jqwik @Property with minSuccess = 100
    - Create entities with cross-module references, delete referencing entity, verify referenced entity still exists
    - **Validates: Requirements 11.4**

- [x] 7. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- All file paths are relative to `staff-engagement-backend/`
- The existing `TestcontainersConfiguration` class provides the PostgreSQL container for all integration tests
- jqwik property tests require the additional Maven dependency added in task 6.1

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["2.4", "4.1", "4.2"] },
    { "id": 3, "tasks": ["3.1", "4.3"] },
    { "id": 4, "tasks": ["3.2", "4.4"] },
    { "id": 5, "tasks": ["4.5", "6.1"] },
    { "id": 6, "tasks": ["6.2"] },
    { "id": 7, "tasks": ["6.3", "6.4", "6.5", "6.6"] }
  ]
}
```
