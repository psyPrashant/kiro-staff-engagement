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
