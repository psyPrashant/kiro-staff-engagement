import '@angular/compiler';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import * as fc from 'fast-check';
import { authGuard } from '../guards/auth.guard';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

@Component({ standalone: true, template: '' })
class DummyComponent {}

/**
 * Preservation Property Test — KSE-61 "[FE] Fix: page refresh logs the user out"
 *
 * Property 2: Preservation - Non-Bootstrap and Invalid-Session Behavior Is Unchanged
 *
 * IMPORTANT: This file is written and run BEFORE the rehydration fix (task 3) is
 * implemented. It captures observed baseline behavior of `logout()`, the
 * unauthenticated `Auth_Guard` redirect, and `login()` on UNFIXED code. These tests
 * are expected to PASS now (that is the success outcome for this task) and MUST be
 * re-run WITHOUT MODIFICATION after the fix (task 3.5) to confirm no regressions.
 *
 * The fourth preservation observation — "any other authenticated /api/** endpoint
 * still returns 401 via unauthorizedEntryPoint" — is already covered by the existing
 * backend `SecurityFilterChainIntegrationTest.unauthenticatedAccessToProtectedEndpointReturns401WithJson`
 * test and is intentionally not duplicated here.
 *
 * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6
 */
describe('Feature: fix-refresh-logout (KSE-61) — Preservation Property Tests', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;
  let router: Router;

  const testUser: User = { id: 1, name: 'Alice Johnson', email: 'alice.johnson@psybergate.com' };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: DummyComponent }]),
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockImplementation(() => Promise.resolve(true));
  });

  afterEach(() => {
    httpTesting.verify();
  });

  describe('Property 2a: logout() clears currentUser and navigates to /login regardless of backend response', () => {
    it('for arbitrary backend logout responses (200, error status, timeout), currentUser becomes null and navigation to /login occurs', () => {
      const responseArb = fc.oneof(
        fc.constant({ kind: 'success' as const }),
        fc.record({ kind: fc.constant('error' as const), status: fc.integer({ min: 400, max: 599 }) }),
        fc.constant({ kind: 'timeout' as const }),
      );

      vi.useFakeTimers();
      try {
        fc.assert(
          fc.property(responseArb, (response) => {
            (router.navigateByUrl as ReturnType<typeof vi.fn>).mockClear();
            service.currentUser.set(testUser);

            service.logout();

            const req = httpTesting.expectOne('/api/auth/logout');

            if (response.kind === 'success') {
              req.flush({});
            } else if (response.kind === 'error') {
              req.flush({}, { status: response.status, statusText: 'Error' });
            } else {
              vi.advanceTimersByTime(10000);
            }

            expect(service.currentUser()).toBeNull();
            expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
          }),
          { numRuns: 50 },
        );
      } finally {
        vi.useRealTimers();
      }
    });

    /** Validates: Requirements 3.1 */
  });

  describe('Property 2b: Auth_Guard redirects to /login with returnUrl for arbitrary protected paths when currentUser is null', () => {
    it('for arbitrary protected route paths, returns a UrlTree to /login with returnUrl truncated to 2048 chars', () => {
      const pathCharArb = fc.constantFrom(
        ...'abcdefghijklmnopqrstuvwxyz0123456789/-_~.'.split(''),
      );
      const pathArb = fc
        .string({ unit: pathCharArb, minLength: 1, maxLength: 4096 })
        .map((s: string) => '/' + s);

      fc.assert(
        fc.property(pathArb, (path: string) => {
          service.currentUser.set(null);

          const mockRoute = {} as ActivatedRouteSnapshot;
          const mockState = { url: path } as RouterStateSnapshot;

          const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

          expect(result).toBeInstanceOf(UrlTree);
          const urlTree = result as UrlTree;
          expect(urlTree.root.children['primary']?.segments[0]?.path).toBe('login');

          const returnUrl = urlTree.queryParams['returnUrl'];
          expect(returnUrl).toBe(path.substring(0, 2048));
          expect(returnUrl.length).toBeLessThanOrEqual(2048);
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 3.2, 3.3 */
  });

  describe('Property 2c: login() POSTs to /api/auth/login and updates/propagates errors on the signal, unaffected by rehydration', () => {
    it('for arbitrary email/password pairs and HTTP 200 payloads, currentUser is set to the returned user', () => {
      const userArb = fc.record({
        id: fc.integer({ min: 1 }),
        name: fc.string({ minLength: 1 }),
        email: fc.string({ minLength: 1 }),
      });

      fc.assert(
        fc.property(fc.string(), fc.string(), userArb, (email, password, user: User) => {
          service.currentUser.set(null);

          service.login(email, password).subscribe();

          const req = httpTesting.expectOne('/api/auth/login');
          expect(req.request.method).toBe('POST');
          expect(req.request.body).toEqual({ email, password });

          req.flush(user);

          expect(service.currentUser()).toEqual(user);
        }),
        { numRuns: 100 },
      );
    });

    it('for arbitrary email/password pairs and arbitrary error statuses, currentUser stays null and the error is propagated', () => {
      const errorStatusArb = fc.integer({ min: 400, max: 599 });

      fc.assert(
        fc.property(fc.string(), fc.string(), errorStatusArb, (email, password, status) => {
          service.currentUser.set(null);
          let errorReceived = false;

          service.login(email, password).subscribe({
            error: () => {
              errorReceived = true;
            },
          });

          const req = httpTesting.expectOne('/api/auth/login');
          req.flush({ message: 'Error' }, { status, statusText: 'Error' });

          expect(service.currentUser()).toBeNull();
          expect(errorReceived).toBe(true);
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 3.5 */
  });
});
