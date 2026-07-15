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

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AvatarComponent],
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
  readonly showLogModal = signal(false);
  readonly showScheduleModal = signal(false);

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

  readonly weekDays = computed(() => {
    const days: { label: string; date: string; isToday: boolean }[] = [];
    const today = new Date();
    const startOfWeek = new Date(today);
    startOfWeek.setDate(today.getDate() - today.getDay() + 1); // Monday

    for (let i = 0; i < 7; i++) {
      const d = new Date(startOfWeek);
      d.setDate(startOfWeek.getDate() + i);
      days.push({
        label: d.toLocaleDateString('en-US', { weekday: 'short' }),
        date: d.toISOString().split('T')[0],
        isToday: d.toDateString() === today.toDateString(),
      });
    }
    return days;
  });

  readonly calendarEntries = computed(() => {
    const days = this.weekDays();
    const items = this.schedule();
    return days.map((day) => ({
      ...day,
      interactions: items.filter((s) => s.scheduledDate === day.date),
    }));
  });

  ngOnInit(): void {
    this.fetchData();
    this.loadGreeting();
  }

  openLogModal(): void {
    this.router.navigate(['/interaction']);
  }

  openScheduleModal(): void {
    this.router.navigate(['/schedule/new']);
  }

  navigateToEmployee(id: number): void {
    this.router.navigate(['/employee', id]);
  }

  formatStatus(status: EngagementStatus): string {
    return formatEngagementStatusLabel(status);
  }

  badgeClass(status: EngagementStatus): string {
    return 'badge ' + engagementStatusBadgeClass(status);
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
