# Quick Start: Run Tests with WSL2 Docker

## 🚀 **Easiest Method - One Command**

```powershell
.\run-tests-wsl2.ps1
```

This script automatically:
1. Starts Docker in WSL2
2. Detects WSL2 IP address
3. Configures TestContainers
4. Runs all Maven tests

## 📋 One-Time Setup (5 minutes)

### Step 1: Setup Docker in WSL2
```powershell
wsl chmod +x setup-wsl2-docker.sh
wsl ./setup-wsl2-docker.sh
```

**That's it!** Docker is now configured to accept connections from Windows.

### Step 2 (Optional): Verify Setup
```powershell
# Get WSL2 IP
wsl hostname -I

# Test Docker (replace IP with your WSL2 IP)
$env:DOCKER_HOST="tcp://172.17.86.168:2375"
docker ps
```

## 🎯 Running Tests

### Method 1: Automated Script (Recommended)
```powershell
.\run-tests-wsl2.ps1
```

### Method 2: Manual Commands
```powershell
# Start Docker
wsl -u root service docker start

# Get WSL2 IP and set Docker host
$wsl2Ip = (wsl hostname -I).Split(" ")[0]
$env:DOCKER_HOST="tcp://${wsl2Ip}:2375"

# Run tests
mvn clean test
```

### Method 3: Run Specific Tests
```powershell
# Get WSL2 IP
$wsl2Ip = (wsl hostname -I).Split(" ")[0]
$env:DOCKER_HOST="tcp://${wsl2Ip}:2375"

# Integration tests only
mvn test -Dtest=*IntegrationTest

# Unit tests only (no Docker needed)
mvn test -Dtest=!*IntegrationTest

# Specific test
mvn test -Dtest=CurrencyFlowIntegrationTest
```

## 🔧 Prerequisites

**Windows Side:**
- ✅ Java 21 installed
- ✅ Maven installed  
- ✅ WSL2 installed (`wsl --install`)

**WSL2 Side:**
- ✅ Docker installed (use `setup-wsl2-docker.sh`)
- ❌ No Java needed
- ❌ No Maven needed

## 🐛 Troubleshooting

### Docker not starting
```powershell
wsl -u root service docker status
wsl -u root service docker restart
```

### Can't connect from Windows
```powershell
# Verify Docker is listening on TCP
wsl docker info | Select-String -Pattern "tcp"

# Re-run setup script
wsl ./setup-wsl2-docker.sh
```

### Tests still fail
```powershell
# Check .testcontainers.properties file
Get-Content .testcontainers.properties

# Manually set Docker host
$wsl2Ip = (wsl hostname -I).Split(" ")[0]
$env:DOCKER_HOST="tcp://${wsl2Ip}:2375"

# Test Docker connection
docker ps
```

### WSL2 IP changed
WSL2 IP can change after restart. Always use the script or get fresh IP:
```powershell
wsl hostname -I
```

## 📊 Test Summary

- **Total Tests**: 359
  - Unit Tests: 336 (no Docker needed)
  - Integration Tests: 23 (require Docker)

**Integration Test Classes:**
- `CurrencyFlowIntegrationTest` - 6 tests
- `ExternalProviderWireMockTest` - 9 tests
- Other integration tests in `/integration` package

## ✅ Quick Verification

After setup, run a quick test:

```powershell
# Start Docker
wsl -u root service docker start

# Run integration test
$wsl2Ip = (wsl hostname -I).Split(" ")[0]
$env:DOCKER_HOST="tcp://${wsl2Ip}:2375"
mvn test -Dtest=CurrencyFlowIntegrationTest
```

## 💡 Tips

1. **Keep Docker Running**: Add to WSL2 startup
2. **Use the Script**: `run-tests-wsl2.ps1` handles everything
3. **WSL2 IP Detection**: The script auto-detects IP on each run
4. **Fast Iteration**: Container reuse enabled for faster tests

## 📁 Generated Files

- `.testcontainers.properties` - Auto-generated with current WSL2 IP
- `setup-wsl2-docker.sh` - One-time Docker setup script  
- `run-tests-wsl2.ps1` - Automated test execution script

## 🎉 Success Indicators

You'll know it's working when you see:
```
[INFO] Tests run: 359, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

And coverage report at: `target/site/jacoco/index.html`

### Option 1: Manual Commands (Most Reliable)

```powershell
# 1. Start Docker
wsl -u root service docker start

# 2. Verify Docker is running
wsl docker ps

# 3. Run tests
wsl bash -c "cd '/mnt/c/Work/Study/AI Copilot/Cur_ex_app'; export DOCKER_HOST=unix:///var/run/docker.sock; mvn clean test"
```

### Option 2: Use the PowerShell Script

```powershell
.\run-tests-wsl2.ps1
```

### Option 3: Work Directly in WSL2

```bash
# Open WSL2 terminal
wsl

# Navigate to project
cd "/mnt/c/Work/Study/AI Copilot/Cur_ex_app"

# Start Docker
sudo service docker start

# Run tests
export DOCKER_HOST=unix:///var/run/docker.sock
mvn clean test
```

## 🐛 Common Issues

### Docker daemon not running
```powershell
# Start it manually
wsl -u root service docker start

# Check status
wsl -u root service docker status
```

### Permission denied
```bash
# Inside WSL2
sudo usermod -aG docker $USER
newgrp docker
```

### Can't find docker.sock
```bash
# Inside WSL2
ls -la /var/run/docker.sock
sudo chmod 666 /var/run/docker.sock
```

## 📊 Test Summary

- **Total Tests**: 359
  - Unit Tests: 336
  - Integration Tests: 23 (require Docker)

## ✅ Verification

After running tests, check results:

```powershell
# View test summary
Get-Content target\surefire-reports\*.txt | Select-String "Tests run"

# View coverage
Invoke-Item target\site\jacoco\index.html
```
