#!/bin/bash

# Livestock360 Spring Boot Backend Docker Management Script

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="livestock360-spring-backend"
TAG="latest"
DOCKER_HUB_USERNAME="" # Replace with your Docker Hub username
DOCKER_HUB_REPO="$DOCKER_HUB_USERNAME/$IMAGE_NAME"

echo -e "${BLUE}🐄 Livestock360 Spring Boot Backend Docker Manager${NC}"
echo "=================================================="

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Function to build Docker image
build_image() {
    print_info "Building Docker image..."
    
    if docker build -t $IMAGE_NAME:$TAG .; then
        print_status "Docker image built successfully!"
        docker images | grep $IMAGE_NAME
    else
        print_error "Failed to build Docker image!"
        exit 1
    fi
}

# Function to run container locally
run_local() {
    print_info "Running container locally..."
    
    # Stop existing container if running
    if docker ps -q -f name=livestock360-backend | grep -q .; then
        print_warning "Stopping existing container..."
        docker stop livestock360-backend
        docker rm livestock360-backend
    fi
    
    # Run new container
    if docker run -d \
        --name livestock360-backend \
        -p 5000:5000 \
        -e SPRING_PROFILES_ACTIVE=docker \
        $IMAGE_NAME:$TAG; then
        print_status "Container started successfully!"
        print_info "Backend is running at: http://localhost:5000"
        print_info "Health check: http://localhost:5000/actuator/health"
        
        # Show logs
        echo ""
        print_info "Container logs (press Ctrl+C to stop viewing):"
        docker logs -f livestock360-backend
    else
        print_error "Failed to start container!"
        exit 1
    fi
}

# Function to run with Docker Compose
run_compose() {
    print_info "Running with Docker Compose..."
    
    if docker-compose up -d; then
        print_status "Services started with Docker Compose!"
        print_info "Backend is running at: http://localhost:5000"
        
        # Show logs
        echo ""
        print_info "Service logs (press Ctrl+C to stop viewing):"
        docker-compose logs -f
    else
        print_error "Failed to start with Docker Compose!"
        exit 1
    fi
}

# Function to push to Docker Hub
push_to_hub() {
    if [ -z "$DOCKER_HUB_USERNAME" ]; then
        print_error "Please set DOCKER_HUB_USERNAME in the script!"
        exit 1
    fi
    
    print_info "Tagging image for Docker Hub..."
    docker tag $IMAGE_NAME:$TAG $DOCKER_HUB_REPO:$TAG
    docker tag $IMAGE_NAME:$TAG $DOCKER_HUB_REPO:$(date +%Y%m%d-%H%M%S)
    
    print_info "Pushing to Docker Hub..."
    if docker push $DOCKER_HUB_REPO:$TAG && docker push $DOCKER_HUB_REPO:$(date +%Y%m%d-%H%M%S); then
        print_status "Successfully pushed to Docker Hub!"
        print_info "Image available at: https://hub.docker.com/r/$DOCKER_HUB_REPO"
    else
        print_error "Failed to push to Docker Hub!"
        print_warning "Make sure you're logged in: docker login"
        exit 1
    fi
}

# Function to stop containers
stop_containers() {
    print_info "Stopping containers..."
    
    if docker ps -q -f name=livestock360-backend | grep -q .; then
        docker stop livestock360-backend
        docker rm livestock360-backend
        print_status "Stopped standalone container"
    fi
    
    if docker-compose ps -q | grep -q .; then
        docker-compose down
        print_status "Stopped Docker Compose services"
    fi
}

# Function to view logs
view_logs() {
    if docker ps -q -f name=livestock360-backend | grep -q .; then
        print_info "Viewing container logs..."
        docker logs -f livestock360-backend
    elif docker-compose ps -q | grep -q .; then
        print_info "Viewing Docker Compose logs..."
        docker-compose logs -f
    else
        print_warning "No running containers found!"
    fi
}

# Function to clean up
cleanup() {
    print_info "Cleaning up Docker resources..."
    
    # Stop containers
    stop_containers
    
    # Remove images
    if docker images -q $IMAGE_NAME | grep -q .; then
        docker rmi $IMAGE_NAME:$TAG
        print_status "Removed local image"
    fi
    
    # Clean up unused resources
    docker system prune -f
    print_status "Cleaned up unused Docker resources"
}

# Main menu
case "$1" in
    "build")
        build_image
        ;;
    "run")
        build_image
        run_local
        ;;
    "compose")
        run_compose
        ;;
    "push")
        if [ -z "$2" ]; then
            print_error "Please provide your Docker Hub username: ./docker-script.sh push <username>"
            exit 1
        fi
        DOCKER_HUB_USERNAME="$2"
        DOCKER_HUB_REPO="$DOCKER_HUB_USERNAME/$IMAGE_NAME"
        build_image
        push_to_hub
        ;;
    "stop")
        stop_containers
        ;;
    "logs")
        view_logs
        ;;
    "cleanup")
        cleanup
        ;;
    "all")
        if [ -z "$2" ]; then
            print_error "Please provide your Docker Hub username: ./docker-script.sh all <username>"
            exit 1
        fi
        DOCKER_HUB_USERNAME="$2"
        DOCKER_HUB_REPO="$DOCKER_HUB_USERNAME/$IMAGE_NAME"
        build_image
        run_local &
        sleep 10
        push_to_hub
        ;;
    *)
        echo -e "${BLUE}Usage:${NC}"
        echo "  $0 build                     - Build Docker image"
        echo "  $0 run                       - Build and run container locally"
        echo "  $0 compose                   - Run with Docker Compose"
        echo "  $0 push <dockerhub-username> - Build and push to Docker Hub"
        echo "  $0 all <dockerhub-username>  - Build, run, and push"
        echo "  $0 stop                      - Stop running containers"
        echo "  $0 logs                      - View container logs"
        echo "  $0 cleanup                   - Clean up Docker resources"
        echo ""
        echo -e "${YELLOW}Examples:${NC}"
        echo "  $0 build"
        echo "  $0 run"
        echo "  $0 push abid5063"
        echo "  $0 all abid5063"
        ;;
esac