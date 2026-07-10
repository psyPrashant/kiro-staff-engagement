# Implementation Plan

## Overview

This plan fixes standalone Vitest execution (`npx vitest --run`) by creating two missing configuration files: `vitest.config.ts` and `src/test-setup.ts`. The fix resolves 5 failure categories affecting 23 out of 27 test files while preserving existing behavior for `ng test` and currently-passing tests.

## Tasks

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Standalone Vitest Execution Fails Without Configuration
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists across all 5 failure categories
  - **Scoped PBT Approach**: Use `fast-check` to generate test file paths from the known set of 23 failing files and assert that `npx vitest --run <file>` exits with code 0
  - Create a property test in `staff-engagement-frontend/src/vitest-config-bug.exploration.spec.ts`
  - Property: for any test file that satisfies `isBugCondition` (uses globals without import, uses TestBed JIT, calls configureTestingModule, is Playwright e2e, or has external templates), running `npx vitest --run <file>` should succeed with exit code 0
  - Concrete failing cases to scope the property:
    - A file using globals without import (e.g., any `.spec.ts` using `describe()` without `import { describe } from 'vitest'`)
    - A file using TestBed with JIT (e.g., `auth.guard.spec.ts`)
    - A file calling `TestBed.configureTestingModule()` (e.g., `auth.service.spec.ts`)
    - A component test with `templateUrl` (e.g., `shell.component.spec.ts`)
  - Run test on UNFIXED code using `npx vitest --run src/vitest-config-bug.exploration.spec.ts`
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples found (specific error messages per failure category)
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing Passing Tests and ng test Remain Unaffected
  - **IMPORTANT**: Follow observation-first methodology
  - Observe: Run the 4 currently-passing test files on UNFIXED code and record pass status
  - Observe: Run `ng test` on UNFIXED code and record pass status
  - Create a property test in `staff-engagement-frontend/src/vitest-config-preservation.spec.ts`
  - Write property-based test using `fast-check`: for all test files that do NOT satisfy `isBugCondition` (files with explicit vitest imports and no TestBed dependency), running `npx vitest --run <file>` should exit with code 0
  - Concrete preservation cases to verify:
    - `not-blank.validator.spec.ts` (explicit vitest imports, no TestBed) — passes today
    - `interaction-type.enum.spec.ts` (pure logic, explicit imports) — passes today
    - Other currently-passing pure logic tests — pass today
  - Verify that `tsconfig.spec.json` is unchanged and still used for compilation
  - Run tests on UNFIXED code using `npx vitest --run src/vitest-config-preservation.spec.ts`
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. Fix: Create standalone Vitest configuration files

  - [x] 3.1 Create `staff-engagement-frontend/vitest.config.ts`
    - Import `defineConfig` from `vitest/config`
    - Import the Angular plugin from `@angular/build` (verify correct export path — likely `@angular/build/vite/plugin`)
    - Register Angular plugin in `plugins` array
    - Set `test.globals: true` for runtime injection of describe/it/expect
    - Set `test.environment: 'jsdom'` for DOM simulation
    - Set `test.setupFiles: ['src/test-setup.ts']` to bootstrap Angular test environment
    - Set `test.include: ['src/**/*.spec.ts']` to restrict test discovery to source files
    - Set `test.exclude: ['e2e/**', 'node_modules/**', 'dist/**']` to prevent Playwright e2e inclusion
    - _Bug_Condition: isBugCondition(input) where input relies on globals/JIT/TestBed/plugin/exclude patterns not present_
    - _Expected_Behavior: All 27 test files execute successfully under `npx vitest --run`_
    - _Preservation: ng test via builder unaffected; tsconfig.spec.json unchanged; pure logic tests still pass_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.2, 3.3_

  - [x] 3.2 Create `staff-engagement-frontend/src/test-setup.ts`
    - Add side-effect import: `import '@angular/compiler'` for JIT compilation support
    - Import `TestBed` from `@angular/core/testing`
    - Import `BrowserDynamicTestingModule` and `platformBrowserDynamicTesting` from `@angular/platform-browser-dynamic/testing`
    - Call `TestBed.initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting())`
    - _Bug_Condition: isBugCondition(input) where input.usesTestBedJIT or input.callsConfigureTestingModule_
    - _Expected_Behavior: Angular test environment initialized before each test file runs_
    - _Preservation: Pure logic tests (no Angular deps) are unaffected by setup file loading_
    - _Requirements: 2.2, 2.3, 3.4_

  - [x] 3.3 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Standalone Vitest Execution Succeeds
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior for all buggy inputs
    - When this test passes, it confirms all 5 failure categories are resolved
    - Run `npx vitest --run src/vitest-config-bug.exploration.spec.ts`
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 3.4 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing Passing Tests and ng test Remain Unaffected
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run `npx vitest --run src/vitest-config-preservation.spec.ts`
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all previously-passing tests still pass after fix (no regressions)
    - Verify `ng test` still works via the builder (ignores vitest.config.ts)

- [x] 4. Checkpoint - Ensure all tests pass
  - Run full test suite: `npx vitest --run` — all 27 test files should pass
  - Run `ng test` — confirm builder-based execution is unaffected
  - Verify no new files are picked up by accident (e2e excluded, only src/**/*.spec.ts included)
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- The `@angular/build` package's Vitest plugin export path needs verification at implementation time (may be `@angular/build/vite/plugin` or similar)
- No additional npm packages need to be installed — all dependencies (`vitest`, `jsdom`, `fast-check`, `@angular/build`) already exist in devDependencies
- The `ng test` command uses `@angular/build:unit-test` builder which constructs its own Vitest config internally and ignores `vitest.config.ts`
- The exploration and preservation test files should be removed or moved to a separate test utility folder after the fix is validated

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1", "2"] },
    { "id": 1, "tasks": ["3.1", "3.2"] },
    { "id": 2, "tasks": ["3.3", "3.4"] },
    { "id": 3, "tasks": ["4"] }
  ]
}
```
