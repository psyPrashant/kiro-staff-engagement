import { Component, input, output } from '@angular/core';
import { EngagementStatus } from '../models/engagement.model';

@Component({
  selector: 'app-status-filter',
  standalone: true,
  templateUrl: './status-filter.component.html',
  styleUrl: './status-filter.component.css',
})
export class StatusFilterComponent {
  readonly activeFilter = input<EngagementStatus | null>(null);
  readonly filterChange = output<EngagementStatus | null>();

  readonly filters: { label: string; value: EngagementStatus | null }[] = [
    { label: 'All', value: null },
    { label: 'Overdue', value: EngagementStatus.OVERDUE },
    { label: 'At Risk', value: EngagementStatus.AT_RISK },
    { label: 'On Track', value: EngagementStatus.ON_TRACK },
  ];

  onFilterSelect(value: EngagementStatus | null): void {
    this.filterChange.emit(value);
  }
}
