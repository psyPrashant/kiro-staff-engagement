/**
 * Bug Condition Exploration Property Test
 *
 * This test encodes the EXPECTED behavior: all test files that satisfy
 * `isBugCondition` should execute successfully under `npx vitest --run`.
 *
 * On UNFIXED code (no vitest.config.ts), this test WILL FAIL — proving the bug exists.
 * After the fix is applied, this test WILL PASS — confirming the fix works.
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**
 */
import { describe, it, expect } from 'vitest';
import * as fc from 'fast-check';
import { execSync } from 'child_process';
import * as path from 'path';

/**
 * Known failing test files categorized by bug condition.
 * These files FAIL under `npx vitest --run` without a vitest.config.ts.
 */
const BUG_CONDITION_FILES = {
  /**
   * Category 1: Files using globals (describe/it/expect/vi) without explicit vitest import.
   * Fails with: "describe is not defined" ReferenceError
   * Requirement: 1.1
   */
  globalsWithoutImport: [
    'src/app/user/user.spec.ts',
    'src/app/core/guards/auth.guard.spec.ts',
    'src/app/core/services/auth.service.spec.ts',
    'src/app/core/interceptors/auth.interceptor.spec.ts',
    'src/app/core/interceptors/base-url.interceptor.spec.ts',
    'src/app/core/interceptors/error.interceptor.spec.ts',
    'src/app/shell/shell.component.spec.ts',
    'src/app/dashboard/dashboard.component.spec.ts',
    'src/app/greeting/greeting.service.spec.ts',
    'src/app/interaction/log-interaction.component.spec.ts',
    'src/app/interaction/services/interaction.service.spec.ts',
    'src/app/app.spec.ts',
    'src/app/app.routes.spec.ts',
  ],

  /**
   * Category 2: Files using TestBed with JIT compilation (require @angular/compiler).
   * Fails with: JIT compilation error
   * Requirement: 1.2
   */
  testBedJIT: [
    'src/app/core/guards/auth.guard.spec.ts',
    'src/app/core/services/auth.service.spec.ts',
    'src/app/core/interceptors/auth.interceptor.spec.ts',
  ],

  /**
   * Category 3: Files calling TestBed.configureTestingModule() without initTestEnvironment().
   * Fails with: TestBed environment not initialized error
   * Requirement: 1.3
   */
  configureTestingModule: [
    'src/app/user/user.spec.ts',
    'src/app/core/guards/auth.guard.spec.ts',
    'src/app/core/services/auth.service.spec.ts',
    'src/app/core/interceptors/auth.interceptor.spec.ts',
    'src/app/core/interceptors/base-url.interceptor.spec.ts',
    'src/app/core/interceptors/error.interceptor.spec.ts',
    'src/app/shell/shell.component.spec.ts',
    'src/app/dashboard/dashboard.component.spec.ts',
    'src/app/greeting/greeting.service.spec.ts',
    'src/app/interaction/log-interaction.component.spec.ts',
    'src/app/interaction/services/interaction.service.spec.ts',
    'src/app/app.spec.ts',
  ],

  /**
   * Category 4: Playwright e2e files included by accident (no exclude pattern).
   * Fails with: Playwright API incompatibility
   * Requirement: 1.4
   */
  playwrightE2E: ['e2e/smoke.spec.ts', 'e2e/log-interaction.spec.ts'],

  /**
   * Category 5: Component tests with external templates (templateUrl/styleUrl).
   * Fails with: resource resolution error (no Angular plugin)
   * Requirement: 1.5
   */
  externalTemplates: [
    'src/app/shell/shell.component.spec.ts',
    'src/app/dashboard/dashboard.component.spec.ts',
    'src/app/interaction/log-interaction.component.spec.ts',
    'src/app/user/user.spec.ts',
    'src/app/app.spec.ts',
  ],
};

/** Deduplicated set of all files that trigger the bug condition */
const ALL_BUG_CONDITION_FILES = [
  ...new Set([
    ...BUG_CONDITION_FILES.globalsWithoutImport,
    ...BUG_CONDITION_FILES.testBedJIT,
    ...BUG_CONDITION_FILES.configureTestingModule,
    ...BUG_CONDITION_FILES.playwrightE2E,
    ...BUG_CONDITION_FILES.externalTemplates,
  ]),
];

/**
 * Determines the bug condition category for a given file.
 */
function getBugCategory(filePath: string): string[] {
  const categories: string[] = [];
  if (BUG_CONDITION_FILES.globalsWithoutImport.includes(filePath)) {
    categories.push('globalsWithoutImport');
  }
  if (BUG_CONDITION_FILES.testBedJIT.includes(filePath)) {
    categories.push('testBedJIT');
  }
  if (BUG_CONDITION_FILES.configureTestingModule.includes(filePath)) {
    categories.push('configureTestingModule');
  }
  if (BUG_CONDITION_FILES.playwrightE2E.includes(filePath)) {
    categories.push('playwrightE2E');
  }
  if (BUG_CONDITION_FILES.externalTemplates.includes(filePath)) {
    categories.push('externalTemplates');
  }
  return categories;
}

/**
 * Runs a single test file using npx vitest --run and returns the result.
 */
function runTestFile(filePath: string): { exitCode: number; output: string } {
  const cwd = path.resolve(__dirname, '..');
  try {
    const output = execSync(`npx vitest --run ${filePath}`, {
      cwd,
      encoding: 'utf-8',
      timeout: 60000,
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    return { exitCode: 0, output };
  } catch (error: unknown) {
    const execError = error as { status?: number; stdout?: string; stderr?: string };
    return {
      exitCode: execError.status ?? 1,
      output: (execError.stdout ?? '') + '\n' + (execError.stderr ?? ''),
    };
  }
}

describe('Bug Condition Exploration: Standalone Vitest Execution', () => {
  it('Property 1: For any test file satisfying isBugCondition, npx vitest --run should succeed with exit code 0', () => {
    /**
     * Property: For all test files in the bug-condition set,
     * running `npx vitest --run <file>` should exit with code 0.
     *
     * On UNFIXED code this property FAILS — confirming the bug exists.
     * On FIXED code this property PASSES — confirming the fix works.
     */
    fc.assert(
      fc.property(fc.constantFrom(...ALL_BUG_CONDITION_FILES), (testFile: string) => {
        const result = runTestFile(testFile);
        const categories = getBugCategory(testFile);

        // The property: vitest should execute successfully
        expect(
          result.exitCode,
          `File "${testFile}" (categories: ${categories.join(', ')}) failed with:\n${result.output.slice(0, 500)}`,
        ).toBe(0);
      }),
      {
        numRuns: 5, // Sample 5 files from the set to keep execution time reasonable
        seed: 42, // Deterministic for reproducibility
        verbose: 2,
      },
    );
  });
});
