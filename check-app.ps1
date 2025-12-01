# Quick diagnostic script for currency exchange app on AWS
$instanceIP = "16.171.110.181"
$sshKey = "$env:USERPROFILE\.ssh\aws-currency-exchange"

Write-Host "`n=== Checking Currency Exchange Application ===" -ForegroundColor Cyan

# Test SSH port
Write-Host "`n1. Testing SSH connectivity..." -ForegroundColor Yellow
$sshTest = Test-NetConnection -ComputerName $instanceIP -Port 22 -WarningAction SilentlyContinue
if ($sshTest.TcpTestSucceeded) {
    Write-Host "   SSH Port 22: OPEN" -ForegroundColor Green
} else {
    Write-Host "   SSH Port 22: BLOCKED" -ForegroundColor Red
}

# Check Docker containers
Write-Host "`n2. Checking Docker containers..." -ForegroundColor Yellow
$containers = ssh -i $sshKey -o ConnectTimeout=10 -o StrictHostKeyChecking=no ec2-user@$instanceIP "docker ps --format 'table {{.Names}}\t{{.Status}}'" 2>&1
Write-Host $containers

# Check application health
Write-Host "`n3. Checking application health..." -ForegroundColor Yellow
$health = ssh -i $sshKey -o ConnectTimeout=10 -o StrictHostKeyChecking=no ec2-user@$instanceIP "curl -s localhost:8080/actuator/health" 2>&1
Write-Host "   Response: $health"

# Check logs
Write-Host "`n4. Recent application logs..." -ForegroundColor Yellow
$logs = ssh -i $sshKey -o ConnectTimeout=10 -o StrictHostKeyChecking=no ec2-user@$instanceIP "docker logs currency-exchange-app --tail 15" 2>&1
Write-Host $logs

Write-Host "`n=== URLs (test from non-proxy network) ===" -ForegroundColor Cyan
Write-Host "Swagger: http://$instanceIP:8080/swagger-ui.html"
Write-Host "Health:  http://$instanceIP:8080/actuator/health"
