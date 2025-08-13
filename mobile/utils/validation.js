// Basic validation functions for the AGL mobile app

/**
 * Validates that an email address is properly formatted
 * @param {string} email - The email address to validate
 * @returns {boolean} - True if email is valid, false otherwise
 */
export const isValidEmail = (email) => {
  if (!email) return false;
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Validates that a password meets minimum requirements
 * @param {string} password - The password to validate
 * @returns {boolean} - True if password is valid, false otherwise
 */
export const isValidPassword = (password) => {
  if (!password) return false;
  return password.length >= 6;
};

/**
 * Validates that a required field is not empty
 * @param {string} value - The value to check
 * @returns {boolean} - True if value is not empty, false otherwise
 */
export const isNotEmpty = (value) => {
  return value !== undefined && value !== null && value.trim() !== '';
};
