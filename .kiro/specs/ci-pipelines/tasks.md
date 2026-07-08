# Implementation Plan: CI Pipelines

## Overview

Create two GitHub Actions workflow files for the Staff Engagement monorepo — one for the backend (Java/Maven) and one for the frontend (Angular/npm). Both workflows use `dorny/paths-filter@v3` for job-level path gating to remain compatible with branch protection required status checks. The frontend also needs `@vitest/coverage-v8` added as a dev dependency for coverage reporting.

## Tasks

- [x] 1. Create directory structure and backend CI workflow
  - [x] 1.1 Create `.github/workflows/backend-ci.yml` with complete backend CI pipeline
    - Create the `.github/workflows/` directory
    - Define workflow triggers: `pull_request` (opened, synchronize, reopened) on `main` and `push` to `main` — no workflow-level `paths:` filter
    - Implement `changes` job using `dorny/paths-filter@v3` to detect changes in `staff-engagement-backend/**` or `.github/workflows/backend-ci.yml`
    - Implement `build` job with condition `if: needs.changes.outputs.backend == 'true'`
    - Steps: `actions/checkout@v4`, `actions/setup-java@v4` (java-version: 21, distribution: temurin, cache: maven), `./mvnw package -B` (working-directory: staff-engagement-backend)
    - Add conditional JaCoCo upload step: check if `jacoco-maven-plugin` is in pom.xml, upload `staff-engagement-backend/target/site/jacoco/` with `actions/upload-artifact@v4` (retention-days: 14, if-no-files-found: ignore)
    - Add conditional coverage summary step writing to `$GITHUB_STEP_SUMMARY`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.1, 3.2, 3.3, 3.4, 9.1, 9.3, 9.4, 9.6_

- [x] 2. Create frontend CI workflow
  - [x] 2.1 Create `.github/workflows/frontend-ci.yml` with path filter and lint-build job
    - Define workflow triggers: `pull_request` (opened, synchronize, reopened) on `main`, `push` to `main`, and `workflow_dispatch` — no workflow-level `paths:` filter
    - Implement `changes` job using `dorny/paths-filter@v3` to detect changes in `staff-engagement-frontend/**` or `.github/workflows/frontend-ci.yml`
    - Implement `lint-build` job with condition `if: needs.changes.outputs.frontend == 'true'`
    - Steps: `actions/checkout@v4`, `actions/setup-node@v4` (node-version: lts/*, cache: npm, cache-dependency-path: staff-engagement-frontend/package-lock.json), `npm ci`, `npm run lint`, `npm run format:check`, `npm run build` (all with working-directory: staff-engagement-frontend)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 9.2, 9.3, 9.5, 9.7_

  - [x] 2.2 Add `test` job to frontend CI workflow
    - Job condition: `if: needs.changes.outputs.frontend == 'true'`
    - Needs: `lint-build` (ensures lint/build pass first)
    - Steps: checkout, setup-node (cached), `npm ci`, `npx vitest --run --coverage` (working-directory: staff-engagement-frontend)
    - Upload coverage artifact with `actions/upload-artifact@v4` (retention-days: 14, if-no-files-found: ignore) using `if: always()` so partial coverage is uploaded on test failure
    - Add coverage summary step writing line, branch, function, and statement percentages to `$GITHUB_STEP_SUMMARY`
    - Handle coverage generation failure: if tests pass but coverage fails, log `::warning::` and don't fail the job
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 2.3 Add conditional `e2e` job to frontend CI workflow
    - Job condition: `if: needs.changes.outputs.frontend == 'true'`
    - Needs: `lint-build`
    - Check if `@playwright/test` exists in `package.json` using grep/jq
    - If present: install Playwright browsers, run e2e tests, upload Playwright report artifact
    - If absent: skip with informational echo message, exit 0
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 3. Add frontend coverage dependency
  - [x] 3.1 Add `@vitest/coverage-v8` as a dev dependency in `staff-engagement-frontend/package.json`
    - Run `npm install --save-dev @vitest/coverage-v8` in the `staff-engagement-frontend/` directory
    - Verify `package.json` and `package-lock.json` are updated
    - _Requirements: 6.1, 9.2_

- [x] 4. Checkpoint - Validate workflow files
  - Ensure all tests pass, ask the user if questions arise.
  - Validate workflow YAML syntax locally (e.g., review structure against GitHub Actions schema)
  - Confirm both workflow files reference correct working directories and action versions
  - Verify the backend workflow will pass on the current scaffold (mvnw package succeeds)
  - Verify the frontend workflow will pass on the current scaffold (lint, format:check, build, vitest all succeed)

- [x] 5. Documentation for branch protection setup
  - [x] 5.1 Add branch protection instructions as comments in workflow files or a dedicated section
    - Add a comment block at the top of each workflow file documenting which job names to use as required status checks
    - Backend: required check name is `build`
    - Frontend: required check name is `lint-build`
    - Document that `test` and `e2e` are informational (not required) to avoid blocking when optional tooling isn't configured
    - Note that skipped jobs (via `if` condition) satisfy required status checks
    - Include instruction to enable "Require branches to be up to date before merging"
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6_

- [x] 6. Final checkpoint - Ensure everything is wired together
  - Ensure all tests pass, ask the user if questions arise.
  - Verify both workflow files are syntactically valid YAML
  - Confirm `@vitest/coverage-v8` is in frontend `package.json` devDependencies
  - Verify all pinned action versions use major version tags (v3, v4)
  - Confirm no source code modifications are required for pipelines to pass

## Notes

- This feature produces declarative YAML workflow files — no application code is modified (except adding a dev dependency)
- Property-based tests are not applicable for GitHub Actions workflow configuration
- The `dorny/paths-filter@v3` approach ensures branch protection compatibility (skipped jobs report as success)
- Coverage upload is non-blocking: uses `if-no-files-found: ignore` and `if: always()` patterns
- The current scaffold must pass without code changes — the backend pom.xml has no `jacoco-maven-plugin`, so coverage steps will be skipped gracefully
- Branch protection configuration is manual (GitHub UI) — documented as instructions, not automated

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "3.1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3"] },
    { "id": 3, "tasks": ["5.1"] }
  ]
}
```
