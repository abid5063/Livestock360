# Livestock360 Spring Boot Backend - Docker Setup

This directory contains the Docker configuration for the Livestock360 Spring Boot backend.

## 📋 Prerequisites

- Docker Desktop installed and running
- Docker Hub account (for pushing images)
- PowerShell (Windows) or Bash (Linux/macOS)

## 🚀 Quick Start

### Option 1: Using PowerShell Script (Windows)

```powershell
# Build and run locally
.\docker-script.ps1 run

# Build and push to Docker Hub
.\docker-script.ps1 push your-dockerhub-username

# Build, run, and push (all in one)
.\docker-script.ps1 all your-dockerhub-username
```

### Option 2: Using Bash Script (Linux/macOS)

```bash
# Make script executable
chmod +x docker-script.sh

# Build and run locally
./docker-script.sh run

# Build and push to Docker Hub
./docker-script.sh push your-dockerhub-username

# Build, run, and push (all in one)
./docker-script.sh all your-dockerhub-username
```

### Option 3: Manual Docker Commands

```bash
# Build the image
docker build -t livestock360-spring-backend:latest .

# Run locally
docker run -d --name livestock360-backend -p 5000:5000 livestock360-spring-backend:latest

# Tag for Docker Hub
docker tag livestock360-spring-backend:latest your-username/livestock360-spring-backend:latest

# Push to Docker Hub
docker push your-username/livestock360-spring-backend:latest
```

### Option 4: Using Docker Compose

```bash
# Start services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🔧 Configuration

### Environment Variables

The Docker container supports the following environment variables:

- `SPRING_PROFILES_ACTIVE`: Spring profile (default: `docker`)
- `SERVER_PORT`: Server port (default: `5000`)
- `SPRING_DATA_MONGODB_URI`: MongoDB connection string
- `JWT_SECRET`: JWT secret key
- `JWT_EXPIRATION`: JWT expiration time in milliseconds

### Ports

- **5000**: Main application port
- Health check available at: `http://localhost:5000/actuator/health`

## 📁 Files

- `Dockerfile`: Multi-stage Docker build configuration
- `docker-compose.yml`: Docker Compose configuration
- `.dockerignore`: Files to exclude from Docker build context
- `docker-script.ps1`: PowerShell automation script
- `docker-script.sh`: Bash automation script

## 🔍 Monitoring

### Health Check

The application includes a health check endpoint:

```bash
curl http://localhost:5000/actuator/health
```

### View Logs

```bash
# Container logs
docker logs -f livestock360-backend

# Or using scripts
.\docker-script.ps1 logs
./docker-script.sh logs
```

### Container Status

```bash
# Check running containers
docker ps

# Check all containers
docker ps -a
```

## 🛠️ Development

### Local Development with Docker

1. Make changes to your Spring Boot code
2. Rebuild the Docker image:
   ```bash
   .\docker-script.ps1 build
   ```
3. Run the updated container:
   ```bash
   .\docker-script.ps1 run
   ```

### Database Connection

The application is configured to connect to your existing MongoDB Atlas cluster. The connection string is defined in the Docker Compose file.

## 🧹 Cleanup

To clean up Docker resources:

```bash
# Using scripts
.\docker-script.ps1 cleanup
./docker-script.sh cleanup

# Manual cleanup
docker stop livestock360-backend
docker rm livestock360-backend
docker rmi livestock360-spring-backend:latest
docker system prune -f
```

## 🐛 Troubleshooting

### Common Issues

1. **Port 5000 already in use**
   ```bash
   # Find what's using port 5000
   netstat -ano | findstr :5000
   
   # Stop the process or use a different port
   docker run -d --name livestock360-backend -p 5001:5000 livestock360-spring-backend:latest
   ```

2. **Permission denied on scripts**
   ```bash
   # Linux/macOS
   chmod +x docker-script.sh
   
   # Windows PowerShell execution policy
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

3. **Docker Hub push fails**
   ```bash
   # Login to Docker Hub
   docker login
   
   # Verify your username and try again
   ```

### Logs and Debugging

```bash
# View application logs
docker logs livestock360-backend

# Access container shell
docker exec -it livestock360-backend sh

# View Docker build logs
docker build -t livestock360-spring-backend:latest . --no-cache
```

## 📚 Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)