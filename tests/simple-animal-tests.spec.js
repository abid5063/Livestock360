// @ts-check
import { test, expect } from '@playwright/test';

test.describe('Simple Animal Management Tests', () => {
  let testUser = null;

  test.beforeEach(async ({ page, context }) => {
    // Clear cookies and context state
    await context.clearCookies();
    
    // Navigate to the app
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    // Register a new test user
    const timestamp = Date.now();
    testUser = {
      name: `TestUser${timestamp}`,
      email: `test${timestamp}@example.com`,
      password: 'testpass123'
    };

    // Switch to register mode
    await page.getByTestId('auth-toggle-mode-button').click();
    
    // Fill registration form
    await page.getByTestId('auth-name-input').fill(testUser.name);
    await page.getByTestId('auth-email-input').fill(testUser.email);
    await page.getByTestId('auth-password-input').fill(testUser.password);
    
    // Submit registration
    await page.getByTestId('auth-submit-button').click();
    
    // Wait for navigation to profile page by waiting for profile elements
    await page.waitForURL(/.*\/profile/, { timeout: 10000 });
    await page.getByTestId('add-animal-button').waitFor({ state: 'visible', timeout: 10000 });
  });

  test('should open and close add animal modal', async ({ page }) => {
    // Click add animal button (already verified as visible in beforeEach)
    await page.getByTestId('add-animal-button').click();
    
    // Verify modal opens
    await expect(page.getByTestId('animal-name-input')).toBeVisible();
    await expect(page.getByTestId('modal-cancel-button')).toBeVisible();
    
    // Close modal
    await page.getByTestId('modal-cancel-button').click();
    
    // Verify modal closes
    await expect(page.getByTestId('animal-name-input')).not.toBeVisible();
  });

  test('should fill animal form fields', async ({ page }) => {
    // Open add animal modal (button already verified as visible in beforeEach)
    await page.getByTestId('add-animal-button').click();
    
    // Fill form fields
    await page.getByTestId('animal-name-input').fill('Test Cow');
    await page.getByTestId('animal-type-input').fill('Cattle');
    await page.getByTestId('animal-breed-input').fill('Holstein');
    await page.getByTestId('animal-age-input').fill('3');
    await page.getByTestId('animal-gender-input').fill('Female');
    await page.getByTestId('animal-details-input').fill('Healthy dairy cow');
    
    // Verify fields are filled
    await expect(page.getByTestId('animal-name-input')).toHaveValue('Test Cow');
    await expect(page.getByTestId('animal-type-input')).toHaveValue('Cattle');
    await expect(page.getByTestId('animal-breed-input')).toHaveValue('Holstein');
    
    // Cancel without saving
    await page.getByTestId('modal-cancel-button').click();
  });

  test('should require name field for animal creation', async ({ page }) => {
    // Open add animal modal (button already verified as visible in beforeEach)
    await page.getByTestId('add-animal-button').click();
    
    // Try to save without filling name
    await page.getByTestId('modal-save-button').click();
    
    // Modal should still be open (validation prevents saving)
    await expect(page.getByTestId('animal-name-input')).toBeVisible();
    
    // Fill name and try again
    await page.getByTestId('animal-name-input').fill('Test Animal');
    
    // Try to save again (might still need more fields)
    await page.getByTestId('modal-save-button').click();
    
    // Cancel modal
    await page.getByTestId('modal-cancel-button').click();
  });

  test('should navigate to AI features from profile', async ({ page }) => {
    // Navigate to AI Chatbot (button already verified as visible in beforeEach)
    await page.getByTestId('ai-chatbot-button').click();
    await page.waitForTimeout(2000);
    
    // Verify we're on chatbot page
    await expect(page.getByTestId('message-input')).toBeVisible();
    
    // Go back to profile
    await page.goBack();
    await page.waitForTimeout(2000);
    
    // Should be back on profile
    await expect(page.getByTestId('add-animal-button')).toBeVisible();
  });

  test('should navigate to symptom checker', async ({ page }) => {
    // Navigate to symptom checker (button already verified as visible in beforeEach)
    await page.getByTestId('disease-detection-button').click();
    await page.waitForTimeout(2000);
    
    // Verify we're on symptom checker page
    await expect(page.getByTestId('symptoms-input')).toBeVisible();
    
    // Fill symptom checker input
    await page.getByTestId('symptoms-input').fill('Cow seems tired');
    
    // Verify input was filled
    await expect(page.getByTestId('symptoms-input')).toHaveValue('Cow seems tired');
  });
});
