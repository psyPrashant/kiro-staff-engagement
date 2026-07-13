import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of, throwError, NEVER } from 'rxjs';
import { signal } from '@angular/core';
import { Employee360Component } from './employee-360.component';
import { Employee360Service } from '../services/employee-360.service';
import { EngagementService } from '../../dashboard/services/engagement.service';
import { TaskService } from '../../task/services/task.service';
import { AuthService } from '../../core/services/auth.service';
import { Employee360Response } from '../models/employee-360.model';

const mockEmployee360Service = {
  getEmployee360: vi.fn(),
};

const mockEngagementService = {
  getMatrix: vi.fn(),
};

const mockTaskService = {
  create: vi.fn(),
};

const mockAuthService = {
  currentUser: signal({ id: 99, name: 'Test User', email: 'test@example.com' }),
};

const mockActivatedRoute = {
  snapshot: {
    paramMap: {
      get: () => '1',
    },
  },
};

const mockResponse: Employee360Response = {
  profile: {
    id: 1,
    name: 'John Doe',
    email: 'john@example.com',
    jobTitle: 'Engineer',
    managerName: 'Jane Manager',
  },
  interactions: [
    {
      id: 10,
      type: 'CHECK_IN',
      occurredAt: '2024-12-15T10:00:00Z',
      conductedByName: 'Jane',
      notes: 'Short notes',
      projectContext: { projectName: 'Alpha', companyName: 'Acme' },
    },
  ],
  openTasks: [
    { id: 20, title: 'Overdue task', dueDate: '2020-01-01', assignedUserName: 'John' },
    { id: 21, title: 'Future task', dueDate: '2099-12-31', assignedUserName: 'John' },
  ],
};

describe('Employee360Component', () => {
  let fixture: ComponentFixture<Employee360Component>;

  beforeEach(async () => {
    mockEmployee360Service.getEmployee360.mockReset();
    mockEngagementService.getMatrix.mockReset();
    mockEngagementService.getMatrix.mockReturnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [Employee360Component],
      providers: [
        provideRouter([]),
        { provide: Employee360Service, useValue: mockEmployee360Service },
        { provide: EngagementService, useValue: mockEngagementService },
        { provide: TaskService, useValue: mockTaskService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: ActivatedRoute, useValue: mockActivatedRoute },
      ],
    }).compileComponents();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(Employee360Component);
  }

  describe('profile rendering', () => {
    it('should render profile summary when data loads', () => {
      mockEmployee360Service.getEmployee360.mockReturnValue(of(mockResponse));
      createComponent();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const profileSummary = el.querySelector('[data-testid="profile-summary"]');
      expect(profileSummary).toBeTruthy();
      expect(profileSummary!.textContent).toContain('John Doe');
    });
  });

  describe('loading state', () => {
    it('should show loading indicator while fetching', () => {
      mockEmployee360Service.getEmployee360.mockReturnValue(NEVER);
      createComponent();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const loadingIndicator = el.querySelector('[data-testid="loading-indicator"]');
      expect(loadingIndicator).toBeTruthy();
    });
  });

  describe('error state', () => {
    it('should display error message on API failure', () => {
      mockEmployee360Service.getEmployee360.mockReturnValue(throwError(() => ({ status: 500 })));
      createComponent();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const errorMessage = el.querySelector('[data-testid="error-message"]');
      expect(errorMessage).toBeTruthy();
      expect(errorMessage!.textContent).toContain('error');

      const retryButton = el.querySelector('.retry-button');
      expect(retryButton).toBeTruthy();
    });

    it('should re-fetch data when retry button is clicked', () => {
      mockEmployee360Service.getEmployee360.mockReturnValue(throwError(() => ({ status: 500 })));
      createComponent();
      fixture.detectChanges();

      expect(mockEmployee360Service.getEmployee360).toHaveBeenCalledTimes(1);

      mockEmployee360Service.getEmployee360.mockReturnValue(of(mockResponse));
      const el = fixture.nativeElement as HTMLElement;
      const retryButton = el.querySelector('.retry-button') as HTMLButtonElement;
      retryButton.click();
      fixture.detectChanges();

      expect(mockEmployee360Service.getEmployee360).toHaveBeenCalledTimes(2);
      const profileSummary = el.querySelector('[data-testid="profile-summary"]');
      expect(profileSummary).toBeTruthy();
    });
  });

  describe('overdue tasks', () => {
    it('should apply .overdue class to tasks with past due dates', () => {
      mockEmployee360Service.getEmployee360.mockReturnValue(of(mockResponse));
      createComponent();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const taskRows = el.querySelectorAll('[data-testid="task-row"]');
      expect(taskRows.length).toBe(2);

      // First task has dueDate '2020-01-01' which is overdue
      expect(taskRows[0].classList.contains('overdue')).toBe(true);
      // Second task has dueDate '2099-12-31' which is in the future
      expect(taskRows[1].classList.contains('overdue')).toBe(false);
    });
  });

  describe('notes truncation', () => {
    it('should truncate notes longer than 200 characters with ellipsis', () => {
      const longNotes = 'A'.repeat(250);
      const responseWithLongNotes: Employee360Response = {
        ...mockResponse,
        interactions: [
          {
            id: 10,
            type: 'CHECK_IN',
            occurredAt: '2024-12-15T10:00:00Z',
            conductedByName: 'Jane',
            notes: longNotes,
            projectContext: null,
          },
        ],
      };
      mockEmployee360Service.getEmployee360.mockReturnValue(of(responseWithLongNotes));
      createComponent();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const notesEl = el.querySelector('.interaction-notes');
      expect(notesEl).toBeTruthy();
      // Should be 200 chars + ellipsis character (…)
      expect(notesEl!.textContent!.length).toBe(201);
      expect(notesEl!.textContent!.endsWith('\u2026')).toBe(true);
    });
  });

  describe('empty states', () => {
    it('should display empty-state message when no interactions exist', () => {
      const emptyResponse: Employee360Response = {
        ...mockResponse,
        interactions: [],
        openTasks: [],
      };
      mockEmployee360Service.getEmployee360.mockReturnValue(of(emptyResponse));
      createComponent();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const emptyInteractions = el.querySelector('[data-testid="empty-interactions"]');
      expect(emptyInteractions).toBeTruthy();
      expect(emptyInteractions!.textContent).toContain('No interaction history');
    });

    it('should display empty-state message when no tasks exist', () => {
      const emptyResponse: Employee360Response = {
        ...mockResponse,
        interactions: [],
        openTasks: [],
      };
      mockEmployee360Service.getEmployee360.mockReturnValue(of(emptyResponse));
      createComponent();
      fixture.detectChanges();

      const el = fixture.nativeElement as HTMLElement;
      const emptyTasks = el.querySelector('[data-testid="empty-tasks"]');
      expect(emptyTasks).toBeTruthy();
      expect(emptyTasks!.textContent).toContain('No open tasks');
    });
  });
});
