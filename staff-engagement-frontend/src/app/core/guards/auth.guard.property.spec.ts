import '@angular/compiler';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { describe, it, expect, beforeEach } from 'vitest';
import * as fc from 'fast-check';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { User } from '../models/user.model';

@Component({ standalone: true, template: '' })
class DummyComponent {}

describe('Feature: frontend-login-auth-guard — AuthGuard Property Tests', () => {
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: DummyComponent }]),
      ],
    });
    authService = TestBed.inject(AuthService);
  });

  describe('Property 9: Auth guard redirects unauthenticated users with returnUrl', () => {
    it('for arbitrary route paths with null user, returns UrlTree to /login with truncated returnUrl', () => {
      const pathCharArb = fc.constantFrom(
        ...'abcdefghijklmnopqrstuvwxyz0123456789/-_~.'.split('')
      );
      const pathArb = fc
        .string({ unit: pathCharArb, minLength: 1, maxLength: 4096 })
        .map((s: string) => '/' + s);

      fc.assert(
        fc.property(pathArb, (path: string) => {
          authService.currentUser.set(null);

          const mockRoute = {} as ActivatedRouteSnapshot;
          const mockState = { url: path } as RouterStateSnapshot;

          const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

          expect(result).toBeInstanceOf(UrlTree);

          const urlTree = result as UrlTree;
          expect(urlTree.root.children['primary']?.segments[0]?.path).toBe('login');

          const returnUrl = urlTree.queryParams['returnUrl'];
          expect(returnUrl).toBeDefined();
          expect(returnUrl.length).toBeLessThanOrEqual(2048);
          expect(returnUrl).toBe(path.substring(0, 2048));
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 6.1, 6.4 */
  });

  describe('Property 10: Auth guard allows authenticated users', () => {
    it('for arbitrary non-null User and arbitrary route paths, returns true', () => {
      const userArb = fc.record({
        id: fc.integer({ min: 1 }),
        name: fc.string({ minLength: 1 }),
        email: fc.string({ minLength: 1 }),
      });

      const pathCharArb = fc.constantFrom(
        ...'abcdefghijklmnopqrstuvwxyz0123456789/-_~.'.split('')
      );
      const pathArb = fc
        .string({ unit: pathCharArb, minLength: 1, maxLength: 200 })
        .map((s: string) => '/' + s);

      fc.assert(
        fc.property(userArb, pathArb, (user: User, path: string) => {
          authService.currentUser.set(user);

          const mockRoute = {} as ActivatedRouteSnapshot;
          const mockState = { url: path } as RouterStateSnapshot;

          const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));

          expect(result).toBe(true);
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 6.2 */
  });
});
