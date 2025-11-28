# TestContainers with WSL2 Docker Setup Guide

This guide explains how to run TestContainers integration tests using Docker in WSL2 instead of Docker Desktop.

## Prerequisites

1. **WSL2 installed and configured**
   ```powershell
   wsl --install
   wsl --set-default-version 2
   ```

2. **Docker installed in WSL2**
   ```bash
   # Inside WSL2
   curl -fsSL https://get.docker.com -o get-docker.sh
   sudo sh get-docker.sh
   sudo usermod -aG docker $USER
   ```

3. **Java and Maven available in WSL2**
   ```bash
   # Inside WSL2
   sudo apt update
   sudo apt install openjdk-21-jdk maven -y
   ```

## Configuration Files

### 1. `.testcontainers.properties`
Located in project root, configures TestContainers to use WSL2 Docker:
```properties
docker.host=unix:///var/run/docker.sock
testcontainers.reuse.enable=true
testcontainers.ryuk.disabled=false
```

### 2. Environment Variables
Set these when running tests from Windows:
```powershell
$env:DOCKER_HOST = "unix:///var/run/docker.sock"
$env:TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE = "/var/run/docker.sock"
```

## Running Tests

### Option 1: Using PowerShell Script (Recommended)
```powershell
.\run-tests-wsl2.ps1
```

This script:
- Starts Docker daemon in WSL2
- Sets up environment variables
- Runs Maven tests with TestContainers

### Option 2: Using Bash Script in WSL2
```bash
# Make script executable
chmod +x run-tests-wsl2.sh

# Run tests
./run-tests-wsl2.sh
```

### Option 3: Manual Execution

**Step 1: Start Docker in WSL2**
```powershell
# From PowerShell/CMD
wsl -u root service docker start
```

**Step 2: Verify Docker is running**
```powershell
wsl docker ps
```

**Step 3: Run tests in WSL2**
```powershell
wsl bash -c "cd /mnt/c/Work/Study/'AI Copilot'/Cur_ex_app && mvn clean test"
```

### Option 4: Run Directly from WSL2 Terminal
```bash
# Inside WSL2
cd "/mnt/c/Work/Study/AI Copilot/Cur_ex_app"

# Start Docker if not running
sudo service docker start

# Run tests
mvn clean test
```

## Troubleshooting

### Docker daemon not starting
```bash
# Check Docker status
sudo service docker status

# Start Docker manually
sudo service docker start

# Check for errors
sudo journalctl -u docker.service --no-pager | tail -50
```

### Permission denied errors
```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Re-login or run:
newgrp docker
```

### TestContainers can't find Docker
```bash
# Verify Docker socket exists
ls -la /var/run/docker.sock

# Test Docker connection
docker info
```

### Port conflicts
```bash
# Check what's using PostgreSQL/Redis ports
sudo netstat -tulpn | grep -E '5432|6379'

# Stop conflicting services
docker ps
docker stop <container_id>
```

### WSL2 Docker socket issues
```powershell
# From PowerShell, expose Docker socket
wsl -u root chmod 666 /var/run/docker.sock
```

## Performance Tips

1. **Enable container reuse** (already configured in `.testcontainers.properties`)
   - Containers are reused between test runs
   - Faster test execution

2. **Keep Docker running**
   ```bash
   # Inside WSL2, add to ~/.bashrc
   if ! docker info > /dev/null 2>&1; then
       sudo service docker start
   fi
   ```

3. **Use WSL2 file system**
   - Project files in `/home/user/projects` are faster than `/mnt/c/`
   - Consider moving project to WSL2 filesystem for better performance

## Advantages of WSL2 Docker

✅ No Docker Desktop license required  
✅ Lighter resource usage  
✅ Better integration with Linux tools  
✅ Faster file I/O in WSL2 filesystem  
✅ Direct Docker CLI access  

## Verification

After setup, verify everything works:

```bash
# Test Docker
docker run hello-world

# Test TestContainers
mvn test -Dtest=BaseIntegrationTest

# Check test logs
cat target/surefire-reports/*.txt
```

## Integration Test Count

- **Total Integration Tests**: 23
  - CacheIntegrationTest: 8 tests
  - CurrencyFlowIntegrationTest: 6 tests
  - ExternalProviderWireMockTest: 9 tests

All tests use TestContainers with PostgreSQL 17-alpine and Redis 7-alpine containers.

## Notes

- Docker daemon in WSL2 must be started before running tests
- Environment variables are automatically set by the PowerShell script
- Tests will pull PostgreSQL and Redis images on first run
- Subsequent runs reuse containers for faster execution
