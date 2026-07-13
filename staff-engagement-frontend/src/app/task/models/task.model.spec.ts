import { describe, it } from 'vitest';
import * as fc from 'fast-check';
import { isOverdue } from './task.model';

function toggleStatus(status: string): string {
  return status === 'OPEN' ? 'DONE' : 'OPEN';
}

describe('Task model - property tests', () => {
  /**
   * **Validates: Requirements 10.4, 10.5**
   */
  describe('Property 6: Task status toggle is involutory', () => {
    it('toggling OPEN↔DONE twice returns to original status', () => {
      const taskStatus = fc.constantFrom('OPEN', 'DONE');

      fc.assert(
        fc.property(taskStatus, (status) => {
          const toggled = toggleStatus(toggleStatus(status));
          return toggled === status;
        }),
        { numRuns: 100 },
      );
    });
  });

  /**
   * **Validates: Requirements 10.4, 10.5**
   */
  describe('Property 7: Overdue detection correctness', () => {
    it('isOverdue returns true only when status is OPEN and dueDate is before today', () => {
      const taskStatus = fc.constantFrom('OPEN', 'DONE');
      const now = Date.now();
      const pastDate = fc
        .date({ min: new Date('2000-01-01T00:00:00.000Z'), max: new Date(now - 86_400_000) })
        .map((d) => d.toISOString().split('T')[0]);
      const futureDate = fc
        .date({ min: new Date(now + 86_400_000), max: new Date('2099-12-31T00:00:00.000Z') })
        .map((d) => d.toISOString().split('T')[0]);
      const todayDate = fc.constant(new Date().toISOString().split('T')[0]);
      const nullDate = fc.constant(null as string | null);

      const dueDate = fc.oneof(pastDate, futureDate, todayDate, nullDate);

      fc.assert(
        fc.property(taskStatus, dueDate, (status, due) => {
          const task = { status, dueDate: due };
          const result = isOverdue(task);

          const today = new Date(new Date().toDateString());
          const isPast = due !== null && new Date(due) < today;
          const expected = status === 'OPEN' && isPast;

          return result === expected;
        }),
        { numRuns: 100 },
      );
    });
  });
});
