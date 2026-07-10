import { defineConfig } from 'vitest/config';
import * as fs from 'node:fs';
import * as path from 'node:path';

/**
 * Custom Vite plugin to resolve Angular component resources (templateUrl, styleUrl/styleUrls).
 * This inlines external template and style files so JIT compilation works in standalone Vitest.
 */
function angularResourceInliner() {
  return {
    name: 'angular-resource-inliner',
    enforce: 'pre' as const,
    transform(code: string, id: string) {
      if (!id.endsWith('.ts') || id.includes('node_modules')) {
        return undefined;
      }

      // Only process files that have templateUrl or styleUrl
      if (!code.includes('templateUrl') && !code.includes('styleUrl')) {
        return undefined;
      }

      const dir = path.dirname(id);
      let transformed = code;

      // Inline templateUrl -> template
      transformed = transformed.replace(
        /templateUrl\s*:\s*['"]([^'"]+)['"]/g,
        (_match, templatePath) => {
          const fullPath = path.resolve(dir, templatePath);
          try {
            const content = fs.readFileSync(fullPath, 'utf-8');
            const escaped = content.replace(/\\/g, '\\\\').replace(/`/g, '\\`').replace(/\$/g, '\\$');
            return `template: \`${escaped}\``;
          } catch {
            // If file not found, leave as-is (will fail at runtime with clear error)
            return _match;
          }
        },
      );

      // Inline styleUrl -> styles (single string form used in Angular 21)
      transformed = transformed.replace(
        /styleUrl\s*:\s*['"]([^'"]+)['"]/g,
        (_match, stylePath) => {
          const fullPath = path.resolve(dir, stylePath);
          try {
            const content = fs.readFileSync(fullPath, 'utf-8');
            const escaped = content.replace(/\\/g, '\\\\').replace(/`/g, '\\`').replace(/\$/g, '\\$');
            return `styles: [\`${escaped}\`]`;
          } catch {
            return `styles: ['']`;
          }
        },
      );

      // Inline styleUrls -> styles (array form)
      transformed = transformed.replace(
        /styleUrls\s*:\s*\[([^\]]*)\]/g,
        (_match, urlsContent) => {
          const urls = urlsContent.match(/['"]([^'"]+)['"]/g);
          if (!urls) return _match;
          const inlinedStyles = urls
            .map((url: string) => {
              const stylePath = url.replace(/['"]/g, '');
              const fullPath = path.resolve(dir, stylePath);
              try {
                const content = fs.readFileSync(fullPath, 'utf-8');
                const escaped = content
                  .replace(/\\/g, '\\\\')
                  .replace(/`/g, '\\`')
                  .replace(/\$/g, '\\$');
                return `\`${escaped}\``;
              } catch {
                return `''`;
              }
            })
            .join(', ');
          return `styles: [${inlinedStyles}]`;
        },
      );

      if (transformed !== code) {
        return { code: transformed, map: null };
      }
      return undefined;
    },
  };
}

export default defineConfig({
  resolve: {
    mainFields: ['es2020', 'module', 'main'],
    conditions: ['es2015', 'es2020', 'module'],
  },
  plugins: [angularResourceInliner()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['src/test-setup.ts'],
    include: ['src/**/*.spec.ts'],
    exclude: ['e2e/**', 'node_modules/**', 'dist/**'],
    passWithNoTests: true,
    testTimeout: 60000,
  },
});
