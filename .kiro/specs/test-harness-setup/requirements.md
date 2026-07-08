# Requirements Document

## Introduction

Stand up the full test harness for the Staff Engagement monorepo so that all parallel feature work is test-first from day one. The harness covers backend unit, integration, BDD, mutation, and coverage tooling, as well as frontend unit and end-to-end tooling. Each layer ships with a sample test proving it runs. Coverage and mutation reporting operate in lenient/report-only mode with no enforced thresholds.

## Glossary

- **Backend**: The Spring Boot 3.5 / Java 21 Maven project located in `staff-engagement-backend/`.
- **Frontend**: The Angular 21 / TypeScript project located in `staff-engagement-frontend/`.
- **Unit_Test_Suite**: The JUnit 5 test classes executed via `./mvnw test` that do not require external infrastructure.
- **Integration_Test_Suite**: The JUnit 5 test classes that use Testcontainers to run against a real PostgreSQL database.
- **BDD_Framework**: Cucumber-JVM wired into the Maven build with Spring Boot integration for behaviour-driven development.
- **Mutation_Reporter**: PITest configured in the Maven build to produce mutation testing reports without failing the build.
- **Coverage_Reporter**: JaCoCo configured in the Maven build to produce code coverage reports on `mvn verify` without enforced minimums.
- **Frontend_Unit_Test_Suite**: Vitest tests executed via Angular CLI's `@angular/build:unit-test` builder with `@vitest/coverage-v8`.
- **E2E_Test_Suite**: Playwright-based end-to-end tests running against the built Angular application.
- **CI_Pipeline**: The GitHub Actions workflows (`backend-ci.yml`, `frontend-ci.yml`) that execute all test harnesses on pull requests and pushes to main.
- **Base_Integration_Test_Class**: A reusable abstract test class that configures Testcontainers PostgreSQL with Flyway migrations for integration tests.
- **Sample_Test**: A minimal test proving a harness layer functions correctly, serving as a template for future feature tests.
- **Acceptance_Test_Module**: A standalone Maven module at `acceptance-tests/` containing Cucumber acceptance tests that run against the deployed application via Java Playwright.
- **TestWorld**: A scenario-scoped Spring bean for sharing state between steps within a single scenario.

## Requirements

### Requirement 1: Backend Unit Testing with JUnit 5, Mockito, and AssertJ

**User Story:** As a developer, I want a configured backend unit testing layer with JUnit 5, Mockito, and AssertJ including a sample test and documented conventions, so that I can write isolated unit tests for any feature from day one.

#### Acceptance Criteria

1. THE Unit_Test_Suite SHALL declare JUnit 5 as the test framework, Mockito for mocking, and AssertJ for assertions via `spring-boot-starter-test` in the project's `pom.xml` dependency list.
2. WHEN `./mvnw test` is executed from the `staff-engagement-backend/` directory, THE Unit_Test_Suite SHALL complete with Maven exit code 0 and zero test failures or errors reported in the summary output.
3. THE Backend SHALL include a sample unit test class located under `src/test/java` in the `com.psybergate.staff_engagement` package hierarchy that uses `@ExtendWith(MockitoExtension.class)` (without loading a Spring application context), contains at least one `@Mock`-annotated dependency, injects it into the class under test via `@InjectMocks`, and verifies behavior using at least one Mockito `verify()` call and at least one AssertJ `assertThat()` assertion.
4. THE Backend SHALL include a sample service class under `src/main/java` in the `com.psybergate.staff_engagement` package hierarchy that the sample unit test exercises, containing at least one method with a dependency suitable for mocking.
5. THE Backend SHALL include a conventions file at `src/test/resources/TESTING-CONVENTIONS.md` containing at minimum the following sections: test class naming pattern, test method naming pattern, recommended package structure mirroring the source tree, and annotation usage guidelines for `@ExtendWith`, `@Mock`, `@InjectMocks`, and `@Test`.
6. WHEN the sample unit test class is executed in isolation via `./mvnw test -Dtest=<SampleTestClassName>`, THE Unit_Test_Suite SHALL report all tests in that class as passed with zero failures.

### Requirement 2: Backend Integration Testing with Testcontainers and Flyway

**User Story:** As a developer, I want a base integration test class that manages Testcontainers PostgreSQL with Flyway migrations, so that I can write repository-level integration tests without manual database setup.

#### Acceptance Criteria

1. THE Backend SHALL provide a Base_Integration_Test_Class annotated with `@SpringBootTest`, `@Import(TestcontainersConfiguration.class)`, and `@ActiveProfiles("local")` that reuses a single shared Testcontainers PostgreSQL container instance across all test classes within the same test JVM via Spring's `@ServiceConnection`.
2. WHILE an integration test extends the Base_Integration_Test_Class, THE Integration_Test_Suite SHALL automatically apply all Flyway migrations from `src/main/resources/db/migration/` during Spring application context startup, before any `@Test` methods execute.
3. THE Backend SHALL include a Flyway migration that creates at least one table, a corresponding JPA entity, a Spring Data JPA repository interface, and a sample repository integration test that extends the Base_Integration_Test_Class and verifies persisting and retrieving the entity by calling `save()` followed by `findById()` against the Testcontainers PostgreSQL instance.
4. WHEN the sample repository integration test is executed via `./mvnw test`, THE Integration_Test_Suite SHALL report a passing result with zero test failures.
5. THE Base_Integration_Test_Class SHALL use the `@ActiveProfiles("local")` annotation to activate the local test profile.

### Requirement 3: Backend BDD with Cucumber (Four-Layer Acceptance Module)

**User Story:** As a developer, I want a standalone Cucumber acceptance-test Maven module that drives the application through Java Playwright in a four-layer architecture, so that I can write BDD scenarios in business language that exercise the full stack end-to-end against the running application.

#### Acceptance Criteria

1. THE Acceptance_Test_Module SHALL exist as a standalone Maven module at the monorepo root (`acceptance-tests/`) with its own `pom.xml` declaring dependencies on cucumber-java, cucumber-spring, cucumber-junit-platform-engine, and com.microsoft.playwright (Java Playwright).
2. THE Acceptance_Test_Module SHALL build independently by executing `cd acceptance-tests && mvn clean test` without requiring a parent reactor POM or the backend module to be built first.
3. THE Acceptance_Test_Module SHALL use a four-layer architecture: Layer 1 — Gherkin feature files under `src/test/resources/features/`; Layer 2 — thin step definition classes in a `stepdefs` package; Layer 3 — domain actor and assertion classes in a `domain` package; Layer 4 — driver classes split into UI page objects under `drivers/ui/pages/` and API drivers under `drivers/api/`.
4. THE Acceptance_Test_Module SHALL provide an `AcceptanceSpringConfig` class that owns Playwright `Browser`, `BrowserContext`, and `Page` beans declared as `@ScenarioScope`, ensuring each scenario gets an isolated browser context.
5. THE Acceptance_Test_Module SHALL connect to the already-running backend (default `http://localhost:8080`) and frontend (default `http://localhost:4200`) rather than starting the application via `@SpringBootTest`.
6. THE Acceptance_Test_Module SHALL provide a TestWorld class annotated as a `@ScenarioScope` Spring bean for sharing state between steps within a single scenario, without using static fields.
7. THE Acceptance_Test_Module SHALL provide a `GlobalTestDataHooks` class that executes `global-cleanup.sql` before each scenario at order `Integer.MIN_VALUE`, ensuring test data isolation.
8. THE Acceptance_Test_Module SHALL provide a `SqlScriptRunner` utility that executes classpath SQL scripts against the test database via JDBC (configured in `src/test/resources/application.properties`) with deadlock retry logic.
9. THE Acceptance_Test_Module SHALL provide a `ScreenshotHooks` class that embeds a full-page screenshot into the Cucumber report on any failing scenario.
10. THE Acceptance_Test_Module SHALL provide a `RunAcceptanceTests` JUnit Platform Suite runner class annotated with `@SelectClasspathResource("features")`, configured with the glue package, and producing both HTML and JSON Cucumber reports.
11. WHEN `cd acceptance-tests && mvn clean test` is executed with the application running, THE BDD_Framework SHALL discover and execute all Cucumber feature files and report results with a zero exit code when all scenarios pass.
12. THE Acceptance_Test_Module SHALL include a smoke scenario tagged `@pre-push` that opens the application root URL in the browser via Playwright and asserts the page loaded successfully, proving the full harness works end-to-end (Spring context → Playwright → browser → running application).
13. THE Acceptance_Test_Module SHALL connect directly to the test database via JDBC (connection properties in `src/test/resources/application.properties`) for seed and cleanup operations.
14. THE monorepo SHALL include Makefile targets for acceptance testing: `accept-test` (run all acceptance tests), `accept-smoke` (run only `@pre-push` tagged scenarios), `accept-compile` (compile the module), `accept-build` (full build without running tests), and `accept-install` (install Playwright browsers via CLI).
15. WHEN `make accept-smoke` is executed with the application running, THE BDD_Framework SHALL execute only scenarios tagged `@pre-push` and report a passing result.

### Requirement 4: Backend Mutation Testing with PITest (Report-Only)

**User Story:** As a developer, I want PITest configured on the backend with report generation, so that I can assess mutation coverage without blocking the build.

#### Acceptance Criteria

1. THE Mutation_Reporter SHALL be configured as a Maven plugin (pitest-maven) in the `pom.xml` with the pitest-junit5-plugin for JUnit 5 compatibility, within a Maven profile named `pitest`.
2. WHEN `./mvnw verify -Ppitest` is executed from the `staff-engagement-backend/` directory, THE Mutation_Reporter SHALL generate an HTML mutation testing report in `target/pit-reports/`.
3. THE Mutation_Reporter SHALL target the `com.psybergate.staff_engagement` package (or a subpackage containing the sample service class from Requirement 1) as a minimum scope via the `targetClasses` configuration.
4. THE Mutation_Reporter SHALL NOT fail the build regardless of mutation score, including when no mutants are generated (failWhenNoMutations set to false and mutationThreshold set to 0).
5. THE Mutation_Reporter SHALL be configured behind a Maven profile (`pitest`) so it does not run during default builds (e.g., `./mvnw verify` without `-Ppitest` SHALL NOT invoke PITest).
6. WHEN the PITest report is generated, THE Mutation_Reporter SHALL produce at least one HTML file in `target/pit-reports/` that lists the mutants created, their status (killed, survived, or no coverage), and the targeted classes.

### Requirement 5: Backend Code Coverage with JaCoCo (Lenient)

**User Story:** As a developer, I want JaCoCo producing coverage reports on `mvn verify`, so that I can track test coverage without blocking the build.

#### Acceptance Criteria

1. THE Coverage_Reporter SHALL be configured as a Maven plugin (jacoco-maven-plugin) in the `pom.xml` with the `prepare-agent` goal bound to the `initialize` phase and the `report` goal bound to the `verify` phase.
2. WHEN `./mvnw verify` is executed, THE Coverage_Reporter SHALL generate an HTML coverage report with an `index.html` entry point in `target/site/jacoco/`.
3. WHEN `./mvnw verify` is executed, THE Coverage_Reporter SHALL generate a CSV coverage report in `target/site/jacoco/jacoco.csv`.
4. THE Coverage_Reporter SHALL NOT enforce any minimum coverage thresholds (no `check` goal or coverage rules configured).
5. THE Coverage_Reporter SHALL instrument all production classes under `com.psybergate.staff_engagement` (excluding test classes).
6. IF tests fail or produce zero coverage, THEN THE Coverage_Reporter SHALL still allow the `./mvnw verify` build to complete successfully without JaCoCo causing a build failure.

### Requirement 6: Frontend Unit Testing with Vitest

**User Story:** As a developer, I want the Vitest unit testing setup confirmed and enhanced with sample tests and coverage configuration, so that I can write component and service tests from day one.

#### Acceptance Criteria

1. THE Frontend_Unit_Test_Suite SHALL use the `@angular/build:unit-test` builder configured in `angular.json` with Vitest as the test runner.
2. WHEN `npx ng test --no-watch --coverage` is executed, THE Frontend_Unit_Test_Suite SHALL run all unit tests, exit with code 0 if all tests pass, and generate a coverage report in both `text` (stdout summary) and `lcov` (file output under a `coverage/` directory) formats.
3. THE Frontend SHALL include a sample component test that uses `TestBed.configureTestingModule` to instantiate a standalone component and asserts that the component instance is truthy and that a known text element is present in the rendered DOM.
4. THE Frontend SHALL include a sample service test that registers the service under test via `TestBed` with at least one dependency replaced by a test double, invokes a service method, and asserts the returned value matches the expected result.
5. WHEN the sample tests are executed, THE Frontend_Unit_Test_Suite SHALL report all tests passing with zero failures and zero errors.
6. THE Frontend_Unit_Test_Suite SHALL generate coverage output using the `@vitest/coverage-v8` provider, reporting at minimum statement and branch coverage percentages in the text summary.

### Requirement 7: Frontend End-to-End Testing with Playwright

**User Story:** As a developer, I want Playwright scaffolded with a smoke test and documented conventions, so that I can write end-to-end tests for any feature from day one.

#### Acceptance Criteria

1. THE Frontend SHALL include `@playwright/test` as a dev dependency in `package.json`.
2. THE Frontend SHALL include a Playwright configuration file (`playwright.config.ts`) that configures the base URL to `http://localhost:4200`, at least the Chromium browser project, and a `webServer` entry that starts the application before tests run.
3. THE E2E_Test_Suite SHALL include a smoke test file located under an `e2e/` directory that navigates to the root path (`/`) and asserts that the page contains visible text rendered by the application root component.
4. WHEN `npx playwright test` is executed from the frontend directory, THE E2E_Test_Suite SHALL start the dev server, execute all e2e test files, and exit with code 0 when all tests pass or a non-zero code when any test fails.
5. WHEN the smoke test is executed against the unmodified application, THE E2E_Test_Suite SHALL report a passing result with zero test failures.
6. THE Frontend SHALL include an e2e conventions document (`e2e/README.md`) that describes the test file location and naming pattern, the preferred selector strategy (e.g., data-testid attributes or accessible roles), and instructions for running tests locally.

### Requirement 8: CI Pipeline Integration

**User Story:** As a developer, I want all test harnesses running green in CI, so that the team has confidence every PR is validated against the full harness.

#### Acceptance Criteria

1. WHEN a pull request or push to main modifies files in `staff-engagement-backend/**` or `.github/workflows/backend-ci.yml`, THE CI_Pipeline SHALL execute `./mvnw verify` in the `staff-engagement-backend` directory, which runs unit tests, integration tests, and generates JaCoCo coverage reports.
2. WHEN a pull request or push to main modifies files in `staff-engagement-frontend/**` or `.github/workflows/frontend-ci.yml`, THE CI_Pipeline SHALL execute unit tests with coverage via Vitest and, if `@playwright/test` is present in `package.json`, Playwright e2e tests.
3. IF a JaCoCo coverage report is generated, THEN THE CI_Pipeline SHALL upload the report as a build artifact with a retention period of 14 days.
4. IF Playwright tests produce a report, THEN THE CI_Pipeline SHALL upload the report as a build artifact with a retention period of 14 days.
5. THE CI_Pipeline SHALL NOT enforce coverage or mutation thresholds as gating checks.
6. WHEN the backend "build" job passes, THE CI_Pipeline SHALL report a successful status for the "build" required status check; WHEN the frontend "lint-build" job passes, THE CI_Pipeline SHALL report a successful status for the "lint-build" required status check. Frontend "test" and "e2e" jobs are informational and SHALL NOT be configured as required status checks.
7. IF a workflow is skipped because path filtering detects no relevant file changes, THEN THE CI_Pipeline SHALL report the corresponding required status check as skipped, which satisfies the branch protection rule without blocking merge.
