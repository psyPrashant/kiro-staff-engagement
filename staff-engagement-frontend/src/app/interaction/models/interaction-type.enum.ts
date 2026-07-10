export enum InteractionType {
  CHECK_IN = 'CHECK_IN',
  MENTORING = 'MENTORING',
  CATCH_UP = 'CATCH_UP',
  OTHER = 'OTHER',
}

export const INTERACTION_TYPES: { value: InteractionType; label: string }[] = [
  { value: InteractionType.CHECK_IN, label: 'Check In' },
  { value: InteractionType.MENTORING, label: 'Mentoring' },
  { value: InteractionType.CATCH_UP, label: 'Catch Up' },
  { value: InteractionType.OTHER, label: 'Other' },
];

export function formatInteractionTypeLabel(type: InteractionType): string {
  return type
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
}
