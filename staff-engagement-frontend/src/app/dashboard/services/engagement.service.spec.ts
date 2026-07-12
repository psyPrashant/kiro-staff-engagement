import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import * as fc from 'fast-check';
import { EngagementService } from './engagement.service';
import { EngagementStatus, MatrixEntry, SortOption } from '../models/engagement.model';

describe('EngagementService', () => {
  let service: EngagementService;
  let httpTesting: HttpTestingController;

  const mockEntries: MatrixEntry[] = [
    {
      employeeId: 1,
      employeeName: 'Alice Smith',
      employeeEmail: 'alice@example.com',
      recency: 5,
      frequency: 12,
      lastInteractionDate: '2024-06-01T10:00:00Z',
      engagementStatus: 'ON_TRACK',
      followUpRequired: false,
    },
    {
      employeeId: 2,
      employeeName: 'Bob Jones',
      employeeEmail: 'bob@example.com',
      recency: 30,
      frequency: 2,
      lastInteractionDate: '2024-05-01T10:00:00Z',
      engagementStatus: 'OVERDUE',
      followUpRequired: true,
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EngagementService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET /api/engagement/matrix with no params when called without arguments', () => {
    let result: MatrixEntry[] | undefined;
    service.getMatrix().subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/engagement/matrix');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush(mockEntries);

    expect(result).toEqual(mockEntries);
  });

  it('should GET with ?status=OVERDUE when status filter provided', () => {
    let result: MatrixEntry[] | undefined;
    service.getMatrix({ status: 'OVERDUE' }).subscribe((res) => (result = res));

    const req = httpTesting.expectOne(
      (r) => r.url === '/api/engagement/matrix' && r.params.get('status') === 'OVERDUE',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('status')).toBe('OVERDUE');
    expect(req.request.params.has('sort')).toBe(false);
    req.flush(mockEntries);

    expect(result).toEqual(mockEntries);
  });

  it('should GET with ?sort=recency when sort provided', () => {
    let result: MatrixEntry[] | undefined;
    service.getMatrix({ sort: 'recency' }).subscribe((res) => (result = res));

    const req = httpTesting.expectOne(
      (r) => r.url === '/api/engagement/matrix' && r.params.get('sort') === 'recency',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('sort')).toBe('recency');
    expect(req.request.params.has('status')).toBe(false);
    req.flush(mockEntries);

    expect(result).toEqual(mockEntries);
  });

  it('should GET with ?status=AT_RISK&sort=recency when both provided', () => {
    let result: MatrixEntry[] | undefined;
    service
      .getMatrix({ status: 'AT_RISK', sort: 'recency' })
      .subscribe((res) => (result = res));

    const req = httpTesting.expectOne(
      (r) =>
        r.url === '/api/engagement/matrix' &&
        r.params.get('status') === 'AT_RISK' &&
        r.params.get('sort') === 'recency',
    );
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('status')).toBe('AT_RISK');
    expect(req.request.params.get('sort')).toBe('recency');
    req.flush(mockEntries);

    expect(result).toEqual(mockEntries);
  });
});

// Feature: dashboard-interaction-matrix, Property 1: Service URL construction
describe('EngagementService - Property Tests', () => {
  let service: EngagementService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EngagementService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  /**
   * Property 1: Service URL construction
   *
   * For any combination of optional status (OVERDUE, AT_RISK, ON_TRACK, or absent)
   * and optional sort (name, recency, or absent), the EngagementService.getMatrix()
   * method SHALL construct a request URL equal to /api/engagement/matrix with only
   * the provided parameters appended as query string key-value pairs.
   *
   * Validates: Requirements 1.2, 1.3, 1.4, 6.5
   */
  it('should construct correct URL for any combination of status and sort params', () => {
    fc.assert(
      fc.property(
        fc.record({
          status: fc.constantFrom(
            'OVERDUE' as EngagementStatus,
            'AT_RISK' as EngagementStatus,
            'ON_TRACK' as EngagementStatus,
            undefined as EngagementStatus | undefined,
          ),
          sort: fc.constantFrom(
            'name' as SortOption,
            'recency' as SortOption,
            undefined as SortOption | undefined,
          ),
        }),
        ({ status, sort }) => {
          const params: { status?: EngagementStatus; sort?: SortOption } = {};
          if (status !== undefined) params.status = status;
          if (sort !== undefined) params.sort = sort;

          service.getMatrix(Object.keys(params).length > 0 ? params : undefined).subscribe();

          const req = httpTesting.expectOne((r) => r.url === '/api/engagement/matrix');

          // Verify base URL
          expect(req.request.url).toBe('/api/engagement/matrix');

          // Verify status param
          if (status !== undefined) {
            expect(req.request.params.get('status')).toBe(status);
          } else {
            expect(req.request.params.has('status')).toBe(false);
          }

          // Verify sort param
          if (sort !== undefined) {
            expect(req.request.params.get('sort')).toBe(sort);
          } else {
            expect(req.request.params.has('sort')).toBe(false);
          }

          // Verify no extra params beyond status and sort
          const allKeys = req.request.params.keys();
          const expectedKeys: string[] = [];
          if (status !== undefined) expectedKeys.push('status');
          if (sort !== undefined) expectedKeys.push('sort');
          expect(allKeys.sort()).toEqual(expectedKeys.sort());

          req.flush([]);
        },
      ),
      { numRuns: 100 },
    );
  });
});
