# Implementation Plan: Add Test Seed Data

## Overview

Expand the existing `SeedDataLoader.java` to generate a larger, more realistic dataset. This involves injecting the `ScheduledInteractionRepository`, adding private helper methods for creating new users, employees, interactions, tasks, and scheduled interactions, and wiring everything into the existing `insertSeedData()` method. Property-based tests with jqwik validate distribution constraints across the seeded dataset.

## Tasks

- [x] 1. Add ScheduledInteractionRepository dependency and new user creation
  - [x] 1.1 Inject ScheduledInteractionRepository and create the `createNewUsers()` method
    - Add `ScheduledInteractionRepository` as a new `final` field in `SeedDataLoader`
    - Import `ScheduledInteraction`, `ScheduledInteractionRepository`, and `CompletionStatus`
    - Create `private List<User> createNewUsers()` that creates Dave Martinez and Eve Thompson using the existing `createUser()` helper
    - Call `createNewUsers()` at the end of `insertSeedData()` after existing seed data
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 1.2 Create the `createNewEmployees()` method
    - Create `private List<Employee> createNewEmployees(List<Employee> existingEmployees)` that generates 20 employees
    - Use an array of at least 10 distinct job titles (Software Engineer, Senior Developer, Team Lead, Product Manager, UX Designer, QA Engineer, DevOps Engineer, Business Analyst, Data Analyst, Scrum Master)
    - Assign managers from existing employees using rotation (`existingEmployees.get(index % existingEmployees.size())`) for at least 10 of the 20 employees
    - Use deterministic name/email patterns (e.g., arrays of first/last names combined)
    - Wrap individual `employeeRepository.saveAndFlush()` calls in try-catch for `DataIntegrityViolationException` to skip duplicates
    - Call from `insertSeedData()` after user creation
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 2. Implement interaction and task generation methods
  - [x] 2.1 Create the `createInteractionsForNewEmployees()` method
    - Create `private List<Interaction> createInteractionsForNewEmployees(List<Employee> newEmployees, List<User> allUsers, List<Project> allProjects)`
    - Generate 20 interactions per employee (400 total)
    - Rotate interaction types using `InteractionType.values()[i % 4]` to cycle through CHECK_IN, MENTORING, CATCH_UP, OTHER (5 of each per employee)
    - Rotate `conductedBy` and `loggedBy` using `allUsers.get(i % 5)` and `allUsers.get((i + 2) % 5)` ensuring ≥3 distinct users per employee
    - Set `occurredAt` using `Instant.now().minus(i * 18, ChronoUnit.DAYS)` to spread across 12 months
    - Assign non-null project for at least 6 of 20 interactions per employee using `allProjects.get(i % 3)` for indices 0–7
    - Generate unique notes using a combination of template strings with employee index and interaction index
    - Call from `insertSeedData()` after employee creation
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

  - [x] 2.2 Create the `createTasksForEmployees()` method
    - Create `private void createTasksForEmployees(List<Employee> employees, List<Interaction> interactions, List<User> allUsers)`
    - Generate 5 tasks for each of the first 5 employees (25 total)
    - Use status pattern `[OPEN, OPEN, OPEN, DONE, DONE]` per employee
    - Set due dates: first 2 OPEN → future (+7d, +14d), third OPEN → past (-7d), DONE → past (-30d, -60d)
    - Link each task to a valid interaction from the new interactions list
    - Generate unique titles and descriptions using deterministic templates with employee/task indices
    - Assign tasks to users using rotation `allUsers.get(taskIndex % 5)`
    - Call from `insertSeedData()` after interaction creation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [x] 2.3 Create the `createScheduledInteractions()` method
    - Create `private void createScheduledInteractions(List<User> allUsers, List<Employee> allEmployees)`
    - Generate 3 scheduled interactions per user (15 total)
    - Rotate interaction types using `[CHECK_IN, MENTORING, CATCH_UP]` per user (≥2 types per user)
    - Rotate completion statuses using `[PENDING, COMPLETED, CANCELLED]` per user (all 3 per user)
    - Set dates: PENDING → future (+7d to +30d), COMPLETED/CANCELLED → past (-1d to -90d)
    - Assign distinct employees using `allEmployees.get((userIndex * 3 + scheduledIndex) % allEmployees.size())`
    - Generate unique notes using deterministic templates
    - Create a `private ScheduledInteraction createScheduledInteraction(...)` factory helper
    - Call from `insertSeedData()` after task creation
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

- [x] 3. Checkpoint - Verify compilation and existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Write property-based tests for seed data distribution constraints
  - [x] 4.1 Write property test for interaction type distribution
    - **Property 1: Interaction type distribution per employee**
    - Create `SeedDataPropertyTest.java` in the test package under `com.psybergate.staff_engagement.seed`
    - Use `@SpringBootTest` with Testcontainers PostgreSQL
    - Query all new employees, assert each has ≥3 of 4 interaction types and no type >10
    - **Validates: Requirements 3.2**

  - [x] 4.2 Write property test for user assignment distribution
    - **Property 2: User assignment distribution per employee**
    - Assert ≥3 distinct `conducted_by_user_id` and ≥3 distinct `logged_by_user_id` per employee's 20 interactions
    - **Validates: Requirements 3.3**

  - [x] 4.3 Write property test for temporal spread of interactions
    - **Property 3: Temporal spread of interactions per employee**
    - Assert ≥8 distinct calendar months represented in each employee's interaction timestamps
    - **Validates: Requirements 3.4**

  - [x] 4.4 Write property test for project assignment coverage
    - **Property 4: Project assignment coverage per employee**
    - Assert ≥6 interactions per employee have a non-null project reference
    - **Validates: Requirements 3.5**

  - [x] 4.5 Write property test for interaction notes uniqueness and length
    - **Property 5: Interaction notes uniqueness and length**
    - Assert all 400 notes are distinct and each is between 20–200 characters
    - **Validates: Requirements 3.6**

  - [x] 4.6 Write property test for task status distribution
    - **Property 6: Task status distribution per employee**
    - Assert each employee has ≥2 OPEN tasks and ≥1 DONE task
    - **Validates: Requirements 4.2**

  - [x] 4.7 Write property test for task due date spread
    - **Property 7: Task due date spread per employee**
    - Assert each employee has ≥1 past due date and ≥1 future due date
    - **Validates: Requirements 4.3**

  - [x] 4.8 Write property test for referential integrity
    - **Property 8: Referential integrity for all new records**
    - Assert every FK field on new Tasks, Interactions, and ScheduledInteractions references an existing parent record
    - **Validates: Requirements 4.4, 4.5, 7.1**

  - [x] 4.9 Write property test for task title and description constraints
    - **Property 9: Task title and description constraints**
    - Assert all 25 task titles are distinct, each title 1–255 chars, each description 1–2000 chars
    - **Validates: Requirements 4.6**

  - [x] 4.10 Write property test for scheduled interaction type diversity
    - **Property 10: Scheduled interaction type diversity per user**
    - Assert each user's 3 scheduled interactions include ≥2 different interaction types
    - **Validates: Requirements 5.2**

  - [x] 4.11 Write property test for scheduled interaction status diversity
    - **Property 11: Scheduled interaction status diversity per user**
    - Assert each user's 3 scheduled interactions include ≥2 different completion statuses
    - **Validates: Requirements 5.3**

  - [x] 4.12 Write property test for date-status consistency
    - **Property 12: Scheduled interaction date-status consistency**
    - Assert PENDING → future date, COMPLETED/CANCELLED → past date for all 15 records
    - **Validates: Requirements 5.5, 5.7, 7.3**

  - [x] 4.13 Write property test for scheduled interaction notes
    - **Property 13: Scheduled interaction notes uniqueness and length**
    - Assert all 15 notes are distinct and each is between 10–200 characters
    - **Validates: Requirements 5.6**

  - [x] 4.14 Write property test for interaction temporal ordering
    - **Property 14: Interaction temporal ordering**
    - Assert every interaction's `occurred_at` precedes current time and `created_at` ≥ `occurred_at`
    - **Validates: Requirements 7.2**

  - [x] 4.15 Write property test for DONE task due date consistency
    - **Property 15: DONE task due date consistency**
    - Assert every DONE task has a due_date that is null or in the past
    - **Validates: Requirements 7.4**

  - [x] 4.16 Write property test for idempotency
    - **Property 16: Seed loader idempotency**
    - Run seed loader twice, assert row counts unchanged after second execution
    - **Validates: Requirements 6.4**

- [x] 5. Write integration test for full seed execution
  - [x] 5.1 Write integration test verifying total record counts after seeding
    - Create `SeedDataLoaderIntegrationTest.java` with `@SpringBootTest` and Testcontainers
    - Run seed loader on empty database, verify: 5 users, 25 employees, 404 interactions, 28 tasks, 15 scheduled interactions
    - Verify existing data preservation: original 3 users, 5 employees, 4 interactions, 3 tasks unchanged
    - _Requirements: 6.1, 6.2, 1.1, 2.1, 3.1, 4.1, 5.1_

  - [x] 5.2 Write integration test for transactional rollback on constraint violation
    - Force a constraint violation scenario, verify no partial data persists
    - _Requirements: 1.4, 7.6_

- [x] 6. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- The implementation modifies only `SeedDataLoader.java` — no new production classes needed
- jqwik is already available as a test dependency in the project's pom.xml
- `ScheduledInteractionRepository` already exists in `com.psybergate.staff_engagement.scheduling`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["2.1"] },
    { "id": 3, "tasks": ["2.2", "2.3"] },
    { "id": 4, "tasks": ["4.1", "4.2", "4.3", "4.4", "4.5", "4.6", "4.7", "4.8", "4.9", "4.10", "4.11", "4.12", "4.13", "4.14", "4.15", "4.16", "5.1", "5.2"] }
  ]
}
```
