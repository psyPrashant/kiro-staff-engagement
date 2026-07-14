# Implementation Plan

- [x] 1. Write bug condition exploration tests
  - **Property 1: Bug Condition** - Reload With Valid Session Restores Authentication
  - **CRITICAL**: These tests MUST demonstrate the bug on unfixed code — failure/absence of the rehydration call confirms the bug exists
  - **DO NOT attempt to fix the code when these tests reveal the missing mechanism**
  - **NOTE**: These tests encode the expected behavior — they will validate the fix when re-run after implementation
  - **GOAL**: Surface counterexamples that demonstrate no rehydration mechanism exists, causing valid sessions to be lost on reload
  - **Scoped approach**: Scope to the concrete failing case — a fresh (simulated post-reload) `AuthService`/`Auth_Guard` instance with `currentUser` at its initial `null` value, where a valid backend session would exist if queried
  - Frontend (Vitest): spy/assert that constructing `AuthService` and evaluating `Auth_Guard` immediately (simulating page reload) makes ZERO HTTP calls to `GET /api/auth/me` on unfixed code, and that `Auth_Guard` returns a `UrlTree` to `/login` even though a stubbed backend would resolve a valid user for that request
  - Backend (JUnit/Spring Boot Test): `GET /api/auth/me` with a valid authenticated session returns 404 (route not mapped) on unfixed code
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Frontend test confirms no rehydration call is made; backend test confirms the endpoint returns 404 (this is correct — it proves the bug/missing mechanism exists)
  - Document counterexamples found (e.g., "no request to /api/auth/me is made during AuthService construction"; "GET /api/auth/me returns 404 - not mapped")
  - Mark task complete when tests are written, run, and failure/absence is documented
  - _Requirements: 1.1, 1.2_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Bootstrap and Invalid-Session Behavior Is Unchanged
  - **IMPORTANT**: Follow observation-first methodology
  - Observe on UNFIXED code: `logout()` always sets `currentUser` to `null` and navigates to `/login`, for both a successful backend logout response and various error/timeout responses
  - Observe on UNFIXED code: `Auth_Guard` returns a `UrlTree` to `/login` with `returnUrl` set to the requested path (truncated to 2048 chars) whenever `currentUser` is `null`, for arbitrary protected paths
  - Observe on UNFIXED code: `login(email, password)` POSTs to `/api/auth/login` and updates/propagates errors on the signal exactly as documented in the `frontend-login-auth-guard` spec, unaffected by anything related to rehydration
  - Observe on UNFIXED backend: any other authenticated `/api/**` endpoint still returns 401 via `unauthorizedEntryPoint` when there is no valid session
  - Write property-based tests (fast-check) capturing these observed behaviors:
    - For arbitrary backend logout responses (200, error status, timeout), `currentUser` becomes `null` and navigation to `/login` occurs
    - For arbitrary protected route paths, the unauthenticated guard redirect shape (`UrlTree` to `/login` with `returnUrl`) is preserved
    - For arbitrary email/password pairs and response payloads, `login()` behavior is preserved
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 3. Fix for page refresh logging the user out

  - [x] 3.1 Add backend rehydration endpoint `GET /api/auth/me`
    - Add a `@GetMapping("/me")` handler to `AuthController` that injects `CurrentUserResolver` and calls `resolve()`, mapping the result to `LoginResponse(id, name, email)` (or a dedicated `CurrentUserResponse` if preferred, keeping the same shape)
    - Rely on `CurrentUserResolver.resolve()`'s existing `ResponseStatusException(HttpStatus.UNAUTHORIZED)` for the no-session case — no new exception handling needed
    - Verify (via a test) that `SecurityConfig`'s `.requestMatchers("/api/**").authenticated()` already covers `GET /api/auth/me`; add an explicit `.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()` only if the test reveals a gap
    - _Bug_Condition: isBugCondition(input) where input.backendSessionValid = true AND input.currentUserSignalRestoredBeforeGuardEvaluation = false_
    - _Expected_Behavior: Rehydrate_Endpoint returns the current user (200) when a valid session exists, and 401 otherwise, per Property 1_
    - _Requirements: 2.2_

  - [x] 3.2 Add `AuthService.rehydrate()` on the frontend
    - Add `rehydrate(): Observable<User | null>` to `AuthService` that calls `GET /api/auth/me`
    - On success (200): set `currentUser` to the returned user, emit that user
    - On error (401 or any other failure, including network errors): set `currentUser` to `null`, emit `null` — this method must never throw/propagate an error, so bootstrap is never blocked by a rejected observable
    - _Bug_Condition: isBugCondition(input) from design_
    - _Expected_Behavior: expectedBehavior(result) — currentUser reflects backend session state after rehydrate() resolves_
    - _Preservation: rehydrate() must not alter login()/logout() behavior or signal state for any non-bootstrap call site_
    - _Requirements: 2.2, 3.6_

  - [x] 3.3 Wire rehydration into app bootstrap via `provideAppInitializer`
    - Register a `provideAppInitializer` in `app.config.ts` that injects `AuthService`, calls `rehydrate()`, and resolves the initializer promise regardless of outcome (success, 401, or network error) so app startup is never blocked
    - Apply a bounded timeout (mirroring the existing 10s pattern in `logout()`) so a hung network call cannot stall the app indefinitely; treat a timeout the same as "not authenticated"
    - Ensure this resolves before the router's initial navigation is evaluated, so `Auth_Guard` sees the restored `Current_User_Signal` on the very first navigation
    - _Bug_Condition: isBugCondition(input) from design_
    - _Expected_Behavior: expectedBehavior(result) — Current_User_Signal is populated before Auth_Guard evaluates the initial route_
    - _Requirements: 2.1, 2.2_

  - [x] 3.4 Verify bug condition exploration tests now pass
    - **Property 1: Expected Behavior** - Reload With Valid Session Restores Authentication
    - **IMPORTANT**: Re-run the SAME tests from task 1 — do NOT write new tests
    - Update/confirm the frontend test now observes a call to `GET /api/auth/me` during bootstrap and that `Auth_Guard` returns `true` (no redirect) for the protected path when the stubbed backend resolves a valid user
    - Update/confirm the backend test now receives 200 with the current user for `GET /api/auth/me` when authenticated
    - **EXPECTED OUTCOME**: Tests PASS (confirms bug is fixed)
    - _Requirements: 2.1, 2.2_

  - [x] 3.5 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Bootstrap and Invalid-Session Behavior Is Unchanged
    - **IMPORTANT**: Re-run the SAME tests from task 2 — do NOT write new tests
    - Run preservation property tests from task 2 (logout, unauthenticated guard redirect, login, other-endpoint 401 handling)
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after the fix (no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4. Additional unit and integration coverage
  - [x] 4.1 Backend unit/integration tests for `GET /api/auth/me`
    - Add tests alongside existing `AuthLoginIntegrationTest` / `SecurityFilterChainIntegrationTest` conventions in `staff-engagement-backend/src/test/java/com/psybergate/staff_engagement/auth/`
    - Test: returns 200 with correct id/name/email for an authenticated session
    - Test: returns 401 when there is no authenticated session
    - _Requirements: 2.2, 3.3_

  - [x] 4.2 Frontend unit tests for `AuthService.rehydrate()` and the app initializer
    - Add tests to `auth.service.spec.ts` (or a new `auth.service.rehydrate.spec.ts`) using `HttpTestingController`
    - Test: `rehydrate()` sets `currentUser` and emits the user on HTTP 200
    - Test: `rehydrate()` sets `currentUser` to `null` and emits `null` on HTTP 401 (no thrown/propagated error)
    - Test: `rehydrate()` sets `currentUser` to `null` and emits `null` on a network error (no thrown/propagated error)
    - Test: the app initializer resolves regardless of `rehydrate()` outcome, and resolves within the bounded timeout even if the request hangs
    - _Requirements: 2.2, 3.6_

  - [x] 4.3 *(Optional)* Cucumber/Playwright acceptance test for reload-preserves-session
    - If pursued, extend the existing four-layer harness from the `frontend-login-auth-guard` spec: add a scenario to `login.feature` (or a new `auth` feature file) that logs in, triggers a full page reload via Playwright, and asserts the user remains on the authenticated route
    - _Requirements: 2.1_

- [x] 5. Checkpoint - Ensure all tests pass
  - Run the full frontend test suite (`npx vitest --run`) and backend test suite (`./mvnw test`)
  - Ensure all tests pass, ask the user if questions arise
