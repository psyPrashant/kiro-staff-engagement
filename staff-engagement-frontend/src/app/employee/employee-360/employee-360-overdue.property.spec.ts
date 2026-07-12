import fc from 'fast-check';
import { Employee360Component } from './employee-360.component';

// Feature: employee-360-view, Property 7: Overdue classification correctness
// **Validates: Requirements 3.3, 3.4**

describe('Employee360Component.isOverdue - Property Test', () => {
  const isOverdue = Employee360Component.prototype.isOverdue;

  it('null dueDate always returns false', () => {
    expect(isOverdue(null)).toBe(false);
  });

  it('past dates (before today) return true', () => {
    fc.assert(
      fc.property(
        fc.date({ min: new Date('2000-01-01'), max: new Date('2020-12-31'), noInvalidDate: true }),
        (date) => {
          const dateStr = date.toISOString().split('T')[0];
          return isOverdue(dateStr) === true;
        },
      ),
      { numRuns: 100 },
    );
  });

  it('future dates (after today) return false', () => {
    fc.assert(
      fc.property(
        fc.date({ min: new Date('2090-01-01'), max: new Date('2099-12-31'), noInvalidDate: true }),
        (date) => {
          const dateStr = date.toISOString().split('T')[0];
          return isOverdue(dateStr) === false;
        },
      ),
      { numRuns: 100 },
    );
  });
});
