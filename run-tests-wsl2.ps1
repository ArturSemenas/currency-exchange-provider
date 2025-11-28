# PowerShell script to run Maven tests using WSL2 Docker

Write-Host "WSL2 Docker TestContainers Setup" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Start Docker
Write-Host "Starting Docker in WSL2..." -ForegroundColor Yellow
wsl -u root service docker start
Start-Sleep -Seconds 2

# Get WSL2 IP
$wsl2Ip = (wsl hostname -I).Split(" ")[0].Trim()
Write-Host "WSL2 IP: $wsl2Ip" -ForegroundColor Green

# Set environment
$env:DOCKER_HOST = "tcp://${wsl2Ip}:2375"
Write-Host "DOCKER_HOST: $env:DOCKER_HOST" -ForegroundColor Green
Write-Host ""

# Test connection
Write-Host "Testing Docker connection..." -ForegroundColor Yellow
docker ps | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Docker connection successful!" -ForegroundColor Green
    Write-Host ""
    
    # Update config file
    $config = "docker.host=tcp://${wsl2Ip}:2375`ntestcontainers.reuse.enable=true`ntestcontainers.ryuk.disabled=false"
    $config | Out-File -FilePath ".testcontainers.properties" -Encoding ASCII
    Write-Host "TestContainers config updated" -ForegroundColor Green
    Write-Host ""
    
    # Run tests
    Write-Host "Running Maven tests..." -ForegroundColor Yellow
    Write-Host "================================" -ForegroundColor Cyan
    mvn clean test
} else {
    Write-Host "Docker connection failed!" -ForegroundColor Red
    Write-Host "Run: wsl ./setup-wsl2-docker.sh" -ForegroundColor Yellow
    exit 1
}
