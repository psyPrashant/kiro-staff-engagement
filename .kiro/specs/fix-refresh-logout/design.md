# Fix Refresh Logout Bugfix Design

## Overview

On a full page reload, Angular re-bootstraps and `AuthService.currentUser` resets to its initial value of `null`, because the signal has no mechanism to reflect the backend's still-valid `JSESSIONID` session cookie. `authGuard` then redirects the user to `/login`, even though the backend would authenticate them if asked.

The fix adds a new backend endpoint, `GET /api/auth/me`, that resolves the current user from the request's session (reusing the existing `CurrentUserResolver`) and returns 401 when there is no valid session. On the frontend, an `APP_INITIALIZER`-style provider (Angular's `provideAppInitializer`) calls this endpoint once during application bootstrap, populating `currentUser` on success or leaving it `null` on failure (401 or network error), and resolves before the router evaluates the initial route. This is a rehydration-from-server approach, not client-side caching: the frontend never trusts locally stored user data, it always re-asks the backend which session (if any) is currently valid.

## Glossary

Reused from `.kiro/specs/frontend-login-auth-guard/requirements.md`:
- **Auth_Service**: The Angular injectable service responsible for calling the backend login/logout endpoints and maintaining signal-based current-user state.
- **Auth_Interceptor**: The Angular functional HTTP interceptor that attaches `withCredentials: true` to outgoing API requests so that session cookies are included.
- **Auth_Guard**: The Angular functional route guard that checks authentication state and redirects unauthenticated users to the Login_Page.
- **Current_User_Signal**: A writable signal within the Auth_Service that holds the authenticated user object (id, name, email) or `null` when no user is authenticated.
- **Protected_Route**: Any route in the application that requires authentication, enforced by the Auth_Guard.
- **Backend_Auth_API**: The backend REST API providing authentication endpoints under `/api/auth`.

New terms for this bugfix:
- **Bug_Condition (C)**: The condition that triggers the bug — a full page reload (Angular re-bootstrap) occurs while a valid backend session cookie exists but `Current_User_Signal` has not yet been restored from it.
- **Rehydration**: The process of restoring `Current_User_Signal` from the backend's session state (via `GET /api/auth/me`) at application startup, as opposed to reading any client-persisted copy of the user.
- **Rehydrate_Endpoint**: The new backend endpoint `GET /api/auth/me` in `AuthController`, which returns the current authenticated user (reusing `CurrentUserResolver.resolve()`) or a `401` when there is no valid session.
- **App_Initializer**: The Angular bootstrap-time provider (registered via `provideAppInitializer` in `app.config.ts`) that invokes the Rehydration call and blocks the initial route evaluation until it settles.

## Bug Details

### Bug Condition

The bug manifests whenever the Angular application (re-)bootstraps — i.e., on a full page reload, direct URL navigation, or bookmark open — while a valid `JSESSIONID` session cookie already exists for the user. Because `AuthService.currentUser` is declared as `signal(null)` with no restoration logic, and because `Auth_Guard` is evaluated synchronously against that signal during the very first navigation, the guard always sees `currentUser() === null` immediately after bootstrap, regardless of whether the backend session is actually still valid.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type AppBootstrapEvent
  OUTPUT: boolean

  RETURN input.isFullPageReload = true
         AND input.backendSessionValid = true
         AND input.currentUserSignalRestoredBeforeGuardEvaluation = false
END FUNCTION
```

Where:
- `input.isFullPageReload`: true when the browser performs a fresh load of `index.html` (reload, direct navigation, bookmark), as opposed to an in-SPA `Router` navigation.
- `input.backendSessionValid`: true when the request's `JSESSIONID` cookie corresponds to an active, unexpired Spring Security session.
- `input.currentUserSignalRestoredBeforeGuardEvaluation`: whether `Current_User_Signal` has been populated from the backend prior to `Auth_Guard` running for the first route.

### Examples

- **Reload on a protected route while logged in**: User logs in, navigates to `/task`, presses browser refresh. Expected: user stays on `/task`, still authenticated. Actual (unfixed): `currentUser` resets to `null` on bootstrap, `Auth_Guard` redirects to `/login?returnUrl=%2Ftask`.
- **Direct URL navigation while logged in**: User with a valid session pastes `/employee/42` into the address bar. Expected: page loads directly. Actual (unfixed): redirected to `/login`.
- **Reload after logout**: User logs out (session cookie cleared via `deleteCookies("JSESSIONID")` and `invalidateHttpSession`), then reloads. Expected (unchanged): redirected to `/login`, since there is no valid session for the backend to resolve. This case does NOT satisfy `isBugCondition` (`backendSessionValid = false`) and must continue to behave this way after the fix.
- **Reload with an expired/invalid session cookie**: Cookie is present but the server-side session has expired. Expected (unchanged): treated as unauthenticated, redirected to `/login`. This does NOT satisfy `isBugCondition` either.
- **In-SPA navigation between protected routes (no reload)**: User clicks a nav link from `/dashboard` to `/task`. Expected (unchanged): `Auth_Guard` uses the already-populated `Current_User_Signal` with no new network call. Does NOT satisfy `isBugCondition` (`isFullPageReload = false`).

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- `logout()` continues to clear `Current_User_Signal` and navigate to `/login`, regardless of the backend response.
- Users with no session cookie, or an expired/invalid one, continue to be redirected to `/login` on both initial load and subsequent guarded navigation.
- In-SPA route navigation (no full reload) continues to rely solely on the existing in-memory `Current_User_Signal` — no additional network calls are introduced per navigation.
- `login()` continues to authenticate via `POST /api/auth/login` and populate `Current_User_Signal` exactly as before.
- `Auth_Interceptor` continues to attach `withCredentials: true` to every outgoing request, now including the new rehydration request, with no change to its own logic.
- All other authenticated `/api/**` endpoints continue to require authentication and return 401 via the existing `unauthorizedEntryPoint` when there is no valid session.

**Scope:**
All inputs that do NOT involve a full application bootstrap with a valid-but-unrestored backend session are unaffected by this fix. This includes:
- Logout flows (any session state)
- Guard evaluation during in-SPA navigation after the app has already bootstrapped
- Login flows and their existing error handling
- Requests to any other `/api/**` endpoint

## Hypothesized Root Cause

1. **No Rehydration Mechanism**: `AuthService.currentUser` is initialized with `signal(null)` and is only ever written to by `login()` (on success) and `logout()` (always, to `null`). There is no code path that reads backend session state at startup and writes it into the signal.

2. **No Backend Endpoint to Query Current Session**: The backend has `CurrentUserResolver.resolve()`, which is already used internally to resolve the authenticated user from the security context, but there is no exposed `AuthController` endpoint that surfaces this to the frontend outside of the login response body. `GET /api/auth/me` does not exist yet.

3. **Guard Evaluates Before Any Async Restoration Could Complete**: Even if a rehydration call existed but were fired reactively inside a component or the guard itself, `Auth_Guard` runs synchronously against the current signal value; without gating app/router bootstrap on the rehydration call, the guard could still see a stale `null` value on the very first navigation due to a race between the HTTP response and route evaluation.

## Correctness Properties

Property 1: Bug Condition - Reload With Valid Session Restores Authentication

_For any_ application bootstrap event where the bug condition holds (`isBugCondition` returns true — a full page reload occurs while a valid backend session cookie exists and `Current_User_Signal` has not yet been restored), the fixed application SHALL call `GET /api/auth/me`, populate `Current_User_Signal` with the resolved user before `Auth_Guard` evaluates the initial route, and SHALL NOT redirect the user to `/login` for that initial navigation to a Protected_Route they were already permitted to access.

**Validates: Requirements 2.1, 2.2**

Property 2: Preservation - Non-Bootstrap and Invalid-Session Behavior Is Unchanged

_For any_ input where the bug condition does NOT hold — i.e., (a) any full page reload where the backend session is invalid, expired, or absent, (b) any in-SPA navigation with no full reload, (c) any logout, or (d) any login attempt — the fixed application SHALL produce the same observable behavior as the original application: unauthenticated reloads/direct-navigations SHALL still redirect to `/login`, in-SPA navigation SHALL still rely purely on the existing in-memory signal with no extra network calls, `logout()` SHALL still clear the signal and navigate to `/login`, and `login()` SHALL still behave exactly as before.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `staff-engagement-backend/src/main/java/com/psybergate/staff_engagement/auth/AuthController.java`

**Function**: new `me()` handler

**Specific Changes**:
1. **Add `GET /api/auth/me` endpoint**: Add a `@GetMapping("/me")` handler that calls `currentUserResolver.resolve()` and maps the result to a response body (id, name, email) — reusing `LoginResponse(Long id, String name, String email)` is sufficient since the shape is identical; a dedicated `CurrentUserResponse` is not required unless naming clarity is preferred.
2. **Inject `CurrentUserResolver`**: Add it as a constructor dependency (Lombok `@RequiredArgsConstructor` already generates this) alongside the existing `AuthService`.
3. **Rely on existing 401 handling**: `CurrentUserResolver.resolve()` already throws `ResponseStatusException(HttpStatus.UNAUTHORIZED)` when there is no authenticated principal or the user cannot be found; no new exception handling is needed.

**File**: `staff-engagement-backend/src/main/java/com/psybergate/staff_engagement/auth/SecurityConfig.java`

**Specific Changes**:
4. **Verify authorization rule coverage**: `.requestMatchers("/api/**").authenticated()` already covers `GET /api/auth/me` since only `POST /api/auth/login` and `POST /api/auth/logout` are explicitly permitted — confirm via a test rather than adding a redundant explicit matcher, unless the test reveals a gap (e.g., method-specific matcher ordering), in which case add an explicit `.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()` for clarity.

**File**: `staff-engagement-frontend/src/app/core/services/auth.service.ts`

**Function**: new `rehydrate()` method

**Specific Changes**:
5. **Add a `rehydrate()` method**: Calls `GET /api/auth/me` and returns an `Observable<User | null>`. On success (HTTP 200), sets `currentUser` to the returned user and emits that user. On error (401 or any other failure, including network errors), sets `currentUser` to `null` (it should already be `null`, but this is explicit/defensive) and emits `null` rather than propagating an error — this method must never throw, so app bootstrap can proceed regardless of session state.

**File**: `staff-engagement-frontend/src/app/app.config.ts`

**Specific Changes**:
6. **Register an app initializer**: Use Angular's `provideAppInitializer` to inject `AuthService`, call `rehydrate()`, and convert it to a promise that always resolves (never rejects) so bootstrap is never blocked indefinitely — apply a bounded timeout so a hung network call cannot stall the app forever (mirroring the existing 10s timeout pattern used in `logout()`), treating a timeout the same as a 401 (i.e., not authenticated).
7. **Ordering**: The initializer must run before the router's initial navigation resolves. `provideAppInitializer` (or `APP_INITIALIZER`) runs before the app becomes stable / before the initial navigation, satisfying this without needing an `APP_BOOTSTRAP_LISTENER` or router resolver-based approach.

**No changes required** to `AuthGuard`, `AuthInterceptor` (already registered globally, will automatically cover the new `/api/auth/me` request), `LoginResponse`, or `logout()`.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, exploratory tests demonstrate the bug on the current (unfixed) code — reload with a valid session incorrectly bounces to `/login` — then, after implementing the fix, the same exploratory test is expected to pass, and separate preservation tests (written by observing the unfixed code for non-buggy inputs) confirm nothing else changed.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix, confirming the root cause (no rehydration mechanism exists).

**Test Plan**: Write a Vitest test that constructs the router bootstrap sequence (or directly invokes `Auth_Guard` immediately after fresh `AuthService` construction, simulating a page reload) while an `HttpTestingController` stub is configured to represent a backend that WOULD return a valid user for `GET /api/auth/me` if asked. On unfixed code, no such call exists, so `currentUser()` remains `null` at guard-evaluation time and the guard redirects to `/login`. Also write a backend integration test asserting `GET /api/auth/me` returns 404 (route not mapped) prior to the fix, confirming the endpoint itself doesn't exist yet.

**Test Cases**:
1. **Guard Redirects Despite Valid Session Test**: Construct a fresh `AuthService`/`Auth_Guard` (simulating post-reload state) with `currentUser` at its initial `null` value; invoke the guard for a protected path; assert it returns a `UrlTree` to `/login` (will "pass" on unfixed code today — this documents the current defect, not a test that fails, since the redirect IS the observed defect once a session is assumed valid).
2. **No Rehydration Call Made Test**: Spy on `HttpClient` calls made during `AuthService` construction / app bootstrap; assert that unfixed code makes zero requests to `/api/auth/me` (confirms the missing mechanism).
3. **Backend Endpoint Missing Test**: Backend integration test hitting `GET /api/auth/me` with a valid authenticated session; on unfixed code this returns 404 (no such mapped route), confirming the endpoint doesn't exist yet.

**Expected Counterexamples**:
- No HTTP call to `/api/auth/me` occurs during app bootstrap on unfixed code.
- `GET /api/auth/me` returns 404 on the unfixed backend (route not yet mapped).
- Because the guard is purely synchronous and there is no restoration path, a reload with a genuinely valid backend session is indistinguishable, from the frontend's perspective, from a reload with no session at all — both result in a redirect to `/login`.

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed application produces the expected behavior (Property 1).

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := appBootstrap_fixed(input)
  ASSERT result.currentUserSignal = resolvedUserFromBackend(input)
  ASSERT result.initialNavigation.redirectedToLogin = false
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed application produces the same result as the original application (Property 2).

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT appBootstrap_original(input) = appBootstrap_fixed(input)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many combinations of (session validity, reload vs. in-SPA navigation, requested path) automatically.
- It catches edge cases such as network timeouts vs. explicit 401s that manual unit tests might miss.
- It provides strong guarantees that logout, login, and guard behavior for already-unauthenticated users are unchanged for all such inputs.

**Test Plan**: Observe behavior on UNFIXED code first for: (a) logout with an active session, (b) logout with no session, (c) guard evaluation with `currentUser === null` on an in-SPA navigation, (d) login with valid/invalid credentials. Then write property-based tests (fast-check) capturing that observed behavior, and re-run them after the fix to confirm no regression.

**Test Cases**:
1. **Logout Preservation**: Observe on unfixed code that `logout()` always sets `currentUser` to `null` and navigates to `/login` regardless of backend response; write a property test generating arbitrary backend responses (200, various error codes, timeout) and asserting this holds after the fix too.
2. **Unauthenticated Guard Preservation**: Observe on unfixed code that `Auth_Guard` redirects to `/login` with a `returnUrl` for any protected path when `currentUser` is `null`; write a property test generating arbitrary protected paths and asserting the same UrlTree shape after the fix, for the case where rehydration legitimately resolves to "no session" (401).
3. **Login Preservation**: Observe on unfixed code that `login(email, password)` POSTs to `/api/auth/login` and updates the signal on success / propagates errors on failure; write a property test generating arbitrary email/password pairs and response payloads, asserting identical behavior after the fix.
4. **Backend Other-Endpoint Preservation**: Observe on unfixed backend that any authenticated `/api/**` endpoint returns 401 via `unauthorizedEntryPoint` when unauthenticated; write/keep an integration test confirming this is unchanged after adding `GET /api/auth/me`.

### Unit Tests

- `AuthService.rehydrate()`: returns the user and sets `currentUser` on HTTP 200 from `/api/auth/me`.
- `AuthService.rehydrate()`: sets/leaves `currentUser` as `null` and does not throw on HTTP 401.
- `AuthService.rehydrate()`: sets/leaves `currentUser` as `null` and does not throw on a network error.
- App initializer: resolves (does not reject) regardless of `rehydrate()` outcome, and resolves within the bounded timeout.
- Backend `GET /api/auth/me`: returns 200 with the current user's id/name/email when a valid session exists.
- Backend `GET /api/auth/me`: returns 401 when there is no authenticated session.
- `Auth_Guard`: unchanged behavior tests (still passing, no modification expected) confirming `true` for authenticated, `UrlTree` for unauthenticated.

### Property-Based Tests

- **Property 1 (Bug Condition)**: For arbitrary valid `User` payloads returned by a stubbed `/api/auth/me`, after rehydration completes, `currentUser()` equals that user and a subsequent guard evaluation for an arbitrary protected path returns `true` (no redirect).
- **Property 2 (Preservation)**: For arbitrary combinations of (rehydration outcome ∈ {401, network error}) × (arbitrary protected path), the guard still returns a `UrlTree` to `/login` with the expected `returnUrl`, exactly as it did before the fix when `currentUser` was `null`. Additionally, for arbitrary login/logout call sequences (independent of rehydration), the resulting `currentUser` state and navigation calls match the pre-fix observed behavior.

### Integration Tests

- Backend: `GET /api/auth/me` integration test using the existing `BaseIntegrationTest` / Testcontainers setup, covering authenticated (200) and unauthenticated (401) cases, following the pattern of `AuthLoginIntegrationTest` / `SecurityFilterChainIntegrationTest`.
- Frontend (Vitest): app bootstrap sequence test verifying that `provideAppInitializer` calls `rehydrate()` before the router's first navigation resolves, using `HttpTestingController` to control the `/api/auth/me` response timing.
- End-to-end (Playwright/Cucumber, if pursued): login → reload the browser → assert the user remains on the authenticated route (not redirected to `/login`), extending the existing `acceptance-tests` four-layer harness and `login.feature` conventions from the `frontend-login-auth-guard` spec.
