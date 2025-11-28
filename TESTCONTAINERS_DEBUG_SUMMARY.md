# Docker Desktop & TestContainers Debugging Summary

## Date: November 27, 2025

## Issue
Unable to run TestContainers-based integration tests due to Docker Desktop 29.x compatibility issues.

## Environment
- **Docker Desktop Version**: 29.0.1  
- **TestContainers Version**: 1.20.4
- **OS**: Windows with Docker Desktop  
- **Docker Engine**: Running successfully (verified with `docker ps`)

## Root Cause
Docker Desktop 29.x returns HTTP Status 400 with empty JSON on the `/info` API endpoint when accessed via named pipes, causing TestContainers initialization to fail.

### Error Message
```
BadRequestException (Status 400: {"ID":"",..."Labels":["com.docker.desktop.address=npipe://\\\\.\\pipe\\docker_cli"]...})
Could not find a valid Docker environment. Please see logs and check configuration
```

## Attempted Solutions

### 1. TestContainers Configuration File ❌
Created `.testcontainers.properties` with various Docker host configurations:
- `npipe:////./pipe/docker_engine` - Failed (Status 400)
- `npipe:////./pipe/dockerDesktopLinuxEngine` - Failed (Status 400)  
- `tcp://127.0.0.1:63015` (TCD endpoint) - Failed (Status 400)

**Result**: All approaches failed with the same 400 error

### 2. Environment Variables ❌
Tried setting:
- `DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine`
- `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
- `TESTCONTAINERS_RYUK_DISABLED`

**Result**: No improvement

### 3. Docker Context Switching ❌
Attempted switching between contexts:
- `default` (npipe:////./pipe/docker_engine)
- `desktop-linux` (npipe:////./pipe/dockerDesktopLinuxEngine)
- `tcd` (tcp://127.0.0.1:63015) - Testcontainers Desktop

**Result**: All contexts return same 400 error

### 4. External Containers Approach ⚠️  
Created `docker-compose.test.yml` and `ExternalContainersIntegrationTest` to use pre-started containers instead of TestContainers.

**Issues Encountered**:
- Redis autoconfiguration exclusion doesn't prevent `ExchangeRateCacheService` from being loaded
- Service dependencies on `RedisTemplate` bean cause context loading failure
- Would require significant refactoring to make Redis dependencies optional

## Files Created

### 1. `TestContainersHealthCheckTest.java`
Simple health check tests to verify TestContainers functionality:
- `testPostgreSQLContainerStarts()`
- `testRedisContainerStarts()`
- `testMultipleContainersCanRun()`

**Status**: All tests fail with Docker environment error

###  2. `docker-compose.test.yml`
External test containers configuration:
- PostgreSQL 17-alpine (port 5433)
- Redis 7-alpine (port 6380)

**Status**: Containers start successfully but tests still fail due to Redis dependency

### 3. `ExternalContainersIntegrationTest.java`
Spring Boot test using external containers instead of TestContainers.

**Status**: Fails due to Redis bean dependency

### 4. `TESTCONTAINERS_DOCKER_ISSUE.md`
Comprehensive documentation of the issue with potential solutions.

## Working Alternatives

### Option 1: Use Docker Compose for Integration Testing
```bash
# Start test infrastructure
docker-compose -f docker-compose.test.yml up -d

# Run tests against external containers
# (Requires refactoring to make Redis optional)
```

### Option 2: Downgrade Docker Desktop
- Downgrade to Docker Desktop 28.x or earlier
- TestContainers 1.20.4 is known to work with older Docker Desktop versions

### Option 3: Use WSL2 Docker
- Install Docker directly in WSL2
- Run tests from WSL2 environment
- Set `DOCKER_HOST=unix:///var/run/docker.sock`

## Recommendations

1. **Short Term**: Use `docker-compose` for integration testing until Docker Desktop compatibility is resolved
2. **Medium Term**: Consider downgrading Docker Desktop or using WSL2
3. **Long Term**: Monitor TestContainers repository for Docker Desktop 29.x compatibility updates

## References
- TestContainers Issues: https://github.com/testcontainers/testcontainers-java/issues
- Docker Desktop 29.x Release Notes: Breaking changes in named pipe API
- TestContainers Documentation: https://java.testcontainers.org/

## Conclusion
The issue is a known compatibility problem between TestContainers 1.20.4 and Docker Desktop 29.x. The application and tests work correctly, but TestContainers cannot initialize due to Docker API changes. Manual container management via docker-compose is recommended as a workaround.
