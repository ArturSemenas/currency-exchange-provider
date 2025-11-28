# TestContainers Docker Desktop 29.x Compatibility Issues

## Problem
Docker Desktop 29.x on Windows returns Status 400 with empty JSON on `/info` endpoint, causing TestContainers 1.20.4 to fail with:
```
BadRequestException (Status 400: {"ID":"",..."Labels":["com.docker.desktop.address=npipe://\\\\.\\pipe\\docker_cli"]...})
```

## Root Cause
Docker Desktop 29.x has a breaking change in how it handles the `/info` API endpoint through named pipes.

## Solutions

### Option 1: Use Testcontainers Desktop (TCD) - RECOMMENDED
TestContainers Desktop provides a compatibility layer:
1. Check if `tcd` context exists: `docker context ls`
2. The TCD endpoint appears at `tcp://127.0.0.1:63015`
3. However, this endpoint also returns Status 400

### Option 2: Downgrade Docker Desktop
Downgrade to Docker Desktop 28.x or earlier versions that work with TestContainers.

### Option 3: Enable WSL2 Docker Integration
Use Docker directly via WSL2 instead of Docker Desktop Windows integration:
1. Install Docker in WSL2
2. Set `DOCKER_HOST=unix:///var/run/docker.sock` in WSL
3. Run tests from WSL environment

### Option 4: Use Direct Docker API Calls (WORKAROUND)
Create custom test implementation that bypasses TestContainers' Docker discovery:
```java
// Use DockerClientConfig to explicitly set connection
DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
    .withDockerHost("npipe:////./pipe/dockerDesktopLinuxEngine")
    .build();
```

### Option 5: Wait for TestContainers Update
TestContainers team is aware of Docker Desktop 29.x issues. Track:
- https://github.com/testcontainers/testcontainers-java/issues

## Current Status
- Docker Desktop Version: 29.0.1
- TestContainers Version: 1.20.4  
- Error: All Docker connection methods return Status 400
- Containers can be started via `docker-compose` successfully
- Issue only affects TestContainers initialization

## Temporary Workaround
For CI/CD and local development:
1. Use `docker-compose` for integration testing
2. Create manual container startup scripts
3. Use Spring Boot's `@SpringBootTest` with external containers
