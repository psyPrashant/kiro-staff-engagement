import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EngagementStatus, MatrixEntry, SortOption } from '../models/engagement.model';

@Injectable({ providedIn: 'root' })
export class EngagementService {
  private http = inject(HttpClient);

  getMatrix(params?: { status?: EngagementStatus; sort?: SortOption }): Observable<MatrixEntry[]> {
    let httpParams = new HttpParams();

    if (params?.status) {
      httpParams = httpParams.set('status', params.status);
    }

    if (params?.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }

    return this.http.get<MatrixEntry[]>('/api/engagement/matrix', { params: httpParams });
  }
}
