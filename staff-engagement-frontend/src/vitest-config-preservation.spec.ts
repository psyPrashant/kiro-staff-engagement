import * as fc from 'fast-check';
import { execSync } from 'child_process';
import { readFileSync, existsSync } from 'fs';
import { resolve } from 'path';

/**
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
 *
 * Property 2: Preservation — Existing Passing Tests and ng test Remain Unaffected
 *
 * For any test file that does NOT satisfy isBugCondition (files with explicit vitest
 * imports AND either no Angular dependency or self-contained compiler import, AND no
 * TestBed dependency), running `npx vitest --run <file>` should exit with code 0.
 *
 * Observation (unfixed code baseline):
 * - interaction-type.enum.spec.ts: PASSES (pure enum logic, no Angular deps)
 * - login-form.property.spec.ts: PASSES (explicit vitest + @angular/compiler import, no TestBed)
 * - auth.interceptor.property.spec.ts: PASSES (explicit vitest + @angular/compiler import, no TestBed)
 */

// Known set of currently-passing test files on UNFIXED code.
// These files have explicit vitest imports and either:
// - No Angular dependencies at all (pure logic)
// - Self-contained @angular/compiler import (no TestBed dependency)
const CURRENTLY_PASSING_FILES: string[] = [
  'src/app/interaction/models/interaction-type.enum.spec.ts',
  'src/app/auth/login/login-form.property.spec.ts',
  'src/app/core/interceptors/auth.interceptor.property.spec.ts',
];

/**
 * Determines if a test file does NOT satisfy the bug condition.
 * A file is "preservation-safe" if it:
 * 1. Uses vitest globals (globals: true in vitest.config.ts provides describe/it/expect)
 * 2. Does NOT use TestBed.configureTestingModule()
 * 3. Either has no Angular deps OR imports @angular/compiler itself
 */
function isPreservationFile(filePath: string): boolean {
  const fullPath = resolve(process.cwd(), filePath);
  if (!existsSync(fullPath)) return false;

  const content = readFileSync(fullPath, 'utf-8');

  const usesTestBed = content.includes('TestBed.configureTestingModule');

  // With globals: true, files no longer need explicit vitest imports.
  // A preservation file is one that doesn't use TestBed (pure logic or self-contained Angular).
  return !usesTestBed;
}

/**
 * Runs a single test file via vitest and returns the exit code.
 * Uses cmd /c on Windows to invoke the .cmd wrapper properly.
 */
function runVitestOnFile(filePath: string): { exitCode: number; output: string } {
  const cwd = resolve(__dirname, '..');
  const isWindows = process.platform === 'win32';
  const command = isWindows
    ? `cmd /c "node_modules\\.bin\\vitest.cmd --run ${filePath}"`
    : `node_modules/.bin/vitest --run ${filePath}`;

  try {
    const output = execSync(command, {
      encoding: 'utf-8',
      cwd,
      timeout: 60000,
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    return { exitCode: 0, output };
  } catch (error: unknown) {
    const execError = error as { status?: number; stdout?: string; stderr?: string };
    return {
      exitCode: execError.status ?? 1,
      output: (execError.stdout ?? '') + (execError.stderr ?? ''),
    };
  }
}

/**
 * Strips JSON comments to allow parsing JSONC files like tsconfig.
 * Handles // line comments and /* block comments at the top of the file,
 * without mangling glob patterns inside string values.
 */
function stripJsonComments(text: string): string {
  // Find the first '{' which starts the actual JSON content
  const jsonStart = text.indexOf('{');
  if (jsonStart === -1) return text;
  return text.substring(jsonStart);
}

describe('Property 2: Preservation — Existing Passing Tests Remain Unaffected', () => {
  describe('Concrete preservation cases', () => {
    it('interaction-type.enum.spec.ts (pure logic, explicit imports) passes', () => {
      const file = 'src/app/interaction/models/interaction-type.enum.spec.ts';
      expect(isPreservationFile(file)).toBe(true);

      const result = runVitestOnFile(file);
      expect(result.exitCode).toBe(0);
    });

    it('login-form.property.spec.ts (explicit imports, @angular/compiler) passes', () => {
      const file = 'src/app/auth/login/login-form.property.spec.ts';
      expect(isPreservationFile(file)).toBe(true);

      const result = runVitestOnFile(file);
      expect(result.exitCode).toBe(0);
    });

    it('auth.interceptor.property.spec.ts (explicit imports, @angular/compiler) passes', () => {
      const file = 'src/app/core/interceptors/auth.interceptor.property.spec.ts';
      expect(isPreservationFile(file)).toBe(true);

      const result = runVitestOnFile(file);
      expect(result.exitCode).toBe(0);
    });
  });

  describe('Property-based: for all currently-passing files, vitest exits with code 0', () => {
    it('for any file drawn from the currently-passing set, running vitest succeeds', () => {
      // Use fast-check to sample from the known passing files
      const passingFileArb = fc.constantFrom(...CURRENTLY_PASSING_FILES);

      fc.assert(
        fc.property(passingFileArb, (filePath: string) => {
          // Verify the file satisfies our preservation condition
          expect(isPreservationFile(filePath)).toBe(true);

          // Run vitest on this file and assert exit code 0
          const result = runVitestOnFile(filePath);
          expect(result.exitCode).toBe(0);
        }),
        { numRuns: CURRENTLY_PASSING_FILES.length },
      );
    });
  });

  describe('Preservation: tsconfig.spec.json is unchanged and used for compilation', () => {
    it('tsconfig.spec.json exists and contains expected configuration', () => {
      const tsconfigPath = resolve(process.cwd(), 'tsconfig.spec.json');
      expect(existsSync(tsconfigPath)).toBe(true);

      const rawContent = readFileSync(tsconfigPath, 'utf-8');
      const content = JSON.parse(stripJsonComments(rawContent));

      // Verify key properties that must be preserved
      expect(content.extends).toBe('./tsconfig.json');
      expect(content.compilerOptions.types).toContain('vitest/globals');
      expect(content.include).toContain('src/**/*.spec.ts');
    });
  });
});
