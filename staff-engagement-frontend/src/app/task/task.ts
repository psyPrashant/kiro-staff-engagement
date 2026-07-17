import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { TaskService } from './services/task.service';
import {
  CreateTaskRequest,
  TaskResponse,
  UpdateTaskRequest,
  formatTaskStatusLabel,
  isOverdue,
} from './models/task.model';
import { TaskFormComponent } from './components/task-form/task-form.component';
import { ToastService, ModalComponent, PaginationComponent } from '../shared';
import { AuthService } from '../core/services/auth.service';
import { DatePipe } from '@angular/common';

const PAGE_SIZE = 10;

@Component({
  selector: 'app-task',
  standalone: true,
  imports: [TaskFormComponent, ModalComponent, PaginationComponent, DatePipe],
  templateUrl: './task.html',
  styleUrl: './task.css',
})
export class TaskListComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly toast = inject(ToastService);
  private readonly authService = inject(AuthService);

  readonly tasks = signal<TaskResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<string>('OPEN');
  readonly assignedToMeOnly = signal<boolean>(false);
  readonly showCreateModal = signal<boolean>(false);
  readonly selectedTask = signal<TaskResponse | null>(null);
  readonly editingTask = signal<TaskResponse | null>(null);
  readonly page = signal<number>(1);

  readonly filteredTasks = computed(() => {
    const filter = this.statusFilter();
    const all = this.tasks();
    let matching = filter === 'ALL' ? all : all.filter((task) => task.status === filter);
    // Narrow to tasks assigned to the logged-in user when the toggle is on.
    if (this.assignedToMeOnly()) {
      const currentUserId = this.authService.currentUser()?.id ?? null;
      matching = matching.filter((task) => task.assignedUserId === currentUserId);
    }
    // Always sort by due date, closest upcoming date first (nulls last).
    return [...matching].sort((a, b) => {
      if (a.dueDate === null && b.dueDate === null) return 0;
      if (a.dueDate === null) return 1;
      if (b.dueDate === null) return -1;
      return a.dueDate.localeCompare(b.dueDate);
    });
  });

  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredTasks().length / PAGE_SIZE)),
  );

  readonly pagedTasks = computed(() => {
    const start = (this.page() - 1) * PAGE_SIZE;
    return this.filteredTasks().slice(start, start + PAGE_SIZE);
  });

  constructor() {
    // Keep the current page within range when the underlying list shrinks.
    effect(() => {
      const total = this.totalPages();
      if (this.page() > total) {
        this.page.set(total);
      }
    });
  }

  ngOnInit(): void {
    this.fetchTasks();
  }

  fetchTasks(): void {
    this.loading.set(true);
    this.error.set(null);

    this.taskService.getAll().subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load tasks. Please try again.');
        this.loading.set(false);
      },
    });
  }

  setStatusFilter(filter: string): void {
    this.statusFilter.set(filter);
    this.page.set(1);
  }

  toggleAssignedToMe(): void {
    this.assignedToMeOnly.update((value) => !value);
    this.page.set(1);
  }

  completeTask(task: TaskResponse): void {
    // The backend has no dedicated status endpoint — status is updated via the
    // full PUT /api/tasks/{id}. Build a complete UpdateTaskRequest from the task.
    const request: UpdateTaskRequest = {
      title: task.title,
      description: task.description,
      interactionId: task.interactionId,
      dueDate: task.dueDate,
      assignedUserId: task.assignedUserId,
      employeeId: task.employeeId,
      status: 'DONE',
    };
    this.taskService.update(task.id, request).subscribe({
      next: (updated) => {
        this.tasks.update((tasks) => tasks.map((t) => (t.id === updated.id ? updated : t)));
        this.toast.success(`"${task.title}" marked as done`);
      },
      error: () => this.toast.error('Failed to update task status'),
    });
  }

  deleteTask(task: TaskResponse): void {
    if (!confirm(`Delete "${task.title}"?`)) return;
    this.taskService.delete(task.id).subscribe({
      next: () => {
        this.tasks.update((tasks) => tasks.filter((t) => t.id !== task.id));
        this.toast.success(`"${task.title}" deleted`);
      },
      error: () => this.toast.error('Failed to delete task'),
    });
  }

  onNewTaskSubmitted(request: CreateTaskRequest): void {
    this.taskService.create(request).subscribe({
      next: () => {
        this.showCreateModal.set(false);
        this.toast.success('Task created');
        this.fetchTasks();
      },
      error: () => this.toast.error('Failed to create task'),
    });
  }

  startEdit(task: TaskResponse): void {
    this.selectedTask.set(null);
    this.editingTask.set(task);
  }

  onEditTaskSubmitted(request: CreateTaskRequest): void {
    const original = this.editingTask();
    if (!original) return;
    const update: UpdateTaskRequest = {
      ...request,
      status: original.status,
    };
    this.taskService.update(original.id, update).subscribe({
      next: (updated) => {
        this.tasks.update((tasks) => tasks.map((t) => (t.id === updated.id ? updated : t)));
        this.editingTask.set(null);
        this.toast.success('Task updated');
      },
      error: () => this.toast.error('Failed to update task'),
    });
  }

  formatStatus(status: string): string {
    return formatTaskStatusLabel(status);
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'OPEN':
        return 'badge badge-warning';
      case 'DONE':
        return 'badge badge-success';
      default:
        return 'badge';
    }
  }

  isTaskOverdue(task: TaskResponse): boolean {
    return isOverdue(task);
  }
}
