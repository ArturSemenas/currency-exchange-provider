# Verify Docker Desktop downgrade
Write-Host "Checking Docker version..." -ForegroundColor Cyan

docker version

Write-Host "`n`nTesting TestContainers..." -ForegroundColor Yellow
Set-Location "C:\Work\Study\AI Copilot\Cur_ex_app"
mvn test -Dtest=MinimalTestContainersTest

Write-Host "`n`nIf test passes, TestContainers is working! ✅" -ForegroundColor Green
