export type CompletionStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';
export type InteractionType = 'CHECK_IN' | 'MENTORING' | 'CATCH_UP' | 'OTHER';

export interface ScheduledInteraction {
  id: number;
  employeeId: number;
  employeeName: string;
  scheduledDate: string; // yyyy-MM-dd
  interactionType: InteractionType;
  completionStatus: CompletionStatus;
  notes: string | null;
  overdue: boolean;
  createdAt: string;
}

export interface CreateScheduledInteractionRequest {
  employeeId: number;
  scheduledDate: string; // yyyy-MM-dd
  interactionType: InteractionType;
  notes?: string;
}
