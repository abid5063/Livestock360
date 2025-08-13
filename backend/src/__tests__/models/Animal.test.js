import mongoose from 'mongoose';
import Animal from '../../models/Animal.js';
import Farmer from '../../models/Farmer.js';

describe('Animal Model', () => {
  let farmerId;

  // Create a test farmer before each test
  beforeEach(async () => {
    const farmer = new Farmer({
      name: 'Test Farmer',
      email: 'test@example.com',
      password: 'password123'
    });
    
    const savedFarmer = await farmer.save();
    farmerId = savedFarmer._id;
  });

  it('should create a new animal successfully', async () => {
    const animalData = {
      farmer: farmerId,
      name: 'Bessie',
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      gender: 'Female',
      details: 'Healthy dairy cow'
    };

    const animal = new Animal(animalData);
    const savedAnimal = await animal.save();

    // Verify animal was saved correctly
    expect(savedAnimal._id).toBeDefined();
    expect(savedAnimal.name).toBe(animalData.name);
    expect(savedAnimal.type).toBe(animalData.type);
    expect(savedAnimal.breed).toBe(animalData.breed);
    expect(savedAnimal.age).toBe(animalData.age);
    expect(savedAnimal.gender).toBe(animalData.gender);
    expect(savedAnimal.details).toBe(animalData.details);
    expect(savedAnimal.farmer.toString()).toBe(farmerId.toString());
  });

  it('should not create an animal without required fields', async () => {
    const animalWithoutRequiredField = new Animal({
      farmer: farmerId,
      name: 'Bessie',
      // missing type
      breed: 'Holstein',
      age: 5,
      gender: 'Female'
    });

    let error;
    try {
      await animalWithoutRequiredField.save();
    } catch (err) {
      error = err;
    }

    expect(error).toBeDefined();
    expect(error.name).toBe('ValidationError');
  });

  it('should not allow duplicate animal names for the same farmer', async () => {
    // Create first animal
    const animalData = {
      farmer: farmerId,
      name: 'Bessie',
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      gender: 'Female'
    };

    await new Animal(animalData).save();

    // Try to create a second animal with the same name
    const duplicateAnimal = new Animal(animalData);

    let error;
    try {
      await duplicateAnimal.save();
    } catch (err) {
      error = err;
    }

    expect(error).toBeDefined();
    expect(error.code).toBe(11000); // MongoDB duplicate key error code
  });

  it('should allow the same animal name for different farmers', async () => {
    // Create a second farmer
    const secondFarmer = new Farmer({
      name: 'Second Farmer',
      email: 'second@example.com',
      password: 'password123'
    });
    
    const savedSecondFarmer = await secondFarmer.save();
    
    // Create first animal
    const firstAnimalData = {
      farmer: farmerId,
      name: 'Bessie',
      type: 'Cattle',
      breed: 'Holstein',
      age: 5,
      gender: 'Female'
    };

    await new Animal(firstAnimalData).save();

    // Create second animal with same name but different farmer
    const secondAnimalData = {
      farmer: savedSecondFarmer._id,
      name: 'Bessie',
      type: 'Cattle',
      breed: 'Jersey',
      age: 3,
      gender: 'Female'
    };

    const secondAnimal = new Animal(secondAnimalData);
    const savedSecondAnimal = await secondAnimal.save();

    expect(savedSecondAnimal._id).toBeDefined();
    expect(savedSecondAnimal.name).toBe('Bessie');
    expect(savedSecondAnimal.farmer.toString()).toBe(savedSecondFarmer._id.toString());
  });
});
