import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import * as fc from 'fast-check';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

describe('Feature: frontend-login-auth-guard — AuthService Property Tests', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  describe('Property 4: Login request body correctness', () => {
    it('for arbitrary email/password strings, POST body contains exactly those values', () => {
      fc.assert(
        fc.property(fc.string(), fc.string(), (email, password) => {
          service.login(email, password).subscribe();

          const req = httpTesting.expectOne('/api/auth/login');
          expect(req.request.method).toBe('POST');
          expect(req.request.body).toEqual({ email, password });

          req.flush({ id: 1, name: 'Test', email });
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 3.3 */
  });

  describe('Property 5: Successful login updates currentUser signal', () => {
    it('for arbitrary User objects (HTTP 200), currentUser signal holds same values', () => {
      const userArb = fc.record({
        id: fc.integer({ min: 1 }),
        name: fc.string({ minLength: 1 }),
        email: fc.string({ minLength: 1 }),
      });

      fc.assert(
        fc.property(userArb, (user: User) => {
          service.currentUser.set(null);
          service.login('any@test.com', 'password123').subscribe();

          const req = httpTesting.expectOne('/api/auth/login');
          req.flush(user);

          expect(service.currentUser()).toEqual(user);
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 3.4 */
  });

  describe('Property 6: Error responses preserve signal state', () => {
    it('for arbitrary HTTP error statuses, currentUser signal remains null and error is propagated', () => {
      const errorStatusArb = fc.integer({ min: 400, max: 599 });

      fc.assert(
        fc.property(errorStatusArb, (status) => {
          service.currentUser.set(null);
          let errorReceived = false;

          service.login('any@test.com', 'password123').subscribe({
            error: () => {
              errorReceived = true;
            },
          });

          const req = httpTesting.expectOne('/api/auth/login');
          req.flush('Error', { status, statusText: 'Error' });

          expect(service.currentUser()).toBeNull();
          expect(errorReceived).toBe(true);
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 3.5, 3.7 */
  });

  describe('Property 7: isAuthenticated is derived from currentUser', () => {
    it('for arbitrary User|null, isAuthenticated() returns true iff currentUser() is non-null', () => {
      const userOrNullArb = fc.oneof(
        fc.constant(null),
        fc.record({
          id: fc.integer({ min: 1 }),
          name: fc.string({ minLength: 1 }),
          email: fc.string({ minLength: 1 }),
        }),
      );

      fc.assert(
        fc.property(userOrNullArb, (userOrNull: User | null) => {
          service.currentUser.set(userOrNull);

          if (userOrNull !== null) {
            expect(service.isAuthenticated()).toBe(true);
          } else {
            expect(service.isAuthenticated()).toBe(false);
          }
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 3.6 */
  });
});
