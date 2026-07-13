import * as fc from 'fast-check';
import { ScheduleCalendarComponent } from './schedule-calendar.component';

// Feature: interaction-scheduling, Property 8: Frontend Notes Truncation

describe('ScheduleCalendarComponent.truncateNotes — Property Test', () => {
  const truncateNotes = ScheduleCalendarComponent.prototype.truncateNotes;

  describe('Property 8: Frontend Notes Truncation', () => {
    it('null input returns empty string', () => {
      expect(truncateNotes(null)).toBe('');
    });

    it('strings ≤ 100 chars are returned unchanged', () => {
      fc.assert(
        fc.property(fc.string({ minLength: 0, maxLength: 100 }), (notes) => {
          const result = truncateNotes(notes);
          expect(result).toBe(notes);
        }),
        { numRuns: 100 },
      );
    });

    it('strings > 100 chars are clipped to 101 chars (first 100 + ellipsis) matching original prefix', () => {
      fc.assert(
        fc.property(fc.string({ minLength: 101, maxLength: 500 }), (notes) => {
          const result = truncateNotes(notes);
          expect(result.length).toBe(101);
          expect(result.substring(0, 100)).toBe(notes.substring(0, 100));
          expect(result[100]).toBe('\u2026');
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 6.3 */
  });
});
