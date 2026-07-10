import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateInteractionRequest, InteractionResponse } from '../models/interaction.model';

@Injectable({ providedIn: 'root' })
export class InteractionService {
  private http = inject(HttpClient);

  create(request: CreateInteractionRequest): Observable<InteractionResponse> {
    return this.http.post<InteractionResponse>('/api/interactions', request);
  }
}
