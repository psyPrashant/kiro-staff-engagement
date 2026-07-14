import '@angular/compiler';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { authGuard } from '../guards/auth.guard';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

@Component({ standalone: true, template: '' })
class DummyComponent {}

/**
 * Bug Condition Exploration Test — KSE-61 "[FE] Fix: page refresh logs the user out"
 *
 * Property 1: Expected Behavior - Reload With Valid Session Restores Authentication
 *
 * These are the SAME tests written in task 1 to demonstrate the bug on unfixed code
 * (where they passed, asserting ZERO calls to `GET /api/auth/me` and a `UrlTree`
 * redirect to `/login` despite a backend session that would have resolved a valid
 * user). Task 3.2/3.3 added `AuthService.rehydrate()` and wired it into app
 * bootstrap via `provideAppInitializer`. Re-running these tests now confirms the
 * fix: calling `rehydrate()` (simulating the app initializer that runs during
 * bootstrap, before `Auth_Guard` evaluates the initial route) DOES call
 * `GET /api/auth/me`, and once the stubbed backend resolves a valid user,
 * `Auth_Guard` returns `true` (no redirect) for the protected path instead of a
 * `UrlTree` to `/login`.
 *
 * Validates: Requirements 2.1, 2.2
 */
describe('Bug Condition Exploration: Reload With Valid Session Restores Authentication (KSE-61)', () => {
  let httpTesting: HttpTestingController;
  let authService: AuthService;
  let httpGetSpy: ReturnType<typeof vi.spyOn>;

  // A backend session that resolves to this user when GET /api/auth/me is called.
  const stubbedValidUser: User = {
    id: 1,
    name: 'Alice Johnson',
    email: 'alice.johnson@psybergate.com',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: DummyComponent }]),
      ],
    });

    httpTesting = TestBed.inject(HttpTestingController);

    // Fresh AuthService construction simulates a page reload: currentUser starts at
    // its declared initial value of null, until rehydration restores it.
    authService = TestBed.inject(AuthService);
    httpGetSpy = vi.spyOn(TestBed.inject(HttpClient), 'get');
  });

  afterEach(() => {
    // Any outstanding/unmatched request here would mean a call was made that this
    // test didn't account for.
    httpTesting.verify();
  });

  it('FIXED: calling rehydrate() during bootstrap makes a call to GET /api/auth/me', () => {
    // Post-reload state, prior to rehydration: currentUser is at its fresh initial value.
    expect(authService.currentUser()).toBeNull();

    // Simulates the app initializer invoking rehydrate() during bootstrap, before
    // Auth_Guard evaluates the initial route.
    authService.rehydrate().subscribe();

    const req = httpTesting.expectOne('/api/auth/me');
    expect(req.request.method).toBe('GET');
    req.flush(stubbedValidUser);

    // FIX CONFIRMED: the rehydration call is made via HttpClient.get.
    const meCalls = httpGetSpy.mock.calls.filter(([url]) => url === '/api/auth/me');
    expect(meCalls).toHaveLength(1);
  });

  it('FIXED: Auth_Guard returns true (no redirect) once rehydrate() resolves a valid backend session', () => {
    // Post-reload state, prior to rehydration: currentUser is at its fresh initial value.
    expect(authService.currentUser()).toBeNull();

    // Simulates the app initializer completing rehydration before the router
    // evaluates the initial navigation.
    authService.rehydrate().subscribe();
    httpTesting.expectOne('/api/auth/me').flush(stubbedValidUser);

    expect(authService.currentUser()).toEqual(stubbedValidUser);

    const mockRoute = {} as ActivatedRouteSnapshot;
    const mockState = { url: '/dashboard' } as RouterStateSnapshot;

    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

    // FIX CONFIRMED: now that currentUser has been restored from the backend
    // session, the guard permits the initial navigation instead of redirecting.
    expect(result).toBe(true);
    expect(result).not.toBeInstanceOf(UrlTree);
  });
});
