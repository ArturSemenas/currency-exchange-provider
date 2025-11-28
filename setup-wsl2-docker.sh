#!/bin/bash
# Setup Docker in WSL2 to work with TestContainers from Windows

echo "==================================="
echo "Configuring Docker for TestContainers"
echo "==================================="

# Create Docker daemon configuration
sudo mkdir -p /etc/docker

# Configure Docker to listen on both Unix socket and TCP
cat <<EOF | sudo tee /etc/docker/daemon.json
{
  "hosts": ["unix:///var/run/docker.sock", "tcp://0.0.0.0:2375"],
  "insecure-registries": []
}
EOF

echo "✓ Docker daemon.json created"

# Create systemd override directory
sudo mkdir -p /etc/systemd/system/docker.service.d

# Override ExecStart to remove -H flag (conflicts with daemon.json)
cat <<EOF | sudo tee /etc/systemd/system/docker.service.d/override.conf
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd
EOF

echo "✓ Systemd override created"

# Reload systemd and restart Docker
if command -v systemctl &> /dev/null; then
    echo "Using systemctl..."
    sudo systemctl daemon-reload
    sudo systemctl restart docker
else
    echo "Using service command..."
    sudo service docker restart
fi

echo ""
echo "Waiting for Docker to start..."
sleep 3

# Verify Docker is running
if docker info > /dev/null 2>&1; then
    echo "✓ Docker is running"
    echo "✓ Docker version: $(docker --version)"
    echo ""
    echo "Docker is now accessible via:"
    echo "  - Unix socket: unix:///var/run/docker.sock"
    echo "  - TCP: tcp://localhost:2375"
    echo ""
    echo "From Windows, you can use:"
    echo '  $env:DOCKER_HOST="tcp://localhost:2375"'
else
    echo "✗ Docker failed to start"
    echo "Check logs with: sudo journalctl -u docker.service"
    exit 1
fi
