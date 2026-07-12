import { FormControl } from '@angular/forms';
import { futureDateValidator, notBlankValidator } from './not-blank.validator';

describe('notBlankValidator', () => {
  it('should return notBlank error for empty string', () => {
    const control = new FormControl('');
    expect(notBlankValidator(control)).toEqual({ notBlank: true });
  });

  it('should return notBlank error for whitespace-only string', () => {
    const control = new FormControl('   ');
    expect(notBlankValidator(control)).toEqual({ notBlank: true });
  });

  it('should return notBlank error for tabs and newlines', () => {
    const control = new FormControl('\t\n\r');
    expect(notBlankValidator(control)).toEqual({ notBlank: true });
  });

  it('should return null for non-blank string', () => {
    const control = new FormControl('hello');
    expect(notBlankValidator(control)).toBeNull();
  });

  it('should return null for string with leading/trailing whitespace but content', () => {
    const control = new FormControl('  hello  ');
    expect(notBlankValidator(control)).toBeNull();
  });

  it('should return null for non-string values', () => {
    const control = new FormControl(123);
    expect(notBlankValidator(control)).toBeNull();
  });

  it('should return null for null value', () => {
    const control = new FormControl(null);
    expect(notBlankValidator(control)).toBeNull();
  });
});

describe('futureDateValidator', () => {
  it('should return null for null value', () => {
    const control = new FormControl(null);
    expect(futureDateValidator(control)).toBeNull();
  });

  it('should return null for empty string value', () => {
    const control = new FormControl('');
    expect(futureDateValidator(control)).toBeNull();
  });

  it('should return null for today', () => {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    const control = new FormControl(`${year}-${month}-${day}`);
    expect(futureDateValidator(control)).toBeNull();
  });

  it('should return null for a future date', () => {
    const future = new Date();
    future.setDate(future.getDate() + 7);
    const year = future.getFullYear();
    const month = String(future.getMonth() + 1).padStart(2, '0');
    const day = String(future.getDate()).padStart(2, '0');
    const control = new FormControl(`${year}-${month}-${day}`);
    expect(futureDateValidator(control)).toBeNull();
  });

  it('should return futureDate error for a past date', () => {
    const past = new Date();
    past.setDate(past.getDate() - 1);
    const year = past.getFullYear();
    const month = String(past.getMonth() + 1).padStart(2, '0');
    const day = String(past.getDate()).padStart(2, '0');
    const control = new FormControl(`${year}-${month}-${day}`);
    expect(futureDateValidator(control)).toEqual({ futureDate: true });
  });

  it('should return futureDate error for a date far in the past', () => {
    const control = new FormControl('2020-01-01');
    expect(futureDateValidator(control)).toEqual({ futureDate: true });
  });
});
