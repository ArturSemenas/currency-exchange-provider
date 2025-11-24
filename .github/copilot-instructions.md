# Currency Exchange Rates Provider Service - Workspace Instructions

## Project Overview
Spring Boot 3.4.1 application that provides up-to-date currency exchange rates from multiple providers (fixer.io and others). Supports dynamic currency list management via REST API with hourly rate updates.

## Technology Stack
- Java 21
- Maven
- Spring Boot 3.4.1
- PostgreSQL + Docker
- Liquibase for database migrations
- Spring Security with Basic Auth (to be implemented)
- MapStruct for DTO mapping
- Lombok for boilerplate reduction
- Swagger/OpenAPI for API documentation

## Quick Start

1. **Start Database**: `docker-compose up -d`
2. **Set API Key**: `export FIXER_API_KEY=your-api-key`
3. **Build**: `mvn clean install`
4. **Run**: `mvn spring-boot:run`
5. **Access Swagger**: http://localhost:8080/swagger-ui.html

## Project Structure

```
src/main/java/com/currencyexchange/provider/
├── controller/          # REST API endpoints
├── model/              # JPA entities (Currency, ExchangeRate)
├── repository/         # Spring Data repositories
├── service/            # Business logic layer
└── CurrencyExchangeProviderApplication.java  # Main class with @EnableScheduling
```

## Key Features Implemented

✅ Basic project structure  
✅ Maven pom.xml with all dependencies  
✅ PostgreSQL database configuration  
✅ Liquibase migration scripts (currencies, exchange_rates, users tables)  
✅ Docker Compose for PostgreSQL + pgAdmin  
✅ Basic Currency entity and repository  
✅ Currency REST controller with GET/POST endpoints  
✅ Swagger/OpenAPI integration  
✅ Application compiles successfully

## Next Implementation Steps

When you provide additional instructions, you can implement:
- [ ] Fixer.io API client service
- [ ] Scheduled task for hourly rate updates
- [ ] Exchange rate service and endpoints
- [ ] Spring Security configuration
- [ ] DTO classes and MapStruct mappers
- [ ] Exception handling and validation
- [ ] Additional exchange rate providers
- [ ] Caching strategy
- [ ] Comprehensive tests
