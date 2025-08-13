

# Step 1: Build the images
Write-Host "Building Docker images..." -ForegroundColor Cyan
docker-compose build


# Step 2: Start the services
Write-Host "Starting services..." -ForegroundColor Cyan
docker-compose up -d

# Step 3: Wait a moment for services to fully initialize
Write-Host "Waiting for services to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Step 4: Check if services are running correctly
$backendRunning = docker ps --filter "name=agrolink--f-backend" --format "{{.Status}}" | Select-String "Up"
$mobileRunning = docker ps --filter "name=agrolink--f-mobile" --format "{{.Status}}" | Select-String "Up"

if ($backendRunning -and $mobileRunning) {
    Write-Host "All services are running correctly!" -ForegroundColor Green
    
    # Step 5: Push the images to Docker Hub
    Write-Host "Pushing images to Docker Hub..." -ForegroundColor Cyan
    
    # Check if user is logged in to Docker Hub
    $loginStatus = docker info 2>&1 | Select-String "Username:"
    if (-not $loginStatus) {
        Write-Host "You need to log in to Docker Hub first." -ForegroundColor Yellow
        docker login
    }
    
    docker push abidhasan83/agrolink-backend:latest
    docker push abidhasan83/agrolink-mobile:latest
    
    Write-Host "Images successfully pushed to Docker Hub!" -ForegroundColor Green
} else {
    Write-Host "Some services are not running correctly. Images will not be pushed." -ForegroundColor Red
    if (-not $backendRunning) {
        Write-Host "Backend service is not running properly. Check logs with: docker-compose logs backend" -ForegroundColor Red
    }
    if (-not $mobileRunning) {
        Write-Host "Mobile service is not running properly. Check logs with: docker-compose logs mobile" -ForegroundColor Red
    }
}

# Ask if user wants to stop the services
$stopServices = Read-Host "Do you want to stop the services? (y/n)"
if ($stopServices -eq "y" -or $stopServices -eq "Y") {
    Write-Host "Stopping services..." -ForegroundColor Cyan
    docker-compose down
    Write-Host "Services stopped." -ForegroundColor Green
} else {
    Write-Host "Services are still running. You can stop them later with 'docker-compose down'" -ForegroundColor Yellow
}
