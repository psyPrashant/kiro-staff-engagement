import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';

import { ModalComponent } from '../shared';
import { SchedulingService } from './services/scheduling.service';
import { ScheduledInteraction } from './models/scheduled-interaction.model';

const TYPE_LABELS: Record<string, string> = {
  CHECK_IN: 'Check In',
  MENTORING: 'Mentoring',
  CATCH_UP: 'Catch Up',
  OTHER: 'Other',
};

@Component({
  selector: 'app-scheduled-interaction-detail-modal',
  standalone: true,
  imports: [ModalComponent, FormsModule, DatePipe],
  template: `
    <app-modal title="Scheduled interaction" (closeModal)="closed.emit()">
      <div class="detail-body" data-testid="scheduled-detail">
        <div class="detail-row">
          <span class="detail-label">Employee</span>
          <span>{{ interaction().employeeName }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">Type</span>
          <span>{{ typeLabel(interaction().interactionType) }}</span>
        </div>

        @if (editing()) {
          <div class="detail-field">
            <label [attr.for]="'sched-date'">Scheduled date</label>
            <input id="sched-date" type="date" [(ngModel)]="editDate" />
            @if (dateError()) {
              <span class="field-error">{{ dateError() }}</span>
            }
          </div>
          <div class="detail-field">
            <label [attr.for]="'sched-notes'">Notes</label>
            <textarea id="sched-notes" rows="3" maxlength="2000" [(ngModel)]="editNotes"></textarea>
          </div>
        } @else {
          <div class="detail-row">
            <span class="detail-label">Scheduled date</span>
            <span>{{ interaction().scheduledDate | date }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Status</span>
            <span class="badge" [class.badge-danger]="interaction().overdue">
              {{ interaction().overdue ? 'Overdue' : 'Pending' }}
            </span>
          </div>
          <div class="detail-row detail-row--block">
            <span class="detail-label">Notes</span>
            <p class="detail-notes">{{ interaction().notes || 'No notes' }}</p>
          </div>
        }

        @if (apiError()) {
          <p class="field-error" role="alert">{{ apiError() }}</p>
        }

        <div class="detail-actions">
          @if (editing()) {
            <button class="btn btn-primary" [disabled]="saving()" (click)="save()">
              {{ saving() ? 'Saving...' : 'Save' }}
            </button>
            <button class="btn btn-secondary" [disabled]="saving()" (click)="cancelEdit()">
              Cancel
            </button>
          } @else {
            <button class="btn btn-secondary" (click)="startEdit()" data-testid="edit-scheduled">
              Edit
            </button>
            <button
              class="btn btn-danger"
              [disabled]="saving()"
              (click)="remove()"
              data-testid="delete-scheduled"
            >
              Delete
            </button>
          }
        </div>
      </div>
    </app-modal>
  `,
  styles: [
    `
      .detail-body {
        display: flex;
        flex-direction: column;
        gap: var(--space-3);
      }
      .detail-row {
        display: flex;
        justify-content: space-between;
        gap: var(--space-3);
      }
      .detail-row--block {
        flex-direction: column;
        gap: var(--space-1);
      }
      .detail-label {
        font-weight: var(--fw-medium);
        color: var(--color-muted);
      }
      .detail-notes {
        margin: 0;
        white-space: pre-wrap;
      }
      .detail-field {
        display: flex;
        flex-direction: column;
        gap: var(--space-1);
      }
      .field-error {
        color: var(--color-danger);
        font-size: var(--fs-xs);
      }
      .detail-actions {
        display: flex;
        gap: var(--space-3);
        margin-top: var(--space-3);
      }
    `,
  ],
})
export class ScheduledInteractionDetailModalComponent {
  private readonly schedulingService = inject(SchedulingService);

  readonly interaction = input.required<ScheduledInteraction>();

  readonly closed = output<void>();
  readonly changed = output<void>();

  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly apiError = signal<string | null>(null);
  readonly dateError = signal<string | null>(null);

  editDate = '';
  editNotes = '';

  typeLabel(type: string): string {
    return TYPE_LABELS[type] ?? type;
  }

  startEdit(): void {
    this.editDate = this.interaction().scheduledDate;
    this.editNotes = this.interaction().notes ?? '';
    this.apiError.set(null);
    this.dateError.set(null);
    this.editing.set(true);
  }

  cancelEdit(): void {
    this.editing.set(false);
  }

  save(): void {
    this.dateError.set(null);
    if (!this.editDate) {
      this.dateError.set('Scheduled date is required.');
      return;
    }
    this.saving.set(true);
    this.apiError.set(null);
    this.schedulingService
      .update(this.interaction().id, {
        scheduledDate: this.editDate,
        notes: this.editNotes,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.editing.set(false);
          this.changed.emit();
        },
        error: (err) => {
          this.saving.set(false);
          this.apiError.set(err?.error?.message ?? 'Failed to update the scheduled interaction.');
        },
      });
  }

  remove(): void {
    this.saving.set(true);
    this.apiError.set(null);
    this.schedulingService.delete(this.interaction().id).subscribe({
      next: () => {
        this.saving.set(false);
        this.changed.emit();
      },
      error: (err) => {
        this.saving.set(false);
        this.apiError.set(err?.error?.message ?? 'Failed to delete the scheduled interaction.');
      },
    });
  }
}
