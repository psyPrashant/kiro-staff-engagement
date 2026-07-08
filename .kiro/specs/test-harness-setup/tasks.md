# Implementation Plan: Test Harness Setup

## Overview

Set up the full test harness for the Staff Engagement monorepo, covering backend unit tests (JUnit 5 + Mockito + AssertJ), integration tests (Testcontainers + Flyway), a standalone four-layer Cucumber acceptance-test module with Java Playwright, mutation testing (PITest), code coverage (JaCoCo), frontend unit tests (Vitest), and frontend end-to-end tests (Playwright). Each layer includes a sample test proving it runs. CI pipelines are updated to exercise the harness.

## Tasks

- [x] 1. Backend unit test setup
  - [x] 1.1 Create sample GreetingService and unit test with conventions doc
    - Create `src/main/java/com/psybergate/staff_engagement/greeting/GreetingService.java` with a dependency (e.g., a Clock or repository) suitable for mocking
    - Create `src/test/java/com/psybergate/staff_engagement/greeting/GreetingServiceTest.java` using `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, at least one `verify()` call, and at least one AssertJ `assertThat()` assertion
    - Create `src/test/resources/TESTING-CONVENTIONS.md` with sections: test class naming, test method naming, package structure mirroring, and annotation usage guidelines (`@ExtendWith`, `@Mock`, `@InjectMocks`, `@Test`)
    - Verify: `./mvnw test -Dtest=GreetingServiceTest` passes with zero failures
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [x] 2. Backend integration test setup
  - [x] 2.1 Create BaseIntegrationTest class and sample repository integration test
    - Create `src/test/java/com/psybergate/staff_engagement/BaseIntegrationTest.java` annotated with `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@Import(TestcontainersConfiguration.class)`, `@ActiveProfiles("local")`
    - Create Flyway migration `src/main/resources/db/migration/V2__create_greeting_table.sql` (id BIGSERIAL PK, message VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT NOW())
    - Create `src/main/java/com/psybergate/staff_engagement/greeting/Greeting.java` JPA entity
    - Create `src/main/java/com/psybergate/staff_engagement/greeting/GreetingRepository.java` Spring Data JPA interface
    - Create `src/test/java/com/psybergate/staff_engagement/greeting/GreetingRepositoryIntegrationTest.java` extending `BaseIntegrationTest`, verifying `save()` + `findById()`
    - Verify: `./mvnw test -Dtest=GreetingRepositoryIntegrationTest` passes
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Backend coverage and mutation testing
  - [x] 3.1 Add JaCoCo plugin to pom.xml
    - Add `jacoco-maven-plugin` (version 0.8.13) to `<build><plugins>` section
    - Bind `prepare-agent` goal to `initialize` phase
    - Bind `report` goal to `verify` phase
    - No `check` goal, no thresholds — report-only
    - Verify: `./mvnw verify` generates `target/site/jacoco/index.html` and `target/site/jacoco/jacoco.csv`
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [x] 3.2 Add PITest plugin behind a Maven profile
    - Add a `<profile>` with id `pitest` to `pom.xml`
    - Configure `pitest-maven` (1.19.6) with `pitest-junit5-plugin` (1.2.2)
    - Set `targetClasses` to `com.psybergate.staff_engagement.*`
    - Set `failWhenNoMutations` to false, `mutationThreshold` to 0
    - Bind `mutationCoverage` goal to `verify` phase within the profile
    - Verify: `./mvnw verify -Ppitest` generates HTML report in `target/pit-reports/`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 4. Checkpoint - Backend layers verified
  - Ensure all tests pass with `./mvnw verify`, ask the user if questions arise.

- [x] 5. Acceptance test module (Cucumber + Java Playwright)
  - [x] 5.1 Scaffold acceptance-tests Maven module structure
    - **NOTE: Implementation MUST follow the `cucumber-acceptance-setup` skill guidelines. Activate the skill before starting this task.**
    - Create `acceptance-tests/pom.xml` as standalone module (no parent reactor) with dependencies: cucumber-java 7.x, cucumber-spring 7.x, cucumber-junit-platform-engine 7.x, junit-platform-suite, com.microsoft.playwright, spring-context, spring-jdbc, postgresql driver
    - Create package structure under `src/test/java/com/psybergate/acceptance/` with subpackages: config, run, support, hooks, world, drivers/ui/pages, drivers/api, domain, stepdefs
    - Create `src/test/resources/features/smoke/` directory
    - Create `src/test/resources/fixtures/sql/` directory
    - Create `src/test/resources/application.properties` with JDBC URL, app base URLs (localhost:4200, localhost:8080)
    - Create `src/test/resources/cucumber.properties` (cucumber.publish.quiet=true)
    - Create `src/test/resources/junit-platform.properties` for Cucumber discovery
    - Verify: `cd acceptance-tests && mvn compile test-compile` succeeds
    - _Requirements: 3.1, 3.2, 3.5, 3.13_

  - [x] 5.2 Implement acceptance test infrastructure classes
    - **NOTE: Implementation MUST follow the `cucumber-acceptance-setup` skill guidelines. Activate the skill before starting this task.**
    - Create `AcceptanceSpringConfig.java` — `@Configuration`, `@ComponentScan`, `@ScenarioScope` beans for Browser, BrowserContext, Page
    - Create `EnvironmentConfig.java` — base URLs and timeouts from properties
    - Create `DatabaseConfig.java` — DataSource bean for JDBC cleanup
    - Create `PropertiesConfig.java` — `@PropertySource` for application.properties
    - Create `TestWorld.java` — `@Component @ScenarioScope` with Map<String,Object> state
    - Create `SqlScriptRunner.java` — `@Component` executing classpath SQL via JDBC with deadlock retry (MAX_RETRIES=3)
    - Create `GlobalTestDataHooks.java` — `@Before(order=Integer.MIN_VALUE)` executing global-cleanup.sql
    - Create `ScreenshotHooks.java` — `@After` embedding full-page screenshot on failure
    - Create `RunAcceptanceTests.java` — `@Suite @IncludeEngines("cucumber") @SelectClasspathResource("features")` with HTML+JSON report plugins
    - Create `BasePage.java` — common Playwright page helpers
    - Create `BaseApiDriver.java` — common HTTP client helpers
    - Create `src/test/resources/fixtures/sql/global-cleanup.sql` — TRUNCATE statement(s) for test isolation
    - _Requirements: 3.3, 3.4, 3.6, 3.7, 3.8, 3.9, 3.10_

  - [x] 5.3 Create smoke scenario and step definitions
    - **NOTE: Implementation MUST follow the `cucumber-acceptance-setup` skill guidelines. Activate the skill before starting this task.**
    - Create `src/test/resources/features/smoke/smoke.feature` with `@pre-push` tag — scenario that opens root URL and asserts page loads
    - Create `stepdefs/SmokeStepDefinitions.java` — thin step defs delegating to page object
    - Create `drivers/ui/pages/HomePage.java` — page object for home page navigation and assertions
    - Verify: `cd acceptance-tests && mvn clean test` passes with application running (manual verification)
    - _Requirements: 3.11, 3.12, 3.15_

  - [x] 5.4 Create Makefile targets for acceptance testing
    - Create `Makefile` at monorepo root with targets: `accept-test`, `accept-smoke`, `accept-compile`, `accept-build`, `accept-install`
    - `accept-test`: `cd acceptance-tests && mvn clean test`
    - `accept-smoke`: `cd acceptance-tests && mvn clean test -Dcucumber.filter.tags="@pre-push"`
    - `accept-compile`: `cd acceptance-tests && mvn compile test-compile`
    - `accept-build`: `cd acceptance-tests && mvn clean package -DskipTests`
    - `accept-install`: `cd acceptance-tests && mvn exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install --with-deps chromium"`
    - _Requirements: 3.14, 3.15_

- [x] 6. Checkpoint - Acceptance test module verified
  - Ensure acceptance-tests compiles with `make accept-compile`, ask the user if questions arise.

- [x] 7. Frontend unit test enhancement
  - [x] 7.1 Create sample component and service tests
    - Verify existing `@angular/build:unit-test` builder configuration in angular.json
    - Create or enhance `src/app/app.spec.ts` — component test using `TestBed.configureTestingModule`, assert component instance is truthy and a known text element is present in the DOM
    - Create `src/app/greeting/greeting.service.ts` — minimal service with a dependency
    - Create `src/app/greeting/greeting.service.spec.ts` — service test using TestBed with a mocked dependency, asserts returned value
    - Verify: `npx ng test --no-watch --coverage` exits with code 0, produces coverage output (text + lcov)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [x] 8. Frontend end-to-end testing with Playwright
  - [x] 8.1 Install and configure Playwright for frontend e2e
    - Run `npm install --save-dev @playwright/test` in `staff-engagement-frontend/`
    - Create `playwright.config.ts` with baseURL `http://localhost:4200`, Chromium project, `webServer` config to start Angular dev server
    - Create `e2e/smoke.spec.ts` — navigates to `/`, asserts visible text from root component
    - Create `e2e/README.md` — conventions: file location/naming, selector strategy (data-testid / accessible roles), instructions for running locally
    - Verify: `npx playwright test` starts dev server and passes the smoke test
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 9. CI pipeline updates
  - [x] 9.1 Update backend-ci.yml to use `verify` instead of `package`
    - Change `./mvnw package -B` to `./mvnw verify -B` in the build step
    - This triggers JaCoCo report generation (already bound to verify phase)
    - The existing JaCoCo upload step already conditionally checks for the plugin — no change needed there
    - _Requirements: 8.1, 8.3, 8.5_

  - [x] 9.2 Verify frontend-ci.yml compatibility with Playwright
    - Confirm the existing `e2e` job in `frontend-ci.yml` correctly detects `@playwright/test` in package.json
    - No structural changes needed — the job already conditionally runs Playwright when the dependency exists
    - Verify path filters include `staff-engagement-frontend/**` (already present)
    - _Requirements: 8.2, 8.4, 8.6, 8.7_

- [x] 10. Final checkpoint - Full harness verified
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- The acceptance-test module (tasks 5.1–5.3) **MUST be implemented following the `cucumber-acceptance-setup` skill guidelines**. When executing these tasks, activate the `cucumber-acceptance-setup` skill first to get the complete four-layer architecture patterns, Spring configuration, and anti-flakiness rules.
- Each task references specific requirements for traceability.
- Checkpoints ensure incremental validation.
- PITest does NOT run in CI — it's behind the `pitest` profile for local use only.
- JaCoCo is lenient — no thresholds, report-only.
- The acceptance-test module is a standalone Maven project at monorepo root, NOT inside `staff-engagement-backend/`.
- Acceptance tests require the application to be running (localhost:4200 + localhost:8080) — they connect to the deployed app, not `@SpringBootTest`.
- Frontend e2e tests auto-start the dev server via Playwright's `webServer` config.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "5.1", "7.1"] },
    { "id": 1, "tasks": ["2.1", "5.2", "8.1"] },
    { "id": 2, "tasks": ["3.1", "3.2", "5.3"] },
    { "id": 3, "tasks": ["5.4", "9.1", "9.2"] }
  ]
}
```
