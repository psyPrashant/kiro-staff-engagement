import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { EngagementService } from './services/engagement.service';
import { SchedulingService } from '../schedule/services/scheduling.service';
import { GreetingService } from '../greeting/greeting.service';
import { AuthService } from '../core/services/auth.service';
import {
  EngagementStatus,
  MatrixEntry,
  formatEngagementStatusLabel,
  engagementStatusBadgeClass,
} from './models/engagement.model';
import { ScheduledInteraction } from '../schedule/models/scheduled-interaction.model';
import { AvatarComponent } from '../shared';
import { ScheduledInteractionDetailModalComponent } from '../schedule/scheduled-interaction-detail-modal.component';

type CalendarView = 'week' | 'month';

interface CalendarCell {
  label: string;
  dayOfMonth: number;
  dateKey: string;
  isToday: boolean;
  inCurrentMonth: boolean;
  interactions: ScheduledInteraction[];
}

const INTERACTION_TYPE_LABELS: Record<string, string> = {
  CHECK_IN: 'Check-in',
  MENTORING: 'Mentoring',
  CATCH_UP: 'Catch-up',
  OTHER: 'Other',
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AvatarComponent, ScheduledInteractionDetailModalComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  private readonly engagementService = inject(EngagementService);
  private readonly schedulingService = inject(SchedulingService);
  private readonly greetingService = inject(GreetingService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly greeting = signal('Welcome back');
  readonly entries = signal<MatrixEntry[]>([]);
  readonly schedule = signal<ScheduledInteraction[]>([]);
  readonly loading = signal(false);

  // Calendar state
  readonly view = signal<CalendarView>('week');
  readonly anchor = signal<Date>(new Date());
  readonly selectedScheduled = signal<ScheduledInteraction | null>(null);

  readonly triageStats = computed(() => {
    const all = this.entries();
    return {
      overdue: all.filter((e) => e.engagementStatus === EngagementStatus.OVERDUE).length,
      atRisk: all.filter((e) => e.engagementStatus === EngagementStatus.AT_RISK).length,
      onTrack: all.filter((e) => e.engagementStatus === EngagementStatus.ON_TRACK).length,
    };
  });

  readonly followUpEntries = computed(() =>
    this.entries().filter((entry) => entry.followUpRequired),
  );

  private readonly scheduleByDate = computed(() => {
    const map = new Map<string, ScheduledInteraction[]>();
    for (const item of this.schedule()) {
      const list = map.get(item.scheduledDate) ?? [];
      list.push(item);
      map.set(item.scheduledDate, list);
    }
    return map;
  });

  // Days rendered for the week view (Monday-first).
  readonly weekCells = computed<CalendarCell[]>(() => {
    const start = this.startOfWeek(this.anchor());
    return this.buildCells(start, 7, this.anchor().getMonth(), 'weekday');
  });

  // Weeks (rows of 7 days) rendered for the month view.
  readonly monthWeeks = computed<CalendarCell[][]>(() => {
    const ref = this.anchor();
    const firstOfMonth = new Date(ref.getFullYear(), ref.getMonth(), 1);
    const gridStart = this.startOfWeek(firstOfMonth);
    const cells = this.buildCells(gridStart, 42, ref.getMonth(), 'day');
    const weeks: CalendarCell[][] = [];
    for (let i = 0; i < cells.length; i += 7) {
      weeks.push(cells.slice(i, i + 7));
    }
    // Drop a trailing week that is entirely outside the current month.
    return weeks.filter((week) => week.some((c) => c.inCurrentMonth));
  });

  readonly weekdayHeaders = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  readonly periodLabel = computed(() => {
    const ref = this.anchor();
    if (this.view() === 'month') {
      return ref.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
    }
    const start = this.startOfWeek(ref);
    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    const startLabel = start.toLocaleDateString('en-US', { day: 'numeric', month: 'short' });
    const endLabel = end.toLocaleDateString('en-US', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
    return `${startLabel} – ${endLabel}`;
  });

  ngOnInit(): void {
    this.fetchData();
    this.loadGreeting();
  }

  setView(view: CalendarView): void {
    this.view.set(view);
  }

  previous(): void {
    this.shift(-1);
  }

  next(): void {
    this.shift(1);
  }

  today(): void {
    this.anchor.set(new Date());
  }

  onScheduledChanged(): void {
    this.selectedScheduled.set(null);
    this.refreshSchedule();
  }

  navigateToEmployee(id: number): void {
    this.router.navigate(['/employee', id]);
  }

  typeLabel(type: string): string {
    return INTERACTION_TYPE_LABELS[type] ?? type;
  }

  formatStatus(status: EngagementStatus): string {
    return formatEngagementStatusLabel(status);
  }

  badgeClass(status: EngagementStatus): string {
    return 'badge ' + engagementStatusBadgeClass(status);
  }

  private shift(direction: 1 | -1): void {
    const ref = new Date(this.anchor());
    if (this.view() === 'month') {
      ref.setMonth(ref.getMonth() + direction);
    } else {
      ref.setDate(ref.getDate() + direction * 7);
    }
    this.anchor.set(ref);
  }

  private buildCells(
    start: Date,
    count: number,
    currentMonth: number,
    labelMode: 'weekday' | 'day',
  ): CalendarCell[] {
    const todayKey = this.toKey(new Date());
    const byDate = this.scheduleByDate();
    const cells: CalendarCell[] = [];
    for (let i = 0; i < count; i++) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      const key = this.toKey(d);
      cells.push({
        label:
          labelMode === 'weekday'
            ? d.toLocaleDateString('en-US', { weekday: 'short' })
            : String(d.getDate()),
        dayOfMonth: d.getDate(),
        dateKey: key,
        isToday: key === todayKey,
        inCurrentMonth: d.getMonth() === currentMonth,
        interactions: byDate.get(key) ?? [],
      });
    }
    return cells;
  }

  private startOfWeek(date: Date): Date {
    const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const day = d.getDay(); // 0 = Sunday
    const diff = day === 0 ? -6 : 1 - day; // shift back to Monday
    d.setDate(d.getDate() + diff);
    return d;
  }

  private toKey(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private fetchData(): void {
    this.loading.set(true);
    this.engagementService.getMatrix().subscribe({
      next: (data) => {
        this.entries.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });

    this.refreshSchedule();
  }

  private refreshSchedule(): void {
    this.schedulingService.list().subscribe({
      next: (data) => this.schedule.set(data),
    });
  }

  private loadGreeting(): void {
    const user = this.authService.currentUser();
    if (user) {
      this.greetingService.getGreeting(user.name).subscribe({
        next: (msg) => this.greeting.set(msg),
      });
    }
  }
}
