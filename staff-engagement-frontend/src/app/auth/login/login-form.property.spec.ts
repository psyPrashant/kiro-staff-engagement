import '@angular/compiler';
import { FormControl, Validators } from '@angular/forms';
import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';

describe('Feature: frontend-login-auth-guard — Login Form Property Tests', () => {
  describe('Property 1: Email validation rejects invalid formats', () => {
    it('for arbitrary strings without valid email format, form control is marked invalid', () => {
      // Strategy: generate strings that Angular's Validators.email will reject.
      // Angular uses a permissive regex based on the HTML5 spec. It rejects:
      // - strings with no @ symbol
      // - strings with spaces
      // - empty local part (@domain)
      // - empty domain part (local@)
      // - strings with multiple @ symbols

      const noAtArb = fc
        .string({ minLength: 1 })
        .filter((s) => !s.includes('@'));

      const emptyLocalArb = fc
        .string({ minLength: 1 })
        .filter((s) => !s.includes('@') && !s.includes(' '))
        .map((domain) => `@${domain}`);

      const emptyDomainArb = fc
        .string({ minLength: 1 })
        .filter((s) => !s.includes('@') && !s.includes(' '))
        .map((local) => `${local}@`);

      const withSpacesArb = fc
        .tuple(
          fc.string({ minLength: 1 }),
          fc.string({ minLength: 0 }),
        )
        .map(([a, b]) => `${a} ${b}`)
        .filter((s) => s.trim().length > 0);

      const invalidEmailArb = fc.oneof(noAtArb, emptyLocalArb, emptyDomainArb, withSpacesArb);

      const emailControl = new FormControl('', [Validators.required, Validators.email]);

      fc.assert(
        fc.property(invalidEmailArb, (value: string) => {
          emailControl.setValue(value);
          expect(emailControl.invalid).toBe(true);
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 1.2, 1.3 */
  });

  describe('Property 2: Password length validation', () => {
    it('for arbitrary strings with length <6, password control is marked invalid', () => {
      const shortPasswordArb = fc.string({ minLength: 1, maxLength: 5 });

      const passwordControl = new FormControl('', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(128),
      ]);

      fc.assert(
        fc.property(shortPasswordArb, (value: string) => {
          passwordControl.setValue(value);
          expect(passwordControl.invalid).toBe(true);
        }),
        { numRuns: 100 }
      );
    });

    it('for arbitrary strings with length >128, password control is marked invalid', () => {
      const longPasswordArb = fc.string({ minLength: 129, maxLength: 300 });

      const passwordControl = new FormControl('', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(128),
      ]);

      fc.assert(
        fc.property(longPasswordArb, (value: string) => {
          passwordControl.setValue(value);
          expect(passwordControl.invalid).toBe(true);
        }),
        { numRuns: 100 }
      );
    });

    it('for arbitrary strings with length 6–128, password control is marked valid', () => {
      const validPasswordArb = fc.string({ minLength: 6, maxLength: 128 });

      const passwordControl = new FormControl('', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(128),
      ]);

      fc.assert(
        fc.property(validPasswordArb, (value: string) => {
          passwordControl.setValue(value);
          expect(passwordControl.valid).toBe(true);
        }),
        { numRuns: 100 }
      );
    });

    /** Validates: Requirements 1.4, 1.5 */
  });
});
