# Docker Deployment Guide

## Overview
This project uses Docker Compose to orchestrate multiple services:
- **currency-exchange-app**: Main Spring Boot application
- **postgres**: PostgreSQL 16 database
- **redis**: Redis 7 for caching
- **pgadmin**: Database management UI
- **mock-provider-1**: Mock exchange rate provider #1
- **mock-provider-2**: Mock exchange rate provider #2

## Prerequisites
- Docker Engine 20.10+
- Docker Compose 2.0+

## Quick Start

### 1. Configure Environment Variables
```bash
# Copy the example environment file
cp .env.example .env

# Edit .env and add your Fixer.io API key
# FIXER_API_KEY=your-actual-api-key-here
```

### 2. Start All Services
```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f currency-exchange-app
```

### 3. Verify Services
```bash
# Check service health
docker-compose ps

# All services should show "healthy" status
```

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Main App | http://localhost:8080 | Currency Exchange API |
| Swagger UI | http://localhost:8080/swagger-ui.html | API Documentation |
| Actuator Health | http://localhost:8080/actuator/health | Health Check |
| pgAdmin | http://localhost:5050 | Database Management |
| PostgreSQL | localhost:5432 | Database (internal) |
| Redis | localhost:6379 | Cache (internal) |
| Mock Provider 1 | http://localhost:8091 | Test provider |
| Mock Provider 2 | http://localhost:8092 | Test provider |

## Docker Commands

### Build and Start
```bash
# Build images and start all services
docker-compose up -d --build

# Start without rebuilding
docker-compose up -d

# Start specific service
docker-compose up -d currency-exchange-app
```

### Stop and Remove
```bash
# Stop all services
docker-compose stop

# Stop and remove containers
docker-compose down

# Remove containers and volumes (WARNING: deletes data)
docker-compose down -v
```

### Logs and Debugging
```bash
# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f currency-exchange-app

# View last 100 lines
docker-compose logs --tail=100 currency-exchange-app

# Execute shell in container
docker exec -it currency-exchange-app sh
```

### Rebuild Application
```bash
# Rebuild only the main app
docker-compose up -d --build currency-exchange-app

# Force rebuild without cache
docker-compose build --no-cache currency-exchange-app
docker-compose up -d currency-exchange-app
```

## Health Checks

All services include health checks with automatic restart:

| Service | Health Check Endpoint |
|---------|----------------------|
| Main App | http://localhost:8080/actuator/health |
| PostgreSQL | `pg_isready` command |
| Redis | `redis-cli ping` |
| Mock Providers | HTTP GET to latest rates endpoint |

Health check configuration:
- **Interval**: 30 seconds
- **Timeout**: 10 seconds
- **Retries**: 3-5 (varies by service)
- **Start Period**: 60 seconds (main app only)

## Environment Variables

### Database
- `POSTGRES_DB`: Database name (default: currency_exchange_db)
- `POSTGRES_USER`: Database user (default: postgres)
- `POSTGRES_PASSWORD`: Database password (default: postgres)

### Application
- `SPRING_PROFILES_ACTIVE`: Spring profile (default: prod)
- `FIXER_API_KEY`: **Required** - Your Fixer.io API key
- `FIXER_API_BASE_URL`: Fixer.io API URL (default: http://data.fixer.io/api)

### Mock Providers
- `MOCK_PROVIDER_1_URL`: Mock provider #1 URL
- `MOCK_PROVIDER_2_URL`: Mock provider #2 URL

## Networking

All services are connected to `currency-exchange-network` bridge network:
- Services can communicate using service names (e.g., `http://postgres:5432`)
- Services are isolated from external networks
- Only specified ports are exposed to host

## Volumes

### Persistent Data
- `postgres_data`: PostgreSQL data (survives container restarts)
- `redis_data`: Redis data (optional persistence)

### Volume Management
```bash
# List volumes
docker volume ls | grep currency

# Inspect volume
docker volume inspect currency-exchange-provider_postgres_data

# Remove volumes (WARNING: deletes data)
docker-compose down -v
```

## Troubleshooting

### Service Won't Start
```bash
# Check service status
docker-compose ps

# View detailed logs
docker-compose logs currency-exchange-app

# Check health
docker-compose exec currency-exchange-app wget -qO- http://localhost:8080/actuator/health
```

### Database Connection Issues
```bash
# Verify PostgreSQL is healthy
docker-compose ps postgres

# Test connection from app container
docker-compose exec currency-exchange-app sh
# Inside container:
wget -qO- http://postgres:5432  # Should timeout but proves network works
```

### Clear and Restart
```bash
# Nuclear option: remove everything and start fresh
docker-compose down -v
docker system prune -f
docker-compose up -d --build
```

### Performance Issues
```bash
# View resource usage
docker stats

# View specific container stats
docker stats currency-exchange-app
```

## Production Considerations

### Security
1. **Never commit `.env` file** - use environment-specific configuration
2. **Change default passwords** in production
3. **Use secrets management** (Docker Secrets, Vault, etc.)
4. **Run containers as non-root** (already configured in Dockerfile)
5. **Enable TLS/SSL** for database connections
6. **Use private registry** for production images

### Performance
1. **Adjust JVM memory** - Add to Dockerfile ENTRYPOINT:
   ```dockerfile
   ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
   ```
2. **Configure connection pools** - Set appropriate DB/Redis pool sizes
3. **Enable health checks** - Already configured
4. **Use production profile** - Set `SPRING_PROFILES_ACTIVE=prod`

### Monitoring
1. **Export logs** to centralized logging (ELK, Splunk)
2. **Monitor health endpoints** with Prometheus/Grafana
3. **Set up alerts** for service failures
4. **Track resource usage** (CPU, memory, disk)

## pgAdmin Configuration

To connect to PostgreSQL via pgAdmin:
1. Open http://localhost:5050
2. Login with:
   - Email: `admin@admin.com`
   - Password: `admin`
3. Add new server:
   - **Name**: Currency Exchange DB
   - **Host**: `postgres` (use service name, not localhost)
   - **Port**: `5432`
   - **Username**: `postgres`
   - **Password**: `postgres`

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Build Docker image
  run: docker-compose build currency-exchange-app

- name: Run tests
  run: docker-compose up -d && docker-compose exec -T currency-exchange-app mvn test

- name: Push to registry
  run: docker push your-registry/currency-exchange-app:latest
```

## Additional Resources
- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot with Docker](https://spring.io/guides/gs/spring-boot-docker/)
