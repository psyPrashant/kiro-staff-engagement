# E2E Tests (Playwright)

End-to-end tests for the Staff Engagement frontend using [Playwright](https://playwright.dev/).

## File Location and Naming

- All e2e test files live in the `e2e/` directory at the frontend project root.
- Test files use the naming pattern: `<feature>.spec.ts` (e.g., `smoke.spec.ts`, `login.spec.ts`).
- Group related tests using `test.describe()` blocks.

## Selector Strategy

Use selectors in the following order of preference:

1. **Accessible roles** — `page.getByRole('button', { name: 'Submit' })` — preferred for interactive elements.
2. **`data-testid` attributes** — `page.getByTestId('user-card')` — use when no accessible role or text is available.
3. **Text content** — `page.getByText('Hello')` — acceptable for asserting visible content.
4. **CSS selectors** — `page.locator('.class-name')` — last resort; avoid relying on implementation details.

When adding new components, include `data-testid` attributes on key elements to support stable test selectors.

## Running Tests Locally

### Prerequisites

Install Playwright browsers (one-time setup):

```bash
npx playwright install --with-deps chromium
```

### Run all e2e tests

```bash
npx playwright test
```

This automatically starts the Angular dev server on port 4200 via the `webServer` config in `playwright.config.ts`.

### Run tests in headed mode (see the browser)

```bash
npx playwright test --headed
```

### Run a specific test file

```bash
npx playwright test e2e/smoke.spec.ts
```

### View the HTML report after a test run

```bash
npx playwright show-report
```

### Debug tests interactively

```bash
npx playwright test --debug
```

## CI

In CI, Playwright tests run automatically as part of the frontend workflow. The `webServer` config handles starting the dev server. Reports are uploaded as build artifacts.
