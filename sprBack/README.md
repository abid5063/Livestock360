# Livestock360 Spring Boot Backend

This is a Spring Boot backend that provides identical API functionality to the original Simple Java backend. It includes all authentication endpoints for both farmers and vets.

## Features

- JWT Authentication with refresh token support
- User registration and login for farmers and vets
- User profile management (edit/delete)
- Farmer search functionality
- MongoDB integration
- CORS support for frontend integration
- Password hashing with SHA-256 + salt

## API Endpoints

### Authentication Endpoints

All endpoints are prefixed with `/api/auth`:

- `POST /register` - Register a new user (farmer or vet)
- `POST /login` - Login user
- `POST /refresh` - Refresh JWT token
- `PUT /edit/{id}` - Edit user profile
- `DELETE /delete/{id}` - Delete user account
- `GET /farmers/search?query=` - Search farmers
- `GET /farmers` - Get all farmers

### Request Parameters

For all endpoints that support both farmers and vets, add `?userType=farmer` or `?userType=vet` to the URL. If not specified, defaults to `farmer`.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- MongoDB Atlas connection (configured in application.properties)

## Setup and Running

1. **Navigate to the project directory:**
   ```bash
   cd sprBack
   ```

2. **Install dependencies:**
   ```bash
   mvn clean install
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

   Or build and run the JAR:
   ```bash
   mvn clean package
   java -jar target/spring-backend-1.0.0.jar
   ```

4. **The server will start on port 5000** (same as the original backend)

## Configuration

The application is configured via `src/main/resources/application.properties`:

- **Server Port:** 5000
- **MongoDB URI:** MongoDB Atlas connection string
- **JWT Secret:** Secret key for JWT signing
- **JWT Expiration:** 24 hours (86400000 ms)

## Database Schema

### Farmers Collection
```json
{
  "_id": "ObjectId",
  "name": "string",
  "email": "string",
  "password": "string (hashed)",
  "salt": "string",
  "phone": "string",
  "location": "string",
  "address": "string",
  "profilePicture": "string",
  "dateJoined": "string"
}
```

### Vets Collection
```json
{
  "_id": "ObjectId",
  "name": "string",
  "email": "string",
  "password": "string (hashed)",
  "salt": "string",
  "phone": "string",
  "location": "string",
  "address": "string",
  "profilePicture": "string",
  "dateJoined": "string",
  "specialization": "string",
  "latitude": "number",
  "longitude": "number"
}
```

## API Examples

### Register a Farmer
```bash
POST http://localhost:5000/api/auth/register?userType=farmer
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "phone": "+1234567890",
  "location": "New York",
  "address": "123 Farm Road"
}
```

### Login
```bash
POST http://localhost:5000/api/auth/login?userType=farmer
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

### Refresh Token
```bash
POST http://localhost:5000/api/auth/refresh
Content-Type: application/json

{
  "token": "your_jwt_token_here"
}
```

## Security Features

- **Password Hashing:** SHA-256 with random salt
- **JWT Tokens:** HMAC SHA256 signed tokens
- **Token Refresh:** Automatic refresh within 1 hour of expiration
- **CORS:** Configured for cross-origin requests
- **Input Validation:** Email format and required field validation

## Differences from Original Backend

This Spring Boot implementation provides:
- Better structured code with service layers
- Enhanced error handling and logging
- Spring Boot's built-in features (auto-configuration, embedded server)
- Same API endpoints and functionality as the original Java backend

## Troubleshooting

1. **Port already in use:** Make sure the original Java backend is not running on port 5000
2. **MongoDB connection issues:** Verify the connection string in application.properties
3. **JWT issues:** Check the JWT secret key configuration

## Development

The project structure follows Spring Boot conventions:
- `src/main/java/com/livestock360/springbackend/` - Main source code
  - `controller/` - REST controllers
  - `service/` - Business logic
  - `model/` - Data models
  - `dto/` - Data transfer objects
  - `config/` - Configuration classes
  - `utils/` - Utility classes
- `src/main/resources/` - Configuration files
