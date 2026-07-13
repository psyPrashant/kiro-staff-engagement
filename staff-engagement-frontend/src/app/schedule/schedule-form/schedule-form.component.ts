import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Location } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { SchedulingService } from '../services/scheduling.service';
import {
  CreateScheduledInteractionRequest,
  InteractionType,
} from '../models/scheduled-interaction.model';

@Component({
  selector: 'app-schedule-form',
  imports: [ReactiveFormsModule],
  templateUrl: './schedule-form.component.html',
  styleUrl: './schedule-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly schedulingService = inject(SchedulingService);
  private readonly destroyRef = inject(DestroyRef);
  private notificationTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly employeeId = signal<number | null>(null);
  readonly employeeError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly apiError = signal<string | null>(null);
  readonly dateError = signal<string | null>(null);
  readonly successNotification = signal(false);

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (this.notificationTimeout) clearTimeout(this.notificationTimeout);
    });
  }

  readonly interactionTypes: { value: InteractionType; label: string }[] = [
    { value: 'CHECK_IN', label: 'Check In' },
    { value: 'MENTORING', label: 'Mentoring' },
    { value: 'CATCH_UP', label: 'Catch Up' },
    { value: 'OTHER', label: 'Other' },
  ];

  form = new FormGroup({
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

  ngOnInit(): void {
    const raw = this.route.snapshot.queryParamMap.get('employeeId');
    const id = Number(raw);

    if (!raw || isNaN(id) || id <= 0) {
      this.employeeError.set('Invalid or missing employee ID. Cannot schedule interaction.');
      return;
    }

    this.employeeId.set(id);
  }

  onDateChange(): void {
    const value = this.form.controls.scheduledDate.value;
    if (!value) {
      this.dateError.set(null);
      return;
    }

    const today = this.todayString();
    if (value < today) {
      this.dateError.set('Scheduled date must be today or in the future.');
    } else {
      this.dateError.set(null);
    }
  }

  get isFormValid(): boolean {
    return (
      this.form.valid &&
      !this.dateError() &&
      !!this.employeeId() &&
      !this.employeeError() &&
      !!this.form.controls.scheduledDate.value
    );
  }

  submit(): void {
    if (!this.isFormValid || this.submitting()) return;

    this.submitting.set(true);
    this.apiError.set(null);

    const request: CreateScheduledInteractionRequest = {
      employeeId: this.employeeId()!,
      scheduledDate: this.form.controls.scheduledDate.value,
      interactionType: this.form.controls.interactionType.value,
      notes: this.form.controls.notes.value || undefined,
    };

    this.schedulingService.create(request).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: () => {
        this.submitting.set(false);
        this.successNotification.set(true);

        this.notificationTimeout = setTimeout(() => {
          this.successNotification.set(false);
          this.notificationTimeout = null;
        }, 3500);

        this.location.back();
      },
      error: (err) => {
        this.submitting.set(false);
        this.apiError.set(
          err.error?.message ?? 'Failed to schedule interaction. Please try again.'
        );
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
