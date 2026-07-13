import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskService } from './services/task.service';
import { CreateTaskRequest, TaskResponse, formatTaskStatusLabel, isOverdue } from './models/task.model';
import { TaskFormComponent } from './components/task-form/task-form.component';

@Component({
  selector: 'app-task',
  standalone: true,
  imports: [CommonModule, TaskFormComponent],
  templateUrl: './task.html',
  styleUrl: './task.css',
})
export class TaskListComponent implements OnInit {
  private readonly taskService = inject(TaskService);

  readonly tasks = signal<TaskResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<string>('OPEN');
  readonly showNewTaskForm = signal<boolean>(false);

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

  toggleStatus(task: TaskResponse): void {
    const newStatus = task.status === 'OPEN' ? 'DONE' : 'OPEN';
    this.taskService.updateStatus(task.id, newStatus).subscribe({
      next: (updated) => {
        this.tasks.update((tasks) =>
          tasks.map((t) => (t.id === updated.id ? updated : t))
        );
      },
      error: () => {
        // Silently fail — could add error toast in future
      },
    });
  }

  onNewTaskSubmitted(request: CreateTaskRequest): void {
    this.taskService.create(request).subscribe({
      next: () => {
        this.showNewTaskForm.set(false);
        this.fetchTasks();
      },
      error: () => {
        // Could show inline error in future
      },
    });
  }

  formatStatus(status: string): string {
    return formatTaskStatusLabel(status);
  }

  isTaskOverdue(task: TaskResponse): boolean {
    return isOverdue(task);
  }
}
