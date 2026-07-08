import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface GreetingResponse {
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class GreetingService {
  constructor(private readonly http: HttpClient) {}

  getGreeting(name: string): Observable<string> {
    return this.http
      .get<GreetingResponse>(`/api/greetings/${encodeURIComponent(name)}`)
      .pipe(map((response) => response.message));
  }
}
