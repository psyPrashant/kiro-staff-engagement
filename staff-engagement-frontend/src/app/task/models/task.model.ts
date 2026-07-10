export interface CreateTaskRequest {
  title: string;
  description?: string | null;
  interactionId?: number | null;
  dueDate?: string | null; // ISO date (YYYY-MM-DD)
  assignedUserId?: number | null;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  status: string;
  dueDate: string | null;
  assignedUser: { id: number; name: string } | null;
  interaction: { id: number } | null;
  createdAt: string;
}
