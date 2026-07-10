import * as fc from 'fast-check';
import { TaskDto } from '../models/employee-360.model';

// Feature: employee-360-view, Property 6: Tasks are ordered by due date ascending with nulls last

/**
 * The sort function extracted from Employee360Component.
 * Sorts tasks by dueDate ascending with nulls last.
 */
function sortTasksByDueDate(tasks: TaskDto[]): TaskDto[] {
  return [...tasks].sort((a, b) => {
    if (a.dueDate === null && b.dueDate === null) return 0;
    if (a.dueDate === null) return 1;
    if (b.dueDate === null) return -1;
    return a.dueDate.localeCompare(b.dueDate);
  });
}

/**
 * Arbitrary that generates a TaskDto with either a null dueDate
 * or an ISO date string (YYYY-MM-DD format).
 */
const taskArbitrary: fc.Arbitrary<TaskDto> = fc.record({
  id: fc.nat({ max: 100000 }),
  title: fc.string({ minLength: 1, maxLength: 50 }),
  dueDate: fc.oneof(
    fc.constant(null),
    fc
      .date({ min: new Date('2020-01-01'), max: new Date('2030-12-31') })
      .map((d) => d.toISOString().split('T')[0]),
  ),
  assignedUserName: fc.string({ minLength: 1, maxLength: 30 }),
});

describe('Feature: employee-360-view, Property 6: Tasks are ordered by due date ascending with nulls last', () => {
  /**
   * Validates: Requirements 3.1
   *
   * For any list of open tasks, the rendered order SHALL satisfy:
   * all tasks with non-null due dates appear before tasks with null due dates,
   * and among tasks with non-null due dates, each task's dueDate is <= the next task's dueDate.
   */
  it('all non-null dueDate tasks appear before null dueDate tasks', () => {
    fc.assert(
      fc.property(fc.array(taskArbitrary, { minLength: 0, maxLength: 30 }), (tasks) => {
        const sorted = sortTasksByDueDate(tasks);

        let seenNull = false;
        for (const task of sorted) {
          if (task.dueDate === null) {
            seenNull = true;
          } else {
            // Once we see a null, no non-null should follow
            expect(seenNull).toBe(false);
          }
        }
      }),
      { numRuns: 100 },
    );
  });

  it('non-null dueDate tasks are in ascending order', () => {
    fc.assert(
      fc.property(fc.array(taskArbitrary, { minLength: 0, maxLength: 30 }), (tasks) => {
        const sorted = sortTasksByDueDate(tasks);

        const nonNullDates = sorted
          .filter((t) => t.dueDate !== null)
          .map((t) => t.dueDate as string);

        for (let i = 0; i < nonNullDates.length - 1; i++) {
          expect(nonNullDates[i] <= nonNullDates[i + 1]).toBe(true);
        }
      }),
      { numRuns: 100 },
    );
  });
});
