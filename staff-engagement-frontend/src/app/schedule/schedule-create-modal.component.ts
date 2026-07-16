import { Component, inject, input, output, signal } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';

import { ModalComponent } from '../shared';
import { SchedulingService } from './services/scheduling.service';
import {
  CreateScheduledInteractionRequest,
  InteractionType,
} from './models/scheduled-interaction.model';

@Component({
  selector: 'app-schedule-create-modal',
  standalone: true,
  imports: [ModalComponent, ReactiveFormsModule],
  template: `
    <app-modal title="Schedule interaction" (closeModal)="closed.emit()">
      @if (apiError()) {
        <div class="error-message" role="alert">{{ apiError() }}</div>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" class="schedule-form" novalidate>
        <div class="form-field">
          <label for="schedule-date">Scheduled date <span class="required">*</span></label>
          <input
            id="schedule-date"
            type="date"
            formControlName="scheduledDate"
            [min]="today"
            data-testid="schedule-date"
          />
          @if (dateError()) {
            <span class="field-error" role="alert">{{ dateError() }}</span>
          }
        </div>

        <div class="form-field">
          <label for="schedule-type">Interaction type <span class="required">*</span></label>
          <select id="schedule-type" formControlName="interactionType">
            @for (type of interactionTypes; track type.value) {
              <option [value]="type.value">{{ type.label }}</option>
            }
          </select>
        </div>

        <div class="form-field">
          <label for="schedule-notes">Notes</label>
          <textarea
            id="schedule-notes"
            formControlName="notes"
            rows="3"
            maxlength="2000"
            placeholder="Optional notes"
          ></textarea>
        </div>

        <div class="form-actions">
          <button type="submit" class="btn btn-primary" [disabled]="submitting()">
            {{ submitting() ? 'Scheduling...' : 'Schedule' }}
          </button>
          <button type="button" class="btn btn-secondary" (click)="closed.emit()">Cancel</button>
        </div>
      </form>
    </app-modal>
  `,
  styles: [
    `
      .schedule-form {
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
export class ScheduleCreateModalComponent {
  private readonly schedulingService = inject(SchedulingService);

  readonly employeeId = input.required<number>();

  readonly closed = output<void>();
  readonly created = output<void>();

  readonly submitting = signal(false);
  readonly apiError = signal<string | null>(null);
  readonly dateError = signal<string | null>(null);

  readonly today = this.todayString();

  readonly interactionTypes: { value: InteractionType; label: string }[] = [
    { value: 'CHECK_IN', label: 'Check In' },
    { value: 'MENTORING', label: 'Mentoring' },
    { value: 'CATCH_UP', label: 'Catch Up' },
    { value: 'OTHER', label: 'Other' },
  ];

  readonly form = new FormGroup({
    scheduledDate: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    interactionType: new FormControl<InteractionType>('CHECK_IN', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    notes: new FormControl<string>('', {
      nonNullable: true,
      validators: [Validators.maxLength(2000)],
    }),
  });

  submit(): void {
    this.dateError.set(null);
    this.apiError.set(null);

    const date = this.form.controls.scheduledDate.value;
    if (!date) {
      this.dateError.set('Scheduled date is required.');
      return;
    }
    if (date < this.today) {
      this.dateError.set('Scheduled date must be today or in the future.');
      return;
    }
    if (this.form.invalid || this.submitting()) return;

    this.submitting.set(true);
    const request: CreateScheduledInteractionRequest = {
      employeeId: this.employeeId(),
      scheduledDate: date,
      interactionType: this.form.controls.interactionType.value,
      notes: this.form.controls.notes.value || undefined,
    };

    this.schedulingService.create(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.created.emit();
      },
      error: (err) => {
        this.submitting.set(false);
        this.apiError.set(err?.error?.message ?? 'Failed to schedule interaction. Please try again.');
      },
    });
  }

  private todayString(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
