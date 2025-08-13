// @ts-check
import { test, expect } from '@playwright/test';

test.describe('Simple AI Features Tests', () => {
  let testUser = null;

  test.beforeEach(async ({ page, context }) => {
    await context.clearCookies();
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    // Register and login a test user
    const timestamp = Date.now();
    testUser = {
      name: `TestUser${timestamp}`,
      email: `test${timestamp}@example.com`,
      password: 'testpass123'
    };

    await page.getByTestId('auth-toggle-mode-button').click();
    await page.getByTestId('auth-name-input').fill(testUser.name);
    await page.getByTestId('auth-email-input').fill(testUser.email);
    await page.getByTestId('auth-password-input').fill(testUser.password);
    await page.getByTestId('auth-submit-button').click();
    
    // Wait for profile page
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
  });

  test('should access AI chatbot', async ({ page }) => {
    // Click AI chatbot button
    await page.getByTestId('ai-chatbot-button').click();
    await page.waitForTimeout(2000);
    
    // Should be on chatbot page
    await expect(page.getByTestId('message-input')).toBeVisible();
    
    // Should see send button
    await expect(page.getByTestId('send-button')).toBeVisible();
  });

  test('should type message in chatbot', async ({ page }) => {
    // Navigate to chatbot
    await page.getByTestId('ai-chatbot-button').click();
    await page.waitForTimeout(2000);
    
    // Type a message
    await page.getByTestId('message-input').fill('Hello, I need help with my cow');
    
    // Verify message was typed
    await expect(page.getByTestId('message-input')).toHaveValue('Hello, I need help with my cow');
  });

  test('should access symptom checker', async ({ page }) => {
    // Click symptom checker button  
    await page.getByTestId('disease-detection-button').click();
    await page.waitForTimeout(2000);
    
    // Should be on symptom checker page
    await expect(page.getByTestId('symptoms-input')).toBeVisible();
  });

  test('should type symptoms in symptom checker', async ({ page }) => {
    // Navigate to symptom checker
    await page.getByTestId('disease-detection-button').click();
    await page.waitForTimeout(2000);
    
    // Type symptoms
    await page.getByTestId('symptoms-input').fill('My cow is not eating and seems lethargic');
    
    // Verify symptoms were typed
    await expect(page.getByTestId('symptoms-input')).toHaveValue('My cow is not eating and seems lethargic');
  });

  test('should navigate back from AI features', async ({ page }) => {
    // Go to chatbot
    await page.getByTestId('ai-chatbot-button').click();
    await page.waitForTimeout(2000);
    await expect(page.getByTestId('message-input')).toBeVisible();
    
    // Navigate back
    await page.goBack();
    await page.waitForTimeout(2000);
    
    // Should be back on profile
    await expect(page.getByTestId('add-animal-button')).toBeVisible();
    
    // Go to symptom checker
    await page.getByTestId('disease-detection-button').click();
    await page.waitForTimeout(2000);
    await expect(page.getByTestId('symptoms-input')).toBeVisible();
    
    // Navigate back again
    await page.goBack();
    await page.waitForTimeout(2000);
    
    // Should be back on profile
    await expect(page.getByTestId('add-animal-button')).toBeVisible();
  });
});
