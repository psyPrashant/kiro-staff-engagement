# Requirements Document

## Introduction

This feature introduces an authenticated app shell (layout frame) and a dashboard landing page for the Staff Engagement platform. The app shell provides consistent navigation and a logout control that wraps all authenticated routes. The dashboard replaces the current default redirect to `/user` and serves as the landing page with skeleton placeholder cards (to be populated in Epic 4). Unauthenticated users are redirected to the login page and cannot access the shell or dashboard.

## Glossary

- **App_Shell**: A layout component that provides consistent navigation, header, and logout control around all authenticated route content
- **Dashboard**: The default landing page displayed within the App_Shell after authentication, showing skeleton placeholder cards
- **Navigation_Bar**: A UI element within the App_Shell containing links to module areas (User, Employee, Client, Interaction, Task)
- **Skeleton_Card**: A placeholder card UI element rendered on the Dashboard that indicates future content areas
- **AuthService**: The existing Angular service providing authentication state via signals (currentUser, isAuthenticated) and logout functionality
- **authGuard**: The existing Angular route guard that redirects unauthenticated users to the login page
- **Module_Area**: One of the feature sections of the application (User, Employee, Client, Interaction, Task)
- **Authenticated_User**: A user for whom AuthService.isAuthenticated() returns true

## Requirements

### Requirement 1: App Shell Layout

**User Story:** As an authenticated user, I want a consistent navigation layout wrapping all authenticated pages, so that I can easily navigate between module areas without losing context.

#### Acceptance Criteria

1. WHEN an Authenticated_User navigates to any protected route, THE App_Shell SHALL render a Navigation_Bar containing links to all Module_Areas in the following order: User (/user), Employee (/employee), Client (/client), Interaction (/interaction), Task (/task)
2. WHEN an Authenticated_User navigates to any protected route, THE App_Shell SHALL render a logout control that invokes AuthService.logout() when activated
3. WHEN an Authenticated_User navigates to any protected route, THE App_Shell SHALL render the child route content within a router-outlet content area below the Navigation_Bar
4. WHILE an Authenticated_User is viewing a protected route, THE App_Shell SHALL visually distinguish the Navigation_Bar link corresponding to the currently active Module_Area from the inactive links
5. THE App_Shell SHALL persist the Navigation_Bar and logout control across all authenticated route transitions without full page reload, so that both elements remain visible and interactive on every protected route

### Requirement 2: Dashboard Landing Page

**User Story:** As an authenticated user, I want a dashboard landing page as my entry point, so that I have a central starting location within the application.

#### Acceptance Criteria

1. WHEN an Authenticated_User navigates to the root path ('/'), THE Router SHALL redirect to the Dashboard route ('/dashboard')
2. WHEN an Authenticated_User accesses the Dashboard route, THE Dashboard SHALL render as a standalone component within the App_Shell's router-outlet content area
3. THE Dashboard SHALL display at least 3 and no more than 6 Skeleton_Cards as placeholder content, where each Skeleton_Card is a rectangular container with a CSS loading shimmer animation indicating future interactive areas
4. WHEN an Authenticated_User navigates to an undefined route ('**'), THE Router SHALL redirect to the Dashboard route ('/dashboard')
5. IF an unauthenticated user navigates to the Dashboard route ('/dashboard'), THEN THE Router SHALL redirect to the Login route

### Requirement 3: Authentication Enforcement

**User Story:** As a system administrator, I want only authenticated users to access the app shell and dashboard, so that unauthenticated users cannot view protected content.

#### Acceptance Criteria

1. WHEN a user with AuthService.isAuthenticated() returning false navigates to the Dashboard route, THE authGuard SHALL redirect the user to /login with a returnUrl query parameter preserving the attempted path
2. WHEN a user with AuthService.isAuthenticated() returning false navigates to any route within the App_Shell, THE authGuard SHALL redirect the user to /login with a returnUrl query parameter preserving the attempted path
3. THE App_Shell SHALL NOT render for the /login route; the login page SHALL render independently without the App_Shell wrapper
4. WHEN a user with AuthService.isAuthenticated() returning false navigates to a wildcard/undefined route, THE authGuard SHALL redirect the user to /login

### Requirement 4: Route Structure

**User Story:** As a developer, I want a clear route hierarchy where the app shell is a parent layout route for all authenticated content, so that navigation is consistent and maintainable.

#### Acceptance Criteria

1. THE Router SHALL define the App_Shell as a parent route with path `''` containing child routes for the Dashboard and all Module_Areas (user, employee, client, interaction, task)
2. THE Router SHALL apply the authGuard via `canActivate` on the App_Shell parent route only, with no guard declarations on any child route definitions
3. THE Router SHALL lazy-load each Module_Area child route using `loadChildren` and SHALL lazy-load the Dashboard child route using `loadComponent`, so that each produces a separate bundle
4. THE Router SHALL keep the `/login` route defined at the top level of the route configuration, outside the App_Shell parent route hierarchy
5. THE Router SHALL define the wildcard route (`**`) as a child of the App_Shell parent route, redirecting to the Dashboard path

### Requirement 5: Navigation Active State

**User Story:** As an authenticated user, I want to see which module area I am currently viewing in the navigation, so that I maintain orientation within the application.

#### Acceptance Criteria

1. WHEN an Authenticated_User is viewing a Module_Area, THE Navigation_Bar SHALL apply a CSS class (e.g., 'active') to the corresponding Module_Area link, and exactly one link SHALL have the active CSS class at any time
2. WHEN an Authenticated_User navigates to a different Module_Area, THE Navigation_Bar SHALL remove the active CSS class from the previously active link and apply it to the newly active Module_Area link within the same rendering cycle
3. WHEN an Authenticated_User is viewing a child route within a Module_Area (e.g., /user/profile), THE Navigation_Bar SHALL apply the active CSS class to the parent Module_Area link (e.g., /user)

### Requirement 6: Testability

**User Story:** As a developer, I want unit and end-to-end tests confirming the app shell and dashboard behavior, so that regressions are caught automatically.

#### Acceptance Criteria

1. THE App_Shell unit test SHALL verify that the Navigation_Bar renders with links to all Module_Areas (User, Employee, Client, Interaction, Task) using data-testid selectors
2. THE Dashboard unit test SHALL verify that the Dashboard component renders Skeleton_Cards for an Authenticated_User
3. THE acceptance smoke test SHALL verify that after login, the Authenticated_User sees the Dashboard rendered within the App_Shell Navigation_Bar, completing within 30 seconds
