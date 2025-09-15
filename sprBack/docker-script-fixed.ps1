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
function Write-Success { param($Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Error { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }
function Write-Warning { param($Message) Write-Host "[WARNING] $Message" -ForegroundColor Yellow }
function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Blue }

Write-Host "Livestock360 Spring Boot Backend Docker Manager" -ForegroundColor Blue
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

# Function to show help
function Show-Help {
    Write-Host ""
    Write-Host "Usage:" -ForegroundColor Blue
    Write-Host "  .\docker-script-fixed.ps1 build                     - Build Docker image"
    Write-Host "  .\docker-script-fixed.ps1 run                       - Build and run container locally"
    Write-Host "  .\docker-script-fixed.ps1 push <dockerhub-username> - Build and push to Docker Hub"
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host "  .\docker-script-fixed.ps1 build"
    Write-Host "  .\docker-script-fixed.ps1 run"
    Write-Host "  .\docker-script-fixed.ps1 push abid5063"
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
    "push" {
        if (-not $DockerHubUsername) {
            Write-Error "Please provide your Docker Hub username: .\docker-script-fixed.ps1 push <username>"
            exit 1
        }
        Build-Image
        Push-ToHub $DockerHubUsername
    }
    "help" {
        Show-Help
    }
    default {
        Show-Help
    }
}