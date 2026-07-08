import { HttpRequest, HttpResponse } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import * as fc from 'fast-check';
import { baseUrlInterceptor } from './base-url.interceptor';

/**
 * Property 2: Absolute URLs pass through unchanged
 * Validates: Requirements 4.2
 *
 * For any absolute URL (starting with http:// or https://), the baseUrlInterceptor
 * SHALL return the request URL identical to the input — no characters added, removed,
 * or modified.
 */
describe('baseUrlInterceptor', () => {
  describe('Feature: frontend-scaffold, Property 2: Absolute URLs pass through unchanged', () => {
    it('should pass absolute URLs through without modification', async () => {
      await fc.assert(
        fc.asyncProperty(
          fc.oneof(
            fc.webUrl({ withFragments: true, withQueryParameters: true }),
            fc
              .record({
                scheme: fc.constantFrom('http', 'https'),
                host: fc.domain(),
                path: fc.webPath(),
                query: fc.option(fc.webQueryParameters(), { nil: undefined }),
                fragment: fc.option(fc.webFragments(), { nil: undefined }),
              })
              .map(({ scheme, host, path, query, fragment }) => {
                let url = `${scheme}://${host}${path}`;
                if (query) url += `?${query}`;
                if (fragment) url += `#${fragment}`;
                return url;
              }),
          ),
          async (absoluteUrl) => {
            const req = new HttpRequest('GET', absoluteUrl);
            let capturedUrl: string | undefined;

            const next = (r: HttpRequest<unknown>) => {
              capturedUrl = r.url;
              return of(new HttpResponse({ status: 200 }));
            };

            await firstValueFrom(baseUrlInterceptor(req, next));

            expect(capturedUrl).toBe(absoluteUrl);
          },
        ),
        { numRuns: 100 },
      );
    });
  });
});
