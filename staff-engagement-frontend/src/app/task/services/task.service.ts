import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, TaskResponse } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);

  getAll(params?: { status?: string }): Observable<TaskResponse[]> {
    const httpParams: Record<string, string> = {};
    if (params?.status) {
      httpParams['status'] = params.status;
    }
    return this.http.get<TaskResponse[]>('/api/tasks', { params: httpParams });
  }

  create(request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>('/api/tasks', request);
  }

  update(taskId: number, request: Partial<CreateTaskRequest>): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`/api/tasks/${taskId}`, request);
  }

  updateStatus(taskId: number, status: string): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`/api/tasks/${taskId}/status`, { status });
  }

  delete(taskId: number): Observable<void> {
    return this.http.delete<void>(`/api/tasks/${taskId}`);
  }
}
