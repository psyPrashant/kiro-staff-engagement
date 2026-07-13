import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  EngagementStatus,
  MatrixEntry,
  formatEngagementStatusLabel,
} from '../models/engagement.model';

@Component({
  selector: 'app-follow-up-section',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './follow-up-section.component.html',
  styleUrl: './follow-up-section.component.css',
})
export class FollowUpSectionComponent {
  readonly entries = input.required<MatrixEntry[]>();

  getBadgeClass(status: EngagementStatus): string {
    switch (status) {
      case 'OVERDUE':
        return 'badge badge-danger';
      case 'AT_RISK':
        return 'badge badge-warning';
      case 'ON_TRACK':
        return 'badge badge-success';
      default:
        return 'badge';
    }
  }

  getStatusLabel(status: EngagementStatus): string {
    return `Status: ${formatEngagementStatusLabel(status)}`;
  }

  formatStatusLabel(status: EngagementStatus): string {
    return formatEngagementStatusLabel(status);
  }

  formatRecency(recency: number | null): string {
    if (recency === null) {
      return 'No interactions';
    }
    return `${recency} days`;
  }
}
