import { test, expect } from '@playwright/test';

test.describe('Smoke Test', () => {
  test('should redirect unauthenticated user to login', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByTestId('login-submit-button')).toBeVisible();
  });
});
