import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';

import { AuthService } from '../core/services/auth.service';
import { InteractionService } from './services/interaction.service';
import { TaskService } from '../task/services/task.service';
import { EmployeeService } from '../shared/services/employee.service';
import { UserService } from '../shared/services/user.service';
import { ProjectService } from '../shared/services/project.service';

import { Employee } from '../shared/models/employee.model';
import { User } from '../core/models/user.model';
import { Project } from '../shared/models/project.model';
import { InteractionType, INTERACTION_TYPES } from './models/interaction-type.enum';
import {
  CreateInteractionRequest,
  InteractionResponse,
  ApiErrorResponse,
} from './models/interaction.model';
import { CreateTaskRequest } from '../task/models/task.model';
import { notBlankValidator, futureDateValidator } from './validators/not-blank.validator';

export function formatDateTimeLocal(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

@Component({
  selector: 'app-log-interaction',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './log-interaction.component.html',
  styleUrl: './log-interaction.component.css',
})
export class LogInteractionComponent implements OnInit {
  // Injected services
  private authService = inject(AuthService);
  private interactionService = inject(InteractionService);
  private taskService = inject(TaskService);
  private employeeService = inject(EmployeeService);
  private userService = inject(UserService);
  private projectService = inject(ProjectService);

  // Picker data signals
  employees = signal<Employee[]>([]);
  users = signal<User[]>([]);
  projects = signal<Project[]>([]);

  // Grouped projects by company for optgroup rendering
  readonly projectsByCompany = computed(() => {
    const groups = new Map<string, Project[]>();
    for (const p of this.projects()) {
      const list = groups.get(p.companyName) ?? [];
      list.push(p);
      groups.set(p.companyName, list);
    }
    return groups;
  });

  // Loading/error signals per picker
  employeesLoading = signal(false);
  usersLoading = signal(false);
  projectsLoading = signal(false);
  employeesError = signal<string | null>(null);
  usersError = signal<string | null>(null);
  projectsError = signal<string | null>(null);

  // Submission state
  submitting = signal(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  serverFieldErrors = signal<Record<string, string>>({});
  taskErrorMessage = signal<string | null>(null);

  // Task section visibility
  taskSectionExpanded = signal(false);

  // Interaction types
  interactionTypes = INTERACTION_TYPES;

  // Reactive form
  form!: FormGroup;

  currentUser = this.authService.currentUser;

  ngOnInit(): void {
    this.form = new FormGroup({
      employeeId: new FormControl<number | null>(null, [Validators.required]),
      conductedByUserId: new FormControl<number | null>(this.currentUser()?.id ?? null, [
        Validators.required,
      ]),
      type: new FormControl<InteractionType | null>(null, [Validators.required]),
      notes: new FormControl<string>('', [Validators.required, notBlankValidator]),
      occurredAt: new FormControl<string>(formatDateTimeLocal(new Date()), [Validators.required]),
      projectId: new FormControl<number | null>(null),
      // Task sub-form (conditionally validated)
      taskTitle: new FormControl<string>('', []),
      taskDescription: new FormControl<string>('', []),
      taskDueDate: new FormControl<string | null>(null, []),
      taskAssignedUserId: new FormControl<number | null>(null, []),
    });

    this.loadEmployees();
    this.loadUsers();
    this.loadProjects();
  }

  loadEmployees(): void {
    this.employeesLoading.set(true);
    this.employeesError.set(null);
    this.form.get('employeeId')!.disable();

    this.employeeService.getAll().subscribe({
      next: (data) => {
        this.employees.set(data);
        this.employeesLoading.set(false);
        this.form.get('employeeId')!.enable();
      },
      error: () => {
        this.employeesError.set('Failed to load employees');
        this.employeesLoading.set(false);
      },
    });
  }

  loadUsers(): void {
    this.usersLoading.set(true);
    this.usersError.set(null);
    this.form.get('conductedByUserId')!.disable();

    this.userService.getAll().subscribe({
      next: (data) => {
        this.users.set(data);
        this.usersLoading.set(false);
        this.form.get('conductedByUserId')!.enable();
      },
      error: () => {
        this.usersError.set('Failed to load users');
        this.usersLoading.set(false);
      },
    });
  }

  loadProjects(): void {
    this.projectsLoading.set(true);
    this.projectsError.set(null);
    this.form.get('projectId')!.disable();

    this.projectService.getAll().subscribe({
      next: (data) => {
        this.projects.set(data);
        this.projectsLoading.set(false);
        this.form.get('projectId')!.enable();
      },
      error: () => {
        this.projectsError.set('Failed to load projects');
        this.projectsLoading.set(false);
      },
    });
  }

  retryEmployees(): void {
    this.loadEmployees();
  }

  retryUsers(): void {
    this.loadUsers();
  }

  retryProjects(): void {
    this.loadProjects();
  }

  toggleTaskSection(): void {
    this.taskSectionExpanded.update((v) => !v);

    const taskTitle = this.form.get('taskTitle')!;
    const taskDescription = this.form.get('taskDescription')!;
    const taskDueDate = this.form.get('taskDueDate')!;
    const taskAssignedUserId = this.form.get('taskAssignedUserId')!;

    if (this.taskSectionExpanded()) {
      taskTitle.setValidators([Validators.required, Validators.maxLength(255), notBlankValidator]);
      taskDueDate.setValidators([futureDateValidator]);
    } else {
      taskTitle.clearValidators();
      taskDescription.clearValidators();
      taskDueDate.clearValidators();
      taskAssignedUserId.clearValidators();

      taskTitle.reset('');
      taskDescription.reset('');
      taskDueDate.reset(null);
      taskAssignedUserId.reset(null);
    }

    taskTitle.updateValueAndValidity();
    taskDueDate.updateValueAndValidity();
    taskDescription.updateValueAndValidity();
    taskAssignedUserId.updateValueAndValidity();
  }

  onSubmit(): void {
    // Clear previous messages
    this.successMessage.set(null);
    this.errorMessage.set(null);
    this.taskErrorMessage.set(null);
    this.serverFieldErrors.set({});

    // Mark all controls as touched to trigger validation display
    this.form.markAllAsTouched();

    // If form is invalid, return early
    if (this.form.invalid) {
      return;
    }

    // Set submitting state
    this.submitting.set(true);

    // Build the CreateInteractionRequest payload
    const rawValue = this.form.getRawValue();
    const payload: CreateInteractionRequest = {
      employeeId: rawValue.employeeId,
      conductedByUserId: rawValue.conductedByUserId,
      loggedByUserId: this.currentUser()!.id,
      type: rawValue.type,
      notes: rawValue.notes,
      occurredAt: new Date(rawValue.occurredAt).toISOString(),
      projectId: rawValue.projectId ?? null,
    };

    // Capture task state before any async work / form reset
    const taskExpanded = this.taskSectionExpanded();
    const taskTitle = rawValue.taskTitle?.trim();

    // Call InteractionService.create()
    this.interactionService.create(payload).subscribe({
      next: (response: InteractionResponse) => {
        if (taskExpanded && taskTitle) {
          // Build and submit the follow-up task
          const taskPayload: CreateTaskRequest = {
            title: rawValue.taskTitle,
            description: rawValue.taskDescription || null,
            interactionId: response.id,
            dueDate: rawValue.taskDueDate || null,
            assignedUserId: rawValue.taskAssignedUserId || null,
            employeeId: rawValue.employeeId,
          };

          this.taskService.create(taskPayload).subscribe({
            next: () => {
              this.successMessage.set('Interaction and task created successfully.');
              this.form.reset({
                conductedByUserId: this.currentUser()?.id ?? null,
                occurredAt: formatDateTimeLocal(new Date()),
              });
              this.taskSectionExpanded.set(false);
              this.submitting.set(false);
            },
            error: (taskError: HttpErrorResponse) => {
              this.successMessage.set('Interaction created successfully.');
              const msg = taskError.error?.message || taskError.message || 'Unknown error';
              this.taskErrorMessage.set('Failed to create follow-up task: ' + msg);
              this.form.reset({
                conductedByUserId: this.currentUser()?.id ?? null,
                occurredAt: formatDateTimeLocal(new Date()),
              });
              this.taskSectionExpanded.set(false);
              this.submitting.set(false);
            },
          });
        } else {
          // No task to create — interaction-only success
          this.successMessage.set('Interaction created successfully.');
          this.form.reset({
            conductedByUserId: this.currentUser()?.id ?? null,
            occurredAt: formatDateTimeLocal(new Date()),
          });
          this.taskSectionExpanded.set(false);
          this.submitting.set(false);
        }
      },
      error: (error: HttpErrorResponse) => {
        if (error.status === 400 && error.error) {
          const apiError = error.error as ApiErrorResponse;
          this.errorMessage.set(apiError.message);
          if (apiError.fieldErrors) {
            this.serverFieldErrors.set(apiError.fieldErrors);
          }
        } else {
          this.errorMessage.set('Request failed. Please try again.');
        }
        this.submitting.set(false);
      },
    });
  }
}
