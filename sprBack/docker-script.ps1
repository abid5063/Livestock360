# Livestock360 Spring Boot Backend Docker Management Script (PowerShell)

param(
    [Parameter(Mandatory=$true, Position=0)]
    [ValidateSet("build", "run", "compose", "push", "stop", "logs", "cleanup", "all", "help")]
    [string]$Action,
    
    [Parameter(Position=1)]
    [string]$DockerHubUsername
)

# Configuration
$ImageName = "livestock360-spring-backend"
$Tag = "latest"
$ContainerName = "livestock360-backend"

# Colors for output
function Write-Success { param($Message) Write-Host "✅ $Message" -ForegroundColor Green }
function Write-Error { param($Message) Write-Host "❌ $Message" -ForegroundColor Red }
function Write-Warning { param($Message) Write-Host "⚠️  $Message" -ForegroundColor Yellow }
function Write-Info { param($Message) Write-Host "ℹ️  $Message" -ForegroundColor Blue }

Write-Host "🐄 Livestock360 Spring Boot Backend Docker Manager" -ForegroundColor Blue
Write-Host "==================================================" -ForegroundColor Blue

# Function to build Docker image
function Build-Image {
    Write-Info "Building Docker image..."
    
    $buildResult = docker build -t "$ImageName`:$Tag" .
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Docker image built successfully!"
        docker images | Where-Object { $_ -match $ImageName }
    } else {
        Write-Error "Failed to build Docker image!"
        exit 1
    }
}

# Function to run container locally
function Run-Local {
    Write-Info "Running container locally..."
    
    # Stop existing container if running
    $existingContainer = docker ps -q -f "name=$ContainerName"
    if ($existingContainer) {
        Write-Warning "Stopping existing container..."
        docker stop $ContainerName
        docker rm $ContainerName
    }
    
    # Run new container
    $runResult = docker run -d `
        --name $ContainerName `
        -p 5000:5000 `
        -e SPRING_PROFILES_ACTIVE=docker `
        "$ImageName`:$Tag"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Container started successfully!"
        Write-Info "Backend is running at: http://localhost:5000"
        Write-Info "Health check: http://localhost:5000/actuator/health"
        
        # Show logs
        Write-Host ""
        Write-Info "Container logs (press Ctrl+C to stop viewing):"
        Start-Sleep 2
        docker logs -f $ContainerName
    } else {
        Write-Error "Failed to start container!"
        exit 1
    }
}

# Function to run with Docker Compose
function Run-Compose {
    Write-Info "Running with Docker Compose..."
    
    $composeResult = docker-compose up -d
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Services started with Docker Compose!"
        Write-Info "Backend is running at: http://localhost:5000"
        
        # Show logs
        Write-Host ""
        Write-Info "Service logs (press Ctrl+C to stop viewing):"
        Start-Sleep 2
        docker-compose logs -f
    } else {
        Write-Error "Failed to start with Docker Compose!"
        exit 1
    }
}

# Function to push to Docker Hub
function Push-ToHub {
    param($Username)
    
    if (-not $Username) {
        Write-Error "Please provide your Docker Hub username!"
        exit 1
    }
    
    $DockerHubRepo = "$Username/$ImageName"
    $TimestampTag = Get-Date -Format "yyyyMMdd-HHmmss"
    
    Write-Info "Tagging image for Docker Hub..."
    docker tag "$ImageName`:$Tag" "$DockerHubRepo`:$Tag"
    docker tag "$ImageName`:$Tag" "$DockerHubRepo`:$TimestampTag"
    
    Write-Info "Pushing to Docker Hub..."
    docker push "$DockerHubRepo`:$Tag"
    docker push "$DockerHubRepo`:$TimestampTag"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Successfully pushed to Docker Hub!"
        Write-Info "Image available at: https://hub.docker.com/r/$DockerHubRepo"
    } else {
        Write-Error "Failed to push to Docker Hub!"
        Write-Warning "Make sure you're logged in: docker login"
        exit 1
    }
}

# Function to stop containers
function Stop-Containers {
    Write-Info "Stopping containers..."
    
    $existingContainer = docker ps -q -f "name=$ContainerName"
    if ($existingContainer) {
        docker stop $ContainerName
        docker rm $ContainerName
        Write-Success "Stopped standalone container"
    }
    
    $composeContainers = docker-compose ps -q
    if ($composeContainers) {
        docker-compose down
        Write-Success "Stopped Docker Compose services"
    }
}

# Function to view logs
function View-Logs {
    $existingContainer = docker ps -q -f "name=$ContainerName"
    $composeContainers = docker-compose ps -q
    
    if ($existingContainer) {
        Write-Info "Viewing container logs..."
        docker logs -f $ContainerName
    } elseif ($composeContainers) {
        Write-Info "Viewing Docker Compose logs..."
        docker-compose logs -f
    } else {
        Write-Warning "No running containers found!"
    }
}

# Function to clean up
function Cleanup {
    Write-Info "Cleaning up Docker resources..."
    
    # Stop containers
    Stop-Containers
    
    # Remove images
    $existingImage = docker images -q $ImageName
    if ($existingImage) {
        docker rmi "$ImageName`:$Tag"
        Write-Success "Removed local image"
    }
    
    # Clean up unused resources
    docker system prune -f
    Write-Success "Cleaned up unused Docker resources"
}

# Function to show help
function Show-Help {
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor Blue
    Write-Host "  .\docker-script.ps1 build                     - Build Docker image"
    Write-Host "  .\docker-script.ps1 run                       - Build and run container locally"
    Write-Host "  .\docker-script.ps1 compose                   - Run with Docker Compose"
    Write-Host "  .\docker-script.ps1 push <dockerhub-username> - Build and push to Docker Hub"
    Write-Host "  .\docker-script.ps1 all <dockerhub-username>  - Build, run, and push"
    Write-Host "  .\docker-script.ps1 stop                      - Stop running containers"
    Write-Host "  .\docker-script.ps1 logs                      - View container logs"
    Write-Host "  .\docker-script.ps1 cleanup                   - Clean up Docker resources"
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  .\docker-script.ps1 build"
    Write-Host "  .\docker-script.ps1 run"
    Write-Host "  .\docker-script.ps1 push abid5063"
    Write-Host "  .\docker-script.ps1 all abid5063"
}

# Main execution logic
switch ($Action) {
    "build" {
        Build-Image
    }
    "run" {
        Build-Image
        Run-Local
    }
    "compose" {
        Run-Compose
    }
    "push" {
        if (-not $DockerHubUsername) {
            Write-Error "Please provide your Docker Hub username: .\docker-script.ps1 push <username>"
            exit 1
        }
        Build-Image
        Push-ToHub $DockerHubUsername
    }
    "stop" {
        Stop-Containers
    }
    "logs" {
        View-Logs
    }
    "cleanup" {
        Cleanup
    }
    "all" {
        if (-not $DockerHubUsername) {
            Write-Error "Please provide your Docker Hub username: .\docker-script.ps1 all <username>"
            exit 1
        }
        Build-Image
        
        # Start container in background
        Write-Info "Starting container locally..."
        $job = Start-Job -ScriptBlock {
            param($ImageName, $Tag, $ContainerName)
            docker run -d --name $ContainerName -p 5000:5000 -e SPRING_PROFILES_ACTIVE=docker "$ImageName`:$Tag"
        } -ArgumentList $ImageName, $Tag, $ContainerName
        
        Start-Sleep 10
        Push-ToHub $DockerHubUsername
    }
    "help" {
        Show-Help
    }
    default {
        Show-Help
    }
}