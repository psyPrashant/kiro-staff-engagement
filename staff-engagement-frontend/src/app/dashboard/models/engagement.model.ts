export enum EngagementStatus {
  OVERDUE = 'OVERDUE',
  AT_RISK = 'AT_RISK',
  ON_TRACK = 'ON_TRACK',
}

export function formatEngagementStatusLabel(status: EngagementStatus): string {
  switch (status) {
    case EngagementStatus.OVERDUE:
      return 'Overdue';
    case EngagementStatus.AT_RISK:
      return 'At risk';
    case EngagementStatus.ON_TRACK:
      return 'On track';
  }
}

export function engagementStatusBadgeClass(status: EngagementStatus): string {
  switch (status) {
    case EngagementStatus.OVERDUE:
      return 'badge-danger';
    case EngagementStatus.AT_RISK:
      return 'badge-warning';
    case EngagementStatus.ON_TRACK:
      return 'badge-success';
  }
}

export type SortOption = 'name' | 'recency' | 'frequency' | 'status';

export interface MatrixEntry {
  employeeId: number;
  employeeName: string;
  engagementStatus: EngagementStatus;
  recency: number | null;
  frequency: number;
  lastInteractionDate: string | null;
  followUpRequired: boolean;
}
