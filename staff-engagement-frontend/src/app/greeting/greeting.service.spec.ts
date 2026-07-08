import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { GreetingService, GreetingResponse } from './greeting.service';

describe('GreetingService', () => {
  let service: GreetingService;
  let httpClientSpy: { get: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    httpClientSpy = { get: vi.fn() };

    TestBed.configureTestingModule({
      providers: [GreetingService, { provide: HttpClient, useValue: httpClientSpy }],
    });

    service = TestBed.inject(GreetingService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return the greeting message from the API', () => {
    const mockResponse: GreetingResponse = { message: 'Hello, World!' };
    httpClientSpy.get.mockReturnValue(of(mockResponse));

    service.getGreeting('World').subscribe((result) => {
      expect(result).toBe('Hello, World!');
    });

    expect(httpClientSpy.get).toHaveBeenCalledWith('/api/greetings/World');
  });

  it('should encode special characters in the name', () => {
    const mockResponse: GreetingResponse = { message: 'Hello, John Doe!' };
    httpClientSpy.get.mockReturnValue(of(mockResponse));

    service.getGreeting('John Doe').subscribe((result) => {
      expect(result).toBe('Hello, John Doe!');
    });

    expect(httpClientSpy.get).toHaveBeenCalledWith('/api/greetings/John%20Doe');
  });
});
