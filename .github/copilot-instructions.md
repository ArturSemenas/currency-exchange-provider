# Currency Exchange Rates Provider Service - Instructions

## Overview
Spring Boot 3.4.1 application providing currency exchange rates from 4 providers (Fixer.io, ExchangeRatesAPI, and 2 mock providers). Features include dynamic currency management, hourly updates, Redis caching, and multi-provider aggregation. Successfully deployed to AWS EC2 (eu-north-1) with automated CI/CD pipeline.

## Tech Stack
- **Java 21**, **Maven**, **Spring Boot 3.4.1**
- **PostgreSQL 17**, **Redis 7**, **Liquibase**
- **Spring Security**, **MapStruct**, **Lombok**
- **Swagger/OpenAPI**, **TestContainers**, **WireMock**
- **JaCoCo**, **Checkstyle**, **PMD**

## Quick Start
1. **Start Infrastructure**: `docker-compose up -d`
2. **Set API Keys** (optional):
   ```bash
   export FIXER_API_KEY=your-api-key
   export EXCHANGERATESAPI_KEY=your-api-key
   ```
3. **Build & Run**:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
4. **Access**:
   - Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - Health Check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
5. **Test Credentials**:
   - User: `user/admin123`
   - Admin: `admin/admin123`

## Key Features
- **Core Infrastructure**: Spring Boot, PostgreSQL, Liquibase, JPA.
- **API Integration**: Fixer.io, ExchangeRatesAPI, Mock Providers.
- **Redis Cache**: 2-hour TTL, fallback strategies.
- **Business Logic**: CRUD, ISO 4217 validation, rate aggregation.
- **Scheduled Tasks**: Hourly updates, manual refresh.
- **Security**: BCrypt, role-based access (USER, PREMIUM_USER, ADMIN).
- **Validation**: Custom annotations for currency and period.
- **Testing**: 359 tests (336 unit, 23 integration), 87% coverage.
- **Code Quality**: Checkstyle, PMD, JaCoCo.
- **AWS Deployment**: Automated CI/CD with GitHub Actions, Terraform infrastructure, EC2 t3.micro (eu-north-1).
- **Docker**: 3-service setup (main app, PostgreSQL, Redis) optimized for 1GB memory.

## Project Structure
- **src/main**: Core application (clients, controllers, services, etc.).
- **src/test**: Unit and integration tests.

## Running Quality Checks
```bash
mvn clean verify
mvn checkstyle:check
mvn pmd:check
```

## Current Status
✅ **Phases 1-17 Complete**: Core features, security, Docker, testing, quality gates, and AWS deployment implemented.  
📊 **Test Coverage**: 87% overall.  
🔐 **Security**: Role-based model without ROLE_ prefix.  
🔌 **4-Provider Integration**: Fixer.io, ExchangeRatesAPI, mock-provider-1, mock-provider-2.  
☁️ **AWS Deployment**: Running on EC2 t3.micro in eu-north-1 (Stockholm), Terraform-managed infrastructure, GitHub Actions CI/CD.  
🐳 **Production Optimization**: 3-container setup (690MB used / 904MB total) - mock services excluded for memory constraints.  
📝 **Next Steps**: Performance optimization, monitoring, cost optimization, GraphQL support.
