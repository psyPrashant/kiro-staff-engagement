import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { User } from '../models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: { navigateByUrl: vi.fn() },
        },
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should initialize currentUser to null', () => {
    expect(service.currentUser()).toBeNull();
  });

  it('should have isAuthenticated as false when currentUser is null', () => {
    expect(service.isAuthenticated()).toBe(false);
  });

  it('should have isAuthenticated as true when currentUser is non-null', () => {
    service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });
    expect(service.isAuthenticated()).toBe(true);
  });

  describe('login', () => {
    it('should send POST to /api/auth/login with correct JSON body', () => {
      const email = 'user@example.com';
      const password = 'secret123';

      service.login(email, password).subscribe();

      const req = httpTesting.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email, password });
      req.flush({ id: 1, name: 'User', email });
    });

    it('should update currentUser signal on HTTP 200', () => {
      const mockUser: User = { id: 42, name: 'Jane Doe', email: 'jane@example.com' };

      service.login('jane@example.com', 'password1').subscribe();

      const req = httpTesting.expectOne('/api/auth/login');
      req.flush(mockUser);

      expect(service.currentUser()).toEqual(mockUser);
      expect(service.isAuthenticated()).toBe(true);
    });

    it('should propagate 401 error without modifying signal', () => {
      let errorResponse: unknown;

      service.login('wrong@example.com', 'bad').subscribe({
        error: (err) => (errorResponse = err),
      });

      const req = httpTesting.expectOne('/api/auth/login');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      expect(service.currentUser()).toBeNull();
      expect(errorResponse).toBeDefined();
    });

    it('should propagate 400 error without modifying signal', () => {
      let errorResponse: unknown;

      service.login('', '').subscribe({
        error: (err) => (errorResponse = err),
      });

      const req = httpTesting.expectOne('/api/auth/login');
      req.flush({ message: 'Bad Request' }, { status: 400, statusText: 'Bad Request' });

      expect(service.currentUser()).toBeNull();
      expect(errorResponse).toBeDefined();
    });

    it('should propagate network error without modifying signal', () => {
      let errorResponse: unknown;

      service.login('user@example.com', 'pass123').subscribe({
        error: (err) => (errorResponse = err),
      });

      const req = httpTesting.expectOne('/api/auth/login');
      req.error(new ProgressEvent('error'));

      expect(service.currentUser()).toBeNull();
      expect(errorResponse).toBeDefined();
    });
  });

  describe('rehydrate', () => {
    it('should send GET to /api/auth/me', () => {
      service.rehydrate().subscribe();

      const req = httpTesting.expectOne('/api/auth/me');
      expect(req.request.method).toBe('GET');
      req.flush({ id: 1, name: 'Test', email: 'test@example.com' });
    });

    it('should set currentUser and emit the user on HTTP 200', () => {
      const mockUser: User = { id: 42, name: 'Jane Doe', email: 'jane@example.com' };
      let emitted: User | null = undefined as unknown as User | null;

      service.rehydrate().subscribe((user) => (emitted = user));

      const req = httpTesting.expectOne('/api/auth/me');
      req.flush(mockUser);

      expect(service.currentUser()).toEqual(mockUser);
      expect(emitted).toEqual(mockUser);
    });

    it('should set currentUser to null and emit null on HTTP 401 without throwing', () => {
      service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });
      let emitted: User | null = undefined as unknown as User | null;
      let errored = false;

      service.rehydrate().subscribe({
        next: (user) => (emitted = user),
        error: () => (errored = true),
      });

      const req = httpTesting.expectOne('/api/auth/me');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      expect(service.currentUser()).toBeNull();
      expect(emitted).toBeNull();
      expect(errored).toBe(false);
    });

    it('should set currentUser to null and emit null on a network error without throwing', () => {
      service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });
      let emitted: User | null = undefined as unknown as User | null;
      let errored = false;

      service.rehydrate().subscribe({
        next: (user) => (emitted = user),
        error: () => (errored = true),
      });

      const req = httpTesting.expectOne('/api/auth/me');
      req.error(new ProgressEvent('error'));

      expect(service.currentUser()).toBeNull();
      expect(emitted).toBeNull();
      expect(errored).toBe(false);
    });

    it('should set currentUser to null and emit null on other HTTP errors without throwing', () => {
      service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });
      let emitted: User | null = undefined as unknown as User | null;
      let errored = false;

      service.rehydrate().subscribe({
        next: (user) => (emitted = user),
        error: () => (errored = true),
      });

      const req = httpTesting.expectOne('/api/auth/me');
      req.flush({ message: 'Server Error' }, { status: 500, statusText: 'Internal Server Error' });

      expect(service.currentUser()).toBeNull();
      expect(emitted).toBeNull();
      expect(errored).toBe(false);
    });
  });

  describe('logout', () => {
    it('should send POST to /api/auth/logout', () => {
      service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });

      service.logout();

      const req = httpTesting.expectOne('/api/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({});
    });

    it('should set currentUser to null and navigate to /login on success', () => {
      service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });

      service.logout();

      const req = httpTesting.expectOne('/api/auth/logout');
      req.flush({});

      expect(service.currentUser()).toBeNull();
      expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    });

    it('should handle errors gracefully - still clears signal and navigates', () => {
      service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });

      service.logout();

      const req = httpTesting.expectOne('/api/auth/logout');
      req.flush({}, { status: 500, statusText: 'Internal Server Error' });

      expect(service.currentUser()).toBeNull();
      expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    });

    it('should skip HTTP when currentUser is already null', () => {
      expect(service.currentUser()).toBeNull();

      service.logout();

      httpTesting.expectNone('/api/auth/logout');
      expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
    });

    it('should time out after 10 seconds', () => {
      vi.useFakeTimers();

      service.currentUser.set({ id: 1, name: 'Test', email: 'test@example.com' });

      service.logout();

      httpTesting.expectOne('/api/auth/logout');

      vi.advanceTimersByTime(10000);

      expect(service.currentUser()).toBeNull();
      expect(router.navigateByUrl).toHaveBeenCalledWith('/login');

      vi.useRealTimers();
    });
  });
});
