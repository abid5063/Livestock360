#!/bin/bash
# Manual Deployment Script for AgroLink
# Usage: ./deploy.sh

set -e  # Exit on any error

echo "🚀 AgroLink Manual Deployment Script"
echo "====================================="

# Configuration
DOCKER_HUB_USERNAME="abidhasan83"
BACKEND_IMAGE="$DOCKER_HUB_USERNAME/agrolink-backend:latest"
MOBILE_IMAGE="$DOCKER_HUB_USERNAME/agrolink-mobile:latest"
PROJECT_DIR="$HOME/agrolink-deployment"

# Create project directory
echo "📁 Creating project directory..."
mkdir -p $PROJECT_DIR
cd $PROJECT_DIR

# Create docker-compose.yml
echo "📝 Creating docker-compose.yml..."
cat > docker-compose.yml << 'EOF'
services:
  backend:
    image: abidhasan83/agrolink-backend:latest
    ports:
      - "3000:3000"
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
      test: ["CMD", "curl", "-f", "http://localhost:3000/health", "||", "exit", "1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  mobile:
    image: abidhasan83/agrolink-mobile:latest
    ports:
      - "8081:8081"
      - "19000:19000"
      - "19001:19001" 
      - "19002:19002"
    environment:
      - EXPO_DEVTOOLS_LISTEN_ADDRESS=0.0.0.0
      - REACT_NATIVE_PACKAGER_HOSTNAME=0.0.0.0
    depends_on:
      backend:
        condition: service_healthy
    restart: unless-stopped
EOF

# Create .env file template
echo "📝 Creating .env template..."
cat > .env.example << 'EOF'
# Copy this to .env and fill in your actual values
MONGO_URI=mongodb+srv://username:password@cluster.mongodb.net/AGROLINK?retryWrites=true&w=majority
JWT_SECRET=your-super-secret-jwt-key
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
EOF

# Check if .env exists
if [ ! -f .env ]; then
    echo "⚠️  WARNING: .env file not found!"
    echo "Please create .env file with your environment variables."
    echo "Use .env.example as a template."
    echo ""
    echo "Example:"
    echo "cp .env.example .env"
    echo "nano .env  # Edit with your actual values"
    echo ""
    read -p "Do you want to continue without .env file? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "❌ Deployment cancelled. Please create .env file first."
        exit 1
    fi
fi

# Install Docker if not already installed
if ! command -v docker &> /dev/null; then
    echo "🐳 Installing Docker..."
    sudo apt-get update
    sudo apt-get install -y docker.io docker-compose
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "✅ Docker installed. You may need to log out and back in for group changes to take effect."
fi

# Stop existing containers
echo "🛑 Stopping existing containers..."
sudo docker-compose down --remove-orphans 2>/dev/null || true

# Pull latest images
echo "📥 Pulling latest Docker images..."
sudo docker pull $BACKEND_IMAGE
sudo docker pull $MOBILE_IMAGE

# Remove old images to save space
echo "🧹 Cleaning up old images..."
sudo docker image prune -f

# Start new containers
echo "🚀 Starting new containers..."
sudo docker-compose up -d

# Wait a moment for containers to start
echo "⏳ Waiting for containers to start..."
sleep 10

# Check container status
echo "📊 Checking container status..."
sudo docker-compose ps

# Show recent logs
echo "📋 Recent logs:"
echo "--- Backend Logs ---"
sudo docker-compose logs --tail=10 backend

echo "--- Mobile Logs ---"
sudo docker-compose logs --tail=10 mobile

# Get server IP
SERVER_IP=$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')

echo ""
echo "✅ Deployment completed successfully!"
echo "=====================================
echo "🌐 Backend API: http://$SERVER_IP:3000"
echo "📱 Mobile App: http://$SERVER_IP:8081"
echo ""
echo "📝 To check logs: sudo docker-compose logs -f"
echo "🛑 To stop: sudo docker-compose down"
echo "🔄 To restart: sudo docker-compose restart"
