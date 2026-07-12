import '@angular/compiler';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { LogInteractionComponent, formatDateTimeLocal } from './log-interaction.component';
import { AuthService } from '../core/services/auth.service';
import { InteractionType } from './models/interaction-type.enum';

describe('LogInteractionComponent - Form Validation', () => {
  let component: LogInteractionComponent;
  let fixture: ComponentFixture<LogInteractionComponent>;
  let httpMock: HttpTestingController;

  const mockCurrentUser = { id: 1, name: 'Test User', email: 'test@example.com' };

  beforeEach(async () => {
    const mockAuthService = {
      currentUser: signal(mockCurrentUser),
    };

    await TestBed.configureTestingModule({
      imports: [LogInteractionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogInteractionComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();

    // Flush the 3 HTTP requests that fire on ngOnInit
    httpMock.expectOne('/api/employees').flush([]);
    httpMock.expectOne('/api/users').flush([]);
    httpMock.expectOne('/api/projects').flush([]);
  });

  describe('required field validation errors', () => {
    it('should show validation error when Employee is empty on submit attempt', () => {
      component.form.patchValue({
        employeeId: null,
        conductedByUserId: 1,
        type: 'CHECK_IN',
        notes: 'Some notes',
        occurredAt: '2025-01-01T10:00',
      });

      component.onSubmit();
      fixture.detectChanges();

      expect(component.form.get('employeeId')!.hasError('required')).toBe(true);
    });

    it('should show validation error when Conducted By is empty on submit attempt', () => {
      component.form.patchValue({
        employeeId: 1,
        conductedByUserId: null,
        type: 'CHECK_IN',
        notes: 'Some notes',
        occurredAt: '2025-01-01T10:00',
      });

      component.onSubmit();
      fixture.detectChanges();

      expect(component.form.get('conductedByUserId')!.hasError('required')).toBe(true);
    });

    it('should show validation error when Type is empty on submit attempt', () => {
      component.form.patchValue({
        employeeId: 1,
        conductedByUserId: 1,
        type: null,
        notes: 'Some notes',
        occurredAt: '2025-01-01T10:00',
      });

      component.onSubmit();
      fixture.detectChanges();

      expect(component.form.get('type')!.hasError('required')).toBe(true);
    });

    it('should show validation error when Notes is empty on submit attempt', () => {
      component.form.patchValue({
        employeeId: 1,
        conductedByUserId: 1,
        type: 'CHECK_IN',
        notes: '',
        occurredAt: '2025-01-01T10:00',
      });

      component.onSubmit();
      fixture.detectChanges();

      expect(component.form.get('notes')!.hasError('required')).toBe(true);
    });

    it('should show validation error when Occurred At is empty on submit attempt', () => {
      component.form.patchValue({
        employeeId: 1,
        conductedByUserId: 1,
        type: 'CHECK_IN',
        notes: 'Some notes',
        occurredAt: '',
      });

      component.onSubmit();
      fixture.detectChanges();

      expect(component.form.get('occurredAt')!.hasError('required')).toBe(true);
    });
  });

  describe('submit button enabled state', () => {
    it('should enable submit button when all required fields have valid values', () => {
      component.form.patchValue({
        employeeId: 1,
        conductedByUserId: 1,
        type: 'CHECK_IN',
        notes: 'Valid notes content',
        occurredAt: '2025-01-15T09:30',
      });

      expect(component.form.valid).toBe(true);
    });
  });

  describe('form defaults', () => {
    it('should default conductedByUserId to current user id', () => {
      expect(component.form.get('conductedByUserId')!.value).toBe(mockCurrentUser.id);
    });

    it('should default occurredAt to approximately current date/time', () => {
      const formValue = component.form.get('occurredAt')!.value as string;
      expect(formValue).toBeTruthy();

      // The value should be in datetime-local format: YYYY-MM-DDTHH:MM
      const dateTimeLocalRegex = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/;
      expect(formValue).toMatch(dateTimeLocalRegex);

      // Verify date components match today
      const formDate = new Date(formValue);
      const now = new Date();
      expect(formDate.getFullYear()).toBe(now.getFullYear());
      expect(formDate.getMonth()).toBe(now.getMonth());
      expect(formDate.getDate()).toBe(now.getDate());
    });
  });

  describe('Inline Task Section', () => {
    it('should show task sub-form when task section is expanded', () => {
      component.toggleTaskSection();

      expect(component.taskSectionExpanded()).toBe(true);

      const taskTitleControl = component.form.get('taskTitle')!;
      expect(taskTitleControl.hasError('required')).toBe(true);
    });

    it('should hide task sub-form when task section is collapsed', () => {
      // Expand first
      component.toggleTaskSection();
      expect(component.taskSectionExpanded()).toBe(true);

      // Then collapse
      component.toggleTaskSection();
      expect(component.taskSectionExpanded()).toBe(false);

      const taskTitleControl = component.form.get('taskTitle')!;
      const taskDueDateControl = component.form.get('taskDueDate')!;

      // After collapsing, validators should be cleared
      expect(taskTitleControl.validator).toBeNull();
      expect(taskDueDateControl.validator).toBeNull();
    });

    it('should show validation error for empty task title when section is expanded', () => {
      component.toggleTaskSection();

      // Leave title empty, attempt submit
      component.onSubmit();

      const taskTitleControl = component.form.get('taskTitle')!;
      expect(taskTitleControl.hasError('required') || taskTitleControl.hasError('notBlank')).toBe(
        true,
      );
    });

    it('should not produce task validation errors when section is collapsed', () => {
      // Leave task section collapsed, fill all required interaction fields
      component.form.patchValue({
        employeeId: 1,
        conductedByUserId: 1,
        type: 'CHECK_IN',
        notes: 'Valid notes content',
        occurredAt: '2025-01-15T10:00',
      });
      component.form.updateValueAndValidity();

      expect(component.form.valid).toBe(true);
    });
  });
});

describe('LogInteractionComponent - Submission Flows', () => {
  let component: LogInteractionComponent;
  let fixture: ComponentFixture<LogInteractionComponent>;
  let httpMock: HttpTestingController;

  const mockCurrentUser = { id: 1, name: 'Test User', email: 'test@example.com' };

  const interactionSuccessResponse = {
    id: 42,
    employee: { id: 1, name: 'Test' },
    conductedBy: { id: 1, name: 'User' },
    loggedBy: { id: 1, name: 'User' },
    project: null,
    type: 'CHECK_IN',
    notes: 'Test notes',
    occurredAt: '2024-01-01T00:00:00Z',
    createdAt: '2024-01-01T00:00:00Z',
  };

  function flushInitialGets(): void {
    httpMock
      .expectOne('/api/employees')
      .flush([{ id: 1, name: 'Employee 1', email: 'e@e.com', jobTitle: 'Dev' }]);
    httpMock
      .expectOne('/api/users')
      .flush([{ id: 1, name: 'Test User', email: 'test@example.com' }]);
    httpMock.expectOne('/api/projects').flush([{ id: 1, name: 'Project 1' }]);
  }

  function fillRequiredFields(): void {
    component.form.patchValue({
      employeeId: 1,
      conductedByUserId: 1,
      type: InteractionType.CHECK_IN,
      notes: 'Test notes',
      occurredAt: formatDateTimeLocal(new Date()),
    });
    component.form.updateValueAndValidity();
  }

  beforeEach(async () => {
    const mockAuthService = {
      currentUser: signal(mockCurrentUser),
    };

    await TestBed.configureTestingModule({
      imports: [LogInteractionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LogInteractionComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    fixture.detectChanges();
    flushInitialGets();
  });

  it('should show success notification and reset form on successful submission', () => {
    fillRequiredFields();

    component.onSubmit();

    const req = httpMock.expectOne('/api/interactions');
    expect(req.request.method).toBe('POST');
    req.flush(interactionSuccessResponse, { status: 201, statusText: 'Created' });

    expect(component.successMessage()).toBe('Interaction created successfully.');
    expect(component.form.get('employeeId')!.value).toBeNull();
    expect(component.form.get('notes')!.value).toBeNull();
    expect(component.form.get('type')!.value).toBeNull();
    expect(component.taskSectionExpanded()).toBe(false);
    expect(component.submitting()).toBe(false);
  });

  it('should display field errors from 400 response', () => {
    fillRequiredFields();

    component.onSubmit();

    const req = httpMock.expectOne('/api/interactions');
    req.flush(
      { message: 'Validation failed', fieldErrors: { notes: 'Too short' } },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(component.errorMessage()).toBe('Validation failed');
    expect(component.serverFieldErrors()).toEqual({ notes: 'Too short' });
    expect(component.submitting()).toBe(false);
  });

  it('should display generic error and re-enable button on network/5xx error', () => {
    fillRequiredFields();

    component.onSubmit();

    const req = httpMock.expectOne('/api/interactions');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

    expect(component.errorMessage()).toBe('Request failed. Please try again.');
    expect(component.submitting()).toBe(false);
  });

  it('should show both interaction success and task error on partial failure', () => {
    fillRequiredFields();

    // Expand task section and fill task title
    component.toggleTaskSection();
    component.form.patchValue({ taskTitle: 'Follow-up task' });
    component.form.updateValueAndValidity();

    component.onSubmit();

    // Flush the interaction POST with success
    const interactionReq = httpMock.expectOne('/api/interactions');
    interactionReq.flush(interactionSuccessResponse, { status: 201, statusText: 'Created' });

    // Error the task POST
    const taskReq = httpMock.expectOne('/api/tasks');
    taskReq.flush(
      { message: 'Task validation failed' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(component.successMessage()).toBe('Interaction created successfully.');
    expect(component.taskErrorMessage()).toContain('Failed to create follow-up task');
    expect(component.submitting()).toBe(false);
  });

  it('should not attempt task POST when interaction fails', () => {
    fillRequiredFields();

    // Expand task section and fill task title
    component.toggleTaskSection();
    component.form.patchValue({ taskTitle: 'Follow-up task' });
    component.form.updateValueAndValidity();

    component.onSubmit();

    // Error the interaction POST
    const interactionReq = httpMock.expectOne('/api/interactions');
    interactionReq.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

    // Verify no task POST was made
    httpMock.expectNone('/api/tasks');

    expect(component.errorMessage()).toBe('Request failed. Please try again.');
    expect(component.submitting()).toBe(false);
  });
});
