# Implementation Plan: Seed Data and Data Layer Sign-off

## Overview

Implement a profile-scoped seed data loader (`SeedDataLoader`) that inserts sample records across all domain entities on startup for `local` and `dev` profiles. Add thin REST controllers (one per entity) exposing `GET` list endpoints. Verify correctness through unit tests, integration tests with Testcontainers, and Cucumber acceptance tests using the existing four-layer harness.

## Tasks

- [x] 1. Implement the SeedDataLoader component
  - [x] 1.1 Create SeedDataLoader class in `com.psybergate.staff_engagement.seed` package
    - Implement `ApplicationRunner` with `@Profile({"local", "dev"})`, `@Component`, `@RequiredArgsConstructor`, `@Slf4j`
    - Inject all six repositories (UserRepository, CompanyRepository, EmployeeRepository, ProjectRepository, InteractionRepository, TaskRepository)
    - Implement `seedDataAlreadyPresent()` method checking for a known seed user by email
    - Implement `insertSeedData()` method inserting in strict FK-dependency order: Users → Companies → Employees (no manager) → Employees (with manager) → Projects → Interactions → Tasks
    - Insert sample data matching design counts: 3 Users, 2 Companies, 5 Employees (3 without manager, 2 with), 3 Projects, 4 Interactions (covering CHECK_IN, MENTORING, CATCH_UP, OTHER), 3 Tasks (1 OPEN, 1 DONE, 1 OPEN with due_date)
    - Mark the `run()` method `@Transactional` for atomicity
    - Log INFO on skip and on success
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 8.1, 8.2, 8.3, 8.4_

  - [x] 1.2 Write unit tests for SeedDataLoader
    - Test skip logic: mock `UserRepository.findByEmail()` returning existing user, verify zero `save()` calls on all repositories
    - Test insertion order: mock all repositories, verify `save()` call order using Mockito `InOrder` (Users and Companies before Employees, Employees before Projects, etc.)
    - Test idempotency: verify no duplicates when `seedDataAlreadyPresent()` returns true
    - _Requirements: 1.4, 2.4, 8.1, 8.2_

- [x] 2. Implement REST controllers for entity listing
  - [x] 2.1 Create UserController in `com.psybergate.staff_engagement.user` package
    - `@RestController` with `@GetMapping("/api/users")` returning `List<User>` via `userRepository.findAll()`
    - _Requirements: 9.4_

  - [x] 2.2 Create EmployeeController in `com.psybergate.staff_engagement.employee` package
    - `@RestController` with `@GetMapping("/api/employees")` returning `List<Employee>` via `employeeRepository.findAll()`
    - _Requirements: 9.1_

  - [x] 2.3 Create CompanyController in `com.psybergate.staff_engagement.client` package
    - `@RestController` with `@GetMapping("/api/companies")` returning `List<Company>` via `companyRepository.findAll()`
    - _Requirements: 9.2_

  - [x] 2.4 Create ProjectController in `com.psybergate.staff_engagement.client` package
    - `@RestController` with `@GetMapping("/api/projects")` returning `List<Project>` via `projectRepository.findAll()`
    - _Requirements: 9.3_

  - [x] 2.5 Create InteractionController in `com.psybergate.staff_engagement.interaction` package
    - `@RestController` with `@GetMapping("/api/interactions")` returning `List<Interaction>` via `interactionRepository.findAll()`
    - _Requirements: 9.5_

  - [x] 2.6 Create TaskController in `com.psybergate.staff_engagement.task` package
    - `@RestController` with `@GetMapping("/api/tasks")` returning `List<Task>` via `taskRepository.findAll()`
    - _Requirements: 9.6_

  - [x] 2.7 Write WebMvcTest unit tests for all controllers
    - Use `@WebMvcTest` slices with mocked repositories
    - Verify each endpoint returns 200 OK with JSON array
    - Verify correct content-type (application/json)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [x] 3. Checkpoint - Ensure compilation and unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Write integration tests with Testcontainers
  - [x] 4.1 Create SeedDataLoaderIntegrationTest
    - Extend `BaseIntegrationTest` (inherits Testcontainers PostgreSQL, `local` profile)
    - Verify seed data row counts after context startup: ≥ 3 users, ≥ 2 companies, ≥ 5 employees, ≥ 3 projects, ≥ 4 interactions, ≥ 3 tasks
    - Verify at least one employee has a non-null manager_id
    - Verify interactions cover at least 2 distinct types
    - Verify tasks include at least one OPEN and one DONE
    - _Requirements: 1.1, 2.1, 2.2, 3.1, 3.2, 4.1, 5.1, 5.2, 8.1, 8.2, 8.3, 8.4_

  - [x] 4.2 Create SeedDataIdempotencyIntegrationTest
    - Extend `BaseIntegrationTest`
    - Trigger the loader logic a second time (or restart context) and assert row counts remain unchanged
    - Confirm no duplicate records by checking unique email constraint satisfaction
    - _Requirements: 1.4, 2.4_

  - [x] 4.3 Create REST controller integration tests
    - Extend `BaseIntegrationTest` with `TestRestTemplate` or `WebTestClient`
    - Verify each `GET /api/{entity}` endpoint returns 200 OK with expected minimum record counts
    - Verify JSON response structure contains expected fields
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

- [x] 5. Checkpoint - Ensure all integration tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement Cucumber acceptance tests
  - [x] 6.1 Create SeedDataApiDriver in `acceptance-tests/.../drivers/api/`
    - Extend `BaseApiDriver`
    - Implement typed methods: `getUsers()`, `getEmployees()`, `getCompanies()`, `getProjects()`, `getInteractions()`, `getTasks()`
    - Each method calls `get("/api/{entity}")` and parses JSON response
    - _Requirements: 9.7_

  - [x] 6.2 Create seed_data.feature in `acceptance-tests/.../resources/features/seed_data/`
    - Write scenarios verifying:
      - `GET /api/employees` returns ≥ 5 records
      - `GET /api/companies` returns ≥ 2 records
      - `GET /api/projects` returns ≥ 3 records
      - `GET /api/users` returns ≥ 3 records
      - `GET /api/interactions` returns ≥ 3 records with ≥ 2 distinct types
      - `GET /api/tasks` returns ≥ 3 records with at least one OPEN and one DONE
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [x] 6.3 Create SeedDataStepDefinitions in `acceptance-tests/.../stepdefs/`
    - Inject `SeedDataApiDriver` via Spring
    - Implement step definitions matching all Gherkin steps from the feature file
    - Use AssertJ for assertions on response status and record counts
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7_

- [x] 7. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Entities and repositories already exist — no need to create them
- The `BaseIntegrationTest` already provides Testcontainers PostgreSQL with `local` profile and Flyway
- The acceptance test harness (Cucumber + Spring) is already in place with `BaseApiDriver`
- Flyway migration integrity (Requirement 6) and cross-module compilation integrity (Requirement 7) are validated automatically by `./mvnw verify` via the existing `FlywayMigrationIntegrationTest` and `DomainModelIntegrationTest`
- The `UserRepository` needs a `findByEmail(String email)` query method for the idempotency check

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.2", "2.3", "2.4", "2.5", "2.6"] },
    { "id": 1, "tasks": ["1.2", "2.7"] },
    { "id": 2, "tasks": ["4.1", "4.2", "4.3"] },
    { "id": 3, "tasks": ["6.1"] },
    { "id": 4, "tasks": ["6.2", "6.3"] }
  ]
}
```
