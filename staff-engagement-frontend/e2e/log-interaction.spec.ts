import { test, expect } from '@playwright/test';

test.describe('Log Interaction - Full Flow', () => {
  test.beforeEach(async ({ page }) => {
    // Login with seeded test credentials
    await page.goto('/login');
    await page.getByTestId('login-email-input').fill('admin@example.com');
    await page.getByTestId('login-password-input').fill('password123');
    await page.getByTestId('login-submit-button').click();

    // Wait for redirect after successful login
    await page.waitForURL(/(?!.*\/login)/);
  });

  test('should submit interaction with follow-up task and show success', async ({ page }) => {
    // 1. Navigate to /interaction
    await page.goto('/interaction');

    // Wait for employee picker to be populated (more than just the placeholder option)
    const employeeSelect = page.locator('#employeeId');
    await expect(employeeSelect.locator('option')).not.toHaveCount(1, { timeout: 10000 });

    // 2. Select the first employee option (index 1 skips the disabled placeholder)
    await employeeSelect.selectOption({ index: 1 });

    // 3. Conducted By is already defaulted to current user — leave as-is

    // 4. Select interaction type "Check In"
    const typeSelect = page.locator('#type');
    await typeSelect.selectOption({ label: 'Check In' });

    // 5. Enter notes
    const notesTextarea = page.locator('#notes');
    await notesTextarea.fill('E2E test interaction notes');

    // 6. occurred_at is already defaulted to now — leave as-is

    // 7. Expand inline task section
    const taskToggleButton = page.getByRole('button', { name: 'Add Follow-Up Task' });
    await taskToggleButton.click();

    // 8. Enter task title
    const taskTitleInput = page.locator('#taskTitle');
    await expect(taskTitleInput).toBeVisible();
    await taskTitleInput.fill('E2E follow-up task');

    // 9. Submit the form
    const submitButton = page.locator('button.submit-btn');
    await submitButton.click();

    // 10. Assert: success notification visible (text contains "created successfully")
    const successMessage = page.locator('.success-message');
    await expect(successMessage).toBeVisible({ timeout: 10000 });
    await expect(successMessage).toContainText('created successfully', { ignoreCase: true });

    // 11. Assert: form is reset
    // Employee picker resets — the selected option text should be the placeholder
    const employeeSelectedText = employeeSelect.locator('option:checked');
    await expect(employeeSelectedText).toHaveText('-- Select --');
    // Type resets to placeholder
    const typeSelectedText = typeSelect.locator('option:checked');
    await expect(typeSelectedText).toHaveText('-- Select --');
    // Notes is empty
    await expect(notesTextarea).toHaveValue('');

    // 12. Assert: task section is collapsed (toggle button says "Add Follow-Up Task")
    await expect(
      page.getByRole('button', { name: 'Add Follow-Up Task' })
    ).toBeVisible();
    await expect(page.locator('#taskTitle')).not.toBeVisible();
  });
});
