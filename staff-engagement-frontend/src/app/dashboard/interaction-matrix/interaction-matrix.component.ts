import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  EngagementStatus,
  MatrixEntry,
  SortOption,
  formatEngagementStatusLabel,
} from '../models/engagement.model';
import { EngagementService } from '../services/engagement.service';
import { StatusFilterComponent } from '../status-filter/status-filter.component';
import { SortControlComponent } from '../sort-control/sort-control.component';
import { FollowUpSectionComponent } from '../follow-up-section/follow-up-section.component';

@Component({
  selector: 'app-interaction-matrix',
  standalone: true,
  imports: [RouterLink, StatusFilterComponent, SortControlComponent, FollowUpSectionComponent],
  templateUrl: './interaction-matrix.component.html',
  styleUrl: './interaction-matrix.component.css',
})
export class InteractionMatrixComponent implements OnInit {
  private readonly engagementService = inject(EngagementService);
  private readonly router = inject(Router);

  readonly entries = signal<MatrixEntry[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly activeFilter = signal<EngagementStatus | null>(null);
  readonly activeSort = signal<SortOption>('name');

  readonly triageStats = computed(() => {
    const all = this.entries();
    return {
      overdue: all.filter((e) => e.engagementStatus === 'OVERDUE').length,
      atRisk: all.filter((e) => e.engagementStatus === 'AT_RISK').length,
      onTrack: all.filter((e) => e.engagementStatus === 'ON_TRACK').length,
    };
  });

  readonly followUpEntries = computed(() =>
    this.entries().filter((entry) => entry.followUpRequired),
  );

  ngOnInit(): void {
    this.fetchMatrix();
  }

  fetchMatrix(): void {
    this.loading.set(true);
    this.error.set(null);

    const params: { status?: EngagementStatus; sort?: SortOption } = {};
    const filter = this.activeFilter();
    const sort = this.activeSort();

    if (filter) {
      params.status = filter;
    }
    if (sort && sort !== 'name') {
      params.sort = sort;
    }

    this.engagementService.getMatrix(params).subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load the engagement matrix. Please try again.');
        this.loading.set(false);
      },
    });
  }

  onFilterChange(filter: EngagementStatus | null): void {
    this.activeFilter.set(filter);
    this.fetchMatrix();
  }

  onSortChange(sort: SortOption): void {
    this.activeSort.set(sort);
    this.fetchMatrix();
  }

  navigateToEmployee(employeeId: number): void {
    this.router.navigate(['/employee', employeeId]);
  }

  navigateToLogInteraction(employeeId: number): void {
    this.router.navigate(['/interaction'], { queryParams: { employeeId } });
  }

  navigateToSchedule(employeeId: number): void {
    this.router.navigate(['/schedule/new'], { queryParams: { employeeId } });
  }

  formatRecency(recency: number | null): string {
    if (recency === null) {
      return 'No interactions';
    }
    return `${recency} days`;
  }

  formatDate(date: string | null): string {
    if (date === null) {
      return 'Never';
    }
    return new Date(date).toLocaleDateString();
  }

  getStatusClass(status: EngagementStatus): string {
    switch (status) {
      case EngagementStatus.OVERDUE:
        return 'status-overdue';
      case EngagementStatus.AT_RISK:
        return 'status-at-risk';
      case EngagementStatus.ON_TRACK:
        return 'status-on-track';
      default:
        return '';
    }
  }

  getStatusLabel(status: EngagementStatus): string {
    switch (status) {
      case EngagementStatus.OVERDUE:
        return 'Status: Overdue';
      case EngagementStatus.AT_RISK:
        return 'Status: At Risk';
      case EngagementStatus.ON_TRACK:
        return 'Status: On Track';
      default:
        return '';
    }
  }

  formatStatusLabel(status: EngagementStatus): string {
    return formatEngagementStatusLabel(status);
  }

  getBadgeClass(status: EngagementStatus): string {
    switch (status) {
      case EngagementStatus.OVERDUE:
        return 'badge badge-danger';
      case EngagementStatus.AT_RISK:
        return 'badge badge-warning';
      case EngagementStatus.ON_TRACK:
        return 'badge badge-success';
      default:
        return 'badge';
    }
  }
}
