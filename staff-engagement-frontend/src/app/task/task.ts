import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { TaskService } from './services/task.service';
import {
  CreateTaskRequest,
  TaskResponse,
  formatTaskStatusLabel,
  isOverdue,
} from './models/task.model';
import { TaskFormComponent } from './components/task-form/task-form.component';
import { ToastService, ModalComponent } from '../shared';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-task',
  standalone: true,
  imports: [TaskFormComponent, ModalComponent, DatePipe],
  templateUrl: './task.html',
  styleUrl: './task.css',
})
export class TaskListComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly toast = inject(ToastService);

  readonly tasks = signal<TaskResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<string>('OPEN');
  readonly showCreateModal = signal<boolean>(false);
  readonly selectedTask = signal<TaskResponse | null>(null);

  readonly filteredTasks = computed(() => {
    const filter = this.statusFilter();
    const all = this.tasks();
    if (filter === 'ALL') {
      return all;
    }
    return all.filter((task) => task.status === filter);
  });

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
  }

  completeTask(task: TaskResponse): void {
    this.taskService.updateStatus(task.id, 'DONE').subscribe({
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
