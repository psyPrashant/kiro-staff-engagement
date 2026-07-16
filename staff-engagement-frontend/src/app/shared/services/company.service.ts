import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Company, CreateCompanyRequest } from '../models/company.model';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private http = inject(HttpClient);

  getAll(): Observable<Company[]> {
    return this.http.get<Company[]>('/api/companies');
  }

  create(request: CreateCompanyRequest): Observable<Company> {
    return this.http.post<Company>('/api/companies', request);
  }
}
