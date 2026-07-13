import '@angular/compiler';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import * as fc from 'fast-check';
import { SchedulingService } from './scheduling.service';
import {
  CompletionStatus,
  CreateScheduledInteractionRequest,
  InteractionType,
} from '../models/scheduled-interaction.model';

// Feature: interaction-scheduling, Property 10: Frontend Service HTTP Method Correctness
// **Validates: Requirements 8.2, 8.3, 8.4**

/**
 * Arbitrary that generates a valid InteractionType value.
 */
function arbInteractionType(): fc.Arbitrary<InteractionType> {
  return fc.constantFrom<InteractionType>('CHECK_IN', 'MENTORING', 'CATCH_UP', 'OTHER');
}

/**
 * Arbitrary that generates a valid CompletionStatus value.
 */
function arbCompletionStatus(): fc.Arbitrary<CompletionStatus> {
  return fc.constantFrom<CompletionStatus>('PENDING', 'COMPLETED', 'CANCELLED');
}

/**
 * Arbitrary that generates a valid date string in yyyy-MM-dd format.
 */
function arbDateString(): fc.Arbitrary<string> {
  return fc
    .record({
      year: fc.integer({ min: 2020, max: 2099 }),
      month: fc.integer({ min: 1, max: 12 }),
      day: fc.integer({ min: 1, max: 28 }),
    })
    .map(({ year, month, day }) => {
      const y = year.toString();
      const m = month.toString().padStart(2, '0');
      const d = day.toString().padStart(2, '0');
      return `${y}-${m}-${d}`;
    });
}

/**
 * Arbitrary that generates a valid CreateScheduledInteractionRequest.
 */
function arbCreateRequest(): fc.Arbitrary<CreateScheduledInteractionRequest> {
  return fc.record({
    employeeId: fc.integer({ min: 1, max: 10000 }),
    scheduledDate: arbDateString(),
    interactionType: arbInteractionType(),
    notes: fc.option(fc.string({ minLength: 0, maxLength: 200 }), { nil: undefined }),
  });
}

/**
 * Arbitrary that generates a valid positive integer ID for update operations.
 */
function arbId(): fc.Arbitrary<number> {
  return fc.integer({ min: 1, max: 100000 });
}

/**
 * Arbitrary that generates a valid update body for PATCH requests.
 */
function arbUpdateBody(): fc.Arbitrary<{
  completionStatus?: CompletionStatus;
  scheduledDate?: string;
  notes?: string;
}> {
  return fc.record(
    {
      completionStatus: fc.option(arbCompletionStatus(), { nil: undefined }),
      scheduledDate: fc.option(arbDateString(), { nil: undefined }),
      notes: fc.option(fc.string({ minLength: 0, maxLength: 200 }), { nil: undefined }),
    },
    { requiredKeys: [] },
  );
}

/**
 * Arbitrary that generates optional list filter parameters.
 */
function arbListParams(): fc.Arbitrary<{
  status?: CompletionStatus;
  employeeId?: number;
  overdue?: boolean;
}> {
  return fc.record(
    {
      status: fc.option(arbCompletionStatus(), { nil: undefined }),
      employeeId: fc.option(fc.integer({ min: 1, max: 10000 }), { nil: undefined }),
      overdue: fc.option(fc.boolean(), { nil: undefined }),
    },
    { requiredKeys: [] },
  );
}

describe('Frontend Service HTTP Method Correctness - Property 10', () => {
  let service: SchedulingService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SchedulingService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('create() issues POST to /api/scheduled-interactions with request body', () => {
    fc.assert(
      fc.property(arbCreateRequest(), (request: CreateScheduledInteractionRequest) => {
        service.create(request).subscribe();

        const req = httpTesting.expectOne('/api/scheduled-interactions');
        expect(req.request.method).toBe('POST');
        expect(req.request.url).toBe('/api/scheduled-interactions');
        expect(req.request.body).toEqual(request);

        req.flush({ id: 1, ...request, completionStatus: 'PENDING', overdue: false });
      }),
      { numRuns: 100 },
    );
  });

  it('list() issues GET to /api/scheduled-interactions with correct query params', () => {
    fc.assert(
      fc.property(arbListParams(), (params) => {
        service.list(params).subscribe();

        const req = httpTesting.expectOne((r) => r.url === '/api/scheduled-interactions');
        expect(req.request.method).toBe('GET');
        expect(req.request.url).toBe('/api/scheduled-interactions');

        // Verify query parameters match inputs
        if (params.status !== undefined) {
          expect(req.request.params.get('status')).toBe(params.status);
        } else {
          expect(req.request.params.has('status')).toBe(false);
        }

        if (params.employeeId !== undefined) {
          expect(req.request.params.get('employeeId')).toBe(params.employeeId.toString());
        } else {
          expect(req.request.params.has('employeeId')).toBe(false);
        }

        if (params.overdue !== undefined && params.overdue) {
          expect(req.request.params.get('overdue')).toBe('true');
        } else {
          expect(req.request.params.has('overdue')).toBe(false);
        }

        req.flush([]);
      }),
      { numRuns: 100 },
    );
  });

  it('update() issues PATCH to /api/scheduled-interactions/{id} with body', () => {
    fc.assert(
      fc.property(arbId(), arbUpdateBody(), (id: number, body) => {
        service.update(id, body).subscribe();

        const expectedUrl = `/api/scheduled-interactions/${id}`;
        const req = httpTesting.expectOne(expectedUrl);
        expect(req.request.method).toBe('PATCH');
        expect(req.request.url).toBe(expectedUrl);
        expect(req.request.body).toEqual(body);

        req.flush({ id, completionStatus: 'COMPLETED', overdue: false });
      }),
      { numRuns: 100 },
    );
  });
});
