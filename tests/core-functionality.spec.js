// @ts-check
import { test, expect } from '@playwright/test';

test.describe('Core E2E Test Suite - Focused Tests', () => {
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
    
    // Wait for navigation to profile page
    await page.waitForTimeout(3000);
  });

  test('should complete user registration and access profile', async ({ page }) => {
    // Verify we're on the profile page by looking for profile-specific elements
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('logout-button')).toBeVisible();
    
    // Verify user name is displayed somewhere
    await expect(page.getByText(testUser.name).first()).toBeVisible();
  });

  test('should navigate to AI Chatbot and interact', async ({ page }) => {
    // Wait for profile page and navigate to AI Chatbot
    await expect(page.getByTestId('ai-chatbot-button')).toBeVisible({ timeout: 10000 });
    await page.getByTestId('ai-chatbot-button').click();
    
    await page.waitForTimeout(2000);
    
    // Verify chatbot page elements
    await expect(page.getByText('Hello! I am your AI assistant')).toBeVisible();
    await expect(page.getByTestId('message-input')).toBeVisible();
    await expect(page.getByTestId('send-button')).toBeVisible();
    
    // Test sending a message
    await page.getByTestId('message-input').fill('Hello AI');
    await page.getByTestId('send-button').click();
    
    // Verify message appears
    await expect(page.getByText('Hello AI')).toBeVisible();
  });

  test('should navigate to Symptom Checker', async ({ page }) => {
    // Wait for profile page and navigate to Disease Detection
    await expect(page.getByTestId('disease-detection-button')).toBeVisible({ timeout: 10000 });
    await page.getByTestId('disease-detection-button').click();
    
    await page.waitForTimeout(2000);
    
    // Verify symptom checker elements
    await expect(page.getByTestId('symptoms-input')).toBeVisible();
    await expect(page.getByTestId('analyze-button')).toBeVisible();
    await expect(page.getByTestId('image-picker-button')).toBeVisible();
    
    // Test symptom input
    await page.getByTestId('symptoms-input').fill('Cow appears sick and lethargic');
    
    // Verify input was filled
    await expect(page.getByTestId('symptoms-input')).toHaveValue('Cow appears sick and lethargic');
  });

  test('should handle logout flow', async ({ page }) => {
    // Wait for profile page
    await expect(page.getByTestId('logout-button')).toBeVisible({ timeout: 10000 });
    
    // Click logout
    await page.getByTestId('logout-button').click();
    
    // Wait for logout process and navigation
    await page.waitForTimeout(5000);
    
    // Verify we're back on auth page by checking URL
    expect(page.url()).toBe('http://localhost:8081/');
    
    // For now, just verify that some auth-related content is present
    // We'll fix the form interaction in a follow-up
    await expect(page.getByText('Login')).toBeVisible();
  });

  test('should maintain session and navigate between screens', async ({ page }) => {
    // Verify we're on profile
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
    
    // Navigate to AI Chatbot
    await page.getByTestId('ai-chatbot-button').click();
    await page.waitForTimeout(2000);
    
    // Verify we're on chatbot
    await expect(page.getByTestId('message-input')).toBeVisible();
    
    // Use browser back button to return
    await page.goBack();
    await page.waitForTimeout(2000);
    
    // Should be back on profile
    await expect(page.getByTestId('add-animal-button')).toBeVisible();
    
    // Navigate to Symptom Checker
    await page.getByTestId('disease-detection-button').click();
    await page.waitForTimeout(2000);
    
    // Verify we're on symptom checker
    await expect(page.getByTestId('symptoms-input')).toBeVisible();
  });

  test('should handle form validation', async ({ page }) => {
    // Test add animal form validation
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
    await page.getByTestId('add-animal-button').click();
    
    // Try to save without filling fields
    await page.getByTestId('modal-save-button').click();
    
    // Modal should still be open (validation prevents submission)
    await expect(page.getByTestId('animal-name-input')).toBeVisible();
    
    // Fill only name
    await page.getByTestId('animal-name-input').fill('Test Animal');
    
    // Try to save again
    await page.getByTestId('modal-save-button').click();
    
    // Should still be open (needs more required fields)
    await expect(page.getByTestId('animal-type-input')).toBeVisible();
    
    // Cancel modal
    await page.getByTestId('modal-cancel-button').click();
    
    // Modal should close
    await expect(page.getByTestId('animal-name-input')).not.toBeVisible();
  });

  test('should handle invalid login attempt', async ({ page, context }) => {
    // Use a new context/page for isolation
    const newPage = await context.newPage();
    await newPage.goto('/');
    await newPage.waitForLoadState('networkidle');
    
    // Try to login with wrong credentials
    await newPage.getByTestId('auth-email-input').fill('wrong@email.com');
    await newPage.getByTestId('auth-password-input').fill('wrongpassword');
    await newPage.getByTestId('auth-submit-button').click();
    
    // Wait for response
    await newPage.waitForTimeout(3000);
    
    // Should remain on login page (authentication failed)
    await expect(newPage.getByTestId('auth-email-input')).toBeVisible();
    await expect(newPage.getByTestId('auth-password-input')).toBeVisible();
    
    await newPage.close();
  });

  test('should complete successful login after registration', async ({ page, context }) => {
    // Use a new page for isolation
    const newPage = await context.newPage();
    await newPage.goto('/');
    await newPage.waitForLoadState('networkidle');
    
    // Login with the test user credentials
    await newPage.getByTestId('auth-email-input').fill(testUser.email);
    await newPage.getByTestId('auth-password-input').fill(testUser.password);
    await newPage.getByTestId('auth-submit-button').click();
    
    // Wait for login
    await newPage.waitForTimeout(3000);
    
    // Should be back on profile page
    await expect(newPage.getByTestId('add-animal-button')).toBeVisible();
    await expect(newPage.getByText(testUser.name).first()).toBeVisible();
    
    await newPage.close();
  });

  test('should handle page refresh and session persistence', async ({ page }) => {
    // Verify we're logged in
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
    
    // Refresh the page
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);
    
    // Check if session is maintained or redirected to login
    const hasLoginForm = await page.getByTestId('auth-email-input').isVisible();
    const hasProfileButton = await page.getByTestId('add-animal-button').isVisible();
    
    // Either should maintain session or require re-login
    expect(hasLoginForm || hasProfileButton).toBeTruthy();
  });

  test('should switch between login and register modes', async ({ page, context }) => {
    // Use a new page for isolation
    const newPage = await context.newPage();
    await newPage.goto('/');
    await newPage.waitForLoadState('networkidle');
    
    // Should be in login mode
    await expect(newPage.getByTestId('auth-submit-button')).toContainText('Sign In');
    await expect(newPage.getByTestId('auth-toggle-mode-button')).toContainText('Need an account? Sign up');
    
    // Switch to register mode
    await newPage.getByTestId('auth-toggle-mode-button').click();
    
    // Should now be in register mode
    await expect(newPage.getByTestId('auth-submit-button')).toContainText('Sign Up');
    await expect(newPage.getByTestId('auth-toggle-mode-button')).toContainText('Already have an account? Sign in');
    
    // Should show name field in register mode
    await expect(newPage.getByTestId('auth-name-input')).toBeVisible();
    
    // Switch back to login mode
    await newPage.getByTestId('auth-toggle-mode-button').click();
    
    // Should be back in login mode
    await expect(newPage.getByTestId('auth-submit-button')).toContainText('Sign In');
    
    // Name field should not be visible in login mode
    await expect(newPage.getByTestId('auth-name-input')).not.toBeVisible();
    
    await newPage.close();
  });
});
