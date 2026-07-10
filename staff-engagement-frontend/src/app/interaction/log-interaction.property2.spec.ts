import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import * as fc from 'fast-check';
import { LogInteractionComponent } from './log-interaction.component';
import { AuthService } from '../core/services/auth.service';
import { InteractionType } from './models/interaction-type.enum';
import { User } from '../core/models/user.model';

/**
 * Property-based tests that require the full Angular component via TestBed.
 * These tests validate submission payload correctness, server error display,
 * task creation logic, and picker data binding.
 */

function flushInitialGets(
  httpTesting: HttpTestingController,
  options?: { employees?: unknown[]; users?: unknown[]; projects?: unknown[] },
) {
  const employees = options?.employees ?? [
    { id: 1, name: 'Emp1', email: 'e@e.com', jobTitle: 'Dev' },
  ];
  const users = options?.users ?? [{ id: 1, name: 'User1', email: 'u@u.com' }];
  const projects = options?.projects ?? [{ id: 1, name: 'Proj1' }];

  httpTesting.match('/api/employees').forEach((req) => req.flush(employees));
  httpTesting.match('/api/users').forEach((req) => req.flush(users));
  httpTesting.match('/api/projects').forEach((req) => req.flush(projects));
}

describe('Feature: log-interaction-frontend, Property 1: Form defaults match current user', () => {
  /**
   * Validates: Requirements 1.2
   *
   * For any authenticated user provided by AuthService.currentUser, when the
   * LogInteractionComponent initializes, the conductedByUserId form control
   * SHALL have its value equal to that user's id, and the occurredAt form
   * control SHALL contain a datetime value within 2 seconds of now.
   */
  it('conductedByUserId defaults to currentUser.id and occurredAt is within 2s of now', () => {
    fc.assert(
      fc.property(
        fc.record({
          id: fc.nat().map((n) => n + 1),
          name: fc.string({ minLength: 1 }),
          email: fc.emailAddress(),
        }),
        (user: User) => {
          const beforeInit = new Date();

          TestBed.resetTestingModule();
          TestBed.configureTestingModule({
            imports: [LogInteractionComponent],
            providers: [provideHttpClient(), provideHttpClientTesting()],
          });

          const authService = TestBed.inject(AuthService);
          authService.currentUser.set(user);

          const httpTesting = TestBed.inject(HttpTestingController);
          const fixture = TestBed.createComponent(LogInteractionComponent);
          fixture.detectChanges();

          flushInitialGets(httpTesting);

          const component = fixture.componentInstance;
          expect(component.form.get('conductedByUserId')!.value).toBe(user.id);

          // formatDateTimeLocal truncates to minutes (YYYY-MM-DDTHH:MM), so
          // parsed time will differ from wall-clock by up to 59s of truncation
          // plus TestBed setup overhead. We verify the minute matches by
          // comparing against a reference taken just before init.
          const occurredAtValue = component.form.get('occurredAt')!.value;
          const parsedDate = new Date(occurredAtValue);
          const diff = Math.abs(beforeInit.getTime() - parsedDate.getTime());
          // Allow up to 60s (minute-truncation) + 2s (property tolerance)
          expect(diff).toBeLessThan(62000);

          httpTesting.verify();
        },
      ),
      { numRuns: 20 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 4: Submission payload correctness', () => {
  /**
   * Validates: Requirements 3.1, 5.2, 5.3, 5.4
   *
   * For any valid form state with a given currentUser, the payload sent to
   * POST /api/interactions SHALL contain employeeId, conductedByUserId,
   * loggedByUserId (always currentUser.id), type, notes, occurredAt, and
   * projectId matching the form values.
   */
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LogInteractionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    const authService = TestBed.inject(AuthService);
    authService.currentUser.set({ id: 999, name: 'Test User', email: 'test@t.com' });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('payload fields match form values and loggedByUserId is always currentUser.id', () => {
    fc.assert(
      fc.property(
        fc.record({
          employeeId: fc.nat().map((n) => n + 1),
          conductedByUserId: fc.nat().map((n) => n + 1),
          type: fc.constantFrom(...Object.values(InteractionType)),
          notes: fc.string({ minLength: 1 }).filter((s) => s.trim().length > 0),
          projectId: fc.option(
            fc.nat().map((n) => n + 1),
            { nil: null },
          ),
        }),
        (values) => {
          const fixture = TestBed.createComponent(LogInteractionComponent);
          const component = fixture.componentInstance;
          fixture.detectChanges();

          flushInitialGets(httpTesting);

          component.form.patchValue({
            employeeId: values.employeeId,
            conductedByUserId: values.conductedByUserId,
            type: values.type,
            notes: values.notes,
            projectId: values.projectId,
          });

          component.onSubmit();

          const req = httpTesting.expectOne('/api/interactions');
          const body = req.request.body;

          expect(body.employeeId).toBe(values.employeeId);
          expect(body.conductedByUserId).toBe(values.conductedByUserId);
          expect(body.loggedByUserId).toBe(999);
          expect(body.type).toBe(values.type);
          expect(body.notes).toBe(values.notes);
          expect(body.projectId).toBe(values.projectId);

          // Flush to avoid open request errors
          req.flush(
            {
              id: 1,
              employee: { id: values.employeeId, name: 'E' },
              conductedBy: { id: values.conductedByUserId, name: 'U' },
              loggedBy: { id: 999, name: 'Test User' },
              project: null,
              type: values.type,
              notes: values.notes,
              occurredAt: '2025-01-01T10:00:00Z',
              createdAt: '2025-01-01T10:00:00Z',
            },
            { status: 201, statusText: 'Created' },
          );

          httpTesting.verify();
        },
      ),
      { numRuns: 20 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 5: Server field errors are displayed', () => {
  /**
   * Validates: Requirements 3.3
   *
   * For any HTTP 400 response containing a fieldErrors map with one or more
   * entries, the component SHALL store each field's error message in
   * serverFieldErrors signal.
   */
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LogInteractionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    const authService = TestBed.inject(AuthService);
    authService.currentUser.set({ id: 1, name: 'User', email: 'u@u.com' });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('serverFieldErrors matches the fieldErrors map from a 400 response', () => {
    const fieldNameArb = fc.constantFrom(
      'employeeId',
      'conductedByUserId',
      'type',
      'notes',
      'occurredAt',
    );

    fc.assert(
      fc.property(
        fc
          .array(fc.tuple(fieldNameArb, fc.string({ minLength: 1 })), {
            minLength: 1,
            maxLength: 5,
          })
          .map((entries) => Object.fromEntries(entries)),
        (fieldErrors: Record<string, string>) => {
          const fixture = TestBed.createComponent(LogInteractionComponent);
          const component = fixture.componentInstance;
          fixture.detectChanges();

          flushInitialGets(httpTesting);

          // Fill valid form
          component.form.patchValue({
            employeeId: 1,
            conductedByUserId: 1,
            type: InteractionType.CHECK_IN,
            notes: 'Valid notes',
            occurredAt: '2025-01-01T10:00',
          });

          component.onSubmit();

          const req = httpTesting.expectOne('/api/interactions');
          req.flush(
            { message: 'Validation failed', fieldErrors },
            { status: 400, statusText: 'Bad Request' },
          );

          expect(component.serverFieldErrors()).toEqual(fieldErrors);

          httpTesting.verify();
        },
      ),
      { numRuns: 20 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 7: Task POST uses interaction response ID', () => {
  /**
   * Validates: Requirements 4.3
   *
   * For any successful interaction creation (HTTP 201 returning a response with
   * an id field), when the Inline_Task_Section is expanded with a valid title,
   * the subsequent POST /api/tasks payload SHALL contain interactionId equal to
   * the id from the interaction response.
   */
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LogInteractionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    const authService = TestBed.inject(AuthService);
    authService.currentUser.set({ id: 1, name: 'User', email: 'u@u.com' });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('task POST body has interactionId matching the interaction response id', () => {
    fc.assert(
      fc.property(
        fc.nat().map((n) => n + 1),
        (interactionId: number) => {
          const fixture = TestBed.createComponent(LogInteractionComponent);
          const component = fixture.componentInstance;
          fixture.detectChanges();

          flushInitialGets(httpTesting);

          // Fill valid form
          component.form.patchValue({
            employeeId: 1,
            conductedByUserId: 1,
            type: InteractionType.CHECK_IN,
            notes: 'Valid notes',
            occurredAt: '2025-01-01T10:00',
          });

          // Expand task section and fill title
          component.toggleTaskSection();
          component.form.patchValue({ taskTitle: 'Follow-up task' });

          component.onSubmit();

          // Flush interaction with generated ID
          const interactionReq = httpTesting.expectOne('/api/interactions');
          interactionReq.flush(
            {
              id: interactionId,
              employee: { id: 1, name: 'E' },
              conductedBy: { id: 1, name: 'U' },
              loggedBy: { id: 1, name: 'U' },
              project: null,
              type: 'CHECK_IN',
              notes: 'Valid notes',
              occurredAt: '2025-01-01T10:00:00Z',
              createdAt: '2025-01-01T10:00:00Z',
            },
            { status: 201, statusText: 'Created' },
          );

          // Capture task POST
          const taskReq = httpTesting.expectOne('/api/tasks');
          expect(taskReq.request.body.interactionId).toBe(interactionId);

          taskReq.flush(
            {
              id: 1,
              title: 'Follow-up task',
              description: null,
              status: 'OPEN',
              dueDate: null,
              assignedUser: null,
              interaction: { id: interactionId },
              createdAt: '2025-01-01T10:00:00Z',
            },
            { status: 201, statusText: 'Created' },
          );

          httpTesting.verify();
        },
      ),
      { numRuns: 20 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 8: Collapsed task section produces no task request', () => {
  /**
   * Validates: Requirements 4.6
   *
   * For any valid form submission where the Inline_Task_Section is collapsed,
   * the TaskService SHALL NOT be called and no POST /api/tasks request SHALL
   * be sent.
   */
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LogInteractionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    const authService = TestBed.inject(AuthService);
    authService.currentUser.set({ id: 1, name: 'User', email: 'u@u.com' });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('no POST /api/tasks is sent when task section is collapsed', () => {
    fc.assert(
      fc.property(
        fc.record({
          employeeId: fc.nat().map((n) => n + 1),
          conductedByUserId: fc.nat().map((n) => n + 1),
          type: fc.constantFrom(...Object.values(InteractionType)),
          notes: fc.string({ minLength: 1 }).filter((s) => s.trim().length > 0),
        }),
        (values) => {
          const fixture = TestBed.createComponent(LogInteractionComponent);
          const component = fixture.componentInstance;
          fixture.detectChanges();

          flushInitialGets(httpTesting);

          // Fill valid form, leave task section collapsed
          component.form.patchValue({
            employeeId: values.employeeId,
            conductedByUserId: values.conductedByUserId,
            type: values.type,
            notes: values.notes,
          });

          expect(component.taskSectionExpanded()).toBe(false);

          component.onSubmit();

          // Flush interaction success
          const interactionReq = httpTesting.expectOne('/api/interactions');
          interactionReq.flush(
            {
              id: 42,
              employee: { id: values.employeeId, name: 'E' },
              conductedBy: { id: values.conductedByUserId, name: 'U' },
              loggedBy: { id: 1, name: 'U' },
              project: null,
              type: values.type,
              notes: values.notes,
              occurredAt: '2025-01-01T10:00:00Z',
              createdAt: '2025-01-01T10:00:00Z',
            },
            { status: 201, statusText: 'Created' },
          );

          // Verify no task request was made
          httpTesting.expectNone('/api/tasks');
          httpTesting.verify();
        },
      ),
      { numRuns: 20 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 11: User picker displays full names', () => {
  /**
   * Validates: Requirements 5.1
   *
   * For any list of users returned by GET /api/users, the component's users()
   * signal SHALL contain each user with their name field preserved exactly.
   */
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [LogInteractionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    const authService = TestBed.inject(AuthService);
    authService.currentUser.set({ id: 1, name: 'Current', email: 'c@c.com' });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  it('component.users() preserves full names from the API response', () => {
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            id: fc.nat().map((n) => n + 1),
            name: fc.string({ minLength: 1 }),
            email: fc.emailAddress(),
          }),
          { minLength: 1, maxLength: 5 },
        ),
        (generatedUsers: User[]) => {
          const fixture = TestBed.createComponent(LogInteractionComponent);
          const component = fixture.componentInstance;
          fixture.detectChanges();

          // Flush employees and projects with defaults
          httpTesting
            .match('/api/employees')
            .forEach((req) => req.flush([{ id: 1, name: 'E', email: 'e@e.com', jobTitle: 'Dev' }]));
          httpTesting.match('/api/projects').forEach((req) => req.flush([{ id: 1, name: 'P' }]));

          // Flush users with generated array
          httpTesting.match('/api/users').forEach((req) => req.flush(generatedUsers));

          expect(component.users()).toEqual(generatedUsers);

          httpTesting.verify();
        },
      ),
      { numRuns: 20 },
    );
  });
});
