import { formatEnumLabel } from '../../shared/utils/format-enum-label';

export type EngagementStatus = 'OVERDUE' | 'AT_RISK' | 'ON_TRACK';

export type SortOption = 'name' | 'recency';

export interface MatrixEntry {
  employeeId: number;
  employeeName: string;
  employeeEmail: string;
  recency: number | null;
  frequency: number;
  lastInteractionDate: string | null;
  engagementStatus: EngagementStatus;
  followUpRequired: boolean;
}

export function formatEngagementStatusLabel(status: EngagementStatus): string {
  switch (status) {
    case 'OVERDUE':
      return 'Overdue';
    case 'AT_RISK':
      return 'At risk';
    case 'ON_TRACK':
      return 'On track';
    default:
      return formatEnumLabel(status);
  }
}
