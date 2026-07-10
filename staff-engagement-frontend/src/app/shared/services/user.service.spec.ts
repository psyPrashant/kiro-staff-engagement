import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { UserService } from './user.service';
import { User } from '../../core/models/user.model';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should GET /api/users and return an array of users', () => {
    const mockUsers: User[] = [
      { id: 1, name: 'Alice Smith', email: 'alice@example.com' },
      { id: 2, name: 'Bob Jones', email: 'bob@example.com' },
    ];

    service.getAll().subscribe((users) => {
      expect(users).toEqual(mockUsers);
      expect(users.length).toBe(2);
    });

    const req = httpMock.expectOne('/api/users');
    expect(req.request.method).toBe('GET');
    req.flush(mockUsers);
  });

  it('should return an empty array when no users exist', () => {
    service.getAll().subscribe((users) => {
      expect(users).toEqual([]);
      expect(users.length).toBe(0);
    });

    const req = httpMock.expectOne('/api/users');
    req.flush([]);
  });
});
