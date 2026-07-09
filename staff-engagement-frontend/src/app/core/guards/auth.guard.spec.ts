import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authService: AuthService;
  let router: Router;
  let mockRoute: ActivatedRouteSnapshot;
  let mockUrlTree: UrlTree;

  beforeEach(() => {
    mockUrlTree = { toString: () => '/login?returnUrl=%2Fdashboard' } as unknown as UrlTree;

    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            currentUser: vi.fn(),
            isAuthenticated: vi.fn(),
          },
        },
        {
          provide: Router,
          useValue: {
            createUrlTree: vi.fn().mockReturnValue(mockUrlTree),
          },
        },
      ],
    });

    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
    mockRoute = {} as ActivatedRouteSnapshot;
  });

  it('should return true when currentUser is non-null', () => {
    (authService.isAuthenticated as unknown as ReturnType<typeof vi.fn>).mockReturnValue(true);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(mockRoute, { url: '/dashboard' } as RouterStateSnapshot)
    );

    expect(result).toBe(true);
  });

  it('should return UrlTree to /login when currentUser is null', () => {
    (authService.isAuthenticated as unknown as ReturnType<typeof vi.fn>).mockReturnValue(false);

    const result = TestBed.runInInjectionContext(() =>
      authGuard(mockRoute, { url: '/dashboard' } as RouterStateSnapshot)
    );

    expect(result).toBe(mockUrlTree);
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/dashboard' },
    });
  });

  it('should include returnUrl query parameter with original path', () => {
    (authService.isAuthenticated as unknown as ReturnType<typeof vi.fn>).mockReturnValue(false);

    const originalPath = '/employees/42/tasks?status=active#details';

    TestBed.runInInjectionContext(() =>
      authGuard(mockRoute, { url: originalPath } as RouterStateSnapshot)
    );

    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: originalPath },
    });
  });

  it('should truncate returnUrl to 2048 characters', () => {
    (authService.isAuthenticated as unknown as ReturnType<typeof vi.fn>).mockReturnValue(false);

    const longPath = '/' + 'a'.repeat(3000);

    TestBed.runInInjectionContext(() =>
      authGuard(mockRoute, { url: longPath } as RouterStateSnapshot)
    );

    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: longPath.substring(0, 2048) },
    });
  });
});
