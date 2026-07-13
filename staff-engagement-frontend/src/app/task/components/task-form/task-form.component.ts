import { Component, effect, inject, input, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';

import { EmployeeService } from '../../../shared/services/employee.service';
import { UserService } from '../../../shared/services/user.service';
import { Employee } from '../../../shared/models/employee.model';
import { User } from '../../../core/models/user.model';
import { CreateTaskRequest } from '../../models/task.model';

@Component({
  selector: 'app-task-form',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './task-form.component.html',
  styleUrl: './task-form.component.css',
})
export class TaskFormComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly userService = inject(UserService);

  // Inputs
  readonly employeeId = input<number | null>(null);
  readonly defaultAssigneeId = input<number | null>(null);
  readonly interactions = input<{ id: number; label: string }[]>([]);

  // Outputs
  readonly submitted = output<CreateTaskRequest>();
  readonly cancelled = output<void>();

  // Picker data
  readonly employees = signal<Employee[]>([]);
  readonly users = signal<User[]>([]);
  readonly employeesLoading = signal(false);
  readonly usersLoading = signal(false);

  // Form
  form!: FormGroup;

  // Track touched fields for blur validation
  readonly fieldTouched = signal<Record<string, boolean>>({});

  constructor() {
    // Effect to pre-fill and disable employee when input changes dynamically
    effect(() => {
      const empId = this.employeeId();
      if (!this.form) return;
      if (empId != null) {
        this.form.get('employeeId')!.setValue(empId);
        this.form.get('employeeId')!.disable();
      } else {
        this.form.get('employeeId')!.enable();
      }
    });
  }

  ngOnInit(): void {
    this.form = new FormGroup({
      employeeId: new FormControl<number | null>(null, [Validators.required]),
      assignedUserId: new FormControl<number | null>(null),
      title: new FormControl<string>('', [Validators.required, Validators.maxLength(255)]),
      description: new FormControl<string>('', [Validators.maxLength(2000)]),
      dueDate: new FormControl<string | null>(null),
      interactionId: new FormControl<number | null>(null),
    });

    // Pre-fill from inputs if already set
    const empId = this.employeeId();
    if (empId != null) {
      this.form.get('employeeId')!.setValue(empId);
      this.form.get('employeeId')!.disable();
    }

    const assigneeId = this.defaultAssigneeId();
    if (assigneeId != null) {
      this.form.get('assignedUserId')!.setValue(assigneeId);
    }

    this.loadEmployees();
    this.loadUsers();
  }

  private loadEmployees(): void {
    this.employeesLoading.set(true);
    this.employeeService.getAll().subscribe({
      next: (data) => {
        this.employees.set(data);
        this.employeesLoading.set(false);
      },
      error: () => {
        this.employeesLoading.set(false);
      },
    });
  }

  private loadUsers(): void {
    this.usersLoading.set(true);
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users.set(data);
        this.usersLoading.set(false);
      },
      error: () => {
        this.usersLoading.set(false);
      },
    });
  }

  onFieldBlur(fieldName: string): void {
    this.fieldTouched.update((touched) => ({ ...touched, [fieldName]: true }));
    this.form.get(fieldName)?.markAsTouched();
  }

  isFieldInvalid(fieldName: string): boolean {
    const control = this.form.get(fieldName);
    if (!control) return false;
    return control.invalid && (control.touched || this.fieldTouched()[fieldName] === true);
  }

  getFieldError(fieldName: string): string | null {
    const control = this.form.get(fieldName);
    if (!control || !control.errors) return null;

    if (control.errors['required']) {
      return this.getFieldLabel(fieldName) + ' is required';
    }
    if (control.errors['maxlength']) {
      const max = control.errors['maxlength'].requiredLength;
      return this.getFieldLabel(fieldName) + ` must be at most ${max} characters`;
    }
    return null;
  }

  private getFieldLabel(fieldName: string): string {
    switch (fieldName) {
      case 'employeeId':
        return 'Employee';
      case 'title':
        return 'Title';
      case 'description':
        return 'Description';
      default:
        return fieldName;
    }
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    // Mark all fields as touched for validation display
    this.fieldTouched.set({
      employeeId: true,
      assignedUserId: true,
      title: true,
      description: true,
      dueDate: true,
      interactionId: true,
    });

    if (this.form.invalid) {
      return;
    }

    const rawValue = this.form.getRawValue();
    const payload: CreateTaskRequest = {
      title: rawValue.title.trim(),
      description: rawValue.description?.trim() || null,
      interactionId: rawValue.interactionId || null,
      dueDate: rawValue.dueDate || null,
      assignedUserId: rawValue.assignedUserId || null,
      employeeId: rawValue.employeeId,
    };

    this.submitted.emit(payload);
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
