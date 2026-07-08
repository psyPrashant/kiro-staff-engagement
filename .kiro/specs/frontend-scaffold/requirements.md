# Requirements Document

## Introduction

This feature establishes the Angular 21 frontend scaffold for the Staff Engagement application. It creates a feature-folder structure mirroring the backend domain modules (user, employee, client, interaction, task), configures environment-based API connectivity with an HTTP interceptor, and enforces ESLint + Prettier code quality tooling. The goal is a clean, buildable, testable, and lintable foundation that allows developers to own vertical slices from frontend to backend.

## Glossary

- **Frontend_App**: The Angular 21 single-page application located in `staff-engagement-frontend/`
- **Feature_Folder**: A directory under `src/app/` that encapsulates a domain module's components, services, and routes
- **Environment_Config**: TypeScript files providing environment-specific configuration values (API base URL, production flag)
- **Base_URL_Interceptor**: An Angular HTTP interceptor that prepends the configured API base URL to outgoing HTTP requests
- **Error_Interceptor**: An Angular HTTP interceptor that intercepts HTTP error responses and provides centralized error handling
- **ESLint**: A static analysis tool that identifies problematic patterns in TypeScript and Angular template code
- **Prettier**: A code formatter that enforces consistent style (single quotes, 100-char print width, 2-space indentation)
- **Lazy_Loading**: Angular routing strategy where feature modules are loaded on demand via dynamic imports
- **Angular_CLI**: The command-line interface tool (`ng`) used to build, serve, test, and lint the Angular application

## Requirements

### Requirement 1: Feature Folder Structure

**User Story:** As a developer, I want the frontend organized into feature folders that mirror the backend domain modules, so that I can work on vertical slices from frontend to backend without navigating unrelated code.

#### Acceptance Criteria

1. THE Frontend_App SHALL contain Feature_Folders for each backend domain module: user, employee, client, interaction, and task
2. WHEN a Feature_Folder is created, THE Frontend_App SHALL include a standalone index route component named `{module-name}.ts` within that folder, using the selector `app-{module-name}` and the component class name in PascalCase (e.g., `User`, `Employee`)
3. THE Frontend_App SHALL place each Feature_Folder directly under `src/app/` following the convention `src/app/{module-name}/`
4. WHEN a Feature_Folder is created, THE Frontend_App SHALL include a routes file (`{module-name}.routes.ts`) within that folder that exports a `Routes` array containing a default path (`''`) mapped to the index route component
5. WHEN a Feature_Folder is created, THE Frontend_App SHALL include a component template file (`{module-name}.html`) and a component style file (`{module-name}.css`) within that folder

### Requirement 2: Routing with Lazy Loading

**User Story:** As a developer, I want centralized routing with lazy-loaded feature modules, so that the application loads quickly and routing conventions are consistent across all features.

#### Acceptance Criteria

1. THE Frontend_App SHALL define top-level routes in `app.routes.ts` that map URL paths to Feature_Folders using `loadChildren` with dynamic imports pointing to each feature's `{module-name}.routes.ts` file
2. WHEN a route for a Feature_Folder is navigated to, THE Frontend_App SHALL lazy-load that feature's routes using dynamic imports so the feature bundle is not included in the initial application payload
3. THE Frontend_App SHALL assign URL path segments matching the Feature_Folder names: `user`, `employee`, `client`, `interaction`, and `task`
4. WHEN the root URL path `/` is navigated to, THE Frontend_App SHALL redirect to the `/user` route
5. WHEN a URL path does not match any defined route, THE Frontend_App SHALL redirect to the default route `/user`

### Requirement 3: Environment Configuration

**User Story:** As a developer, I want environment-specific configuration files, so that I can switch between local and dev backends without modifying source code.

#### Acceptance Criteria

1. THE Frontend_App SHALL provide an environment configuration file at `src/environments/environment.ts` for the local environment with `apiBaseUrl` set to `http://localhost:8080` and `production` set to `false`
2. THE Frontend_App SHALL provide an environment configuration file at `src/environments/environment.development.ts` for the dev environment with `apiBaseUrl` set to a placeholder value (e.g., `https://dev-api.example.com`) and `production` set to `false`
3. WHEN the application is built with a non-production configuration, THE Frontend_App SHALL use Angular `fileReplacements` in `angular.json` to substitute the default environment file with the configuration-specific environment file
4. THE Environment_Config SHALL export an object containing a `production` property of type `boolean` and an `apiBaseUrl` property of type `string`
5. WHEN application source code imports the environment configuration, THE Frontend_App SHALL import from the default path `src/environments/environment` so that file replacement can swap the underlying file at build time

### Requirement 4: HTTP Interceptor Scaffold

**User Story:** As a developer, I want a centralized HTTP interceptor that prepends the API base URL and handles errors, so that individual services do not need to manage URL construction or error handling repeatedly.

#### Acceptance Criteria

1. WHEN an HTTP request is made to a relative URL (one that does not start with `http://` or `https://`), THE Base_URL_Interceptor SHALL prepend the `apiBaseUrl` from the active Environment_Config to the request URL, joining them with exactly one `/` separator between the base and the path
2. WHEN an HTTP request is made to an absolute URL (starting with `http://` or `https://`), THE Base_URL_Interceptor SHALL pass the request through without modification
3. WHEN an HTTP response returns a status code in the 4xx or 5xx range, THE Error_Interceptor SHALL log the HTTP status code, request URL, and error message to the browser console, then rethrow the error so calling code can handle it
4. THE Frontend_App SHALL register the Base_URL_Interceptor and Error_Interceptor as HTTP interceptors via `provideHttpClient(withInterceptors([...]))` in `app.config.ts`, with the Base_URL_Interceptor registered before the Error_Interceptor in the interceptor array
5. THE Frontend_App SHALL provide `HttpClient` through the application configuration so that feature services can inject it

### Requirement 5: ESLint Configuration

**User Story:** As a developer, I want ESLint enforced on the project, so that code quality issues are caught early and consistent patterns are maintained across the team.

#### Acceptance Criteria

1. THE Frontend_App SHALL include `angular-eslint` as a dev dependency configured for Angular 21 standalone components
2. THE Frontend_App SHALL include an ESLint flat configuration file (`eslint.config.js`) at the project root that extends `@angular-eslint/recommended` and `typescript-eslint/recommended` rule sets
3. WHEN `ng lint` is executed, THE Angular_CLI SHALL analyze all TypeScript files (`*.ts`) and HTML template files (`*.html`) under `src/` and report violations
4. THE Frontend_App SHALL include a `lint` script in `package.json` that invokes `ng lint`
5. THE Frontend_App SHALL configure a `lint` architect target in `angular.json` using the `@angular-eslint/builder:lint` builder

### Requirement 6: Prettier Integration

**User Story:** As a developer, I want Prettier enforced and integrated with ESLint, so that formatting is automated and does not conflict with linting rules.

#### Acceptance Criteria

1. THE Frontend_App SHALL configure Prettier with single quotes, 100-character print width, 2-space indentation, trailing commas set to "all", and an override for `*.html` files using the Angular parser
2. THE Frontend_App SHALL include `eslint-config-prettier` as the last extension in the ESLint configuration to disable all ESLint rules that conflict with Prettier formatting
3. THE Frontend_App SHALL include a `format` script in `package.json` that runs Prettier on all `*.ts`, `*.html`, `*.css`, and `*.json` files in the `src/` directory
4. WHEN ESLint and Prettier are both executed, THE Frontend_App SHALL produce no conflicting rule violations
5. THE Frontend_App SHALL include a `format:check` script in `package.json` that runs Prettier in check mode and exits with a non-zero code if any file is not formatted

### Requirement 7: Build and Test Verification

**User Story:** As a developer, I want `ng build` and `ng test` to pass cleanly on a fresh checkout, so that CI pipelines and new team members have a reliable starting point.

#### Acceptance Criteria

1. WHEN `npm install` followed by `ng build` is executed on a fresh checkout, THE Angular_CLI SHALL compile the Frontend_App in production configuration without errors or warnings
2. WHEN `ng test` is executed (via Vitest), THE Angular_CLI SHALL run all unit tests and report zero failures
3. WHEN `ng lint` is executed, THE Angular_CLI SHALL report zero linting violations
4. WHEN `npx prettier --check .` is executed, THE Frontend_App SHALL report zero formatting violations
5. THE Frontend_App SHALL include a sample unit test in at least one Feature_Folder that creates the index route component via `TestBed.createComponent()` and asserts that `fixture.componentInstance` is truthy
