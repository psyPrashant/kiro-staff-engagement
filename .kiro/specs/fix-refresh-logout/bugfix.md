# Bugfix Requirements Document

## Introduction

**Jira: KSE-61 — "[FE] Fix: page refresh logs the user out"**

The backend already persists an authenticated session correctly across page reloads via the `JSESSIONID` session cookie (`SessionCreationPolicy.IF_REQUIRED`). The bug is entirely client-side: `AuthService.currentUser` (`staff-engagement-frontend/src/app/core/services/auth.service.ts`) is an in-memory `WritableSignal<User | null>` that only gets populated as a side effect of a successful `POST /api/auth/login` call. It is never restored from the still-valid session cookie when the Angular application re-bootstraps.

As a result, any full page reload (browser refresh, typing the URL, opening a bookmark) resets `currentUser` to `null`. `authGuard` (`staff-engagement-frontend/src/app/core/guards/auth.guard.ts`) reads `isAuthenticated()`, which derives from `currentUser`, and redirects the user to `/login` — even though their session cookie is still valid and the backend would happily authenticate the request. This bounces genuinely logged-in users out of the application on every reload.

This bugfix adds a backend rehydration endpoint (`GET /api/auth/me`) and calls it during Angular app initialization so the `currentUser` signal is restored from the server-side session state *before* the router evaluates the initial route, without ever trusting client-cached data.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN a user with a valid backend session performs a full page reload (or opens a URL directly) on any Protected_Route THEN the system redirects them to `/login`, even though their session cookie is still valid on the backend.

1.2 WHEN the Angular application bootstraps THEN the Current_User_Signal always initializes to `null` with no mechanism to restore it from the existing session cookie, causing Auth_Guard to treat every reload as unauthenticated regardless of actual server-side session state.

### Expected Behavior (Correct)

2.1 WHEN a user with a valid backend session performs a full page reload on any route THEN the system SHALL restore the Current_User_Signal from the backend before Auth_Guard evaluates the initial navigation, so the user remains authenticated and stays on the originally requested route.

2.2 WHEN the Angular application bootstraps THEN the system SHALL call a backend endpoint (`GET /api/auth/me`) to resolve the current user from the session cookie and SHALL populate the Current_User_Signal with the returned user data before the router evaluates the initial route, resolving promptly (treating any error, including a 401 or network failure, as "not authenticated" without blocking app startup indefinitely).

### Unchanged Behavior (Regression Prevention)

3.1 WHEN a user logs out THEN the system SHALL CONTINUE TO clear the Current_User_Signal and navigate to `/login`.

3.2 WHEN a user with no session cookie (never logged in) reloads or navigates to a Protected_Route THEN the system SHALL CONTINUE TO redirect them to `/login`.

3.3 WHEN a user's session cookie is expired, invalid, or absent at app bootstrap (rehydration returns HTTP 401) THEN the system SHALL CONTINUE TO leave the Current_User_Signal as `null` and Auth_Guard SHALL CONTINUE TO redirect Protected_Route navigation to `/login`.

3.4 WHEN a user navigates between routes within the SPA without a full page reload THEN the system SHALL CONTINUE TO use the existing in-memory Current_User_Signal state to determine access, without requiring a new rehydration call per navigation.

3.5 WHEN a user submits valid credentials on the Login_Page THEN the system SHALL CONTINUE TO authenticate via `POST /api/auth/login` and populate the Current_User_Signal exactly as before.

3.6 WHEN any HTTP request is made by the frontend THEN the Auth_Interceptor SHALL CONTINUE TO attach `withCredentials: true` so session cookies are sent, including for the new rehydration request.
