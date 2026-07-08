import { HttpRequest } from '@angular/common/http';
import { throwError } from 'rxjs';
import * as fc from 'fast-check';
import { errorInterceptor } from './error.interceptor';

/**
 * Property 3: Error interceptor logs and rethrows for all error status codes
 *
 * For any HTTP response with a status code in the range 400–599, the errorInterceptor
 * SHALL call console.error with a message containing the status code, the request URL,
 * and the error message, then rethrow the original error so the calling code's error
 * handler receives it.
 *
 * Validates: Requirements 4.3
 */
describe('Feature: frontend-scaffold, Property 3: Error interceptor logs and rethrows for all error status codes', () => {
  it('should log and rethrow for all error status codes (400-599)', () => {
    const statusArb = fc.integer({ min: 400, max: 599 });
    const urlArb = fc.webUrl();
    const messageArb = fc.string({ minLength: 1, maxLength: 200 });

    fc.assert(
      fc.property(statusArb, urlArb, messageArb, (status, url, message) => {
        const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

        const req = new HttpRequest('GET', url);
        const errorObj = { status, url, message };
        const next = () => throwError(() => errorObj);

        let caughtError: unknown;
        errorInterceptor(req, next).subscribe({
          error: (err) => {
            caughtError = err;
          },
        });

        // Assert console.error was called with the expected format
        expect(consoleSpy).toHaveBeenCalledOnce();
        expect(consoleSpy).toHaveBeenCalledWith(`HTTP Error ${status} on ${url}: ${message}`);

        // Assert the original error is rethrown
        expect(caughtError).toBe(errorObj);

        consoleSpy.mockRestore();
      }),
      { numRuns: 100 },
    );
  });
});
