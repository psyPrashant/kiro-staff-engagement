import '@angular/compiler';
import { routes } from './app.routes';
import { authGuard } from './core/guards/auth.guard';
import { ShellComponent } from './shell/shell.component';

describe('Route Configuration', () => {
  const loginRoute = routes.find((r) => r.path === 'login');
  const shellRoute = routes.find((r) => r.path === '' && r.component === ShellComponent);
  const shellChildren = shellRoute?.children ?? [];

  describe('login route', () => {
    it('should be defined at top level, outside any parent wrapper', () => {
      expect(loginRoute).toBeDefined();
      expect(loginRoute!.path).toBe('login');
      expect(loginRoute!.loadComponent).toBeDefined();
    });

    it('should not be nested inside the shell route', () => {
      const loginAsChild = shellChildren.find((r) => r.path === 'login');
      expect(loginAsChild).toBeUndefined();
    });
  });

  describe('shell route', () => {
    it('should have canActivate with authGuard', () => {
      expect(shellRoute).toBeDefined();
      expect(shellRoute!.canActivate).toContain(authGuard);
    });

    it('should contain child routes for dashboard, user, employee, client, interaction, task, and wildcard', () => {
      const childPaths = shellChildren.map((r) => r.path);

      expect(childPaths).toContain('dashboard');
      expect(childPaths).toContain('user');
      expect(childPaths).toContain('employee');
      expect(childPaths).toContain('client');
      expect(childPaths).toContain('interaction');
      expect(childPaths).toContain('task');
      expect(childPaths).toContain('**');
    });
  });

  describe('child route guards', () => {
    it('should not have canActivate on any child route', () => {
      for (const child of shellChildren) {
        expect(child.canActivate).toBeUndefined();
      }
    });
  });

  describe('redirects', () => {
    it('should redirect empty path child to dashboard', () => {
      const emptyChild = shellChildren.find((r) => r.path === '' && r.redirectTo === 'dashboard');
      expect(emptyChild).toBeDefined();
      expect(emptyChild!.pathMatch).toBe('full');
    });

    it('should redirect wildcard child to dashboard', () => {
      const wildcardChild = shellChildren.find(
        (r) => r.path === '**' && r.redirectTo === 'dashboard',
      );
      expect(wildcardChild).toBeDefined();
    });
  });
});
