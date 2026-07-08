import { test, expect } from '@playwright/test';

test.describe('Smoke Test', () => {
  test('should display the application title', async ({ page }) => {
    await page.goto('/');

    const heading = page.getByRole('heading', { name: 'Hello, staff-engagement' });
    await expect(heading).toBeVisible();
  });
});
