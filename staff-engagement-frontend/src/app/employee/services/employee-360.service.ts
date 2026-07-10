import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Employee360Response } from '../models/employee-360.model';

@Injectable({ providedIn: 'root' })
export class Employee360Service {
  private readonly http = inject(HttpClient);

  getEmployee360(id: number): Observable<Employee360Response> {
    return this.http.get<Employee360Response>(`/api/employees/${id}/360`);
  }
}
