# Implementation Plan: Frontend Login & Auth Guard

## Overview

Implement a complete authentication layer for the Angular frontend: User model, signal-based AuthService, HTTP interceptor for session credentials, functional route guard, login page with reactive form validation, route configuration updates, and comprehensive tests (unit, property-based, and Cucumber acceptance). Each task builds incrementally on the previous, ensuring no orphaned code.

## Tasks

- [x] 1. Create User model and AuthService foundation
  - [x] 1.1 Create User model interface and AuthService with signal-based state
    - Create `src/app/core/models/user.model.ts` with `User` interface (id: number, name: string, email: string)
    - Create `src/app/core/services/auth.service.ts` as `@Injectable({ providedIn: 'root' })` with:
      - `currentUser: WritableSignal<User | null>` initialized to `null`
      - `isAuthenticated: Signal<boolean>` as `computed(() => this.currentUser() !== null)`
      - `login(email: string, password: string): Observable<User>` — POST to `/api/auth/login`, update signal on success, propagate errors without modifying signal
      - `logout(): void` — POST to `/api/auth/logout` with 10s timeout, set signal to null, navigate to `/login`; skip HTTP if already null; always navigate on error
    - Inject `HttpClient` and `Router`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [x] 1.2 Write unit tests for AuthService
    - Create `src/app/core/services/auth.service.spec.ts`
    - Test: initializes currentUser to null
    - Test: sends POST to /api/auth/login with correct JSON body
    - Test: updates currentUser signal on HTTP 200
    - Test: propagates errors without modifying signal (401, 400, network)
    - Test: isAuthenticated computed correctly from currentUser
    - Test: logout sends POST to /api/auth/logout
    - Test: logout sets currentUser to null and navigates to /login
    - Test: logout handles errors gracefully (still clears signal, still navigates)
    - Test: logout skips HTTP when currentUser is already null
    - Test: logout times out after 10 seconds
    - _Requirements: 3.1–3.7, 4.1–4.6_

  - [x] 1.3 Write property tests for AuthService
    - Create `src/app/core/services/auth.service.property.spec.ts` using fast-check
    - **Property 4: Login request body correctness** — for arbitrary email/password strings, verify POST body matches
    - **Property 5: Successful login updates currentUser signal** — for arbitrary User objects, verify signal updated
    - **Property 6: Error responses preserve signal state** — for arbitrary error statuses, verify signal unchanged
    - **Property 7: isAuthenticated is derived from currentUser** — for arbitrary User|null, verify computed
    - _Requirements: 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 2. Implement AuthInterceptor
  - [x] 2.1 Create AuthInterceptor as functional HttpInterceptorFn
    - Create `src/app/core/interceptors/auth.interceptor.ts`
    - Implement as `HttpInterceptorFn` that clones request with `withCredentials: true`
    - Follow same pattern as existing `baseUrlInterceptor` and `errorInterceptor`
    - Update `src/app/app.config.ts` to insert `authInterceptor` between `baseUrlInterceptor` and `errorInterceptor` in `withInterceptors` array
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 2.2 Write unit tests for AuthInterceptor
    - Create `src/app/core/interceptors/auth.interceptor.spec.ts`
    - Test: outgoing requests cloned with withCredentials set to true
    - Test: all other request properties (method, url, headers, body) unchanged
    - _Requirements: 5.1_

  - [x] 2.3 Write property test for AuthInterceptor
    - Create `src/app/core/interceptors/auth.interceptor.property.spec.ts` using fast-check
    - **Property 8: Auth interceptor adds withCredentials without mutating other properties** — for arbitrary HTTP requests, verify clone has withCredentials true and all other properties unchanged
    - **Validates: Requirements 5.1**

- [x] 3. Implement AuthGuard
  - [x] 3.1 Create AuthGuard as functional CanActivateFn
    - Create `src/app/core/guards/auth.guard.ts`
    - Implement as `CanActivateFn` using `inject(AuthService)` and `inject(Router)`
    - If `isAuthenticated()` is true, return `true`
    - If not authenticated, return `router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url.substring(0, 2048) } })`
    - _Requirements: 6.1, 6.2, 6.4_

  - [x] 3.2 Write unit tests for AuthGuard
    - Create `src/app/core/guards/auth.guard.spec.ts`
    - Test: returns true when currentUser is non-null
    - Test: returns UrlTree to /login when currentUser is null
    - Test: includes returnUrl query parameter with original path
    - Test: truncates returnUrl to 2048 characters
    - _Requirements: 6.1, 6.2, 6.4_

  - [x] 3.3 Write property tests for AuthGuard
    - Create `src/app/core/guards/auth.guard.property.spec.ts` using fast-check
    - **Property 9: Auth guard redirects unauthenticated users with returnUrl** — for arbitrary route paths with null user, verify UrlTree to /login with truncated returnUrl
    - **Property 10: Auth guard allows authenticated users** — for arbitrary non-null User and arbitrary route paths, verify returns true
    - **Validates: Requirements 6.1, 6.2, 6.4**

- [x] 4. Checkpoint - Core services verified
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement LoginComponent
  - [x] 5.1 Create LoginComponent with reactive form and template
    - Create directory `src/app/auth/login/`
    - Create `src/app/auth/login/login.component.ts` as standalone component with:
      - Reactive form: email (Validators.required, Validators.email), password (Validators.required, Validators.minLength(6), Validators.maxLength(128))
      - Signals: `errorMessage: WritableSignal<string | null>`, `isLoading: WritableSignal<boolean>`
      - Submit handler: validate form, set loading, call AuthService.login(), handle success/error
      - Error mapping: 401 → "Invalid email or password", 400 → "Required fields are missing or malformed", other → "Login failed. Please try again later."
      - Clear error on field valueChanges subscription
      - ReturnUrl validation: must start with `/`, must not start with `//`, max 2048 chars; invalid → navigate to `/user`
      - Preserve full returnUrl path including query params and fragments
      - OnInit: redirect authenticated users to `/user`
    - Create `src/app/auth/login/login.component.html` with:
      - `data-testid="login-email-input"` on email input
      - `data-testid="login-password-input"` on password input
      - `data-testid="login-submit-button"` on submit button
      - `data-testid="login-error-message"` on error container
      - Validation messages beneath fields when touched and invalid
      - Submit button disabled when form invalid or loading
      - Loading indicator visible during request
    - Create `src/app/auth/login/login.component.css` with basic login page styling
    - _Requirements: 1.1–1.11, 2.1–2.5, 7.1–7.5, 8.4, 10.8_

  - [x] 5.2 Write unit tests for LoginComponent
    - Create `src/app/auth/login/login.component.spec.ts`
    - Test: form renders with email and password controls
    - Test: submit button disabled when form invalid
    - Test: submit button disabled while loading
    - Test: validation error messages displayed for touched invalid fields
    - Test: calls AuthService.login() on valid form submission
    - Test: shows loading indicator during login request
    - Test: displays "Invalid email or password" for 401
    - Test: displays "Required fields are missing or malformed" for 400
    - Test: displays "Login failed. Please try again later." for other errors
    - Test: clears error when user modifies fields
    - Test: preserves email value on error
    - Test: re-enables submit button on error
    - Test: navigates to returnUrl on success
    - Test: navigates to /user when no returnUrl
    - Test: discards invalid returnUrl (no leading /, starts with //, exceeds 2048 chars)
    - Test: redirects authenticated user to /user on init
    - _Requirements: 1.1–1.11, 2.1–2.5, 7.1–7.5, 8.4_

  - [x] 5.3 Write property tests for login form validation
    - Create `src/app/auth/login/login-form.property.spec.ts` using fast-check
    - **Property 1: Email validation rejects invalid formats** — for arbitrary strings without valid email format, verify form control marked invalid
    - **Property 2: Password length validation** — for arbitrary strings with length <6 or >128, verify invalid; for length 6–128, verify valid
    - **Validates: Requirements 1.2, 1.3, 1.4, 1.5**

  - [x] 5.4 Write property tests for login redirect logic
    - Create `src/app/auth/login/login-redirect.property.spec.ts` using fast-check
    - **Property 3: Unexpected HTTP errors produce generic message** — for arbitrary status codes not in {200, 400, 401}, verify generic message displayed
    - **Property 11: Valid returnUrl navigation preserves full path** — for arbitrary valid relative paths with query/fragments, verify navigation preserves full URL
    - **Property 12: Invalid returnUrl falls back to default route** — for arbitrary strings not starting with `/`, starting with `//`, or exceeding 2048 chars, verify navigation to `/user`
    - **Validates: Requirements 2.3, 7.1, 7.3, 7.4, 7.5**

- [x] 6. Update route configuration
  - [x] 6.1 Update app.routes.ts and wire AuthGuard to all protected routes
    - Add `/login` route with `loadComponent` pointing to `LoginComponent` (no guard)
    - Add `canActivate: [authGuard]` to all existing routes (user, employee, client, interaction, task)
    - Ensure `/login` is listed before the default redirect
    - Keep wildcard route (`**`) pointing to `user` (guarded by the user route's guard)
    - _Requirements: 6.3, 8.1, 8.2, 8.3, 8.5_

- [x] 7. Checkpoint - Frontend implementation complete
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Cucumber acceptance tests
  - [x] 8.1 Create LoginPage page object
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/drivers/ui/pages/LoginPage.java`
    - Extend `BasePage`, annotate `@Component @ScenarioScope`
    - Use `getByTestId()` locators: `login-email-input`, `login-password-input`, `login-submit-button`, `login-error-message`
    - Methods: `open()`, `fillEmail(String)`, `fillPassword(String)`, `submit()`, `getErrorMessage()`, `isOnLoginPage()`, `getCurrentUrl()`
    - _Requirements: 10.5, 10.8_

  - [x] 8.2 Create LoginActor and LoginAssertions domain layer
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/auth/LoginActor.java`
      - `@Component @ScenarioScope`, inject `LoginPage` and `TestWorld`
      - Methods: `loginAs(String email, String password)`, `navigateToLogin()`
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/domain/auth/LoginAssertions.java`
      - `@Component @ScenarioScope`, inject `LoginPage` and `TestWorld`
      - Methods: `assertRedirectedToHome()`, `assertOnLoginPage()`, `assertErrorMessageVisible(String)`
    - _Requirements: 10.4, 10.7_

  - [x] 8.3 Create login.feature Gherkin file and step definitions
    - Create `acceptance-tests/src/test/resources/features/auth/login.feature`
      - Tag `@story:KSE-47`
      - Scenario Outline for successful login with Examples table
      - Scenario Outline for failed login with Examples table
    - Create `acceptance-tests/src/test/java/com/psybergate/acceptance/stepdefs/auth/LoginStepDefinitions.java`
      - Inject `LoginActor`, `LoginAssertions`, `TestWorld`
      - Delegate all actions to domain layer (no Playwright/HTTP calls)
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.6, 10.7_

- [x] 9. Final checkpoint - All tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document (12 properties across 5 test files)
- Unit tests validate specific examples and edge cases
- Cucumber acceptance tests validate end-to-end flows through real UI against running backend
- The `data-testid` attributes are created in task 5.1 and consumed by the page object in task 8.1
- AuthInterceptor is registered in app.config.ts as part of task 2.1 (between baseUrl and error interceptors)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2", "1.3", "2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "3.1"] },
    { "id": 3, "tasks": ["3.2", "3.3", "5.1"] },
    { "id": 4, "tasks": ["5.2", "5.3", "5.4", "6.1"] },
    { "id": 5, "tasks": ["8.1"] },
    { "id": 6, "tasks": ["8.2"] },
    { "id": 7, "tasks": ["8.3"] }
  ]
}
```
