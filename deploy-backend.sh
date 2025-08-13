#!/bin/bash

# Simple deployment script for AgroLink Backend Only
# Run this on your Azure VM to manually deploy the latest backend

set -e

echo "🚀 Starting AgroLink Backend deployment..."

# Variables
PROJECT_DIR="$HOME/agrolink-deployment"
DOCKER_IMAGE="abidhasan83/agrolink-backend:latest"

# Create project directory
mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"

# Stop existing containers
echo "Stopping existing containers..."
sudo docker-compose down --remove-orphans 2>/dev/null || true

# Pull latest backend image
echo "Pulling latest backend image..."
sudo docker pull "$DOCKER_IMAGE"

# Create docker-compose.yml for backend only
echo "Creating docker-compose.yml..."
cat > docker-compose.yml << 'EOF'
services:
  backend:
    image: abidhasan83/agrolink-backend:latest
    ports:
      - "3000:3000"
      - "80:3000"
    environment:
      - MONGO_URI=${MONGO_URI}
      - PORT=3000
      - JWT_SECRET=${JWT_SECRET}
      - CLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME}
      - CLOUDINARY_API_KEY=${CLOUDINARY_API_KEY}
      - CLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET}
      - NODE_ENV=production
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health", "||", "exit", "1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
EOF

# Create .env file with environment variables
echo "Creating .env file..."
cat > .env << EOF
MONGO_URI=${MONGO_URI:-mongodb+srv://abidhasan7116:YpLEaZIAY0AmR4j0@cluster0.lx8cqwh.mongodb.net/AGROLINK?retryWrites=true&w=majority&appName=Cluster0}
JWT_SECRET=${JWT_SECRET:-mysecret}
CLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME:-dcvkttktl}
CLOUDINARY_API_KEY=${CLOUDINARY_API_KEY:-931199518597945}
CLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET:-efPviLx3FH1b_NsMxPamZKS3_jo}
NODE_ENV=production
EOF

# Remove old images to save space
echo "Cleaning up old images..."
sudo docker image prune -f

# Start backend container
echo "Starting backend container..."
sudo docker-compose up -d

# Wait for container to be ready
echo "Waiting for backend to start..."
sleep 30

# Check status
echo "Checking container status..."
sudo docker-compose ps

# Show logs
echo "Recent backend logs:"
sudo docker-compose logs --tail=20 backend

# Test the API
echo "Testing backend API..."
if curl -s -f http://localhost:3000/api/health; then
    echo "✅ Backend health check passed!"
else
    echo "⚠️  Backend health check failed, but container might still be starting..."
fi

echo "✅ Deployment completed!"
echo "Backend API available at:"
echo "  - http://localhost:3000"
echo "  - http://$(curl -s ifconfig.me):3000"
echo "  - http://$(curl -s ifconfig.me):80"

echo ""
echo "📋 Useful commands:"
echo "  View logs: sudo docker-compose logs -f backend"
echo "  Restart:   sudo docker-compose restart backend"
echo "  Stop:      sudo docker-compose down"
echo "  Update:    ./deploy.sh"
