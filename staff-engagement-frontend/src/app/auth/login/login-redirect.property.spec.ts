import '@angular/compiler';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, ActivatedRoute } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import * as fc from 'fast-check';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';

describe('Feature: frontend-login-auth-guard — LoginComponent Redirect Property Tests', () => {
  describe('Property 3: Unexpected HTTP errors produce generic message', () => {
    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;
    let httpTesting: HttpTestingController;

    beforeEach(() => {
      TestBed.configureTestingModule({
        imports: [LoginComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          {
            provide: ActivatedRoute,
            useValue: { snapshot: { queryParamMap: { get: () => null } } },
          },
        ],
      });

      const authService = TestBed.inject(AuthService);
      authService.currentUser.set(null);
      httpTesting = TestBed.inject(HttpTestingController);
      const router = TestBed.inject(Router);
      vi.spyOn(router, 'navigateByUrl').mockImplementation(() => Promise.resolve(true));

      fixture = TestBed.createComponent(LoginComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('for arbitrary status codes not in {200, 400, 401}, displays generic message', () => {
      const unexpectedStatusArb = fc.oneof(
        fc.integer({ min: 402, max: 599 }),
        fc.integer({ min: 300, max: 399 })
      );

      fc.assert(
        fc.property(unexpectedStatusArb, (status) => {
          component.errorMessage.set(null);
          component.isLoading.set(false);
          component.loginForm.setValue({ email: 'test@example.com', password: 'password123' });
          component.onSubmit();

          const req = httpTesting.expectOne('/api/auth/login');
          req.flush('Error', { status, statusText: 'Error' });

          expect(component.errorMessage()).toBe('Login failed. Please try again later.');
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 2.3 */
  });

  describe('Property 11: Valid returnUrl navigation preserves full path', () => {
    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;
    let httpTesting: HttpTestingController;
    let router: Router;
    let mockReturnUrl: { value: string | null };

    beforeEach(() => {
      mockReturnUrl = { value: null };

      TestBed.configureTestingModule({
        imports: [LoginComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: {
                  get: (key: string) => (key === 'returnUrl' ? mockReturnUrl.value : null),
                },
              },
            },
          },
        ],
      });

      const authService = TestBed.inject(AuthService);
      authService.currentUser.set(null);
      httpTesting = TestBed.inject(HttpTestingController);
      router = TestBed.inject(Router);
      vi.spyOn(router, 'navigateByUrl').mockImplementation(() => Promise.resolve(true));

      fixture = TestBed.createComponent(LoginComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('for arbitrary valid relative paths with query/fragments, navigation preserves full URL', () => {
      const segmentCharArb = fc.constantFrom(
        ...'abcdefghijklmnopqrstuvwxyz0123456789-_~.'.split('')
      );
      const segmentArb = fc.string({ unit: segmentCharArb, minLength: 1, maxLength: 20 });

      const pathArb = fc
        .array(segmentArb, { minLength: 1, maxLength: 5 })
        .map((segments) => '/' + segments.join('/'));

      const queryArb = fc.oneof(
        fc.constant(''),
        segmentArb.map((key) => `?${key}=value`)
      );

      const fragmentArb = fc.oneof(
        fc.constant(''),
        segmentArb.map((frag) => `#${frag}`)
      );

      const validReturnUrlArb = fc
        .tuple(pathArb, queryArb, fragmentArb)
        .map(([path, query, fragment]) => `${path}${query}${fragment}`)
        .filter((url) => !url.startsWith('//') && url.length <= 2048);

      fc.assert(
        fc.property(validReturnUrlArb, (returnUrl) => {
          mockReturnUrl.value = returnUrl;
          component.isLoading.set(false);
          component.loginForm.setValue({ email: 'test@example.com', password: 'password123' });
          (router.navigateByUrl as ReturnType<typeof vi.fn>).mockClear();

          component.onSubmit();

          const req = httpTesting.expectOne('/api/auth/login');
          req.flush({ id: 1, name: 'Test User', email: 'test@example.com' });

          expect(router.navigateByUrl).toHaveBeenCalledWith(returnUrl);
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 7.1, 7.5 */
  });

  describe('Property 12: Invalid returnUrl falls back to default route', () => {
    let component: LoginComponent;
    let fixture: ComponentFixture<LoginComponent>;
    let httpTesting: HttpTestingController;
    let router: Router;
    let mockReturnUrl: { value: string | null };

    beforeEach(() => {
      mockReturnUrl = { value: null };

      TestBed.configureTestingModule({
        imports: [LoginComponent],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          provideRouter([]),
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: {
                  get: (key: string) => (key === 'returnUrl' ? mockReturnUrl.value : null),
                },
              },
            },
          },
        ],
      });

      const authService = TestBed.inject(AuthService);
      authService.currentUser.set(null);
      httpTesting = TestBed.inject(HttpTestingController);
      router = TestBed.inject(Router);
      vi.spyOn(router, 'navigateByUrl').mockImplementation(() => Promise.resolve(true));

      fixture = TestBed.createComponent(LoginComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('for arbitrary strings not starting with /, starting with //, or exceeding 2048 chars, navigates to /user', () => {
      const noLeadingSlashArb = fc
        .string({ minLength: 1, maxLength: 200 })
        .filter((s) => !s.startsWith('/'));

      const doubleSlashArb = fc
        .string({ minLength: 1, maxLength: 200 })
        .map((s) => '//' + s);

      const tooLongArb = fc
        .string({ minLength: 2049, maxLength: 2200 })
        .map((s) => '/' + s);

      const invalidReturnUrlArb = fc.oneof(noLeadingSlashArb, doubleSlashArb, tooLongArb);

      fc.assert(
        fc.property(invalidReturnUrlArb, (returnUrl) => {
          mockReturnUrl.value = returnUrl;
          component.isLoading.set(false);
          component.loginForm.setValue({ email: 'test@example.com', password: 'password123' });
          (router.navigateByUrl as ReturnType<typeof vi.fn>).mockClear();

          component.onSubmit();

          const req = httpTesting.expectOne('/api/auth/login');
          req.flush({ id: 1, name: 'Test User', email: 'test@example.com' });

          expect(router.navigateByUrl).toHaveBeenCalledWith('/user');
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 7.3, 7.4 */
  });
});
