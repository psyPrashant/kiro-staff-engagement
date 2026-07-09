import { HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import { firstValueFrom, of } from 'rxjs';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  describe('outgoing requests cloned with withCredentials set to true', () => {
    it('should set withCredentials to true on a GET request', async () => {
      const req = new HttpRequest('GET', '/api/users');
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.withCredentials).toBe(true);
    });

    it('should set withCredentials to true on a POST request with body', async () => {
      const body = { email: 'user@example.com', password: 'secret123' };
      const req = new HttpRequest('POST', '/api/auth/login', body);
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.withCredentials).toBe(true);
    });

    it('should set withCredentials to true on a PUT request', async () => {
      const req = new HttpRequest('PUT', '/api/users/1', { name: 'Updated' });
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.withCredentials).toBe(true);
    });

    it('should set withCredentials to true on a DELETE request', async () => {
      const req = new HttpRequest('DELETE', '/api/users/1');
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.withCredentials).toBe(true);
    });
  });

  describe('all other request properties unchanged', () => {
    it('should preserve the request method', async () => {
      const req = new HttpRequest('PATCH', '/api/resource', { field: 'value' });
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.method).toBe('PATCH');
    });

    it('should preserve the request url', async () => {
      const req = new HttpRequest('GET', '/api/users?page=2&size=10');
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.url).toBe('/api/users?page=2&size=10');
    });

    it('should preserve custom headers', async () => {
      const headers = new HttpHeaders().set('X-Custom-Header', 'custom-value');
      const req = new HttpRequest('GET', '/api/data', { headers });
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.headers.get('X-Custom-Header')).toBe('custom-value');
    });

    it('should preserve the request body', async () => {
      const body = { name: 'Test User', email: 'test@example.com' };
      const req = new HttpRequest('POST', '/api/users', body);
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.body).toEqual(body);
    });

    it('should preserve all properties together on a complex request', async () => {
      const body = { data: [1, 2, 3] };
      const headers = new HttpHeaders()
        .set('Authorization', 'Bearer token123')
        .set('Content-Type', 'application/json');
      const req = new HttpRequest<{ data: number[] }>('POST', '/api/complex/path', body, {
        headers,
      });
      let capturedReq: HttpRequest<unknown> | undefined;

      const next = (r: HttpRequest<unknown>) => {
        capturedReq = r;
        return of(new HttpResponse({ status: 200 }));
      };

      await firstValueFrom(authInterceptor(req, next));

      expect(capturedReq!.withCredentials).toBe(true);
      expect(capturedReq!.method).toBe('POST');
      expect(capturedReq!.url).toBe('/api/complex/path');
      expect(capturedReq!.body).toEqual(body);
      expect(capturedReq!.headers.get('Authorization')).toBe('Bearer token123');
      expect(capturedReq!.headers.get('Content-Type')).toBe('application/json');
    });
  });
});
