import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ShellComponent } from './shell.component';
import { AuthService } from '../core/services/auth.service';

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;
  let authService: { logout: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = {
      logout: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [{ provide: AuthService, useValue: authService }, provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
  });

  describe('navigation bar rendering', () => {
    it('should render visible links for Dashboard, Employees, Log interaction, and Tasks', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const dashboardLink = compiled.querySelector(
        '[data-testid="nav-link-dashboard"]',
      ) as HTMLAnchorElement;
      const employeeLink = compiled.querySelector(
        '[data-testid="nav-link-employee"]',
      ) as HTMLAnchorElement;
      const interactionLink = compiled.querySelector(
        '[data-testid="nav-link-interaction"]',
      ) as HTMLAnchorElement;
      const taskLink = compiled.querySelector('[data-testid="nav-link-task"]') as HTMLAnchorElement;

      expect(dashboardLink).toBeTruthy();
      expect(employeeLink).toBeTruthy();
      expect(interactionLink).toBeTruthy();
      expect(taskLink).toBeTruthy();

      expect(dashboardLink.getAttribute('href')).toBe('/dashboard');
      expect(employeeLink.getAttribute('href')).toBe('/employee');
      expect(interactionLink.getAttribute('href')).toBe('/interaction');
      expect(taskLink.getAttribute('href')).toBe('/task');
    });

    it('should render brand label linked to /dashboard', () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const brand = compiled.querySelector('[data-testid="nav-brand"]') as HTMLAnchorElement;

      expect(brand).toBeTruthy();
      expect(brand.textContent?.trim()).toBe('Staff Engagement');
      expect(brand.getAttribute('href')).toBe('/dashboard');
    });

    it('should hide User and Client links from visible navigation', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const userLink = compiled.querySelector('[data-testid="nav-link-user"]') as HTMLAnchorElement;
      const clientLink = compiled.querySelector(
        '[data-testid="nav-link-client"]',
      ) as HTMLAnchorElement;

      expect(userLink).toBeTruthy();
      expect(clientLink).toBeTruthy();
      expect(userLink.hidden).toBe(true);
      expect(clientLink.hidden).toBe(true);
    });

    it('should display link labels matching the workflow navigation', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const employeeLink = compiled.querySelector(
        '[data-testid="nav-link-employee"]',
      ) as HTMLAnchorElement;
      const interactionLink = compiled.querySelector(
        '[data-testid="nav-link-interaction"]',
      ) as HTMLAnchorElement;
      const taskLink = compiled.querySelector('[data-testid="nav-link-task"]') as HTMLAnchorElement;

      expect(employeeLink.textContent?.trim()).toBe('Employees');
      expect(interactionLink.textContent?.trim()).toBe('Log interaction');
      expect(taskLink.textContent?.trim()).toBe('Tasks');
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

  describe('data-testid attributes', () => {
    it('should have data-testid attributes on all navigation links', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const expectedTestIds = [
        'nav-link-user',
        'nav-link-employee',
        'nav-link-client',
        'nav-link-interaction',
        'nav-link-task',
      ];

      for (const testId of expectedTestIds) {
        const element = compiled.querySelector(`[data-testid="${testId}"]`);
        expect(element).toBeTruthy();
      }
    });
  });
});
