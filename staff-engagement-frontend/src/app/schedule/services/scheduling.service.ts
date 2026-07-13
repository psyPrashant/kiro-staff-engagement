import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  CompletionStatus,
  CreateScheduledInteractionRequest,
  ScheduledInteraction,
} from '../models/scheduled-interaction.model';

@Injectable({ providedIn: 'root' })
export class SchedulingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/scheduled-interactions';

  create(request: CreateScheduledInteractionRequest): Observable<ScheduledInteraction> {
    return this.http.post<ScheduledInteraction>(this.baseUrl, request);
  }

  list(params?: {
    status?: CompletionStatus;
    employeeId?: number;
    overdue?: boolean;
  }): Observable<ScheduledInteraction[]> {
    let httpParams = new HttpParams();
    if (params?.status) httpParams = httpParams.set('status', params.status);
    if (params?.employeeId) httpParams = httpParams.set('employeeId', params.employeeId.toString());
    if (params?.overdue) httpParams = httpParams.set('overdue', 'true');
    return this.http.get<ScheduledInteraction[]>(this.baseUrl, { params: httpParams });
  }

  update(
    id: number,
    body: {
      completionStatus?: CompletionStatus;
      scheduledDate?: string;
      notes?: string;
    }
  ): Observable<ScheduledInteraction> {
    return this.http.patch<ScheduledInteraction>(`${this.baseUrl}/${id}`, body);
  }
}
