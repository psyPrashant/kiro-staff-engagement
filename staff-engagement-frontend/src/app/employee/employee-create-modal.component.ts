import { Component, inject, input, output, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';

import { ModalComponent } from '../shared';
import { EmployeeService } from '../shared/services/employee.service';
import { CreateEmployeeRequest } from '../shared/models/employee.model';

@Component({
  selector: 'app-employee-create-modal',
  standalone: true,
  imports: [ModalComponent, ReactiveFormsModule],
  template: `
    <app-modal title="Add employee" (close)="closed.emit()">
      @if (apiError()) {
        <div class="error-message" role="alert">{{ apiError() }}</div>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" class="employee-form" novalidate>
        <div class="form-field">
          <label for="emp-name">Name <span class="required">*</span></label>
          <input
            id="emp-name"
            type="text"
            formControlName="name"
            maxlength="255"
            placeholder="Full name"
            data-testid="employee-name"
          />
          @if (isInvalid('name')) {
            <span class="field-error" role="alert">Name is required</span>
          }
        </div>

        <div class="form-field">
          <label for="emp-email">Email <span class="required">*</span></label>
          <input
            id="emp-email"
            type="email"
            formControlName="email"
            maxlength="255"
            placeholder="name@example.com"
            data-testid="employee-email"
          />
          @if (isInvalid('email')) {
            <span class="field-error" role="alert">
              {{ form.get('email')?.hasError('required') ? 'Email is required' : 'Enter a valid email' }}
            </span>
          }
        </div>

        <div class="form-field">
          <label for="emp-title">Job title</label>
          <input
            id="emp-title"
            type="text"
            formControlName="jobTitle"
            maxlength="255"
            placeholder="e.g. Software Engineer"
          />
        </div>

        <div class="form-field">
          <label for="emp-manager">Manager</label>
          <select id="emp-manager" formControlName="managerId">
            <option [ngValue]="null">None</option>
            @for (mgr of managers(); track mgr.id) {
              <option [ngValue]="mgr.id">{{ mgr.name }}</option>
            }
          </select>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" [disabled]="submitting()">
            {{ submitting() ? 'Adding...' : 'Add employee' }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="closed.emit()">Cancel</button>
        </div>
      </form>
    </app-modal>
  `,
  styles: [
    `
      .employee-form {
        display: flex;
        flex-direction: column;
        gap: var(--space-3);
      }
      .required {
        color: var(--color-danger);
      }
      .field-error {
        font-size: var(--fs-xs);
        color: var(--color-danger);
        margin-top: var(--space-1);
      }
      .error-message {
        color: var(--color-danger);
        margin-bottom: var(--space-3);
      }
      .form-actions {
        display: flex;
        gap: var(--space-3);
        margin-top: var(--space-2);
      }
    `,
  ],
})
export class EmployeeCreateModalComponent {
  private readonly employeeService = inject(EmployeeService);

  readonly managers = input<{ id: number; name: string }[]>([]);

  readonly closed = output<void>();
  readonly created = output<void>();

  readonly submitting = signal(false);
  readonly apiError = signal<string | null>(null);

  readonly form = new FormGroup({
    name: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(255)],
    }),
    email: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(255)],
    }),
    jobTitle: new FormControl<string>('', { nonNullable: true }),
    managerId: new FormControl<number | null>(null),
  });

  isInvalid(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && control.touched;
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) return;

    this.apiError.set(null);
    this.submitting.set(true);

    const raw = this.form.getRawValue();
    const request: CreateEmployeeRequest = {
      name: raw.name.trim(),
      email: raw.email.trim(),
      jobTitle: raw.jobTitle?.trim() || null,
      managerId: raw.managerId ?? null,
    };

    this.employeeService.create(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.created.emit();
      },
      error: (err) => {
        this.submitting.set(false);
        this.apiError.set(err?.error?.message ?? 'Failed to add employee. Please try again.');
      },
    });
  }
}
