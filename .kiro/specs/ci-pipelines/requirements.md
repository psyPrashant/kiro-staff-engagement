# Requirements Document

## Introduction

This feature establishes GitHub Actions CI pipelines for the Staff Engagement monorepo. The pipelines validate backend and frontend code on every pull request and push to main, ensuring that no failing code merges. Coverage metrics are surfaced as informational artifacts without blocking merges initially.

## Glossary

- **Backend_Workflow**: The GitHub Actions workflow responsible for building, testing, and reporting coverage for the Spring Boot backend sub-project.
- **Frontend_Workflow**: The GitHub Actions workflow responsible for linting, building, testing, and reporting coverage for the Angular frontend sub-project.
- **CI_Pipeline**: The collective set of GitHub Actions workflows that run automated checks on code changes.
- **Status_Check**: A GitHub required status check that must pass before a pull request can be merged into the protected branch.
- **Coverage_Report**: A JaCoCo (backend) or Vitest (frontend) generated report showing code coverage percentages.
- **Branch_Protection**: GitHub repository settings that enforce required status checks and prevent merging when checks fail.
- **PR**: A GitHub pull request submitted for code review and merge into main.
- **Scaffold**: The current minimal working state of the codebase with basic tests passing.

## Requirements

### Requirement 1: Backend CI Workflow Triggers

**User Story:** As a developer, I want the backend CI workflow to run automatically on PRs and pushes to main, so that code quality is validated before and after merging.

#### Acceptance Criteria

1. WHEN a pull request targeting main is opened, synchronized, or reopened with changes in the `staff-engagement-backend/` directory, THE Backend_Workflow SHALL execute.
2. WHEN a push to main includes changes in the `staff-engagement-backend/` directory, THE Backend_Workflow SHALL execute.
3. WHEN a pull request or push contains changes only outside the `staff-engagement-backend/` directory, THE Backend_Workflow SHALL NOT execute.
4. WHEN a pull request or push modifies the Backend_Workflow definition file (`.github/workflows/backend-ci.yml`), THE Backend_Workflow SHALL execute regardless of whether `staff-engagement-backend/` files changed.
5. WHEN a pull request or push includes changes to both `staff-engagement-backend/` files and files outside that directory, THE Backend_Workflow SHALL execute.

### Requirement 2: Backend Build and Test Execution

**User Story:** As a developer, I want the backend workflow to compile the code and run all test suites, so that I receive fast feedback on regressions.

#### Acceptance Criteria

1. THE Backend_Workflow SHALL execute `./mvnw package` from the `staff-engagement-backend/` directory using Java 21 to compile and test the backend project.
2. THE Backend_Workflow SHALL run unit tests as part of the Maven build lifecycle triggered by the `package` goal.
3. THE Backend_Workflow SHALL run integration tests that use Testcontainers with PostgreSQL, with Docker services available in the CI runner environment.
4. IF Cucumber test dependencies are declared in the project `pom.xml`, THEN THE Backend_Workflow SHALL execute Cucumber tests as part of the Maven test phase.
5. IF Spring Modulith dependencies are declared in the project `pom.xml`, THEN THE Backend_Workflow SHALL execute the Spring Modulith verification test as part of the Maven test phase.
6. IF any test fails, THEN THE Backend_Workflow SHALL report a failing status check on the pull request or push.
7. IF compilation fails, THEN THE Backend_Workflow SHALL report a failing status check on the pull request or push without proceeding to test execution.

### Requirement 3: Backend Coverage Reporting

**User Story:** As a developer, I want code coverage metrics published as workflow artifacts, so that the team can track test coverage trends.

#### Acceptance Criteria

1. WHEN the backend build succeeds and the `jacoco-maven-plugin` is declared in the project `pom.xml`, THE Backend_Workflow SHALL generate a JaCoCo HTML coverage report.
2. WHEN a JaCoCo coverage report is generated, THE Backend_Workflow SHALL upload the report directory as a workflow artifact with a retention period of 14 days.
3. THE Backend_Workflow SHALL surface line and branch coverage percentages in the workflow summary without failing the build based on coverage thresholds.
4. IF the `jacoco-maven-plugin` is not declared in the project `pom.xml`, THEN THE Backend_Workflow SHALL skip coverage generation and reporting without error.

### Requirement 4: Frontend CI Workflow Triggers

**User Story:** As a developer, I want the frontend CI workflow to run automatically on PRs and pushes to main, so that frontend code quality is validated continuously.

#### Acceptance Criteria

1. WHEN a pull request targeting main is opened, reopened, or synchronized with changes in the `staff-engagement-frontend/` directory, THE Frontend_Workflow SHALL execute.
2. WHEN a push to main includes changes in the `staff-engagement-frontend/` directory, THE Frontend_Workflow SHALL execute.
3. WHEN a pull request or push contains changes only outside the `staff-engagement-frontend/` directory (including root-level files and `.github/` directory), THE Frontend_Workflow SHALL NOT execute.
4. WHEN a pull request or push contains changes both inside and outside the `staff-engagement-frontend/` directory, THE Frontend_Workflow SHALL execute.
5. WHEN a workflow_dispatch event is triggered manually, THE Frontend_Workflow SHALL execute regardless of path filters.

### Requirement 5: Frontend Lint and Build

**User Story:** As a developer, I want the frontend workflow to enforce code style and verify the production build, so that formatting issues and build failures are caught early.

#### Acceptance Criteria

1. THE Frontend_Workflow SHALL install project dependencies via `npm ci` before executing lint, format check, or build steps.
2. THE Frontend_Workflow SHALL run ESLint via `npm run lint` against the frontend source code.
3. THE Frontend_Workflow SHALL run Prettier format checking via `npm run format:check`.
4. THE Frontend_Workflow SHALL produce a production build via `npm run build`.
5. IF linting fails, THEN THE Frontend_Workflow SHALL report a failing status check without proceeding to the build step.
6. IF format checking fails, THEN THE Frontend_Workflow SHALL report a failing status check without proceeding to the build step.
7. IF the production build fails, THEN THE Frontend_Workflow SHALL report a failing status check.

### Requirement 6: Frontend Test Execution with Coverage

**User Story:** As a developer, I want the frontend workflow to run Vitest with coverage, so that I can verify tests pass and track frontend coverage.

#### Acceptance Criteria

1. THE Frontend_Workflow SHALL run Vitest tests with coverage enabled via `npx vitest --run --coverage` using the `@vitest/coverage-v8` provider.
2. WHEN tests complete and coverage is generated, THE Frontend_Workflow SHALL upload the coverage report directory as a workflow artifact with a retention period of 14 days.
3. THE Frontend_Workflow SHALL surface line, branch, function, and statement coverage percentages in the workflow summary without failing the build based on coverage thresholds.
4. IF any Vitest test fails, THEN THE Frontend_Workflow SHALL report a failing status check and still upload any partial coverage report that was generated.
5. IF coverage generation fails but all tests pass, THEN THE Frontend_Workflow SHALL report a passing status check and log a warning indicating coverage generation failure.

### Requirement 7: Frontend E2E Test Support

**User Story:** As a developer, I want the CI pipeline to accommodate Playwright e2e tests when they are added, so that the pipeline remains extensible.

#### Acceptance Criteria

1. WHEN `@playwright/test` is listed as a dependency in `package.json`, THE Frontend_Workflow SHALL install Playwright browsers and execute Playwright e2e tests using the project's defined test script.
2. IF Playwright e2e tests are executed and any test fails, THEN THE Frontend_Workflow SHALL report a failing status check.
3. IF `@playwright/test` is not listed as a dependency in `package.json`, THEN THE Frontend_Workflow SHALL skip the e2e test step and complete the step with a success status.
4. WHEN Playwright e2e tests are executed, THE Frontend_Workflow SHALL upload the Playwright test report as a workflow artifact.

### Requirement 8: Branch Protection and Required Status Checks

**User Story:** As a team lead, I want both CI workflows registered as required status checks on main, so that no failing code can be merged.

#### Acceptance Criteria

1. THE Branch_Protection SHALL require the Backend_Workflow job name (as defined in the workflow YAML) to pass as a status check before a PR merges into main.
2. THE Branch_Protection SHALL require the Frontend_Workflow job name (as defined in the workflow YAML) to pass as a status check before a PR merges into main.
3. WHEN a PR has a failing Backend_Workflow or Frontend_Workflow status check, THE Branch_Protection SHALL prevent the merge by disabling the merge action on the PR.
4. WHEN a PR has all required status checks passing, THE Branch_Protection SHALL allow the merge to proceed.
5. WHEN a workflow is skipped due to path filtering (e.g., a PR contains no backend changes), THE Branch_Protection SHALL treat the skipped status check as satisfied and not block the merge.
6. THE Branch_Protection SHALL require status checks to be run against the latest commit on the PR branch before allowing merge (require branches to be up to date).

### Requirement 9: CI Pipeline Reliability on Current Scaffold

**User Story:** As a developer, I want the CI pipelines to pass on the current scaffold code, so that the team can start using branch protection immediately.

#### Acceptance Criteria

1. THE Backend_Workflow SHALL pass on the current scaffold codebase without requiring any source code modifications.
2. THE Frontend_Workflow SHALL pass on the current scaffold codebase without requiring any source code modifications.
3. THE CI_Pipeline SHALL use pinned versions of GitHub Actions (major version tags, e.g., `actions/checkout@v4`) to ensure reproducible builds.
4. THE Backend_Workflow SHALL cache Maven dependencies using the `actions/setup-java` built-in cache or `actions/cache` between runs to reduce build times.
5. THE Frontend_Workflow SHALL cache npm dependencies using `actions/setup-node` built-in cache or `actions/cache` between runs to reduce build times.
6. THE Backend_Workflow SHALL run on an `ubuntu-latest` runner with Docker available for Testcontainers.
7. THE Frontend_Workflow SHALL run on an `ubuntu-latest` runner with Node.js LTS available.
