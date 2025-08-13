import mongoose from 'mongoose';
import Farmer from '../../models/Farmer.js';

describe('Farmer Model', () => {
  it('should create a new farmer successfully', async () => {
    const farmerData = {
      name: 'Test Farmer',
      email: 'test@example.com',
      password: 'password123'
    };

    const farmer = new Farmer(farmerData);
    const savedFarmer = await farmer.save();

    // Verify farmer was saved correctly
    expect(savedFarmer._id).toBeDefined();
    expect(savedFarmer.name).toBe(farmerData.name);
    expect(savedFarmer.email).toBe(farmerData.email);
    
    // Password should be hashed and not equal to the original
    expect(savedFarmer.password).not.toBe(farmerData.password);
    
    // Optional fields should be set to defaults
    expect(savedFarmer.profileImage).toBe('');
    expect(savedFarmer.phoneNo).toBe('');
  });

  it('should not create a farmer without required fields', async () => {
    const farmerWithoutName = new Farmer({
      email: 'test@example.com',
      password: 'password123'
    });

    let error;
    try {
      await farmerWithoutName.save();
    } catch (err) {
      error = err;
    }

    expect(error).toBeDefined();
    expect(error.name).toBe('ValidationError');
    expect(error.errors.name).toBeDefined();
  });

  it('should not allow duplicate email addresses', async () => {
    // Create first farmer
    const farmerData = {
      name: 'Test Farmer',
      email: 'duplicate@example.com',
      password: 'password123'
    };

    await new Farmer(farmerData).save();

    // Try to create a second farmer with the same email
    const duplicateFarmer = new Farmer({
      name: 'Another Farmer',
      email: 'duplicate@example.com',  // Same email as above
      password: 'different123'
    });

    let error;
    try {
      await duplicateFarmer.save();
    } catch (err) {
      error = err;
    }

    expect(error).toBeDefined();
    expect(error.code).toBe(11000); // MongoDB duplicate key error code
  });

  it('should enforce minimum password length', async () => {
    const farmerWithShortPassword = new Farmer({
      name: 'Test Farmer',
      email: 'test@example.com',
      password: '12345' // Too short (minimum is 6)
    });

    let error;
    try {
      await farmerWithShortPassword.save();
    } catch (err) {
      error = err;
    }

    expect(error).toBeDefined();
    expect(error.name).toBe('ValidationError');
    expect(error.errors.password).toBeDefined();
  });

  it('should correctly compare passwords', async () => {
    const password = 'testPassword123';
    
    // Create a farmer with the test password
    const farmer = new Farmer({
      name: 'Test Farmer',
      email: 'password-test@example.com',
      password: password
    });
    
    await farmer.save();
    
    // Test correct password
    const validPassword = await farmer.comparePassword(password);
    expect(validPassword).toBe(true);
    
    // Test incorrect password
    const invalidPassword = await farmer.comparePassword('wrongPassword');
    expect(invalidPassword).toBe(false);
  });

  it('should not rehash password if it was not modified', async () => {
    // Create a farmer
    const farmer = new Farmer({
      name: 'Test Farmer',
      email: 'nohash@example.com',
      password: 'password123'
    });
    
    // Save to trigger initial password hashing
    await farmer.save();
    const originalHash = farmer.password;
    
    // Update a non-password field
    farmer.name = 'Updated Name';
    await farmer.save();
    
    // Password hash should remain the same
    expect(farmer.password).toBe(originalHash);
  });

  describe('Field Validation Tests', () => {
    test('Email is required', () => {
      const farmer = new Farmer({
        name: 'Test Farmer',
        password: 'password123'
        // Missing email
      });
  
      const validationError = farmer.validateSync();
      expect(validationError.errors.email).toBeDefined();
    });
  
    test('Name is required', () => {
      const farmer = new Farmer({
        email: 'test@example.com',
        password: 'password123'
        // Missing name
      });
  
      const validationError = farmer.validateSync();
      expect(validationError.errors.name).toBeDefined();
    });
  
    test('Password is required', () => {
      const farmer = new Farmer({
        name: 'Test Farmer',
        email: 'test@example.com'
        // Missing password
      });
  
      const validationError = farmer.validateSync();
      expect(validationError.errors.password).toBeDefined();
    });
  
    test('Password must be at least 6 characters', () => {
      const farmer = new Farmer({
        name: 'Test Farmer',
        email: 'test@example.com',
        password: '12345' // 5 characters, too short
      });
  
      const validationError = farmer.validateSync();
      expect(validationError.errors.password).toBeDefined();
    });
  
    test('Valid farmer data should not have validation errors', () => {
      const farmer = new Farmer({
        name: 'Test Farmer',
        email: 'test@example.com',
        password: 'password123'
      });
  
      const validationError = farmer.validateSync();
      expect(validationError).toBeUndefined();
    });
  });
});
