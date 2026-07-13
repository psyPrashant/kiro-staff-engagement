import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatrixEntry } from '../models/engagement.model';

@Component({
  selector: 'app-follow-up-section',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './follow-up-section.component.html',
  styleUrl: './follow-up-section.component.css',
})
export class FollowUpSectionComponent {
  readonly entries = input.required<MatrixEntry[]>();

  getStatusClass(status: string): string {
    switch (status) {
      case 'OVERDUE':
        return 'status-overdue';
      case 'AT_RISK':
        return 'status-at-risk';
      case 'ON_TRACK':
        return 'status-on-track';
      default:
        return '';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'OVERDUE':
        return 'Status: Overdue';
      case 'AT_RISK':
        return 'Status: At Risk';
      case 'ON_TRACK':
        return 'Status: On Track';
      default:
        return '';
    }
  }

  formatRecency(recency: number | null): string {
    if (recency === null) {
      return 'No interactions';
    }
    return `${recency} days`;
  }
}
