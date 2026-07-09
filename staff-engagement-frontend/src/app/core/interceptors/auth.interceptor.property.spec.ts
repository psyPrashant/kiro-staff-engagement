import '@angular/compiler';
import { HttpRequest, HttpResponse } from '@angular/common/http';
import { describe, it, expect } from 'vitest';
import { of } from 'rxjs';
import * as fc from 'fast-check';
import { authInterceptor } from './auth.interceptor';

describe('Feature: frontend-login-auth-guard — AuthInterceptor Property Tests', () => {
  describe('Property 8: Auth interceptor adds withCredentials without mutating other properties', () => {
    it('for arbitrary HTTP requests, clone has withCredentials true and all other properties unchanged', () => {
      const methodArb = fc.constantFrom('GET', 'POST', 'PUT', 'DELETE', 'PATCH');
      const urlArb = fc.webUrl();
      const bodyArb = fc.oneof(
        fc.constant(null),
        fc.string({ minLength: 1 }),
        fc.dictionary(fc.string({ minLength: 1, maxLength: 10 }), fc.string())
      );

      fc.assert(
        fc.property(methodArb, urlArb, bodyArb, (method, url, body) => {
          const req = new HttpRequest(method, url, body);
          let capturedReq: HttpRequest<unknown> | undefined;

          const next = (r: HttpRequest<unknown>) => {
            capturedReq = r;
            return of(new HttpResponse({ status: 200 }));
          };

          authInterceptor(req, next);

          expect(capturedReq).toBeDefined();
          expect(capturedReq!.withCredentials).toBe(true);
          expect(capturedReq!.method).toBe(req.method);
          expect(capturedReq!.url).toBe(req.url);
          expect(capturedReq!.body).toEqual(req.body);
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 5.1 */
  });
});
