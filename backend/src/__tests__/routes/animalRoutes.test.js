import mongoose from 'mongoose';
import Animal from '../../models/Animal.js';

// Simple mock implementation of Animal model
const mockAnimal = {
  _id: new mongoose.Types.ObjectId(),
  name: 'Test Animal',
  type: 'Cattle',
  breed: 'Holstein',
  age: 5,
  gender: 'Female',
  details: 'Test details',
  photo_url: 'http://example.com/photo.jpg',
  farmer: new mongoose.Types.ObjectId()
};

describe('Animal Model Tests', () => {
  // Testing validation logic
  test('Animal should require name', () => {
    const animalWithoutName = new Animal({
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithoutName.validateSync();
    expect(validationError.errors.name).toBeDefined();
  });

  test('Animal should require type', () => {
    const animalWithoutType = new Animal({
      name: 'Test Animal',
      breed: 'Holstein',
      age: 5,
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithoutType.validateSync();
    expect(validationError.errors.type).toBeDefined();
  });

  test('Animal should require breed', () => {
    const animalWithoutBreed = new Animal({
      name: 'Test Animal',
      type: 'Cattle',
      age: 5,
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithoutBreed.validateSync();
    expect(validationError.errors.breed).toBeDefined();
  });

  test('Animal should require age', () => {
    const animalWithoutAge = new Animal({
      name: 'Test Animal',
      type: 'Cattle',
      breed: 'Holstein',
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithoutAge.validateSync();
    expect(validationError.errors.age).toBeDefined();
  });

  test('Animal should require gender', () => {
    const animalWithoutGender = new Animal({
      name: 'Test Animal',
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithoutGender.validateSync();
    expect(validationError.errors.gender).toBeDefined();
  });

  test('Animal should validate age min', () => {
    const animalWithInvalidAge = new Animal({
      name: 'Test Animal',
      type: 'Cattle',
      breed: 'Holstein',
      age: -1, // Negative age should be invalid
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithInvalidAge.validateSync();
    expect(validationError.errors.age).toBeDefined();
  });

  test('Animal should validate age max', () => {
    const animalWithInvalidAge = new Animal({
      name: 'Test Animal',
      type: 'Cattle',
      breed: 'Holstein',
      age: 100, // Age > 50 should be invalid
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithInvalidAge.validateSync();
    expect(validationError.errors.age).toBeDefined();
  });

  test('Animal should validate name min length', () => {
    const animalWithShortName = new Animal({
      name: 'A', // 1 character, too short
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithShortName.validateSync();
    expect(validationError.errors.name).toBeDefined();
  });

  test('Animal should validate name max length', () => {
    const animalWithLongName = new Animal({
      name: 'A'.repeat(51), // 51 characters, too long
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = animalWithLongName.validateSync();
    expect(validationError.errors.name).toBeDefined();
  });

  test('Valid animal should not have validation errors', () => {
    const validAnimal = new Animal({
      name: 'Test Animal',
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      gender: 'Female',
      farmer: new mongoose.Types.ObjectId()
    });

    const validationError = validAnimal.validateSync();
    expect(validationError).toBeUndefined();
  });
});
