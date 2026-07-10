import '@angular/compiler';
import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { notBlankValidator, futureDateValidator } from './validators/not-blank.validator';
import { InteractionType, formatInteractionTypeLabel } from './models/interaction-type.enum';
import { formatDateTimeLocal } from './log-interaction.component';

describe('Feature: log-interaction-frontend, Property 2: Whitespace-only notes are rejected', () => {
  /**
   * Validates: Requirements 2.3
   *
   * For any string composed entirely of whitespace characters (spaces, tabs,
   * newlines, of any length including empty string), setting it as the notes
   * field value SHALL cause the form to be invalid with a notBlank or required
   * validation error.
   */
  it('rejects whitespace-only strings', () => {
    fc.assert(
      fc.property(fc.stringMatching(/^[ \t\n\r]+$/), (whitespaceStr) => {
        const control = new FormControl<string>(whitespaceStr, [
          Validators.required,
          notBlankValidator,
        ]);
        expect(control.valid).toBe(false);
        expect(control.hasError('notBlank') || control.hasError('required')).toBe(true);
      }),
      { numRuns: 100 },
    );
  });

  it('rejects the empty string', () => {
    const control = new FormControl<string>('', [Validators.required, notBlankValidator]);
    expect(control.valid).toBe(false);
    expect(control.hasError('notBlank') || control.hasError('required')).toBe(true);
  });
});

describe('Feature: log-interaction-frontend, Property 3: Valid required fields enable submission', () => {
  /**
   * Validates: Requirements 2.6
   *
   * For any combination of valid values (non-null employeeId, non-null
   * conductedByUserId, non-null type from InteractionType enum, non-whitespace
   * notes string, non-empty occurredAt), the form SHALL be valid.
   */
  it('form is valid when all required fields have valid values', () => {
    fc.assert(
      fc.property(
        fc.record({
          employeeId: fc.nat().map((n) => n + 1),
          conductedByUserId: fc.nat().map((n) => n + 1),
          type: fc.constantFrom(...Object.values(InteractionType)),
          notes: fc.string({ minLength: 1 }).filter((s) => s.trim().length > 0),
          occurredAt: fc.constant(formatDateTimeLocal(new Date())),
        }),
        (values) => {
          const form = new FormGroup({
            employeeId: new FormControl<number | null>(null, [Validators.required]),
            conductedByUserId: new FormControl<number | null>(null, [Validators.required]),
            type: new FormControl<InteractionType | null>(null, [Validators.required]),
            notes: new FormControl<string>('', [Validators.required, notBlankValidator]),
            occurredAt: new FormControl<string>('', [Validators.required]),
            projectId: new FormControl<number | null>(null),
            taskTitle: new FormControl<string>('', []),
            taskDescription: new FormControl<string>('', []),
            taskDueDate: new FormControl<string | null>(null, []),
            taskAssignedUserId: new FormControl<number | null>(null, []),
          });

          form.patchValue({
            employeeId: values.employeeId,
            conductedByUserId: values.conductedByUserId,
            type: values.type,
            notes: values.notes,
            occurredAt: values.occurredAt,
          });

          expect(form.valid).toBe(true);
        },
      ),
      { numRuns: 100 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 6: Task title required when section expanded', () => {
  /**
   * Validates: Requirements 4.2
   *
   * For any string that is empty or composed entirely of whitespace characters,
   * when the Inline_Task_Section is expanded and that string is set as the task
   * title, the form SHALL be invalid with a validation error on the task title
   * field.
   */
  it('rejects empty or whitespace-only task titles when section is expanded', () => {
    fc.assert(
      fc.property(fc.stringMatching(/^[ \t\n\r]*$/), (whitespaceStr) => {
        const taskTitle = new FormControl<string>(whitespaceStr, [
          Validators.required,
          Validators.maxLength(255),
          notBlankValidator,
        ]);

        expect(taskTitle.valid).toBe(false);
        expect(taskTitle.hasError('required') || taskTitle.hasError('notBlank')).toBe(true);
      }),
      { numRuns: 100 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 9: Past due dates are rejected', () => {
  /**
   * Validates: Requirements 4.8
   *
   * For any date value that represents a calendar day before today, setting it
   * as the task dueDate value SHALL cause the form to be invalid with a
   * futureDate validation error on the dueDate control.
   */
  it('rejects dates before today', () => {
    fc.assert(
      fc.property(
        fc
          .date({
            min: new Date('1970-01-01'),
            max: new Date(Date.now() - 86400000),
          })
          .filter((d) => !isNaN(d.getTime())),
        (pastDate) => {
          const formatted = pastDate.toISOString().split('T')[0]; // YYYY-MM-DD
          const control = new FormControl<string | null>(formatted, [futureDateValidator]);

          expect(control.valid).toBe(false);
          expect(control.hasError('futureDate')).toBe(true);
        },
      ),
      { numRuns: 100 },
    );
  });
});

describe('Feature: log-interaction-frontend, Property 10: Interaction type label formatting', () => {
  /**
   * Validates: Requirements 8.2
   *
   * For any InteractionType enum value, the formatInteractionTypeLabel function
   * SHALL produce a string where underscores are replaced by spaces and each
   * word is capitalized (Title Case).
   */
  it('formats each interaction type as Title Case', () => {
    fc.assert(
      fc.property(fc.constantFrom(...Object.values(InteractionType)), (type) => {
        const expected = type
          .split('_')
          .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
          .join(' ');
        expect(formatInteractionTypeLabel(type)).toBe(expected);
      }),
      { numRuns: 100 },
    );
  });
});
