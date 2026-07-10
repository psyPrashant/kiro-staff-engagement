# Bugfix Requirements Document

## Introduction

Running frontend unit tests via `npx vitest --run` (standalone, outside of `ng test`) fails for 23 out of 27 test files. The `@angular/build:unit-test` builder handles test environment setup internally when using `ng test`, but no standalone `vitest.config.ts` exists to provide equivalent configuration for direct Vitest execution. This causes failures across five distinct categories: missing globals, missing JIT compiler import, uninitialized test environment, e2e file inclusion, and unresolved component resources.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN running `npx vitest --run` on test files that use `describe`/`it`/`expect` without explicit imports THEN the system throws "describe is not defined" ReferenceError because `globals: true` is not configured at runtime despite `tsconfig.spec.json` declaring `"types": ["vitest/globals"]` for type-checking only

1.2 WHEN running `npx vitest --run` on test files that use Angular TestBed with JIT compilation (interceptors, validators) THEN the system throws a JIT compilation failure because `@angular/compiler` is not imported in any setup file

1.3 WHEN running `npx vitest --run` on test files that call `TestBed.configureTestingModule()` THEN the system throws an error because `TestBed.initTestEnvironment()` has not been called to bootstrap the Angular testing environment

1.4 WHEN running `npx vitest --run` THEN the system picks up Playwright e2e test files (under `e2e/`) and attempts to execute them, causing failures because Playwright's `test.describe()` is not compatible with Vitest's test runner

1.5 WHEN running `npx vitest --run` on test files for components with external templates (`templateUrl`/`styleUrl`) THEN the system fails to resolve Angular component resources because no Vitest plugin is configured to handle Angular resource compilation

### Expected Behavior (Correct)

2.1 WHEN running `npx vitest --run` on test files that use `describe`/`it`/`expect` without explicit imports THEN the system SHALL execute tests successfully with Vitest globals injected at runtime via `globals: true` configuration

2.2 WHEN running `npx vitest --run` on test files that use Angular TestBed with JIT compilation THEN the system SHALL have `@angular/compiler` loaded via a setup file so JIT compilation succeeds

2.3 WHEN running `npx vitest --run` on test files that call `TestBed.configureTestingModule()` THEN the system SHALL have the Angular test environment already initialized via `TestBed.initTestEnvironment()` in a setup file

2.4 WHEN running `npx vitest --run` THEN the system SHALL exclude Playwright e2e test files (under `e2e/`) from the test run via the Vitest `exclude` configuration

2.5 WHEN running `npx vitest --run` on test files for components with external templates THEN the system SHALL resolve Angular component resources correctly via an appropriate Vitest plugin (e.g., `@angular/build`'s Vitest plugin or `@analogjs/vite-plugin-angular`)

### Unchanged Behavior (Regression Prevention)

3.1 WHEN running `npx vitest --run` on test files that explicitly import from `vitest` (e.g., `import { describe, it, expect } from 'vitest'`) and do not use TestBed THEN the system SHALL CONTINUE TO execute those tests successfully as they do today

3.2 WHEN running `ng test` via the Angular CLI THEN the system SHALL CONTINUE TO execute all tests successfully using the `@angular/build:unit-test` builder without being affected by the new `vitest.config.ts`

3.3 WHEN running `npx vitest --run` THEN the system SHALL CONTINUE TO use the existing `tsconfig.spec.json` configuration for TypeScript compilation of test files

3.4 WHEN running `npx vitest --run` on pure logic tests (no Angular dependencies, no TestBed) THEN the system SHALL CONTINUE TO execute those tests without requiring the Angular setup file to be loaded
