import { isValidEmail, isValidPassword, isNotEmpty } from '../../utils/validation';

describe('Validation Utils', () => {
  describe('isValidEmail', () => {
    it('should return true for valid email addresses', () => {
      expect(isValidEmail('test@example.com')).toBe(true);
      expect(isValidEmail('user.name@domain.co')).toBe(true);
      expect(isValidEmail('user+tag@example.org')).toBe(true);
    });

    it('should return false for invalid email addresses', () => {
      expect(isValidEmail('')).toBe(false);
      expect(isValidEmail(null)).toBe(false);
      expect(isValidEmail('test@')).toBe(false);
      expect(isValidEmail('test@domain')).toBe(false);
      expect(isValidEmail('test.com')).toBe(false);
    });
  });

  describe('isValidPassword', () => {
    it('should return true for passwords with at least 6 characters', () => {
      expect(isValidPassword('password123')).toBe(true);
      expect(isValidPassword('123456')).toBe(true);
    });

    it('should return false for passwords with less than 6 characters', () => {
      expect(isValidPassword('12345')).toBe(false);
      expect(isValidPassword('')).toBe(false);
      expect(isValidPassword(null)).toBe(false);
    });
  });

  describe('isNotEmpty', () => {
    it('should return true for non-empty values', () => {
      expect(isNotEmpty('test')).toBe(true);
      expect(isNotEmpty('0')).toBe(true);
    });

    it('should return false for empty values', () => {
      expect(isNotEmpty('')).toBe(false);
      expect(isNotEmpty('  ')).toBe(false);
      expect(isNotEmpty(null)).toBe(false);
      expect(isNotEmpty(undefined)).toBe(false);
    });
  });
});
