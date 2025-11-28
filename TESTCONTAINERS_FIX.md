# TestContainers Fix for Docker Desktop 29.x

## Problem Identified
Docker Desktop **29.0.1** (released November 2025) has an incompatibility with `docker-java` library **3.4.0** used by TestContainers **1.20.4**.

### Symptoms
- TestContainers fails with: `Status 400: {"ID":"","Containers":0,...}` (empty JSON)
- Docker CLI works fine (`docker info`, `docker ps`, etc.)
- Regular Docker Compose works perfectly
- Only TestContainers/Java Docker API fails

### Root Cause
The `docker-java 3.4.0` client cannot properly communicate with Docker Desktop 29.x's updated API endpoint, returning malformed JSON on `/info` requests through named pipes.

## Solutions (Choose One)

### ✅ RECOMMENDED: Downgrade Docker Desktop
**Easiest and fastest fix:**
1. Uninstall Docker Desktop 29.0.1
2. Install Docker Desktop 28.x from: https://docs.docker.com/desktop/release-notes/
3. TestContainers will work immediately

### Option 2: Wait for Updates
- Wait for `docker-java 3.5.0+` or `testcontainers 1.21.0+` with compatibility fix
- Track: https://github.com/testcontainers/testcontainers-java/issues

### Option 3: Use External Containers (Temporary Workaround)
Run containers manually and connect tests to them:

```bash
# Start test infrastructure
docker-compose -f docker-compose.test.yml up -d

# Run tests
mvn test -Dtest=ExternalContainersIntegrationTest

# Cleanup
docker-compose -f docker-compose.test.yml down
```

### Option 4: Use WSL2 Docker  
Install Docker directly in WSL2 instead of Docker Desktop:
1. Uninstall Docker Desktop
2. Install Docker in WSL2: `sudo apt-get install docker.io`
3. Start Docker: `sudo service docker start`
4. Run tests from WSL2 environment

## Verification
After applying fix, run:
```bash
mvn test -Dtest=MinimalTestContainersTest
```

Should see: ✅ Container started successfully!

## Timeline
- **Yesterday**: Docker Desktop 28.x - TestContainers worked ✅
- **Today**: Upgraded to Docker Desktop 29.0.1 - TestContainers broken ❌  
- **Fix**: Downgrade Docker Desktop or wait for library updates

## Technical Details
- Docker Desktop Version: 29.0.1 (Build 210994)
- docker-java Version: 3.4.0
- TestContainers Version: 1.20.4
- Error: Named pipe communication returns HTTP 400 with empty values
- Docker CLI works: Uses different communication method
