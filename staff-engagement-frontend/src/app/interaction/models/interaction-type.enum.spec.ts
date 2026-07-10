import { describe, it, expect } from 'vitest';
import {
  InteractionType,
  INTERACTION_TYPES,
  formatInteractionTypeLabel,
} from './interaction-type.enum';

describe('InteractionType enum', () => {
  it('should have exactly 4 enum values', () => {
    const values = Object.values(InteractionType);
    expect(values).toHaveLength(4);
    expect(values).toEqual(['CHECK_IN', 'MENTORING', 'CATCH_UP', 'OTHER']);
  });
});

describe('INTERACTION_TYPES array', () => {
  it('should contain entries for all enum values', () => {
    const enumValues = Object.values(InteractionType);
    const arrayValues = INTERACTION_TYPES.map((t) => t.value);
    expect(arrayValues).toEqual(enumValues);
  });

  it('should have human-readable labels', () => {
    const labels = INTERACTION_TYPES.map((t) => t.label);
    expect(labels).toEqual(['Check In', 'Mentoring', 'Catch Up', 'Other']);
  });
});

describe('formatInteractionTypeLabel', () => {
  it('should format CHECK_IN as "Check In"', () => {
    expect(formatInteractionTypeLabel(InteractionType.CHECK_IN)).toBe('Check In');
  });

  it('should format MENTORING as "Mentoring"', () => {
    expect(formatInteractionTypeLabel(InteractionType.MENTORING)).toBe('Mentoring');
  });

  it('should format CATCH_UP as "Catch Up"', () => {
    expect(formatInteractionTypeLabel(InteractionType.CATCH_UP)).toBe('Catch Up');
  });

  it('should format OTHER as "Other"', () => {
    expect(formatInteractionTypeLabel(InteractionType.OTHER)).toBe('Other');
  });
});
