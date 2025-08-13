import { MongoMemoryServer } from 'mongodb-memory-server';
import mongoose from 'mongoose';

// Setup MongoDB Memory Server for testing
let mongoServer;

// Connect to the in-memory database before all tests
beforeAll(async () => {
  mongoServer = await MongoMemoryServer.create();
  const uri = mongoServer.getUri();
  
  // Connect with settings appropriate for testing
  await mongoose.connect(uri, {
    // These options are no longer needed in newer mongoose versions
    // but added here for compatibility
  });
});

// Clear all test data after each test
afterEach(async () => {
  const collections = mongoose.connection.collections;
  for (const key in collections) {
    const collection = collections[key];
    await collection.deleteMany({});
  }
});

// Disconnect and stop MongoDB Memory Server after all tests
afterAll(async () => {
  await mongoose.disconnect();
  await mongoServer.stop();
});

// Mock the environment variables
process.env.JWT_SECRET = 'test-jwt-secret';

// This is a dummy test to prevent the "Your test suite must contain at least one test" error
describe('Setup', () => {
  test('MongoDB Memory Server setup successful', () => {
    expect(mongoServer).toBeDefined();
  });
});
