import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, KeyValuePipe } from '@angular/common';

import { ScheduledInteraction } from '../models/scheduled-interaction.model';
import { SchedulingService } from '../services/scheduling.service';

@Component({
  selector: 'app-schedule-calendar',
  standalone: true,
  imports: [CommonModule, KeyValuePipe],
  templateUrl: './schedule-calendar.component.html',
  styleUrl: './schedule-calendar.component.css',
})
export class ScheduleCalendarComponent implements OnInit {
  private readonly schedulingService = inject(SchedulingService);

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
    this.schedulingService.list({ status: 'PENDING' }).subscribe({
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
    this.schedulingService.update(id, { completionStatus: 'COMPLETED' }).subscribe({
      next: () => this.entries.update((e) => e.filter((i) => i.id !== id)),
    });
  }

  cancel(id: number): void {
    this.schedulingService.update(id, { completionStatus: 'CANCELLED' }).subscribe({
      next: () => this.entries.update((e) => e.filter((i) => i.id !== id)),
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
    const sorted = [...entries].sort(
      (a, b) => a.scheduledDate.localeCompare(b.scheduledDate)
    );
    for (const entry of sorted) {
      const group = map.get(entry.scheduledDate) ?? [];
      group.push(entry);
      map.set(entry.scheduledDate, group);
    }
    return map;
  }
}
