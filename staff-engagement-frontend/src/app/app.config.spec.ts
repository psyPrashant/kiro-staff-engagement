import { TestBed } from '@angular/core/testing';
import { ApplicationInitStatus } from '@angular/core';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';

import { appConfig } from './app.config';
import { AuthService } from './core/services/auth.service';

describe('appConfig app initializer', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ...appConfig.providers,
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: { navigateByUrl: vi.fn() },
        },
      ],
    });

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('resolves the app initializer when rehydrate() succeeds', async () => {
    const initStatus = TestBed.inject(ApplicationInitStatus);

    const req = httpTesting.expectOne('http://localhost:8080/api/auth/me');
    req.flush({ id: 1, name: 'Test', email: 'test@example.com' });

    await expect(initStatus.donePromise).resolves.toBeUndefined();

    const authService = TestBed.inject(AuthService);
    expect(authService.currentUser()).toEqual({ id: 1, name: 'Test', email: 'test@example.com' });
  });

  it('resolves the app initializer when rehydrate() returns a 401', async () => {
    const initStatus = TestBed.inject(ApplicationInitStatus);

    const req = httpTesting.expectOne('http://localhost:8080/api/auth/me');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    await expect(initStatus.donePromise).resolves.toBeUndefined();

    const authService = TestBed.inject(AuthService);
    expect(authService.currentUser()).toBeNull();
  });

  it('resolves the app initializer when rehydrate() errors with a network failure', async () => {
    const initStatus = TestBed.inject(ApplicationInitStatus);

    const req = httpTesting.expectOne('http://localhost:8080/api/auth/me');
    req.error(new ProgressEvent('error'));

    await expect(initStatus.donePromise).resolves.toBeUndefined();

    const authService = TestBed.inject(AuthService);
    expect(authService.currentUser()).toBeNull();
  });

  it('resolves within the bounded timeout even if the request hangs', async () => {
    const initStatus = TestBed.inject(ApplicationInitStatus);

    httpTesting.expectOne('http://localhost:8080/api/auth/me');
    // Never flush the request, simulating a hung network call. The bounded 10s
    // timeout on the initializer's rehydrate() call must still resolve donePromise.

    await expect(initStatus.donePromise).resolves.toBeUndefined();

    const authService = TestBed.inject(AuthService);
    expect(authService.currentUser()).toBeNull();
  });
});
