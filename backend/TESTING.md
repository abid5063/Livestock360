# Backend Unit Testing Implementation

This document outlines the unit testing setup implemented for the backend of the AGROLINK application.

## Testing Tools Used

- **Jest**: The testing framework
- **MongoDB Memory Server**: For in-memory database testing
- **Mongoose**: For data modeling and validation testing

## Files Created/Modified

1. **jest.config.json**: Configured Jest for ES modules and test setup
2. **package.json**: Added test scripts and dependencies
3. **src/__tests__/setup.js**: Created setup file for MongoDB Memory Server
4. **src/__tests__/models/Animal.test.js**: Tests for the Animal model
5. **src/__tests__/models/Farmer.test.js**: Tests for the Farmer model
6. **src/__tests__/routes/animalRoutes.test.js**: Tests for animal validation logic

## Types of Tests Implemented

### Animal Model Tests

- Schema validation tests for required fields
- Tests for unique constraints (duplicate animal names for the same farmer)
- Tests for allowing the same animal name for different farmers
- Validation of field constraints (min/max length, min/max values)

### Farmer Model Tests

- Schema validation tests for required fields (name, email, password)
- Tests for unique constraints (duplicate email addresses)
- Password hashing and comparison functionality
- Password minimum length validation
- Tests for conditional password rehashing

### Route Validation Tests

- Tests for required fields validation
- Tests for field value constraints
- Tests for field length validation

## Mocking Strategy

- **Database**: Used MongoDB Memory Server to create an in-memory database for tests
- **Validation**: Used Mongoose's validateSync method to test schema validation without database calls

## Running the Tests

```bash
# Run all tests
npm test

# Run specific test file
npm test -- src/__tests__/models/Animal.test.js

# Run tests with watch mode
npm run test:watch
```

## Future Test Recommendations

1. **Authentication Tests**: Test JWT authentication and farmer authorization
2. **Route Integration Tests**: Extend testing to include complete API route testing using supertest
3. **Cloudinary Service Mocking**: Implement more complete mocks for Cloudinary service
4. **Error Handling Tests**: Test error scenarios and error response handling

## Note on ES Modules and Jest

This project uses ES modules (import/export) which requires:
- Node's experimental VM modules flag
- Special configuration in jest.config.json
- Careful approach to mocking (traditional jest.mock can be problematic with ES modules)
