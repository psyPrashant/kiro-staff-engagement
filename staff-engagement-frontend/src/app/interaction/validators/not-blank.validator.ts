import { AbstractControl, ValidationErrors } from '@angular/forms';

export function notBlankValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  if (typeof value === 'string' && value.trim().length === 0) {
    return { notBlank: true };
  }
  return null;
}

export function futureDateValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const selected = new Date(control.value);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  if (selected < today) {
    return { futureDate: true };
  }
  return null;
}
