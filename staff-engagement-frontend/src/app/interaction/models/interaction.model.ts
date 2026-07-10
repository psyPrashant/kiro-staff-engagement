import { InteractionType } from './interaction-type.enum';

export interface CreateInteractionRequest {
  employeeId: number;
  conductedByUserId: number;
  loggedByUserId: number;
  type: InteractionType;
  notes: string;
  occurredAt: string; // ISO 8601 instant
  projectId?: number | null;
}

export interface InteractionResponse {
  id: number;
  employee: { id: number; name: string };
  conductedBy: { id: number; name: string };
  loggedBy: { id: number; name: string };
  project: { id: number; name: string } | null;
  type: InteractionType;
  notes: string;
  occurredAt: string;
  createdAt: string;
}

export interface ApiErrorResponse {
  message: string;
  fieldErrors: Record<string, string> | null;
}
