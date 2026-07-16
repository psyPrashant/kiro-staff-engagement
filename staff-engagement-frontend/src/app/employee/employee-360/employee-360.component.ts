import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TimeoutError, timeout } from 'rxjs';
import { Employee360Service } from '../services/employee-360.service';
import { Employee360Response, InteractionDto } from '../models/employee-360.model';
import { EngagementService } from '../../dashboard/services/engagement.service';
import {
  EngagementStatus,
  formatEngagementStatusLabel,
} from '../../dashboard/models/engagement.model';
import {
  InteractionType,
  formatInteractionTypeLabel,
} from '../../interaction/models/interaction-type.enum';
import { TaskFormComponent } from '../../task/components/task-form/task-form.component';
import { TaskService } from '../../task/services/task.service';
import { CreateTaskRequest } from '../../task/models/task.model';
import { SchedulingService } from '../../schedule/services/scheduling.service';
import { ScheduledInteraction } from '../../schedule/models/scheduled-interaction.model';
import { EmployeeService } from '../../shared/services/employee.service';
import { AuthService } from '../../core/services/auth.service';
import { AvatarComponent, ModalComponent, PaginationComponent, ToastService } from '../../shared';
import { ScheduleCreateModalComponent } from '../../schedule/schedule-create-modal.component';
import { ScheduledInteractionDetailModalComponent } from '../../schedule/scheduled-interaction-detail-modal.component';
import { InteractionDetailModalComponent } from '../../interaction/interaction-detail-modal.component';
import { LogInteractionModalComponent } from '../../interaction/log-interaction-modal.component';

const INTERACTION_PAGE_SIZE = 3;

@Component({
  selector: 'app-employee-360',
  standalone: true,
  imports: [
    CommonModule,
    AvatarComponent,
    ModalComponent,
    PaginationComponent,
    TaskFormComponent,
    ScheduleCreateModalComponent,
    ScheduledInteractionDetailModalComponent,
    InteractionDetailModalComponent,
    LogInteractionModalComponent,
  ],
  templateUrl: './employee-360.component.html',
  styleUrl: './employee-360.component.css',
})
export class Employee360Component implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly employee360Service = inject(Employee360Service);
  private readonly engagementService = inject(EngagementService);
  private readonly taskService = inject(TaskService);
  private readonly schedulingService = inject(SchedulingService);
  private readonly employeeService = inject(EmployeeService);
  private readonly authService = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<Employee360Response | null>(null);
  readonly engagementStatus = signal<EngagementStatus | null>(null);
  readonly scheduled = signal<ScheduledInteraction[]>([]);

  // Modal / selection state
  readonly showTaskModal = signal(false);
  readonly showScheduleModal = signal(false);
  readonly showLogModal = signal(false);
  readonly selectedInteraction = signal<InteractionDto | null>(null);
  readonly selectedScheduled = signal<ScheduledInteraction | null>(null);
  readonly selectedTask = signal<Employee360Response['openTasks'][number] | null>(null);
  readonly deleting = signal(false);

  readonly interactionPage = signal(1);

  readonly currentUserId = computed(() => this.authService.currentUser()?.id ?? null);

  readonly interactionsForDialog = computed(() => {
    const employeeData = this.data();
    if (!employeeData) return [];
    return employeeData.interactions.map((interaction) => ({
      id: interaction.id,
      label: `${formatInteractionTypeLabel(interaction.type as InteractionType)} - ${interaction.occurredAt.substring(0, 10)}`,
    }));
  });

  // Interaction history sorted most-recent first.
  readonly sortedInteractions = computed(() => {
    const employeeData = this.data();
    if (!employeeData) return [];
    return [...employeeData.interactions].sort((a, b) =>
      b.occurredAt.localeCompare(a.occurredAt),
    );
  });

  readonly interactionTotalPages = computed(() =>
    Math.max(1, Math.ceil(this.sortedInteractions().length / INTERACTION_PAGE_SIZE)),
  );

  readonly pagedInteractions = computed(() => {
    const start = (this.interactionPage() - 1) * INTERACTION_PAGE_SIZE;
    return this.sortedInteractions().slice(start, start + INTERACTION_PAGE_SIZE);
  });

  // Upcoming interactions: scheduled on or after today (past ones are hidden).
  readonly upcomingInteractions = computed(() => {
    const todayKey = this.todayKey();
    return this.scheduled()
      .filter((s) => s.scheduledDate >= todayKey && s.completionStatus === 'PENDING')
      .sort((a, b) => a.scheduledDate.localeCompare(b.scheduledDate));
  });

  ngOnInit(): void {
    this.fetchData();
  }

  get employeeId(): number {
    return Number(this.route.snapshot.paramMap.get('id'));
  }

  getEngagementBadgeClass(): string {
    const status = this.engagementStatus();
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

  getEngagementStatusLabel(): string {
    const status = this.engagementStatus();
    if (!status) return '';
    return formatEngagementStatusLabel(status);
  }

  formatInteractionType(type: string): string {
    return formatInteractionTypeLabel(type as InteractionType);
  }

  onNewTask(): void {
    this.showTaskModal.set(true);
  }

  onTaskSubmitted(request: CreateTaskRequest): void {
    this.taskService.create(request).subscribe({
      next: () => {
        this.showTaskModal.set(false);
        this.toast.success('Task created');
        this.fetchData();
      },
      error: () => {
        this.toast.error('Failed to create task');
      },
    });
  }

  onScheduleCreated(): void {
    this.showScheduleModal.set(false);
    this.toast.success('Interaction scheduled');
    this.refreshSchedule();
  }

  onInteractionLogged(): void {
    this.showLogModal.set(false);
    this.toast.success('Interaction logged');
    this.fetchData();
  }

  onInteractionChanged(): void {
    this.selectedInteraction.set(null);
    this.fetchData();
  }

  onScheduledChanged(): void {
    this.selectedScheduled.set(null);
    this.refreshSchedule();
  }

  deleteEmployee(): void {
    const current = this.data();
    if (!current) return;
    if (!confirm(`Delete ${current.profile.name}? This cannot be undone.`)) return;

    this.deleting.set(true);
    this.employeeService.delete(this.employeeId).subscribe({
      next: () => {
        this.deleting.set(false);
        this.toast.success(`${current.profile.name} deleted`);
        this.router.navigate(['/employee']);
      },
      error: () => {
        this.deleting.set(false);
        this.toast.error('Failed to delete employee');
      },
    });
  }

  fetchData(): void {
    const id = this.employeeId;
    this.loading.set(true);
    this.error.set(null);

    this.employee360Service
      .getEmployee360(id)
      .pipe(timeout(30000))
      .subscribe({
        next: (response) => {
          response.openTasks.sort((a, b) => {
            if (a.dueDate === null && b.dueDate === null) return 0;
            if (a.dueDate === null) return 1;
            if (b.dueDate === null) return -1;
            return a.dueDate.localeCompare(b.dueDate);
          });
          this.data.set(response);
          this.interactionPage.set(1);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(this.mapError(err));
          this.loading.set(false);
        },
      });

    this.engagementService.getMatrix().subscribe({
      next: (entries) => {
        const entry = entries.find((e) => e.employeeId === id);
        if (entry) {
          this.engagementStatus.set(entry.engagementStatus);
        }
      },
    });

    this.refreshSchedule();
  }

  retry(): void {
    this.fetchData();
  }

  goBack(): void {
    this.router.navigate(['/employee']);
  }

  isOverdue(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date(new Date().toDateString());
  }

  truncateNotes(notes: string, maxLength = 200): string {
    if (notes.length <= maxLength) return notes;
    return notes.substring(0, maxLength) + '…';
  }

  private refreshSchedule(): void {
    this.schedulingService.list({ employeeId: this.employeeId }).subscribe({
      next: (data) => this.scheduled.set(data),
    });
  }

  private todayKey(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private mapError(err: unknown): string {
    if (err instanceof TimeoutError) {
      return 'Request timed out. Please try again.';
    }
    const httpError = err as { status?: number };
    if (httpError.status === 404) {
      return 'Employee not found';
    }
    if (httpError.status === 0) {
      return 'Request timed out. Please try again.';
    }
    return 'An error occurred while loading data. Please try again.';
  }
}
