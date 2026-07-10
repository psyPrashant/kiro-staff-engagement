import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { InteractionService } from './interaction.service';
import { CreateInteractionRequest, InteractionResponse } from '../models/interaction.model';
import { InteractionType } from '../models/interaction-type.enum';

describe('InteractionService', () => {
  let service: InteractionService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InteractionService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST to /api/interactions and return the response', () => {
    const request: CreateInteractionRequest = {
      employeeId: 1,
      conductedByUserId: 2,
      loggedByUserId: 2,
      type: InteractionType.CHECK_IN,
      notes: 'Test interaction notes',
      occurredAt: '2024-01-15T10:00:00Z',
      projectId: 3,
    };

    const mockResponse: InteractionResponse = {
      id: 42,
      employee: { id: 1, name: 'John Doe' },
      conductedBy: { id: 2, name: 'Jane Smith' },
      loggedBy: { id: 2, name: 'Jane Smith' },
      project: { id: 3, name: 'Project Alpha' },
      type: InteractionType.CHECK_IN,
      notes: 'Test interaction notes',
      occurredAt: '2024-01-15T10:00:00Z',
      createdAt: '2024-01-15T10:01:00Z',
    };

    let result: InteractionResponse | undefined;
    service.create(request).subscribe((res) => (result = res));

    const req = httpTesting.expectOne('/api/interactions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);

    expect(result).toEqual(mockResponse);
  });

  it('should send projectId as null when not provided', () => {
    const request: CreateInteractionRequest = {
      employeeId: 1,
      conductedByUserId: 2,
      loggedByUserId: 2,
      type: InteractionType.MENTORING,
      notes: 'Mentoring session',
      occurredAt: '2024-01-15T10:00:00Z',
      projectId: null,
    };

    service.create(request).subscribe();

    const req = httpTesting.expectOne('/api/interactions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.projectId).toBeNull();
    req.flush({ id: 1 });
  });
});
