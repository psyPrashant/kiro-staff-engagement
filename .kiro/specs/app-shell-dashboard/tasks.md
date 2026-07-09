# Implementation Plan: App Shell & Dashboard

## Overview

Implement an authenticated app shell layout and dashboard landing page for the Staff Engagement frontend. The shell provides persistent navigation wrapping all authenticated routes via a parent layout route. The dashboard serves as the default landing page with skeleton placeholder cards. Acceptance tests verify the integration using the existing four-layer Cucumber + Playwright architecture.

## Tasks

- [x] 1. Create the ShellComponent
  - [x] 1.1 Create `src/app/shell/shell.component.ts`, `shell.component.html`, and `shell.component.css`
    - Standalone component with selector `app-shell`
    - Import `RouterOutlet`, `RouterLink`, `RouterLinkActive`
    - Template: `<nav>` with links to /user, /employee, /client, /interaction, /task using `routerLink` and `routerLinkActive="active"`
    - Logout button calling `AuthService.logout()`
    - `<main>` with nested `<router-outlet />`
    - All interactive elements have `data-testid` attributes (app-nav, nav-link-user, nav-link-employee, nav-link-client, nav-link-interaction, nav-link-task, logout-button)
    - CSS for navigation layout, active link styling
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 5.1, 5.2, 5.3, 6.1_

  - [x] 1.2 Write unit tests for ShellComponent (`src/app/shell/shell.component.spec.ts`)
    - Verify navigation bar renders links to all 5 module areas with correct `routerLink` values
    - Verify logout button calls `AuthService.logout()` when clicked
    - Verify a `<router-outlet>` element exists for child content
    - Verify all navigation links have `data-testid` attributes
    - Use Vitest + Angular TestBed with `RouterTestingHarness` or `provideRouter`
    - _Requirements: 6.1_

- [x] 2. Create the DashboardComponent
  - [x] 2.1 Create `src/app/dashboard/dashboard.component.ts`, `dashboard.component.html`, and `dashboard.component.css`
    - Standalone component with selector `app-dashboard`
    - Template: `<section data-testid="dashboard">` containing heading and 4 skeleton cards using `@for` block syntax
    - Each card has `class="skeleton-card"` and `data-testid="skeleton-card"`
    - CSS with shimmer animation, responsive grid layout
    - _Requirements: 2.2, 2.3_

  - [x] 2.2 Write unit tests for DashboardComponent (`src/app/dashboard/dashboard.component.spec.ts`)
    - Verify exactly 4 skeleton cards are rendered
    - Verify each card has the `skeleton-card` CSS class
    - Verify dashboard container has `data-testid="dashboard"`
    - Use Vitest + Angular TestBed
    - _Requirements: 6.2_

- [x] 3. Restructure route configuration
  - [x] 3.1 Update `src/app/app.routes.ts` to use shell as parent layout route
    - Keep `/login` as top-level route (outside shell)
    - Define shell route at path `''` with `component: ShellComponent` and `canActivate: [authGuard]`
    - Add child routes: `'' → redirect to 'dashboard'`, lazy-loaded dashboard via `loadComponent`, lazy-loaded module area routes via `loadChildren` (user, employee, client, interaction, task), wildcard `** → redirect to 'dashboard'`
    - Remove individual `canActivate` guards from module routes
    - Remove top-level wildcard and top-level default redirect
    - _Requirements: 2.1, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 3.2 Write route configuration tests (`src/app/app.routes.spec.ts`)
    - Verify login route is at top level, outside any parent wrapper
    - Verify shell route has `canActivate: [authGuard]`
    - Verify shell route contains child routes for dashboard, user, employee, client, interaction, task, and wildcard
    - Verify no child route has its own `canActivate` guard
    - Verify empty path child redirects to `dashboard`
    - Verify wildcard child redirects to `dashboard`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 4. Checkpoint - Verify frontend builds and unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Acceptance test: page objects layer
  - [x] 5.1 Create `AppShellPage.java` in `acceptance-tests/src/test/java/com/psybergate/acceptance/drivers/ui/pages/`
    - Extend `BasePage`
    - Locators using `data-testid`: `[data-testid="app-nav"]`, `[data-testid="nav-link-user"]`, `[data-testid="nav-link-employee"]`, `[data-testid="nav-link-client"]`, `[data-testid="nav-link-interaction"]`, `[data-testid="nav-link-task"]`, `[data-testid="logout-button"]`
    - Methods: `isNavBarVisible()`, `getNavLinkTexts()`, `isNavLinkVisible(String testId)`
    - Annotate with `@Component` and `@ScenarioScope`
    - _Requirements: 1.1, 6.3_

  - [x] 5.2 Create `DashboardPage.java` in `acceptance-tests/src/test/java/com/psybergate/acceptance/drivers/ui/pages/`
    - Extend `BasePage`
    - Locators using `data-testid`: `[data-testid="dashboard"]`, `[data-testid="skeleton-card"]`
    - Methods: `isDashboardVisible()`, `waitForDashboard()`
    - Annotate with `@Component` and `@ScenarioScope`
    - _Requirements: 2.2, 6.3_

- [x] 6. Acceptance test: domain layer (actors and assertions)
  - [x] 6.1 Create `DashboardActor.java` in `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/shell/`
    - Inject `DashboardPage` and `TestWorld`
    - Methods: `waitForDashboard()` — waits for dashboard to be visible after login
    - Annotate with `@Component` and `@ScenarioScope`
    - _Requirements: 6.3_

  - [x] 6.2 Create `DashboardAssertions.java` in `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/shell/`
    - Inject `DashboardPage`, `AppShellPage`, and `TestWorld`
    - Methods: `assertDashboardVisible()`, `assertNavBarVisibleWithAllLinks()`
    - Use AssertJ assertions
    - Annotate with `@Component` and `@ScenarioScope`
    - _Requirements: 6.3_

- [x] 7. Acceptance test: step definitions and feature file
  - [x] 7.1 Create `AppShellStepDefinitions.java` in `acceptance-tests/src/test/java/com/psybergate/acceptance/stepdefs/shell/`
    - Inject `DashboardActor`, `DashboardAssertions`, and `TestWorld`
    - Map Gherkin steps: "the user should see the dashboard", "the navigation bar should be visible with links to all module areas"
    - Reuse existing `LoginStepDefinitions` steps for Given/When (no duplication)
    - _Requirements: 6.3_

  - [x] 7.2 Create `app_shell_dashboard.feature` in `acceptance-tests/src/test/resources/features/shell/`
    - Scenario: "Authenticated user sees dashboard within the app shell"
    - Steps: Given navigates to login → When logs in → Then sees dashboard → And navigation bar visible with all module links
    - Tag with appropriate story tag
    - _Requirements: 6.3_

- [x] 8. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- No property-based tests needed — the feature is UI layout and declarative routing with no pure functions or large input spaces
- The acceptance test follows the four-layer architecture: page objects → domain actors/assertions → step definitions → feature file
- Existing `LoginStepDefinitions` step definitions ("Given the user navigates to the login page", "When the user logs in with email ... and password ...") are reused in the acceptance scenario — no duplication

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "2.2", "3.1"] },
    { "id": 2, "tasks": ["3.2", "5.1", "5.2"] },
    { "id": 3, "tasks": ["6.1", "6.2"] },
    { "id": 4, "tasks": ["7.1", "7.2"] }
  ]
}
```
