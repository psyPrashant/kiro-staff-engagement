import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { ShellComponent } from './shell.component';
import { AuthService } from '../core/services/auth.service';

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let authService: {
    logout: ReturnType<typeof vi.fn>;
    currentUser: ReturnType<typeof signal>;
  };

  beforeEach(async () => {
    authService = {
      logout: vi.fn(),
      currentUser: signal({ id: 1, name: 'Test User', email: 'test@example.com' }),
    };

    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [{ provide: AuthService, useValue: authService }, provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
  });

  describe('navigation bar rendering', () => {
    it('should render visible links for Dashboard, Employees, and Tasks', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const dashboardLink = compiled.querySelector(
        '[data-testid="nav-link-dashboard"]',
      ) as HTMLAnchorElement;
      const employeeLink = compiled.querySelector(
        '[data-testid="nav-link-employee"]',
      ) as HTMLAnchorElement;
      const taskLink = compiled.querySelector('[data-testid="nav-link-task"]') as HTMLAnchorElement;

      expect(dashboardLink).toBeTruthy();
      expect(employeeLink).toBeTruthy();
      expect(taskLink).toBeTruthy();

      expect(dashboardLink.getAttribute('href')).toBe('/dashboard');
      expect(employeeLink.getAttribute('href')).toBe('/employee');
      expect(taskLink.getAttribute('href')).toBe('/task');
    });

    it('should render the Staff Engagement brand label', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const brand = compiled.querySelector('.brand-text');

      expect(brand).toBeTruthy();
      expect(brand!.textContent?.trim()).toBe('Staff Engagement');
    });

    it('should display link labels matching the workflow navigation', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const dashboardLink = compiled.querySelector(
        '[data-testid="nav-link-dashboard"]',
      ) as HTMLAnchorElement;
      const employeeLink = compiled.querySelector(
        '[data-testid="nav-link-employee"]',
      ) as HTMLAnchorElement;
      const taskLink = compiled.querySelector('[data-testid="nav-link-task"]') as HTMLAnchorElement;

      expect(dashboardLink.textContent?.trim()).toContain('Dashboard');
      expect(employeeLink.textContent?.trim()).toContain('Employees');
      expect(taskLink.textContent?.trim()).toContain('Tasks');
    });

    it('should have data-testid attributes on all navigation links', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const expectedTestIds = ['nav-link-dashboard', 'nav-link-employee', 'nav-link-task'];

      for (const testId of expectedTestIds) {
        const element = compiled.querySelector(`[data-testid="${testId}"]`);
        expect(element).toBeTruthy();
      }
    });
  });

  describe('settings', () => {
    it('should render a settings button', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const settingsButton = compiled.querySelector('[data-testid="nav-link-settings"]');

      expect(settingsButton).toBeTruthy();
    });
  });

  describe('logged-in user', () => {
    it('should display the current user name', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const loggedInUser = compiled.querySelector('[data-testid="logged-in-user"]');

      expect(loggedInUser).toBeTruthy();
      expect(loggedInUser!.textContent).toContain('Test User');
    });
  });

  describe('logout button', () => {
    it('should call AuthService.logout() when logout button is clicked', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const logoutButton = compiled.querySelector(
        '[data-testid="logout-button"]',
      ) as HTMLButtonElement;

      expect(logoutButton).toBeTruthy();
      logoutButton.click();

      expect(authService.logout).toHaveBeenCalled();
    });
  });

  describe('router outlet', () => {
    it('should contain a router-outlet element for child content', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const routerOutlet = compiled.querySelector('router-outlet');

      expect(routerOutlet).toBeTruthy();
    });
  });
});
