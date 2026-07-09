# Requirements Document

## Introduction

Frontend login page and authentication guard for the Staff Engagement application. This feature provides a login UI with form validation, a session-based authentication service backed by the existing backend auth API, an HTTP interceptor that ensures session cookies are sent with all API requests, and a route guard that redirects unauthenticated users to the login page. Signal-based current-user state enables reactive UI updates and logout functionality.

## Glossary

- **Login_Page**: The Angular standalone component at route `/login` that presents an email/password form and communicates login results to the user.
- **Auth_Service**: The Angular injectable service responsible for calling the backend login/logout endpoints and maintaining signal-based current-user state.
- **Auth_Interceptor**: The Angular functional HTTP interceptor that attaches `withCredentials: true` to outgoing API requests so that session cookies are included.
- **Auth_Guard**: The Angular functional route guard that checks authentication state and redirects unauthenticated users to the Login_Page.
- **Current_User_Signal**: A writable signal within the Auth_Service that holds the authenticated user object (id, name, email) or `null` when no user is authenticated.
- **Login_Form**: The Angular reactive form within the Login_Page containing email and password form controls with synchronous validators.
- **Protected_Route**: Any route in the application that requires authentication, enforced by the Auth_Guard (all routes except `/login`).
- **Backend_Auth_API**: The backend REST API providing `POST /api/auth/login` and `POST /api/auth/logout` endpoints for session-based authentication.

## Requirements

### Requirement 1: Login Page Component

**User Story:** As a user, I want a login page with email and password fields, so that I can authenticate and access the application.

#### Acceptance Criteria

1. THE Login_Page SHALL display a reactive form with an email input field and a password input field.
2. THE Login_Form SHALL mark the email control as invalid when the value is empty.
3. THE Login_Form SHALL mark the email control as invalid when the value does not contain exactly one "@" symbol followed by a domain with at least one "." separator (e.g., user@example.com).
4. THE Login_Form SHALL mark the password control as invalid when the value is empty.
5. THE Login_Form SHALL mark the password control as invalid when the value has fewer than 6 characters or more than 128 characters.
6. THE Login_Page SHALL disable the submit button while the Login_Form is in an invalid state.
7. WHEN a form control is invalid and has been touched by the user, THE Login_Page SHALL display a validation error message beneath that field indicating the specific validation failure (empty field, invalid email format, or password length violation).
8. WHEN the user submits the Login_Form with valid values, THE Login_Page SHALL call the Auth_Service login method with the email and password values.
9. WHILE the Auth_Service login request is in progress, THE Login_Page SHALL display a visible loading indicator and disable the submit button to prevent duplicate submissions.
10. IF the Auth_Service login request fails, THEN THE Login_Page SHALL re-enable the submit button so the user can retry.
11. IF the Auth_Service login request fails, THEN THE Login_Page SHALL preserve the email value in the form field.

### Requirement 2: Login Error Display

**User Story:** As a user, I want to see a clear error message when login fails, so that I know my credentials were incorrect.

#### Acceptance Criteria

1. WHEN the Backend_Auth_API returns an HTTP 401 response, THE Login_Page SHALL display an error message stating "Invalid email or password" without revealing which field was incorrect.
2. WHEN the Backend_Auth_API returns an HTTP 400 response, THE Login_Page SHALL display an error message indicating that required fields are missing or malformed.
3. IF the Backend_Auth_API returns an unexpected error (status other than 200, 400, or 401) or a network timeout occurs, THEN THE Login_Page SHALL display a generic error message "Login failed. Please try again later."
4. WHEN the user modifies either the email or password field after an error is displayed, THE Login_Page SHALL clear the previously displayed error message.
5. THE Login_Page SHALL display at most one error message at a time in a dedicated error container element above or below the form fields.

### Requirement 3: Auth Service with Signal-Based State

**User Story:** As a developer, I want a centralized authentication service with signal-based state, so that components can reactively observe the current user and trigger login/logout operations.

#### Acceptance Criteria

1. THE Auth_Service SHALL expose a Current_User_Signal that holds either the authenticated user object (with `id` as number, `name` as string, and `email` as string fields) or `null`.
2. THE Auth_Service SHALL initialize the Current_User_Signal to `null` on application startup.
3. WHEN the Auth_Service login method is called with email and password, THE Auth_Service SHALL send a POST request to `/api/auth/login` with a JSON body containing the `email` and `password` fields.
4. WHEN the Backend_Auth_API returns an HTTP 200 response to a login request, THE Auth_Service SHALL update the Current_User_Signal with the user object from the response body.
5. WHEN the Backend_Auth_API returns an error response to a login request, THE Auth_Service SHALL propagate the error as an Observable error to the caller without modifying the Current_User_Signal.
6. THE Auth_Service SHALL expose an `isAuthenticated` computed signal that returns `true` when the Current_User_Signal holds a non-null value, and `false` otherwise.
7. IF a network failure occurs during a login request, THEN THE Auth_Service SHALL propagate the error as an Observable error to the caller without modifying the Current_User_Signal.

### Requirement 4: Logout Functionality

**User Story:** As a user, I want to log out of the application, so that my session is invalidated and other users cannot access my account on this device.

#### Acceptance Criteria

1. WHEN the Auth_Service logout method is called, THE Auth_Service SHALL send a POST request to `/api/auth/logout`.
2. WHEN the Backend_Auth_API returns an HTTP 200 response to a logout request, THE Auth_Service SHALL set the Current_User_Signal to `null`.
3. WHEN the Auth_Service logout method completes successfully, THE Auth_Service SHALL navigate the user to the `/login` route after setting the Current_User_Signal to `null`.
4. IF the Backend_Auth_API returns an error response to a logout request, THEN THE Auth_Service SHALL set the Current_User_Signal to `null` and then navigate the user to the `/login` route.
5. IF the Auth_Service logout method is called when the Current_User_Signal is already `null`, THEN THE Auth_Service SHALL navigate the user to the `/login` route without sending a POST request to the backend.
6. IF the logout POST request does not receive a response within 10 seconds, THEN THE Auth_Service SHALL treat the request as failed, set the Current_User_Signal to `null`, and navigate the user to the `/login` route.

### Requirement 5: Auth Interceptor

**User Story:** As a developer, I want an HTTP interceptor that includes session credentials on all API requests, so that the backend can identify the authenticated user from the session cookie.

#### Acceptance Criteria

1. THE Auth_Interceptor SHALL clone each outgoing HTTP request with `withCredentials` set to `true` and all other request properties unchanged, then pass the cloned request to the next handler.
2. THE Auth_Interceptor SHALL be registered in the application's `provideHttpClient` configuration in the `withInterceptors` array after the baseUrlInterceptor and before the errorInterceptor.
3. THE Auth_Interceptor SHALL be implemented as an `HttpInterceptorFn` and exported from the `core/interceptors` directory, following the same pattern as the existing baseUrlInterceptor and errorInterceptor.

### Requirement 6: Route Guard

**User Story:** As a product owner, I want unauthenticated users redirected to the login page, so that protected application data is not accessible without authentication.

#### Acceptance Criteria

1. WHEN an unauthenticated user (Current_User_Signal is `null`) attempts to navigate to a Protected_Route, THE Auth_Guard SHALL cancel the navigation and redirect the user to the `/login` route.
2. WHEN an authenticated user (Current_User_Signal is non-null) attempts to navigate to a Protected_Route, THE Auth_Guard SHALL allow the navigation to proceed without modification.
3. THE Auth_Guard SHALL be applied to all routes except the `/login` route, including wildcard and redirect routes.
4. WHEN the Auth_Guard redirects to `/login`, THE Auth_Guard SHALL preserve the originally requested URL as a query parameter named `returnUrl`, limited to a relative path of no more than 2048 characters, so the user can be redirected after successful login.

### Requirement 7: Post-Login Redirect

**User Story:** As a user, I want to be redirected to my originally requested page after logging in, so that I do not lose my navigation context.

#### Acceptance Criteria

1. WHEN login succeeds and a `returnUrl` query parameter is present on the `/login` route, THE Login_Page SHALL navigate the user to the URL specified by the decoded `returnUrl` value.
2. WHEN login succeeds and no `returnUrl` query parameter is present, THE Login_Page SHALL navigate the user to the default route (`/user`).
3. IF the decoded `returnUrl` value does not start with `/` or contains a protocol-relative prefix (`//`), THEN THE Login_Page SHALL discard the `returnUrl` value and navigate the user to the default route (`/user`).
4. IF the `returnUrl` query parameter value exceeds 2048 characters in length, THEN THE Login_Page SHALL discard the `returnUrl` value and navigate the user to the default route (`/user`).
5. WHEN login succeeds and the `returnUrl` value includes query parameters or fragment identifiers (e.g., `/page?tab=1#section`), THE Login_Page SHALL preserve the full path, query parameters, and fragment when navigating.

### Requirement 8: Login Route Configuration

**User Story:** As a developer, I want the login page registered as an unguarded route, so that unauthenticated users can reach it and the Auth_Guard does not create a redirect loop.

#### Acceptance Criteria

1. THE application route configuration SHALL include a route with path `login` that lazy-loads the Login_Page component.
2. THE `/login` route SHALL NOT have any route guard (including Auth_Guard) applied via `canActivate` or `canMatch`.
3. WHEN an unauthenticated user navigates to `/login`, THE application SHALL render the Login_Page without performing any redirect.
4. WHEN an authenticated user navigates to `/login`, THE Login_Page SHALL redirect the user to the default route (`/user`) before rendering the login form.
5. IF the Auth_Guard redirects an unauthenticated user, THEN THE Auth_Guard SHALL redirect to `/login`, and THE `/login` route SHALL render without triggering a further redirect, preventing a redirect loop.

### Requirement 9: Unit Tests

**User Story:** As a developer, I want comprehensive Vitest unit tests, so that login form validation, guard redirect logic, and interceptor behavior are verified in isolation.

#### Acceptance Criteria

1. THE Login_Form unit tests SHALL verify that the form is invalid when the email field is empty, when the email does not contain "@" followed by a domain with a "." separator, when the password field is empty, when the password is fewer than 6 characters, and when the password exceeds 128 characters.
2. THE Login_Form unit tests SHALL verify that the form is valid when the email contains a valid format (e.g., user@example.com) and the password is between 6 and 128 characters.
3. THE Auth_Guard unit tests SHALL verify that navigation is cancelled and a `UrlTree` pointing to `/login?returnUrl=<encoded-original-path>` is returned when the Current_User_Signal is `null`.
4. THE Auth_Guard unit tests SHALL verify that `true` is returned (navigation proceeds) when the Current_User_Signal holds a user object.
5. THE Auth_Interceptor unit tests SHALL verify that outgoing requests are cloned with `withCredentials` set to `true` and all other properties unchanged.

### Requirement 10: Cucumber Acceptance Tests

**User Story:** As a developer, I want Cucumber acceptance tests covering the login flow using the existing four-layer harness, so that the feature is validated end-to-end through the real UI against a running backend.

#### Acceptance Criteria

1. THE acceptance test SHALL include a Gherkin feature file at `acceptance-tests/src/test/resources/features/auth/login.feature` tagged with `@story:KSE-47` and containing Scenario Outlines with Examples tables for both successful and failed login flows.
2. WHEN the successful-login scenario is executed, THE test SHALL drive the Login_Page via a `LoginPage` page object (in `drivers/ui/pages/`), submit the seeded user's email and the password `Password1`, and assert that the browser navigates to the authenticated default route.
3. WHEN the failed-login scenario is executed, THE test SHALL submit invalid credentials via the `LoginPage` page object and assert that a visible error message is displayed and the browser URL remains on `/login`.
4. THE step definitions SHALL be placed in `stepdefs/auth/` and SHALL delegate to domain-layer actor/assertion classes — no Playwright or HTTP calls in step definitions directly.
5. THE `LoginPage` page object SHALL use `getByTestId()` as the primary locator strategy for the email field, password field, submit button, and error message container, with `getByRole()` as fallback for interactive elements without a test ID.
6. THE feature file scenarios SHALL use `Scenario Outline` with `Examples` tables even for single data rows, and SHALL NOT inline test data into step text.
7. THE acceptance test SHALL share state within a scenario via the existing `TestWorld` component — no static fields.
8. THE Login_Page component in the Angular frontend SHALL include `data-testid` attributes on the email input, password input, submit button, and error message container to support stable locator contracts for the acceptance tests.
