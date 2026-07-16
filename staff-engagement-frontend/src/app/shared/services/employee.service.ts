import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateEmployeeRequest, Employee } from '../models/employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private http = inject(HttpClient);

  getAll(): Observable<Employee[]> {
    return this.http.get<Employee[]>('/api/employees');
  }

  create(request: CreateEmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>('/api/employees', request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/employees/${id}`);
  }
}
