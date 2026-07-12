import * as fc from 'fast-check';
import { Employee360Component } from './employee-360.component';

// Feature: employee-360-view, Property 5: Notes truncation preserves short strings and clips long ones

describe('Employee360Component.truncateNotes — Property Test', () => {
  const truncateNotes = Employee360Component.prototype.truncateNotes;

  describe('Property 5: Notes truncation preserves short strings and clips long ones', () => {
    it('strings ≤ 200 chars are returned unchanged', () => {
      fc.assert(
        fc.property(fc.string({ minLength: 0, maxLength: 200 }), (notes) => {
          const result = truncateNotes(notes);
          expect(result).toBe(notes);
        }),
        { numRuns: 100 },
      );
    });

    it('strings > 200 chars are clipped to 201 chars (200 + ellipsis) matching original prefix', () => {
      fc.assert(
        fc.property(fc.string({ minLength: 201, maxLength: 1000 }), (notes) => {
          const result = truncateNotes(notes);
          expect(result.length).toBe(201);
          expect(result.substring(0, 200)).toBe(notes.substring(0, 200));
          expect(result[200]).toBe('\u2026');
        }),
        { numRuns: 100 },
      );
    });

    /** Validates: Requirements 2.1 */
  });
});
