// @ts-check
import { test, expect } from '@playwright/test';

test.describe('Simple Navigation Tests', () => {
  
  test('should display login form by default', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    // Should see login form elements
    await expect(page.getByTestId('auth-email-input')).toBeVisible();
    await expect(page.getByTestId('auth-password-input')).toBeVisible();
    await expect(page.getByTestId('auth-submit-button')).toContainText('Sign In');
  });

  test('should switch between login and register modes', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    // Start in login mode
    await expect(page.getByTestId('auth-submit-button')).toContainText('Sign In');
    
    // Switch to register mode
    await page.getByTestId('auth-toggle-mode-button').click();
    
    // Should now see register form
    await expect(page.getByTestId('auth-name-input')).toBeVisible();
    await expect(page.getByTestId('auth-submit-button')).toContainText('Sign Up');
    
    // Switch back to login mode
    await page.getByTestId('auth-toggle-mode-button').click();
    
    // Should be back to login
    await expect(page.getByTestId('auth-submit-button')).toContainText('Sign In');
    await expect(page.getByTestId('auth-name-input')).not.toBeVisible();
  });

  test('should fill form fields correctly', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    // Fill login form
    await page.getByTestId('auth-email-input').fill('test@example.com');
    await page.getByTestId('auth-password-input').fill('password123');
    
    // Verify fields are filled
    await expect(page.getByTestId('auth-email-input')).toHaveValue('test@example.com');
    await expect(page.getByTestId('auth-password-input')).toHaveValue('password123');
    
    // Switch to register and fill name
    await page.getByTestId('auth-toggle-mode-button').click();
    await page.getByTestId('auth-name-input').fill('Test User');
    
    // Verify name field
    await expect(page.getByTestId('auth-name-input')).toHaveValue('Test User');
  });

  test('should complete registration flow', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    // Create unique user data
    const timestamp = Date.now();
    const testUser = {
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
    
    // Wait for navigation
    await page.waitForTimeout(5000);
    
    // Should be on profile page
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(testUser.name)).toBeVisible();
  });

  test('should handle page refresh after login', async ({ page }) => {
    // First register a user
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    const timestamp = Date.now();
    const testUser = {
      name: `TestUser${timestamp}`,
      email: `test${timestamp}@example.com`,
      password: 'testpass123'
    };

    // Register
    await page.getByTestId('auth-toggle-mode-button').click();
    await page.getByTestId('auth-name-input').fill(testUser.name);
    await page.getByTestId('auth-email-input').fill(testUser.email);
    await page.getByTestId('auth-password-input').fill(testUser.password);
    await page.getByTestId('auth-submit-button').click();
    
    // Wait for profile page
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
    
    // Refresh the page
    await page.reload();
    await page.waitForLoadState('networkidle');
    
    // Should still be on profile page (session persistence)
    await expect(page.getByTestId('add-animal-button')).toBeVisible({ timeout: 10000 });
  });
});
