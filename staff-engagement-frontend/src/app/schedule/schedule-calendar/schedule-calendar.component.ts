import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { KeyValuePipe } from '@angular/common';

import { ScheduledInteraction } from '../models/scheduled-interaction.model';
import { SchedulingService } from '../services/scheduling.service';

@Component({
  selector: 'app-schedule-calendar',
  imports: [KeyValuePipe],
  templateUrl: './schedule-calendar.component.html',
  styleUrl: './schedule-calendar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleCalendarComponent implements OnInit {
  private readonly schedulingService = inject(SchedulingService);
  private readonly destroyRef = inject(DestroyRef);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly entries = signal<ScheduledInteraction[]>([]);
  readonly expandedId = signal<number | null>(null);

  readonly groupedEntries = computed(() => {
    return this.groupByDate(this.entries());
  });

  ngOnInit(): void {
    this.fetchEntries();
  }

  fetchEntries(): void {
    this.loading.set(true);
    this.error.set(null);
    this.schedulingService
      .list({ status: 'PENDING' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.entries.set(data);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('Failed to load schedule');
          this.loading.set(false);
        },
      });
  }

  toggleExpand(id: number): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  complete(id: number): void {
    this.schedulingService
      .update(id, { completionStatus: 'COMPLETED' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.entries.update((e) => e.filter((i) => i.id !== id)),
        error: () => this.error.set('Failed to complete interaction'),
      });
  }

  cancel(id: number): void {
    this.schedulingService
      .update(id, { completionStatus: 'CANCELLED' })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.entries.update((e) => e.filter((i) => i.id !== id)),
        error: () => this.error.set('Failed to cancel interaction'),
      });
  }

  isOverdue(entry: ScheduledInteraction): boolean {
    return entry.overdue;
  }

  truncateNotes(notes: string | null, maxLength = 100): string {
    if (!notes) return '';
    if (notes.length <= maxLength) return notes;
    return notes.substring(0, maxLength) + '\u2026';
  }

  dateOrder = (): number => 0;

  private groupByDate(entries: ScheduledInteraction[]): Map<string, ScheduledInteraction[]> {
    const map = new Map<string, ScheduledInteraction[]>();
    const sorted = [...entries].sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate));
    for (const entry of sorted) {
      const group = map.get(entry.scheduledDate) ?? [];
      group.push(entry);
      map.set(entry.scheduledDate, group);
    }
    return map;
  }
}
