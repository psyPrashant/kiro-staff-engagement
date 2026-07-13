import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { formatEnumLabel } from './format-enum-label';
import { formatEngagementStatusLabel } from '../../dashboard/models/engagement.model';
import { formatTaskStatusLabel } from '../../task/models/task.model';

describe('formatEnumLabel', () => {
  it('should title-case a single word', () => {
    expect(formatEnumLabel('OVERDUE')).toBe('Overdue');
  });

  it('should split on underscore and title-case each word', () => {
    expect(formatEnumLabel('AT_RISK')).toBe('At Risk');
  });

  it('should handle multiple underscores', () => {
    expect(formatEnumLabel('ON_TRACK')).toBe('On Track');
  });

  it('should handle already lowercase input', () => {
    expect(formatEnumLabel('some_value')).toBe('Some Value');
  });

  it('should handle mixed case input', () => {
    expect(formatEnumLabel('Mixed_CASE_input')).toBe('Mixed Case Input');
  });

  it('should handle single character segments', () => {
    expect(formatEnumLabel('A_B_C')).toBe('A B C');
  });

  it('should handle empty string', () => {
    expect(formatEnumLabel('')).toBe('');
  });
});

describe('Label helpers - property tests', () => {
  /**
   * **Validates: Requirements 5.1, 5.2, 5.3**
   */
  it('Property 1: formatEngagementStatusLabel returns non-empty transformed string for any valid status', () => {
    const statuses = ['OVERDUE', 'AT_RISK', 'ON_TRACK'] as const;
    fc.assert(
      fc.property(fc.constantFrom(...statuses), (status) => {
        const result = formatEngagementStatusLabel(status);
        return result.length > 0 && result !== status;
      }),
      { numRuns: 100 }
    );
  });

  /**
   * **Validates: Requirements 5.4**
   */
  it('Property 2: formatTaskStatusLabel returns non-empty transformed string for any valid status', () => {
    const statuses = ['OPEN', 'DONE'] as const;
    fc.assert(
      fc.property(fc.constantFrom(...statuses), (status) => {
        const result = formatTaskStatusLabel(status);
        return result.length > 0 && result !== status;
      }),
      { numRuns: 100 }
    );
  });

  /**
   * **Validates: Requirements 5.5**
   */
  it('Property 3: formatEnumLabel produces title case without underscores for any UPPER_SNAKE_CASE input', () => {
    const upperWord = fc
      .integer({ min: 1, max: 10 })
      .chain((len) =>
        fc.tuple(...Array.from({ length: len }, () => fc.constantFrom(...'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split(''))))
      )
      .map((chars) => chars.join(''));

    const upperSnakeCase = fc
      .array(upperWord, { minLength: 1, maxLength: 5 })
      .map((parts) => parts.join('_'));

    fc.assert(
      fc.property(upperSnakeCase, (input) => {
        const result = formatEnumLabel(input);
        return !result.includes('_') && result.length > 0;
      }),
      { numRuns: 100 }
    );
  });
});
