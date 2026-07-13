import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { EngagementStatus } from '../dashboard/models/engagement.model';
import { EmployeeListEntry } from './models/employee-list.model';

/**
 * Replicates the filtering logic from EmployeesListComponent.
 * Filters by name or jobTitle containing the search term (case insensitive).
 */
function filterBySearchTerm(employees: EmployeeListEntry[], term: string): EmployeeListEntry[] {
  const lower = term.toLowerCase();
  if (!lower) return employees;
  return employees.filter(
    (e) => e.name.toLowerCase().includes(lower) || e.jobTitle.toLowerCase().includes(lower),
  );
}

/**
 * Replicates the status filtering logic from EmployeesListComponent.
 * Filters employees by engagement status when a status is provided.
 */
function filterByStatus(
  employees: EmployeeListEntry[],
  status: EngagementStatus | null,
): EmployeeListEntry[] {
  if (!status) return employees;
  return employees.filter((e) => e.engagementStatus === status);
}

const engagementStatusArb: fc.Arbitrary<EngagementStatus | null> = fc.constantFrom(
  'OVERDUE' as EngagementStatus,
  'AT_RISK' as EngagementStatus,
  'ON_TRACK' as EngagementStatus,
  null,
);

const employeeArb: fc.Arbitrary<EmployeeListEntry> = fc.record({
  id: fc.integer({ min: 1, max: 10000 }),
  name: fc.string({ minLength: 1, maxLength: 50 }),
  email: fc.emailAddress(),
  jobTitle: fc.string({ minLength: 1, maxLength: 50 }),
  managerName: fc.option(fc.string({ minLength: 1, maxLength: 50 }), { nil: null }),
  engagementStatus: engagementStatusArb,
  lastInteractionDate: fc.option(
    fc
      .integer({ min: 2020, max: 2030 })
      .chain((year) =>
        fc.tuple(
          fc.constant(year),
          fc.integer({ min: 1, max: 12 }),
          fc.integer({ min: 1, max: 28 }),
        ),
      )
      .map(([y, m, d]) => `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`),
    { nil: null },
  ),
});

const employeeListArb = fc.array(employeeArb, { minLength: 0, maxLength: 30 });

describe('Employee list filtering - property tests', () => {
  /**
   * **Property 5: Employee list filtering is monotonic**
   * For any list and search term, filtered result length ≤ unfiltered length.
   *
   * **Validates: Requirements 9.3, 9.4**
   */
  it('search filtering is monotonic: filtered.length <= unfiltered.length', () => {
    fc.assert(
      fc.property(
        employeeListArb,
        fc.string({ minLength: 0, maxLength: 20 }),
        (employees, term) => {
          const filtered = filterBySearchTerm(employees, term);
          expect(filtered.length).toBeLessThanOrEqual(employees.length);
        },
      ),
      { numRuns: 100 },
    );
  });

  /**
   * **Property 5 (continued): Status filtering is monotonic**
   * Adding a status filter never increases the result count.
   *
   * **Validates: Requirements 9.3, 9.4**
   */
  it('status filtering is monotonic: adding a status filter never increases results', () => {
    fc.assert(
      fc.property(
        employeeListArb,
        fc.constantFrom(
          'OVERDUE' as EngagementStatus,
          'AT_RISK' as EngagementStatus,
          'ON_TRACK' as EngagementStatus,
        ),
        (employees, status) => {
          const unfiltered = filterByStatus(employees, null);
          const filtered = filterByStatus(employees, status);
          expect(filtered.length).toBeLessThanOrEqual(unfiltered.length);
        },
      ),
      { numRuns: 100 },
    );
  });

  /**
   * Combined search + status filtering is monotonic.
   * Applying both filters never produces more results than applying either alone.
   *
   * **Validates: Requirements 9.3, 9.4**
   */
  it('combined search + status filtering is monotonic', () => {
    fc.assert(
      fc.property(
        employeeListArb,
        fc.string({ minLength: 0, maxLength: 20 }),
        fc.constantFrom(
          'OVERDUE' as EngagementStatus,
          'AT_RISK' as EngagementStatus,
          'ON_TRACK' as EngagementStatus,
        ),
        (employees, term, status) => {
          const searchOnly = filterBySearchTerm(employees, term);
          const statusOnly = filterByStatus(employees, status);
          const combined = filterByStatus(filterBySearchTerm(employees, term), status);

          expect(combined.length).toBeLessThanOrEqual(searchOnly.length);
          expect(combined.length).toBeLessThanOrEqual(statusOnly.length);
          expect(combined.length).toBeLessThanOrEqual(employees.length);
        },
      ),
      { numRuns: 100 },
    );
  });
});
