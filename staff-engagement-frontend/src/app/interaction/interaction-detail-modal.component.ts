import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';

import { ModalComponent } from '../shared';
import { InteractionService } from './services/interaction.service';
import { InteractionDto } from '../employee/models/employee-360.model';
import {
  InteractionType,
  INTERACTION_TYPES,
  formatInteractionTypeLabel,
} from './models/interaction-type.enum';

@Component({
  selector: 'app-interaction-detail-modal',
  standalone: true,
  imports: [ModalComponent, FormsModule, DatePipe],
  template: `
    <app-modal title="Interaction" (closeModal)="closed.emit()">
      <div class="detail-body" data-testid="interaction-detail">
        <div class="detail-row">
          <span class="detail-label">Conducted by</span>
          <span>{{ interaction().conductedByName }}</span>
        </div>

        @if (editing()) {
          <div class="detail-field">
            <label for="int-type">Type</label>
            <select id="int-type" [(ngModel)]="editType">
              @for (type of interactionTypes; track type.value) {
                <option [ngValue]="type.value">{{ type.label }}</option>
              }
            </select>
          </div>
          <div class="detail-field">
            <label for="int-date">Occurred at</label>
            <input id="int-date" type="datetime-local" [(ngModel)]="editOccurredAt" />
          </div>
          <div class="detail-field">
            <label for="int-notes">Notes</label>
            <textarea id="int-notes" rows="4" [(ngModel)]="editNotes"></textarea>
            @if (notesError()) {
              <span class="field-error">{{ notesError() }}</span>
            }
          </div>
        } @else {
          <div class="detail-row">
            <span class="detail-label">Type</span>
            <span class="badge badge-primary">{{ typeLabel(interaction().type) }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Occurred at</span>
            <span>{{ interaction().occurredAt | date: 'medium' }}</span>
          </div>
          @if (interaction().projectContext; as ctx) {
            <div class="detail-row">
              <span class="detail-label">Project</span>
              <span>{{ ctx.projectName }} ({{ ctx.companyName }})</span>
            </div>
          }
          <div class="detail-row detail-row--block">
            <span class="detail-label">Notes</span>
            <p class="detail-notes">{{ interaction().notes }}</p>
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
            <button class="btn btn-secondary" [disabled]="saving()" (click)="editing.set(false)">
              Cancel
            </button>
          } @else {
            <button class="btn btn-secondary" (click)="startEdit()" data-testid="edit-interaction">
              Edit
            </button>
            <button
              class="btn btn-danger"
              [disabled]="saving()"
              (click)="remove()"
              data-testid="delete-interaction"
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
export class InteractionDetailModalComponent {
  private readonly interactionService = inject(InteractionService);

  readonly interaction = input.required<InteractionDto>();

  readonly closed = output<void>();
  readonly changed = output<void>();

  readonly editing = signal(false);
  readonly saving = signal(false);
  readonly apiError = signal<string | null>(null);
  readonly notesError = signal<string | null>(null);

  readonly interactionTypes = INTERACTION_TYPES;

  editType: InteractionType = InteractionType.CHECK_IN;
  editNotes = '';
  editOccurredAt = '';

  readonly currentProjectId = computed(() => this.interaction().projectId ?? null);

  typeLabel(type: string): string {
    return formatInteractionTypeLabel(type as InteractionType);
  }

  startEdit(): void {
    this.editType = this.interaction().type as InteractionType;
    this.editNotes = this.interaction().notes;
    this.editOccurredAt = this.toDateTimeLocal(this.interaction().occurredAt);
    this.apiError.set(null);
    this.notesError.set(null);
    this.editing.set(true);
  }

  save(): void {
    this.notesError.set(null);
    if (!this.editNotes || this.editNotes.trim().length === 0) {
      this.notesError.set('Notes are required.');
      return;
    }
    this.saving.set(true);
    this.apiError.set(null);
    this.interactionService
      .update(this.interaction().id, {
        type: this.editType,
        notes: this.editNotes.trim(),
        occurredAt: new Date(this.editOccurredAt).toISOString(),
        projectId: this.currentProjectId(),
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.editing.set(false);
          this.changed.emit();
        },
        error: (err) => {
          this.saving.set(false);
          this.apiError.set(err?.error?.message ?? 'Failed to update the interaction.');
        },
      });
  }

  remove(): void {
    this.saving.set(true);
    this.apiError.set(null);
    this.interactionService.delete(this.interaction().id).subscribe({
      next: () => {
        this.saving.set(false);
        this.changed.emit();
      },
      error: (err) => {
        this.saving.set(false);
        this.apiError.set(err?.error?.message ?? 'Failed to delete the interaction.');
      },
    });
  }

  private toDateTimeLocal(iso: string): string {
    const date = new Date(iso);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  }
}
