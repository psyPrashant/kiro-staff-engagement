# Implementation Plan: Frontend Scaffold

## Overview

This plan implements the Angular 21 frontend scaffold: five feature folders with standalone components and lazy-loaded routing, environment configuration with fileReplacements, functional HTTP interceptors, ESLint + Prettier integration, and property-based tests for interceptor correctness. Each task builds incrementally toward a fully buildable, testable, and lintable foundation.

## Tasks

- [x] 1. Create environment configuration files
  - [x] 1.1 Create `src/environments/environment.ts` with `production: false` and `apiBaseUrl: 'http://localhost:8080'`
    - Create the `src/environments/` directory
    - Export an object with `production` (boolean) and `apiBaseUrl` (string)
    - _Requirements: 3.1, 3.4, 3.5_

  - [x] 1.2 Create `src/environments/environment.development.ts` with `production: false` and `apiBaseUrl: 'https://dev-api.example.com'`
    - Same shape as the default environment file
    - _Requirements: 3.2, 3.4_

  - [x] 1.3 Add `fileReplacements` to the `development` build configuration in `angular.json`
    - Replace `src/environments/environment.ts` with `src/environments/environment.development.ts`
    - _Requirements: 3.3_

- [x] 2. Create feature folders and standalone components
  - [x] 2.1 Create the `user` feature folder with component, template, styles, and routes
    - Create `src/app/user/user.ts` — standalone component with selector `app-user` and class `User`
    - Create `src/app/user/user.html` — simple template with a heading
    - Create `src/app/user/user.css` — empty scoped styles
    - Create `src/app/user/user.routes.ts` — exports `routes` array with `{ path: '', component: User }`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 2.2 Create the `employee` feature folder with component, template, styles, and routes
    - Same structure as `user`: `employee.ts`, `employee.html`, `employee.css`, `employee.routes.ts`
    - Selector `app-employee`, class `Employee`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 2.3 Create the `client` feature folder with component, template, styles, and routes
    - Same structure: `client.ts`, `client.html`, `client.css`, `client.routes.ts`
    - Selector `app-client`, class `Client`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 2.4 Create the `interaction` feature folder with component, template, styles, and routes
    - Same structure: `interaction.ts`, `interaction.html`, `interaction.css`, `interaction.routes.ts`
    - Selector `app-interaction`, class `Interaction`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 2.5 Create the `task` feature folder with component, template, styles, and routes
    - Same structure: `task.ts`, `task.html`, `task.css`, `task.routes.ts`
    - Selector `app-task`, class `Task`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 2.6 Write a unit test for the `user` component
    - Create `src/app/user/user.spec.ts`
    - Use `TestBed.createComponent(User)` and assert `fixture.componentInstance` is truthy
    - _Requirements: 7.2, 7.5_

- [x] 3. Configure lazy-loaded routing in `app.routes.ts`
  - [x] 3.1 Update `app.routes.ts` with lazy-loaded routes and redirects
    - Add `loadChildren` entries for all 5 feature folders using dynamic imports
    - Add default redirect from `''` to `'user'` with `pathMatch: 'full'`
    - Add wildcard route `'**'` redirecting to `'user'`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 4. Implement HTTP interceptors and wire into app config
  - [x] 4.1 Create `src/app/core/interceptors/base-url.interceptor.ts`
    - Implement `baseUrlInterceptor` as `HttpInterceptorFn`
    - Pass through absolute URLs unchanged
    - Prepend `environment.apiBaseUrl` to relative URLs with exactly one `/` separator
    - _Requirements: 4.1, 4.2_

  - [x] 4.2 Create `src/app/core/interceptors/error.interceptor.ts`
    - Implement `errorInterceptor` as `HttpInterceptorFn`
    - Catch errors, log status code, request URL, and error message to `console.error`
    - Rethrow the error for calling code to handle
    - _Requirements: 4.3_

  - [x] 4.3 Update `app.config.ts` to register interceptors and provide HttpClient
    - Import `provideHttpClient` and `withInterceptors` from `@angular/common/http`
    - Register `baseUrlInterceptor` before `errorInterceptor` in the interceptors array
    - _Requirements: 4.4, 4.5_

- [x] 5. Checkpoint - Ensure build and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Set up ESLint with angular-eslint
  - [x] 6.1 Install ESLint and angular-eslint dependencies and create `eslint.config.js`
    - Install `angular-eslint`, `@angular-eslint/builder`, `@angular-eslint/eslint-plugin`, `@angular-eslint/eslint-plugin-template`, `@angular-eslint/template-parser`, `typescript-eslint`, and `eslint` as devDependencies
    - Create `eslint.config.js` flat config extending `tsRecommended`, `angular.configs.tsRecommended`, `templateRecommended`, `templateAccessibility`, and `eslint-config-prettier`
    - _Requirements: 5.1, 5.2, 6.2_

  - [x] 6.2 Add `lint` architect target in `angular.json` and `lint` script in `package.json`
    - Add builder `@angular-eslint/builder:lint` with `lintFilePatterns` for `src/**/*.ts` and `src/**/*.html`
    - Add `"lint": "ng lint"` to `package.json` scripts
    - _Requirements: 5.3, 5.4, 5.5_

- [x] 7. Integrate Prettier with ESLint
  - [x] 7.1 Install `eslint-config-prettier` and update `.prettierrc` configuration
    - Install `eslint-config-prettier` as devDependency
    - Update `.prettierrc` with single quotes, 100-char print width, 2-space indentation, trailing commas "all", and Angular HTML parser override
    - _Requirements: 6.1, 6.2_

  - [x] 7.2 Add `format` and `format:check` scripts to `package.json`
    - `"format": "prettier --write \"src/**/*.{ts,html,css,json}\"""`
    - `"format:check": "prettier --check \"src/**/*.{ts,html,css,json}\"""`
    - _Requirements: 6.3, 6.4, 6.5_

- [x] 8. Checkpoint - Ensure build, test, lint, and format all pass
  - Run `ng build`, `ng test`, `ng lint`, and `npx prettier --check .`
  - Ensure all tests pass, ask the user if questions arise.
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 9. Add property-based tests for HTTP interceptors
  - [x] 9.1 Install `fast-check` as a devDependency
    - Add `fast-check` to `devDependencies` in `package.json`
    - _Requirements: 7.2_

  - [x] 9.2 Write property test for base-url interceptor (relative URL joining)
    - Create `src/app/core/interceptors/base-url.interceptor.spec.ts`
    - **Property 1: Relative URL joining produces exactly one separator**
    - Generate arbitrary base URLs and relative paths using fast-check
    - Assert the joined URL contains exactly one `/` between base and path
    - **Validates: Requirements 4.1**

  - [x] 9.3 Write property test for base-url interceptor (absolute URL passthrough)
    - Add test to `src/app/core/interceptors/base-url.interceptor.spec.ts`
    - **Property 2: Absolute URLs pass through unchanged**
    - Generate arbitrary absolute URLs starting with `http://` or `https://`
    - Assert the output URL equals the input URL exactly
    - **Validates: Requirements 4.2**

  - [x] 9.4 Write property test for error interceptor (logs and rethrows)
    - Create `src/app/core/interceptors/error.interceptor.spec.ts`
    - **Property 3: Error interceptor logs and rethrows for all error status codes**
    - Generate arbitrary status codes in 400–599 range, URLs, and error messages
    - Assert `console.error` is called with the status, URL, and message, and the error is rethrown
    - **Validates: Requirements 4.3**

- [x] 10. Final checkpoint - Ensure all tests pass
  - Run `ng build`, `ng test`, `ng lint`, and `npx prettier --check .`
  - Ensure all tests pass, ask the user if questions arise.
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The project uses Angular 21 with standalone components (no NgModules), TypeScript 5.9, and Vitest 4
- ESLint uses flat config format (`eslint.config.js`) as required by angular-eslint v19+
- All code must pass Prettier formatting with single quotes, 100-char print width, 2-space indentation

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "2.1", "2.2", "2.3", "2.4", "2.5"] },
    { "id": 2, "tasks": ["2.6", "3.1", "4.1", "4.2"] },
    { "id": 3, "tasks": ["4.3"] },
    { "id": 4, "tasks": ["6.1", "7.1", "9.1"] },
    { "id": 5, "tasks": ["6.2", "7.2"] },
    { "id": 6, "tasks": ["9.2", "9.3", "9.4"] }
  ]
}
```
