# Fix Vitest Configuration Bugfix Design

## Overview

Running `npx vitest --run` standalone fails for 23 of 27 test files because no `vitest.config.ts` exists to provide the runtime configuration that the `@angular/build:unit-test` builder handles internally. The fix introduces two files — `vitest.config.ts` and `src/test-setup.ts` — that provide globals injection, environment setup, e2e exclusion, and Angular resource resolution via the `@angular/build` Vitest plugin.

## Glossary

- **Bug_Condition (C)**: Running `npx vitest --run` on test files that depend on runtime configuration not present without a standalone Vitest config (globals, compiler import, TestBed init, plugin, or exclude patterns)
- **Property (P)**: All 27 test files execute successfully under `npx vitest --run` with correct Angular environment bootstrapping
- **Preservation**: The 4 currently-passing tests (explicit vitest imports, no TestBed) continue to pass; `ng test` via the builder is unaffected; `tsconfig.spec.json` continues to be used
- **vitest.config.ts**: The Vitest configuration file at the frontend project root that defines runtime settings for standalone execution
- **src/test-setup.ts**: A setup file loaded before each test file that imports `@angular/compiler` and calls `TestBed.initTestEnvironment()`
- **@angular/build Vitest plugin**: The plugin exported from `@angular/build` that handles Angular component resource resolution (templateUrl, styleUrl) for Vitest

## Bug Details

### Bug Condition

The bug manifests when running `npx vitest --run` on any test file that relies on configuration the `@angular/build:unit-test` builder provides internally but is absent when Vitest runs standalone. Five distinct failure categories exist: missing globals, missing JIT compiler, uninitialized test environment, e2e file inclusion, and unresolved component resources.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type TestFile
  OUTPUT: boolean

  RETURN (input.usesGlobalsWithoutImport AND NOT configHasGlobalsTrue())
         OR (input.usesTestBedJIT AND NOT setupFileImportsCompiler())
         OR (input.callsConfigureTestingModule AND NOT setupFileInitsEnvironment())
         OR (input.isPlaywrightE2E AND NOT configExcludesE2E())
         OR (input.hasExternalTemplates AND NOT configHasAngularPlugin())
END FUNCTION
```

### Examples

- `user.spec.ts` uses `describe()` without importing from `vitest` → fails with "describe is not defined" because `globals: true` is not set
- `auth.guard.spec.ts` calls `TestBed.configureTestingModule()` without prior `initTestEnvironment()` → fails with initialization error
- `shell.component.spec.ts` has a component with `templateUrl` → fails with resource resolution error because no plugin processes `.html` files
- `e2e/smoke.spec.ts` uses `@playwright/test` → Vitest picks it up and fails because `test.describe()` is Playwright's API, not Vitest's
- `not-blank.validator.spec.ts` explicitly imports `{ describe, expect, it } from 'vitest'` and has no TestBed → passes today (unchanged)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Test files that explicitly import from `vitest` and do not use TestBed must continue to pass as they do today
- `ng test` via `@angular/build:unit-test` must continue to work independently — the builder ignores `vitest.config.ts`
- `tsconfig.spec.json` must remain the TypeScript configuration for test compilation
- Pure logic tests (no Angular deps, no TestBed) must not require the Angular setup file to execute

**Scope:**
All inputs that do NOT trigger the bug condition should be completely unaffected by this fix. This includes:
- Test files that already pass (4 files with explicit vitest imports)
- The `ng test` command and its internal builder configuration
- TypeScript compilation settings in `tsconfig.spec.json`
- Non-test source files and build configuration

## Hypothesized Root Cause

Based on the bug analysis, the root cause is straightforward — **missing configuration files**:

1. **No `vitest.config.ts`**: Without this file, Vitest uses defaults: no globals injection, no jsdom environment, no exclude patterns beyond `node_modules`, and no plugins. The `@angular/build:unit-test` builder programmatically constructs this configuration internally, but standalone `npx vitest` has no way to discover it.

2. **No `src/test-setup.ts`**: Without a setup file, the Angular compiler is never loaded for JIT compilation, and `TestBed.initTestEnvironment()` is never called. The builder handles this step internally when running `ng test`.

3. **No Angular plugin registration**: The `@angular/build` package exports a Vitest plugin that resolves `templateUrl` and `styleUrl` references. Without it registered in a config file, component tests with external resources fail.

4. **No e2e exclusion**: Vitest's default `include` pattern (`**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts}`) matches the Playwright e2e files under `e2e/` because they use `.spec.ts` naming.

## Correctness Properties

Property 1: Bug Condition - Standalone Vitest Execution Succeeds

_For any_ test file under `src/` that uses Vitest globals, Angular TestBed, JIT compilation, or external component templates, the fixed configuration SHALL enable `npx vitest --run` to execute that test successfully with all required runtime setup (globals injected, compiler loaded, environment initialized, resources resolved).

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

Property 2: Preservation - Existing Passing Tests and ng test Unaffected

_For any_ test file that currently passes (explicitly imports from `vitest`, no TestBed dependency) or any invocation of `ng test`, the fixed configuration SHALL produce the same result as before — no regressions, no changed behavior, no additional setup overhead for pure logic tests.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

**File**: `staff-engagement-frontend/vitest.config.ts` (new file)

**Specific Changes**:
1. **Import and register the `@angular/build` Vitest plugin**: Use `import { plugin as angularPlugin } from '@angular/build/vite/plugin'` (or the correct export path) to handle Angular resource resolution for `templateUrl`/`styleUrl`
2. **Set `globals: true`**: Inject `describe`, `it`, `expect`, `beforeEach`, etc. into the global scope so tests without explicit imports work
3. **Set `environment: 'jsdom'`**: Provide a browser-like DOM environment required by Angular components and services that interact with the DOM
4. **Configure `setupFiles: ['src/test-setup.ts']`**: Point to the setup file that bootstraps Angular's test environment
5. **Configure `exclude` patterns**: Add `['e2e/**', 'node_modules/**', 'dist/**']` to prevent Playwright e2e tests from being picked up
6. **Configure `include` patterns**: Restrict to `['src/**/*.spec.ts']` for explicit scoping

**File**: `staff-engagement-frontend/src/test-setup.ts` (new file)

**Specific Changes**:
1. **Import `@angular/compiler`**: Side-effect import (`import '@angular/compiler'`) to enable JIT compilation for TestBed
2. **Import and call `TestBed.initTestEnvironment()`**: Bootstrap the Angular testing environment with `BrowserDynamicTestingModule` and `platformBrowserDynamicTesting` from `@angular/platform-browser-dynamic/testing`
3. **Import `provideZonelessTesting` or ensure zone.js compatibility**: Since Angular 21 uses signals, ensure the test environment matches the application's zone strategy

### Configuration File Structure

```typescript
// vitest.config.ts
import { defineConfig } from 'vitest/config';
import { plugin as angularPlugin } from '@angular/build/vite/plugin';

export default defineConfig({
  plugins: [angularPlugin()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['src/test-setup.ts'],
    include: ['src/**/*.spec.ts'],
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
  },
});
```

```typescript
// src/test-setup.ts
import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import {
  BrowserDynamicTestingModule,
  platformBrowserDynamicTesting,
} from '@angular/platform-browser-dynamic/testing';

TestBed.initTestEnvironment(BrowserDynamicTestingModule, platformBrowserDynamicTesting());
```

### Interaction with Existing Configuration

- **`angular.json` test architect**: The `@angular/build:unit-test` builder constructs its own Vitest config programmatically and does NOT read `vitest.config.ts`. The two paths are independent.
- **`tsconfig.spec.json`**: Vitest uses this via the Angular plugin's TypeScript integration. The `"types": ["vitest/globals"]` declaration provides compile-time types; the config's `globals: true` provides runtime injection. Both are needed.
- **`package.json` scripts**: No changes needed. `npm test` continues to run `ng test`; developers use `npx vitest --run` for standalone execution.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, confirm the bug exists by running `npx vitest --run` before the fix and observing failures, then verify the fix resolves all 5 failure categories while preserving existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm that the 5 failure categories are reproducible.

**Test Plan**: Run `npx vitest --run` on the unfixed codebase and categorize failures by root cause. Document specific error messages for each category.

**Test Cases**:
1. **Globals Missing Test**: Run `user.spec.ts` — expect "describe is not defined" (will fail on unfixed code)
2. **JIT Compiler Missing Test**: Run `auth.guard.spec.ts` — expect JIT compilation error (will fail on unfixed code)
3. **TestBed Uninitialized Test**: Run `auth.service.spec.ts` — expect environment initialization error (will fail on unfixed code)
4. **E2E Inclusion Test**: Run full suite — expect Playwright files to be picked up and fail (will fail on unfixed code)
5. **Resource Resolution Test**: Run `shell.component.spec.ts` — expect templateUrl resolution error (will fail on unfixed code)

**Expected Counterexamples**:
- 17 files fail with "describe is not defined" (globals-dependent without explicit import)
- Multiple files fail with JIT or TestBed initialization errors
- 2 e2e files fail with incompatible test API
- Component tests with external templates fail with resource errors

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed configuration enables successful test execution.

**Pseudocode:**
```
FOR ALL testFile WHERE isBugCondition(testFile) DO
  result := runVitest(testFile, WITH vitest.config.ts AND test-setup.ts)
  ASSERT result.status == PASSED
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed configuration produces the same result as before.

**Pseudocode:**
```
FOR ALL testFile WHERE NOT isBugCondition(testFile) DO
  ASSERT runVitest_original(testFile) == runVitest_fixed(testFile)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It can generate arbitrary valid test configurations and verify no regressions
- It validates that `ng test` results are identical before and after the fix
- It ensures the setup file does not interfere with pure-logic tests

**Test Plan**: Run the 4 currently-passing tests both before and after adding the configuration files, and verify identical pass/fail status and output.

**Test Cases**:
1. **Explicit Import Preservation**: Verify `not-blank.validator.spec.ts` continues to pass with identical behavior
2. **Enum Logic Preservation**: Verify `interaction-type.enum.spec.ts` continues to pass
3. **ng test Preservation**: Verify `ng test` continues to work (uses builder, ignores vitest.config.ts)
4. **tsconfig Preservation**: Verify TypeScript compilation uses `tsconfig.spec.json` unchanged

### Unit Tests

- Verify `vitest.config.ts` is syntactically valid and exports a proper Vitest configuration
- Verify `src/test-setup.ts` successfully imports compiler and initializes TestBed
- Verify exclude patterns correctly filter out `e2e/**` files
- Verify globals are available without explicit imports after configuration

### Property-Based Tests

- Generate random subsets of the 27 test files and verify all pass under the fixed configuration
- Generate test files with various combinations of imports (explicit vs globals) and verify both work
- Verify that adding/removing the setup file import (`@angular/compiler`) is idempotent for tests that already import it explicitly

### Integration Tests

- Run full `npx vitest --run` suite and verify all 27 test files pass
- Run `ng test` and verify it remains unaffected by the new files
- Verify coverage reporting works correctly with the new configuration
- Test that `npx vitest --run src/app/interaction/models/interaction-type.enum.spec.ts` (a currently-passing test) still passes individually
