#!/bin/bash
# Script to start Docker in WSL2 and run Maven tests

echo "==================================="
echo "Starting Docker in WSL2"
echo "==================================="

# Start Docker daemon
sudo service docker start

# Wait for Docker to be ready
echo "Waiting for Docker daemon to be ready..."
timeout 30 bash -c 'until docker info > /dev/null 2>&1; do sleep 1; done'

if docker info > /dev/null 2>&1; then
    echo "✓ Docker daemon is running"
    docker --version
    echo ""
    
    echo "==================================="
    echo "Running Maven Tests"
    echo "==================================="
    
    # Navigate to project directory
    PROJECT_DIR="/mnt/c/Work/Study/AI Copilot/Cur_ex_app"
    cd "$PROJECT_DIR" || exit 1
    
    # Set DOCKER_HOST for TestContainers
    export DOCKER_HOST=unix:///var/run/docker.sock
    export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
    export TESTCONTAINERS_RYUK_DISABLED=false
    
    # Run Maven tests
    ./mvnw clean test
    
else
    echo "✗ Failed to start Docker daemon"
    exit 1
fi
