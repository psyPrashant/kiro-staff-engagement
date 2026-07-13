import '@angular/compiler';
import * as fc from 'fast-check';

// Feature: interaction-scheduling, Property 9: Frontend Date Validation
// **Validates: Requirements 7.6, 12.5**

/**
 * Pure function extracted from ScheduleFormComponent.onDateChange().
 * Returns an error message string if the date is invalid (before today),
 * or null if valid (today or future).
 *
 * Uses string comparison on yyyy-MM-dd formatted dates, which produces
 * correct chronological ordering.
 */
export function validateScheduledDate(
  dateValue: string | null,
  today: string
): string | null {
  if (!dateValue) {
    return null;
  }
  if (dateValue < today) {
    return 'Scheduled date must be today or in the future.';
  }
  return null;
}

/**
 * Arbitrary that generates random dates in yyyy-MM-dd format
 * between 2000-01-01 and 2099-12-28.
 * Uses day max of 28 to avoid invalid dates across all months.
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

describe('Frontend Date Validation - Property 9', () => {
  // Fixed "today" for deterministic tests
  const FIXED_TODAY = '2025-06-15';

  it('dates strictly before today produce a non-null error message', () => {
    // Generate dates guaranteed to be before FIXED_TODAY
    const arbPastDate = fc
      .record({
        year: fc.integer({ min: 2000, max: 2025 }),
        month: fc.integer({ min: 1, max: 12 }),
        day: fc.integer({ min: 1, max: 28 }),
      })
      .filter(({ year, month, day }) => {
        const d = `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
        return d < FIXED_TODAY;
      })
      .map(({ year, month, day }) => {
        const m = month.toString().padStart(2, '0');
        const d = day.toString().padStart(2, '0');
        return `${year}-${m}-${d}`;
      });

    fc.assert(
      fc.property(arbPastDate, (date: string) => {
        const result = validateScheduledDate(date, FIXED_TODAY);
        expect(result).not.toBeNull();
        expect(result).toBe('Scheduled date must be today or in the future.');
      }),
      { numRuns: 100 }
    );
  });

  it('dates equal to or after today produce a null error (no error)', () => {
    // Generate dates guaranteed to be >= FIXED_TODAY
    const arbFutureOrTodayDate = fc
      .record({
        year: fc.integer({ min: 2025, max: 2099 }),
        month: fc.integer({ min: 1, max: 12 }),
        day: fc.integer({ min: 1, max: 28 }),
      })
      .filter(({ year, month, day }) => {
        const d = `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
        return d >= FIXED_TODAY;
      })
      .map(({ year, month, day }) => {
        const m = month.toString().padStart(2, '0');
        const d = day.toString().padStart(2, '0');
        return `${year}-${m}-${d}`;
      });

    fc.assert(
      fc.property(arbFutureOrTodayDate, (date: string) => {
        const result = validateScheduledDate(date, FIXED_TODAY);
        expect(result).toBeNull();
      }),
      { numRuns: 100 }
    );
  });

  it('for any random date: before today → error, today/future → no error', () => {
    fc.assert(
      fc.property(arbDateString(), (date: string) => {
        const result = validateScheduledDate(date, FIXED_TODAY);
        if (date < FIXED_TODAY) {
          expect(result).toBe('Scheduled date must be today or in the future.');
        } else {
          expect(result).toBeNull();
        }
      }),
      { numRuns: 100 }
    );
  });

  it('null or empty date value produces null (no error)', () => {
    expect(validateScheduledDate(null, FIXED_TODAY)).toBeNull();
    expect(validateScheduledDate('', FIXED_TODAY)).toBeNull();
  });

  it('the exact "today" date produces no error', () => {
    const result = validateScheduledDate(FIXED_TODAY, FIXED_TODAY);
    expect(result).toBeNull();
  });
});
