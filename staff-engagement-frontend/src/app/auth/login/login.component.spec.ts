import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: {
    login: ReturnType<typeof vi.fn>;
    isAuthenticated: ReturnType<typeof vi.fn>;
  };
  let router: { navigateByUrl: ReturnType<typeof vi.fn> };
  let queryParams: Record<string, string>;

  beforeEach(async () => {
    authService = {
      login: vi.fn(),
      isAuthenticated: vi.fn().mockReturnValue(false),
    };

    router = {
      navigateByUrl: vi.fn(),
    };

    queryParams = {};

    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(queryParams),
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('form rendering', () => {
    it('should render a form with email and password controls', () => {
      expect(component.loginForm).toBeDefined();
      expect(component.loginForm.get('email')).toBeDefined();
      expect(component.loginForm.get('password')).toBeDefined();

      const compiled = fixture.nativeElement as HTMLElement;
      const emailInput = compiled.querySelector('[data-testid="login-email-input"]');
      const passwordInput = compiled.querySelector('[data-testid="login-password-input"]');
      expect(emailInput).toBeTruthy();
      expect(passwordInput).toBeTruthy();
    });
  });

  describe('submit button disabled states', () => {
    it('should disable submit button when form is invalid', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector(
        '[data-testid="login-submit-button"]'
      ) as HTMLButtonElement;

      expect(button.disabled).toBe(true);
    });

    it('should disable submit button while loading', () => {
      component.loginForm.setValue({ email: 'test@example.com', password: 'password123' });
      component.isLoading.set(true);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector(
        '[data-testid="login-submit-button"]'
      ) as HTMLButtonElement;

      expect(button.disabled).toBe(true);
    });

    it('should enable submit button when form is valid and not loading', () => {
      component.loginForm.setValue({ email: 'test@example.com', password: 'password123' });
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector(
        '[data-testid="login-submit-button"]'
      ) as HTMLButtonElement;

      expect(button.disabled).toBe(false);
    });
  });

  describe('validation error messages', () => {
    it('should display email required error when email is touched and empty', () => {
      const emailControl = component.loginForm.get('email')!;
      emailControl.markAsTouched();
      emailControl.setValue('');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorText = compiled.textContent;
      expect(errorText).toContain('Email is required');
    });

    it('should display invalid email error when email format is wrong', () => {
      const emailControl = component.loginForm.get('email')!;
      emailControl.markAsTouched();
      emailControl.setValue('invalid-email');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorText = compiled.textContent;
      expect(errorText).toContain('Please enter a valid email address');
    });

    it('should display password required error when password is touched and empty', () => {
      const passwordControl = component.loginForm.get('password')!;
      passwordControl.markAsTouched();
      passwordControl.setValue('');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorText = compiled.textContent;
      expect(errorText).toContain('Password is required');
    });

    it('should display minlength error when password is too short', () => {
      const passwordControl = component.loginForm.get('password')!;
      passwordControl.markAsTouched();
      passwordControl.setValue('abc');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const errorText = compiled.textContent;
      expect(errorText).toContain('Password must be at least 6 characters');
    });
  });

  describe('login submission', () => {
    it('should call AuthService.login() on valid form submission', () => {
      const loginSubject = new Subject<{ id: number; name: string; email: string }>();
      authService.login.mockReturnValue(loginSubject.asObservable());

      component.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      component.onSubmit();

      expect(authService.login).toHaveBeenCalledWith('user@example.com', 'secret123');
    });

    it('should show loading indicator during login request', () => {
      const loginSubject = new Subject<{ id: number; name: string; email: string }>();
      authService.login.mockReturnValue(loginSubject.asObservable());

      component.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      component.onSubmit();
      fixture.detectChanges();

      expect(component.isLoading()).toBe(true);

      const compiled = fixture.nativeElement as HTMLElement;
      const loadingText = compiled.textContent;
      expect(loadingText).toContain('Signing in...');
    });
  });

  describe('error handling', () => {
    it('should display "Invalid email or password" for 401 error', () => {
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      authService.login.mockReturnValue(throwError(() => error));

      component.loginForm.setValue({ email: 'user@example.com', password: 'wrongpass' });
      component.onSubmit();
      fixture.detectChanges();

      expect(component.errorMessage()).toBe('Invalid email or password');

      const compiled = fixture.nativeElement as HTMLElement;
      const errorContainer = compiled.querySelector('[data-testid="login-error-message"]');
      expect(errorContainer?.textContent?.trim()).toContain('Invalid email or password');
    });

    it('should display "Required fields are missing or malformed" for 400 error', () => {
      const error = new HttpErrorResponse({ status: 400, statusText: 'Bad Request' });
      authService.login.mockReturnValue(throwError(() => error));

      component.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      component.onSubmit();
      fixture.detectChanges();

      expect(component.errorMessage()).toBe('Required fields are missing or malformed');
    });

    it('should display "Login failed. Please try again later." for other errors', () => {
      const error = new HttpErrorResponse({ status: 500, statusText: 'Server Error' });
      authService.login.mockReturnValue(throwError(() => error));

      component.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      component.onSubmit();
      fixture.detectChanges();

      expect(component.errorMessage()).toBe('Login failed. Please try again later.');
    });

    it('should clear error when user modifies email field', () => {
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      authService.login.mockReturnValue(throwError(() => error));

      component.loginForm.setValue({ email: 'user@example.com', password: 'wrongpass' });
      component.onSubmit();

      expect(component.errorMessage()).toBe('Invalid email or password');

      component.loginForm.get('email')!.setValue('new@example.com');

      expect(component.errorMessage()).toBeNull();
    });

    it('should clear error when user modifies password field', () => {
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      authService.login.mockReturnValue(throwError(() => error));

      component.loginForm.setValue({ email: 'user@example.com', password: 'wrongpass' });
      component.onSubmit();

      expect(component.errorMessage()).toBe('Invalid email or password');

      component.loginForm.get('password')!.setValue('newpassword');

      expect(component.errorMessage()).toBeNull();
    });

    it('should preserve email value on error', () => {
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      authService.login.mockReturnValue(throwError(() => error));

      component.loginForm.setValue({ email: 'keep@example.com', password: 'wrong' });
      component.onSubmit();

      expect(component.loginForm.get('email')!.value).toBe('keep@example.com');
    });

    it('should re-enable submit button on error', () => {
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      authService.login.mockReturnValue(throwError(() => error));

      component.loginForm.setValue({ email: 'user@example.com', password: 'wrongpass' });
      component.onSubmit();
      fixture.detectChanges();

      expect(component.isLoading()).toBe(false);

      const compiled = fixture.nativeElement as HTMLElement;
      const button = compiled.querySelector(
        '[data-testid="login-submit-button"]'
      ) as HTMLButtonElement;
      expect(button.disabled).toBe(false);
    });
  });

  describe('post-login navigation', () => {
    it('should navigate to returnUrl on success', async () => {
      TestBed.resetTestingModule();

      const localAuthService = {
        login: vi.fn(),
        isAuthenticated: vi.fn().mockReturnValue(false),
      };
      const localRouter = { navigateByUrl: vi.fn() };

      await TestBed.configureTestingModule({
        imports: [LoginComponent, ReactiveFormsModule],
        providers: [
          { provide: AuthService, useValue: localAuthService },
          { provide: Router, useValue: localRouter },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: convertToParamMap({ returnUrl: '/dashboard' }),
              },
            },
          },
        ],
      }).compileComponents();

      const localFixture = TestBed.createComponent(LoginComponent);
      const localComponent = localFixture.componentInstance;
      localFixture.detectChanges();

      localAuthService.login.mockReturnValue(
        of({ id: 1, name: 'User', email: 'user@example.com' })
      );
      localComponent.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      localComponent.onSubmit();

      expect(localRouter.navigateByUrl).toHaveBeenCalledWith('/dashboard');
    });

    it('should navigate to /user when no returnUrl', () => {
      authService.login.mockReturnValue(
        of({ id: 1, name: 'User', email: 'user@example.com' })
      );
      component.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      component.onSubmit();

      expect(router.navigateByUrl).toHaveBeenCalledWith('/user');
    });

    it('should discard returnUrl without leading /', async () => {
      TestBed.resetTestingModule();

      const localAuthService = {
        login: vi.fn(),
        isAuthenticated: vi.fn().mockReturnValue(false),
      };
      const localRouter = { navigateByUrl: vi.fn() };

      await TestBed.configureTestingModule({
        imports: [LoginComponent, ReactiveFormsModule],
        providers: [
          { provide: AuthService, useValue: localAuthService },
          { provide: Router, useValue: localRouter },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: convertToParamMap({ returnUrl: 'no-slash' }),
              },
            },
          },
        ],
      }).compileComponents();

      const localFixture = TestBed.createComponent(LoginComponent);
      const localComponent = localFixture.componentInstance;
      localFixture.detectChanges();

      localAuthService.login.mockReturnValue(
        of({ id: 1, name: 'User', email: 'user@example.com' })
      );
      localComponent.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      localComponent.onSubmit();

      expect(localRouter.navigateByUrl).toHaveBeenCalledWith('/user');
    });

    it('should discard returnUrl starting with //', async () => {
      TestBed.resetTestingModule();

      const localAuthService = {
        login: vi.fn(),
        isAuthenticated: vi.fn().mockReturnValue(false),
      };
      const localRouter = { navigateByUrl: vi.fn() };

      await TestBed.configureTestingModule({
        imports: [LoginComponent, ReactiveFormsModule],
        providers: [
          { provide: AuthService, useValue: localAuthService },
          { provide: Router, useValue: localRouter },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: convertToParamMap({ returnUrl: '//evil.com' }),
              },
            },
          },
        ],
      }).compileComponents();

      const localFixture = TestBed.createComponent(LoginComponent);
      const localComponent = localFixture.componentInstance;
      localFixture.detectChanges();

      localAuthService.login.mockReturnValue(
        of({ id: 1, name: 'User', email: 'user@example.com' })
      );
      localComponent.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      localComponent.onSubmit();

      expect(localRouter.navigateByUrl).toHaveBeenCalledWith('/user');
    });

    it('should discard returnUrl exceeding 2048 chars', async () => {
      TestBed.resetTestingModule();

      const localAuthService = {
        login: vi.fn(),
        isAuthenticated: vi.fn().mockReturnValue(false),
      };
      const localRouter = { navigateByUrl: vi.fn() };
      const longUrl = '/' + 'a'.repeat(2048);

      await TestBed.configureTestingModule({
        imports: [LoginComponent, ReactiveFormsModule],
        providers: [
          { provide: AuthService, useValue: localAuthService },
          { provide: Router, useValue: localRouter },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: convertToParamMap({ returnUrl: longUrl }),
              },
            },
          },
        ],
      }).compileComponents();

      const localFixture = TestBed.createComponent(LoginComponent);
      const localComponent = localFixture.componentInstance;
      localFixture.detectChanges();

      localAuthService.login.mockReturnValue(
        of({ id: 1, name: 'User', email: 'user@example.com' })
      );
      localComponent.loginForm.setValue({ email: 'user@example.com', password: 'secret123' });
      localComponent.onSubmit();

      expect(localRouter.navigateByUrl).toHaveBeenCalledWith('/user');
    });
  });

  describe('authenticated user redirect', () => {
    it('should redirect authenticated user to /user on init', async () => {
      TestBed.resetTestingModule();

      const localAuthService = {
        login: vi.fn(),
        isAuthenticated: vi.fn().mockReturnValue(true),
      };
      const localRouter = { navigateByUrl: vi.fn() };

      await TestBed.configureTestingModule({
        imports: [LoginComponent, ReactiveFormsModule],
        providers: [
          { provide: AuthService, useValue: localAuthService },
          { provide: Router, useValue: localRouter },
          {
            provide: ActivatedRoute,
            useValue: {
              snapshot: {
                queryParamMap: convertToParamMap({}),
              },
            },
          },
        ],
      }).compileComponents();

      const localFixture = TestBed.createComponent(LoginComponent);
      localFixture.detectChanges();

      expect(localRouter.navigateByUrl).toHaveBeenCalledWith('/user');
    });
  });
});
