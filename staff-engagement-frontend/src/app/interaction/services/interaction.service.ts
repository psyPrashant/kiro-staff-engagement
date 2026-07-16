import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateInteractionRequest,
  InteractionResponse,
  UpdateInteractionRequest,
} from '../models/interaction.model';

@Injectable({ providedIn: 'root' })
export class InteractionService {
  private http = inject(HttpClient);

  create(request: CreateInteractionRequest): Observable<InteractionResponse> {
    return this.http.post<InteractionResponse>('/api/interactions', request);
  }

  update(id: number, request: UpdateInteractionRequest): Observable<InteractionResponse> {
    return this.http.put<InteractionResponse>(`/api/interactions/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/interactions/${id}`);
  }
}
