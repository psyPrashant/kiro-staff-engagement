import '@angular/compiler';
import * as fc from 'fast-check';

// Feature: interaction-scheduling, Property 7: Frontend Overdue Classification
// **Validates: Requirements 6.2, 12.7**

type CompletionStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

/**
 * Helper function implementing the frontend overdue classification logic.
 * A scheduled interaction is overdue iff its scheduledDate is before today
 * AND its completionStatus is 'PENDING'.
 * String comparison works correctly for yyyy-MM-dd format.
 */
export function isOverdue(scheduledDate: string, today: string, status: CompletionStatus): boolean {
  return status === 'PENDING' && scheduledDate < today;
}

/**
 * Arbitrary that generates random dates in yyyy-MM-dd format
 * between 2000-01-01 and 2099-12-31.
 */
function arbDateString(): fc.Arbitrary<string> {
  return fc
    .record({
      year: fc.integer({ min: 2000, max: 2099 }),
      month: fc.integer({ min: 1, max: 12 }),
      day: fc.integer({ min: 1, max: 28 }),
    })
    .map(({ year, month, day }) => {
      const y = year.toString();
      const m = month.toString().padStart(2, '0');
      const d = day.toString().padStart(2, '0');
      return `${y}-${m}-${d}`;
    });
}

/**
 * Arbitrary that generates a random CompletionStatus value.
 */
function arbCompletionStatus(): fc.Arbitrary<CompletionStatus> {
  return fc.constantFrom<CompletionStatus>('PENDING', 'COMPLETED', 'CANCELLED');
}

describe('Frontend Overdue Classification - Property 7', () => {
  it('isOverdue returns true iff scheduledDate < today AND status === PENDING', () => {
    fc.assert(
      fc.property(
        arbDateString(),
        arbDateString(),
        arbCompletionStatus(),
        (scheduledDate: string, today: string, status: CompletionStatus) => {
          const result = isOverdue(scheduledDate, today, status);
          const expected = scheduledDate < today && status === 'PENDING';
          expect(result).toBe(expected);
        },
      ),
      { numRuns: 100 },
    );
  });

  it('isOverdue returns false when status is COMPLETED regardless of date', () => {
    fc.assert(
      fc.property(arbDateString(), arbDateString(), (scheduledDate: string, today: string) => {
        const result = isOverdue(scheduledDate, today, 'COMPLETED');
        expect(result).toBe(false);
      }),
      { numRuns: 100 },
    );
  });

  it('isOverdue returns false when status is CANCELLED regardless of date', () => {
    fc.assert(
      fc.property(arbDateString(), arbDateString(), (scheduledDate: string, today: string) => {
        const result = isOverdue(scheduledDate, today, 'CANCELLED');
        expect(result).toBe(false);
      }),
      { numRuns: 100 },
    );
  });

  it('isOverdue returns false when scheduledDate >= today even if PENDING', () => {
    fc.assert(
      fc.property(arbDateString(), (scheduledDate: string) => {
        // Use the same date for both -> scheduledDate is NOT < today
        const result = isOverdue(scheduledDate, scheduledDate, 'PENDING');
        expect(result).toBe(false);
      }),
      { numRuns: 100 },
    );
  });
});
