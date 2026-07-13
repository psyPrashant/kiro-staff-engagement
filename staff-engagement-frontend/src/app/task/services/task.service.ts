import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateTaskRequest, TaskResponse } from '../models/task.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);

  getAll(params?: { status?: string }): Observable<TaskResponse[]> {
    return this.http.get<TaskResponse[]>('/api/tasks', { params: params as any });
  }

  create(request: CreateTaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>('/api/tasks', request);
  }

  updateStatus(taskId: number, status: string): Observable<TaskResponse> {
    return this.http.patch<TaskResponse>(`/api/tasks/${taskId}/status`, { status });
  }
}
