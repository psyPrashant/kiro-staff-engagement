import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component, Input } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, RouterLink } from '@angular/router';
import * as fc from 'fast-check';
import { InteractionMatrixComponent } from './interaction-matrix.component';
import { EngagementStatus, MatrixEntry, SortOption } from '../models/engagement.model';

@Component({
  selector: 'app-status-filter',
  standalone: true,
  template: '',
})
class MockStatusFilterComponent {
  @Input() activeFilter: EngagementStatus | null = null;
}

@Component({
  selector: 'app-sort-control',
  standalone: true,
  template: '',
})
class MockSortControlComponent {
  @Input() activeSort: SortOption = 'name';
}

@Component({
  selector: 'app-follow-up-section',
  standalone: true,
  template: '<div data-testid="follow-up-mock"></div>',
})
class MockFollowUpSectionComponent {
  @Input() entries: MatrixEntry[] = [];
}

function createMockEntry(overrides: Partial<MatrixEntry> = {}): MatrixEntry {
  return {
    employeeId: 1,
    employeeName: 'John Doe',
    employeeEmail: 'john@example.com',
    recency: 5,
    frequency: 3,
    lastInteractionDate: '2024-01-15T00:00:00Z',
    engagementStatus: 'ON_TRACK',
    followUpRequired: false,
    ...overrides,
  };
}

describe('InteractionMatrixComponent', () => {
  let fixture: ComponentFixture<InteractionMatrixComponent>;
  let component: InteractionMatrixComponent;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InteractionMatrixComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    })
      .overrideComponent(InteractionMatrixComponent, {
        set: {
          imports: [
            RouterLink,
            MockStatusFilterComponent,
            MockSortControlComponent,
            MockFollowUpSectionComponent,
          ],
        },
      })
      .compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(InteractionMatrixComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function triggerInitialLoad(entries: MatrixEntry[] = []): void {
    fixture.detectChanges();
    const req = httpTesting.expectOne('/api/engagement/matrix');
    req.flush(entries);
    fixture.detectChanges();
  }

  describe('rows rendered per entry count', () => {
    it('should render one table row per matrix entry', () => {
      const entries = [
        createMockEntry({ employeeId: 1, employeeName: 'Alice' }),
        createMockEntry({ employeeId: 2, employeeName: 'Bob' }),
        createMockEntry({ employeeId: 3, employeeName: 'Charlie' }),
      ];

      triggerInitialLoad(entries);

      const rows = fixture.nativeElement.querySelectorAll('tbody tr');
      expect(rows.length).toBe(3);
    });

    it('should render empty state when no entries returned', () => {
      triggerInitialLoad([]);

      const emptyMessage = fixture.nativeElement.querySelector('.empty-state');
      expect(emptyMessage).toBeTruthy();
      expect(emptyMessage.textContent).toContain('No engagement data available');
    });
  });

  describe('loading indicator', () => {
    it('should show loading indicator while fetching data', () => {
      fixture.detectChanges();

      const loading = fixture.nativeElement.querySelector('.loading-indicator');
      expect(loading).toBeTruthy();
      expect(loading.textContent).toContain('Loading engagement data...');

      const req = httpTesting.expectOne('/api/engagement/matrix');
      req.flush([]);
      fixture.detectChanges();
    });

    it('should hide loading indicator once data arrives', () => {
      triggerInitialLoad([createMockEntry()]);

      const loading = fixture.nativeElement.querySelector('.loading-indicator');
      expect(loading).toBeNull();
    });
  });

  describe('error message and retry button', () => {
    it('should display error message on HTTP error', () => {
      fixture.detectChanges();
      const req = httpTesting.expectOne('/api/engagement/matrix');
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
      fixture.detectChanges();

      const errorState = fixture.nativeElement.querySelector('.error-state');
      expect(errorState).toBeTruthy();
      expect(errorState.textContent).toContain(
        'Unable to load the engagement matrix. Please try again.',
      );
    });

    it('should display a retry button on error', () => {
      fixture.detectChanges();
      const req = httpTesting.expectOne('/api/engagement/matrix');
      req.flush(null, { status: 500, statusText: 'Error' });
      fixture.detectChanges();

      const retryBtn = fixture.nativeElement.querySelector('.error-state button');
      expect(retryBtn).toBeTruthy();
      expect(retryBtn.textContent).toContain('Retry');
    });

    it('should re-fetch data when retry button is clicked', () => {
      fixture.detectChanges();
      const req = httpTesting.expectOne('/api/engagement/matrix');
      req.flush(null, { status: 500, statusText: 'Error' });
      fixture.detectChanges();

      const retryBtn: HTMLButtonElement =
        fixture.nativeElement.querySelector('.error-state button');
      retryBtn.click();
      fixture.detectChanges();

      const retryReq = httpTesting.expectOne('/api/engagement/matrix');
      retryReq.flush([createMockEntry()]);
      fixture.detectChanges();

      const errorState = fixture.nativeElement.querySelector('.error-state');
      expect(errorState).toBeNull();
      const rows = fixture.nativeElement.querySelectorAll('tbody tr');
      expect(rows.length).toBe(1);
    });
  });

  describe('correct CSS class per EngagementStatus value', () => {
    it('should apply status-overdue class for OVERDUE status', () => {
      triggerInitialLoad([createMockEntry({ engagementStatus: 'OVERDUE' })]);

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-overdue')).toBe(true);
    });

    it('should apply status-at-risk class for AT_RISK status', () => {
      triggerInitialLoad([createMockEntry({ engagementStatus: 'AT_RISK' })]);

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-at-risk')).toBe(true);
    });

    it('should apply status-on-track class for ON_TRACK status', () => {
      triggerInitialLoad([createMockEntry({ engagementStatus: 'ON_TRACK' })]);

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-on-track')).toBe(true);
    });
  });

  describe('drill-through links with correct router paths', () => {
    it('should render Employee 360 link with correct route', () => {
      triggerInitialLoad([createMockEntry({ employeeId: 42 })]);

      const links: HTMLAnchorElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('.action-link'),
      );
      const profileLink = links.find((l) => l.textContent?.includes('View Profile'));
      expect(profileLink).toBeTruthy();
      expect(profileLink!.getAttribute('href')).toBe('/employee/42');
    });

    it('should render Log Interaction link with correct route and query param', () => {
      triggerInitialLoad([createMockEntry({ employeeId: 42 })]);

      const links: HTMLAnchorElement[] = Array.from(
        fixture.nativeElement.querySelectorAll('.action-link'),
      );
      const logLink = links.find((l) => l.textContent?.includes('Log Interaction'));
      expect(logLink).toBeTruthy();
      expect(logLink!.getAttribute('href')).toBe('/interaction?employeeId=42');
    });
  });

  describe('follow-up section receives only followUpRequired entries', () => {
    it('should pass only followUpRequired=true entries to follow-up section', () => {
      const entries = [
        createMockEntry({ employeeId: 1, followUpRequired: true }),
        createMockEntry({ employeeId: 2, followUpRequired: false }),
        createMockEntry({ employeeId: 3, followUpRequired: true }),
      ];

      triggerInitialLoad(entries);

      const followUpEntries = component.followUpEntries();
      expect(followUpEntries.length).toBe(2);
      expect(followUpEntries.every((e) => e.followUpRequired)).toBe(true);
      expect(followUpEntries.map((e) => e.employeeId)).toEqual([1, 3]);
    });

    it('should pass empty array when no entries have followUpRequired=true', () => {
      const entries = [
        createMockEntry({ employeeId: 1, followUpRequired: false }),
        createMockEntry({ employeeId: 2, followUpRequired: false }),
      ];

      triggerInitialLoad(entries);

      const followUpEntries = component.followUpEntries();
      expect(followUpEntries.length).toBe(0);
    });
  });

  describe('filter selection triggers new API call', () => {
    it('should make API call with status query parameter when filter changes', () => {
      fixture.detectChanges();
      const initialReq = httpTesting.expectOne('/api/engagement/matrix');
      initialReq.flush([]);
      fixture.detectChanges();

      component.onFilterChange('OVERDUE');
      fixture.detectChanges();

      const filterReq = httpTesting.expectOne(
        (req) => req.url === '/api/engagement/matrix' && req.params.get('status') === 'OVERDUE',
      );
      expect(filterReq).toBeTruthy();
      filterReq.flush([]);
    });

    it('should make API call without status param when filter set to null (All)', () => {
      fixture.detectChanges();
      const initialReq = httpTesting.expectOne('/api/engagement/matrix');
      initialReq.flush([]);
      fixture.detectChanges();

      component.onFilterChange(null);
      fixture.detectChanges();

      const req = httpTesting.expectOne(
        (req) => req.url === '/api/engagement/matrix' && !req.params.has('status'),
      );
      expect(req).toBeTruthy();
      req.flush([]);
    });
  });

  describe('semantic HTML structure', () => {
    it('should render a table element', () => {
      triggerInitialLoad([createMockEntry()]);

      const table = fixture.nativeElement.querySelector('table');
      expect(table).toBeTruthy();
    });

    it('should render thead element', () => {
      triggerInitialLoad([createMockEntry()]);

      const thead = fixture.nativeElement.querySelector('thead');
      expect(thead).toBeTruthy();
    });

    it('should render tbody element', () => {
      triggerInitialLoad([createMockEntry()]);

      const tbody = fixture.nativeElement.querySelector('tbody');
      expect(tbody).toBeTruthy();
    });
  });

  describe('aria-busy and aria-live during loading', () => {
    it('should have aria-busy=true on matrix content while loading', () => {
      fixture.detectChanges();

      const content = fixture.nativeElement.querySelector('.matrix-content');
      expect(content.getAttribute('aria-busy')).toBe('true');

      const req = httpTesting.expectOne('/api/engagement/matrix');
      req.flush([]);
      fixture.detectChanges();
    });

    it('should have aria-busy=false after data loads', () => {
      triggerInitialLoad([createMockEntry()]);

      const content = fixture.nativeElement.querySelector('.matrix-content');
      expect(content.getAttribute('aria-busy')).toBe('false');
    });

    it('should have aria-live polite region in loading indicator', () => {
      fixture.detectChanges();

      const loading = fixture.nativeElement.querySelector('.loading-indicator');
      expect(loading.getAttribute('aria-live')).toBe('polite');

      const req = httpTesting.expectOne('/api/engagement/matrix');
      req.flush([]);
      fixture.detectChanges();
    });
  });
});

// --- Property-Based Tests ---

function arbitraryMatrixEntry(): fc.Arbitrary<MatrixEntry> {
  return fc.record({
    employeeId: fc.integer({ min: 1 }),
    employeeName: fc.string({ minLength: 1, maxLength: 50 }),
    employeeEmail: fc.emailAddress(),
    recency: fc.option(fc.nat(), { nil: null }),
    frequency: fc.nat(),
    lastInteractionDate: fc.option(
      fc.date().map((d) => d.toISOString()),
      { nil: null },
    ),
    engagementStatus: fc.constantFrom(
      'OVERDUE' as EngagementStatus,
      'AT_RISK' as EngagementStatus,
      'ON_TRACK' as EngagementStatus,
    ),
    followUpRequired: fc.boolean(),
  });
}

// Feature: dashboard-interaction-matrix, Property 2: Follow-up filtering correctness
describe('InteractionMatrixComponent - Property 2: Follow-up filtering correctness', () => {
  let component: InteractionMatrixComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [InteractionMatrixComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(InteractionMatrixComponent);
    component = fixture.componentInstance;
  });

  /**
   * Property 2: Follow-up filtering correctness
   *
   * For any array of MatrixEntry items with arbitrary followUpRequired boolean values,
   * the computed follow-up list SHALL contain exactly the subset of entries where
   * followUpRequired === true, preserving order and excluding all entries where it is false.
   *
   * Validates: Requirements 2.4, 4.1
   */
  it('should filter entries to only those with followUpRequired === true, preserving order', () => {
    fc.assert(
      fc.property(fc.array(arbitraryMatrixEntry()), (entries) => {
        component.entries.set(entries);

        const result = component.followUpEntries();
        const expected = entries.filter((e) => e.followUpRequired);

        expect(result.length).toBe(expected.length);
        expect(result).toEqual(expected);
      }),
      { numRuns: 100 },
    );
  });
});

// Feature: dashboard-interaction-matrix, Property 3: Status indicator mapping
describe('InteractionMatrixComponent - Property 3: Status indicator mapping', () => {
  let component: InteractionMatrixComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [InteractionMatrixComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(InteractionMatrixComponent);
    component = fixture.componentInstance;
  });

  /**
   * Property 3: Status indicator mapping
   *
   * For any EngagementStatus value, the status rendering logic SHALL produce both
   * the correct CSS class (status-overdue for OVERDUE, status-at-risk for AT_RISK,
   * status-on-track for ON_TRACK) AND the correct aria-label containing the
   * human-readable status name.
   *
   * Validates: Requirements 2.3, 7.3, 8.5
   */
  it('should return correct CSS class and aria-label for every status value', () => {
    const statusToClass: Record<EngagementStatus, string> = {
      OVERDUE: 'status-overdue',
      AT_RISK: 'status-at-risk',
      ON_TRACK: 'status-on-track',
    };

    const statusToLabel: Record<EngagementStatus, string> = {
      OVERDUE: 'Status: Overdue',
      AT_RISK: 'Status: At Risk',
      ON_TRACK: 'Status: On Track',
    };

    fc.assert(
      fc.property(
        fc.constantFrom(
          'OVERDUE' as EngagementStatus,
          'AT_RISK' as EngagementStatus,
          'ON_TRACK' as EngagementStatus,
        ),
        (status) => {
          const cssClass = component.getStatusClass(status);
          const label = component.getStatusLabel(status);

          expect(cssClass).toBe(statusToClass[status]);
          expect(label).toBe(statusToLabel[status]);
        },
      ),
      { numRuns: 100 },
    );
  });
});

// Feature: dashboard-interaction-matrix, Property 4: Drill-through link correctness
describe('InteractionMatrixComponent - Property 4: Drill-through link correctness', () => {
  let component: InteractionMatrixComponent;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [InteractionMatrixComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(InteractionMatrixComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  /**
   * Property 4: Drill-through link correctness
   *
   * For any MatrixEntry with a positive integer employeeId, the Employee 360 link
   * SHALL produce the route path /employee/{employeeId} and the log-interaction link
   * SHALL produce the route path /interaction with query parameter employeeId={employeeId}.
   *
   * Validates: Requirements 3.1, 3.2, 3.3, 3.4, 4.3
   */
  it('should navigate to correct employee and interaction paths for any employeeId', () => {
    fc.assert(
      fc.property(fc.integer({ min: 1 }), (employeeId) => {
        const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

        component.navigateToEmployee(employeeId);
        expect(navigateSpy).toHaveBeenCalledWith(['/employee', employeeId]);

        navigateSpy.mockClear();

        component.navigateToLogInteraction(employeeId);
        expect(navigateSpy).toHaveBeenCalledWith(['/interaction'], {
          queryParams: { employeeId },
        });

        navigateSpy.mockRestore();
      }),
      { numRuns: 100 },
    );
  });
});

// Feature: dashboard-interaction-matrix, Property 5: Recency and date display formatting
describe('InteractionMatrixComponent - Property 5: Recency and date display formatting', () => {
  let component: InteractionMatrixComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [InteractionMatrixComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const fixture = TestBed.createComponent(InteractionMatrixComponent);
    component = fixture.componentInstance;
  });

  /**
   * Property 5: Recency and date display formatting
   *
   * For any MatrixEntry, when recency is null the displayed recency text SHALL be
   * "No interactions", and when recency is a non-negative number it SHALL be displayed
   * as "{n} days". When lastInteractionDate is null the displayed date text SHALL be
   * "Never", and when it is a non-null ISO date string it SHALL be formatted as a
   * human-readable date.
   *
   * Validates: Requirements 2.2
   */
  it('should format recency and date correctly for any input', () => {
    fc.assert(
      fc.property(
        fc.record({
          recency: fc.option(fc.nat(), { nil: null }),
          lastInteractionDate: fc.option(
            fc.date().map((d) => d.toISOString()),
            { nil: null },
          ),
        }),
        ({ recency, lastInteractionDate }) => {
          const recencyText = component.formatRecency(recency);
          if (recency === null) {
            expect(recencyText).toBe('No interactions');
          } else {
            expect(recencyText).toBe(`${recency} days`);
          }

          const dateText = component.formatDate(lastInteractionDate);
          if (lastInteractionDate === null) {
            expect(dateText).toBe('Never');
          } else {
            const formatted = new Date(lastInteractionDate).toLocaleDateString();
            expect(dateText).toBe(formatted);
          }
        },
      ),
      { numRuns: 100 },
    );
  });
});
