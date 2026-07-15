import { formatEnumLabel } from '../../shared/utils/format-enum-label';

export interface CreateTaskRequest {
  title: string;
  description?: string | null;
  interactionId?: number | null;
  dueDate?: string | null; // ISO date (YYYY-MM-DD)
  assignedUserId?: number | null;
  employeeId: number;
}

export interface UpdateTaskRequest {
  title: string;
  description?: string | null;
  interactionId?: number | null;
  dueDate?: string | null; // ISO date (YYYY-MM-DD)
  assignedUserId?: number | null;
  employeeId?: number | null;
  status: string; // 'OPEN' | 'DONE'
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  status: string;
  dueDate: string | null;
  assignedUser: { id: number; name: string } | null;
  interaction: { id: number } | null;
  employeeId: number | null;
  employeeName: string | null;
  createdAt: string;
}

export function formatTaskStatusLabel(status: string): string {
  switch (status) {
    case 'OPEN':
      return 'Open';
    case 'DONE':
      return 'Done';
    default:
      return formatEnumLabel(status);
  }
}

export function isOverdue(task: { status: string; dueDate: string | null }): boolean {
  if (task.status !== 'OPEN' || !task.dueDate) return false;
  return new Date(task.dueDate) < new Date(new Date().toDateString());
}
