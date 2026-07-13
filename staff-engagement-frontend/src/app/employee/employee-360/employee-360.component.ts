import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TimeoutError, timeout } from 'rxjs';
import { Employee360Service } from '../services/employee-360.service';
import { Employee360Response } from '../models/employee-360.model';
import { EngagementService } from '../../dashboard/services/engagement.service';
import {
  EngagementStatus,
  formatEngagementStatusLabel,
} from '../../dashboard/models/engagement.model';
import {
  InteractionType,
  formatInteractionTypeLabel,
} from '../../interaction/models/interaction-type.enum';
import { TaskCreateDialogComponent } from '../../task/components/task-create-dialog/task-create-dialog.component';
import { TaskService } from '../../task/services/task.service';
import { CreateTaskRequest } from '../../task/models/task.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-employee-360',
  standalone: true,
  imports: [CommonModule, RouterLink, TaskCreateDialogComponent],
  templateUrl: './employee-360.component.html',
  styleUrl: './employee-360.component.css',
})
export class Employee360Component implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly employee360Service = inject(Employee360Service);
  private readonly engagementService = inject(EngagementService);
  private readonly taskService = inject(TaskService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly data = signal<Employee360Response | null>(null);
  readonly engagementStatus = signal<EngagementStatus | null>(null);
  readonly dialogOpen = signal(false);

  readonly currentUserId = computed(() => this.authService.currentUser()?.id ?? null);

  readonly interactionsForDialog = computed(() => {
    const employeeData = this.data();
    if (!employeeData) return [];
    return employeeData.interactions.map((interaction) => ({
      id: interaction.id,
      label: `${formatInteractionTypeLabel(interaction.type as InteractionType)} - ${interaction.occurredAt.substring(0, 10)}`,
    }));
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
    this.dialogOpen.set(true);
  }

  onTaskSubmitted(request: CreateTaskRequest): void {
    this.taskService.create(request).subscribe({
      next: () => {
        this.dialogOpen.set(false);
        this.fetchData();
      },
      error: () => {
        this.dialogOpen.set(false);
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
  }

  retry(): void {
    this.fetchData();
  }

  isOverdue(dueDate: string | null): boolean {
    if (!dueDate) return false;
    return new Date(dueDate) < new Date(new Date().toDateString());
  }

  truncateNotes(notes: string, maxLength = 200): string {
    if (notes.length <= maxLength) return notes;
    return notes.substring(0, maxLength) + '\u2026';
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
