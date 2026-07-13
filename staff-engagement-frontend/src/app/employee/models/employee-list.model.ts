import { EngagementStatus } from '../../dashboard/models/engagement.model';

export interface EmployeeListEntry {
  id: number;
  name: string;
  email: string;
  jobTitle: string;
  managerName: string | null;
  engagementStatus: EngagementStatus | null;
  lastInteractionDate: string | null;
}
