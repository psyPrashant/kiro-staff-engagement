import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateProjectRequest,
  Project,
  ProjectDetail,
  ProjectSummary,
  UpdateProjectRequest,
} from '../models/project.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private http = inject(HttpClient);

  getAll(): Observable<Project[]> {
    return this.http.get<Project[]>('/api/projects');
  }

  getSummaries(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>('/api/projects/summaries');
  }

  getDetail(id: number): Observable<ProjectDetail> {
    return this.http.get<ProjectDetail>(`/api/projects/${id}`);
  }

  create(request: CreateProjectRequest): Observable<ProjectDetail> {
    return this.http.post<ProjectDetail>('/api/projects', request);
  }

  update(id: number, request: UpdateProjectRequest): Observable<ProjectDetail> {
    return this.http.put<ProjectDetail>(`/api/projects/${id}`, request);
  }
}
