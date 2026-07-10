import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { EmployeeService } from './employee.service';
import { Employee } from '../models/employee.model';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EmployeeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET /api/employees and return an array of employees', () => {
    const mockEmployees: Employee[] = [
      { id: 1, name: 'Alice Smith', email: 'alice@example.com', jobTitle: 'Engineer' },
      { id: 2, name: 'Bob Jones', email: 'bob@example.com', jobTitle: 'Designer' },
    ];

    service.getAll().subscribe((employees) => {
      expect(employees).toEqual(mockEmployees);
      expect(employees.length).toBe(2);
    });

    const req = httpMock.expectOne('/api/employees');
    expect(req.request.method).toBe('GET');
    req.flush(mockEmployees);
  });

  it('should return an empty array when no employees exist', () => {
    service.getAll().subscribe((employees) => {
      expect(employees).toEqual([]);
      expect(employees.length).toBe(0);
    });

    const req = httpMock.expectOne('/api/employees');
    req.flush([]);
  });
});
