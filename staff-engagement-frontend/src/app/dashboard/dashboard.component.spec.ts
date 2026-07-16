import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { DashboardComponent } from './dashboard.component';
import { EngagementService } from './services/engagement.service';
import { SchedulingService } from '../schedule/services/scheduling.service';
import { GreetingService } from '../greeting/greeting.service';
import { AuthService } from '../core/services/auth.service';

const mockEngagementService = {
  getMatrix: vi.fn().mockReturnValue(of([])),
};

const mockSchedulingService = {
  list: vi.fn().mockReturnValue(of([])),
};

const mockGreetingService = {
  getGreeting: vi.fn().mockReturnValue(of('Welcome back')),
};

const mockAuthService = {
  currentUser: signal({ id: 1, name: 'Test User', email: 'test@example.com' }),
};

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;

  beforeEach(async () => {
    mockEngagementService.getMatrix.mockReturnValue(of([]));
    mockSchedulingService.list.mockReturnValue(of([]));
    mockGreetingService.getGreeting.mockReturnValue(of('Welcome back'));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: EngagementService, useValue: mockEngagementService },
        { provide: SchedulingService, useValue: mockSchedulingService },
        { provide: GreetingService, useValue: mockGreetingService },
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
  });

  it('should have data-testid="dashboard" on the container', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const dashboard = compiled.querySelector('[data-testid="dashboard"]');
    expect(dashboard).toBeTruthy();
  });

  it('should render the greeting heading', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const heading = compiled.querySelector('h1');
    expect(heading).toBeTruthy();
    expect(heading!.textContent).toContain('Welcome back');
  });

  it('should render the triage stat tiles', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const statTiles = compiled.querySelector('[data-testid="stat-tiles"]');
    expect(statTiles).toBeTruthy();
    expect(statTiles!.querySelectorAll('.stat-tile').length).toBe(3);
  });

  it('should render the calendar section', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const calendar = compiled.querySelector('[data-testid="calendar-week"]');
    expect(calendar).toBeTruthy();
  });
});
