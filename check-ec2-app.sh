#!/bin/bash
# Run this script in EC2 Instance Connect to diagnose the application

echo "=== Docker Container Status ==="
docker ps -a

echo -e "\n=== Currency Exchange App Logs (last 100 lines) ==="
docker logs currency-exchange-app --tail 100

echo -e "\n=== Checking Memory Usage ==="
free -h

echo -e "\n=== Checking Container Resource Usage ==="
docker stats --no-stream

echo -e "\n=== Testing Health Endpoint from Inside ==="
curl -v localhost:8080/actuator/health

echo -e "\n=== Checking if App Port is Listening ==="
netstat -tlnp | grep 8080
