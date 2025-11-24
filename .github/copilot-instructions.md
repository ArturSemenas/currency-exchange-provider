# Currency Exchange Rates Provider Service - Workspace Instructions

## Project Overview
Spring Boot 3.4.1 application that provides up-to-date currency exchange rates from multiple providers (Fixer.io and ExchangeRatesAPI). Supports dynamic currency list management via REST API with hourly scheduled rate updates, Redis caching, and comprehensive testing.

## Technology Stack
- **Java 21** - Modern Java features (Records, Switch Expressions, Pattern Matching)
- **Maven** - Dependency management and build tool
- **Spring Boot 3.4.1** - Core framework
- **PostgreSQL 17** - Primary database (with Docker support)
- **Redis 7** - Caching layer for exchange rates
- **Liquibase** - Database migrations and schema versioning
- **Spring Security** - Basic Auth with BCrypt password hashing
- **MapStruct 1.6.3** - DTO mapping
- **Lombok** - Boilerplate reduction
- **Swagger/OpenAPI 2.7.0** - API documentation
- **TestContainers 1.20.4** - Integration testing with PostgreSQL and Redis
- **WireMock 3.10.0** - External API mocking for tests
- **JaCoCo** - Code coverage reporting (planned Phase 15)

## Quick Start

### Prerequisites
- **Java 21** - Required JDK version
- **Docker Desktop** - For PostgreSQL, Redis, and TestContainers
- **Maven 3.9+** - Build tool

### Running the Application

1. **Start Infrastructure**: 
   ```bash
   docker-compose up -d
   ```
   This starts PostgreSQL, Redis, and pgAdmin

2. **Set API Keys** (optional - test keys provided):
   ```bash
   export FIXER_API_KEY=your-api-key
   export EXCHANGERATESAPI_KEY=your-api-key
   ```

3. **Build Project**:
   ```bash
   mvn clean install
   ```

4. **Run Application**:
   ```bash
   mvn spring-boot:run
   ```

5. **Access Endpoints**:
   - **Swagger UI**: http://localhost:8080/swagger-ui.html
   - **API Docs**: http://localhost:8080/v3/api-docs
   - **Health Check**: http://localhost:8080/actuator/health

6. **Test Credentials**:
   - **User**: username: `user`, password: `password123` (ROLE_USER)
   - **Admin**: username: `admin`, password: `password123` (ROLE_ADMIN)

## Project Structure

```
src/main/java/com/currencyexchange/provider/
├── client/             # External API clients
│   ├── ExchangeRateProvider.java (interface)
│   ├── impl/
│   │   ├── FixerIoProvider.java
│   │   └── ExchangeratesApiProvider.java
│   └── dto/            # API response DTOs
├── config/             # Spring configuration
│   ├── OpenApiConfig.java
│   ├── RedisConfig.java
│   ├── RestClientConfig.java
│   └── SecurityConfig.java
├── controller/         # REST API endpoints
│   ├── CurrencyController.java
│   ├── ExchangeRateController.java
│   └── TrendController.java
├── dto/                # Request/Response DTOs
│   ├── ConversionRequestDto.java
│   ├── ConversionResponseDto.java
│   ├── CurrencyDto.java
│   ├── ErrorResponseDto.java
│   └── ExchangeRateDto.java
├── exception/          # Custom exceptions
│   ├── CurrencyAlreadyExistsException.java
│   ├── CurrencyNotFoundException.java
│   ├── ExchangeRateNotFoundException.java
│   ├── ExternalApiException.java
│   ├── GlobalExceptionHandler.java
│   └── InsufficientDataException.java
├── mapper/             # MapStruct mappers
│   ├── CurrencyMapper.java
│   └── ExchangeRateMapper.java
├── model/              # JPA entities
│   ├── Currency.java
│   ├── ExchangeRate.java
│   ├── Role.java
│   └── User.java
├── repository/         # Spring Data JPA repositories
│   ├── CurrencyRepository.java
│   ├── ExchangeRateRepository.java
│   ├── RoleRepository.java
│   └── UserRepository.java
├── scheduler/          # Scheduled tasks
│   └── ExchangeRateScheduler.java
├── security/           # Security components
│   └── UserDetailsServiceImpl.java
├── service/            # Business logic layer
│   ├── CurrencyService.java
│   ├── ExchangeRateCacheService.java
│   ├── ExchangeRateRetrievalService.java
│   ├── ExchangeRateService.java
│   ├── RateAggregationService.java
│   ├── TrendAnalysisService.java
│   └── UserService.java
├── validation/         # Custom validators
│   ├── ValidCurrency.java
│   ├── CurrencyValidator.java
│   ├── ValidPeriod.java
│   └── PeriodValidator.java
└── CurrencyExchangeProviderApplication.java  # Main class with @EnableScheduling

src/test/java/com/currencyexchange/provider/
├── controller/         # Controller tests (@WebMvcTest)
├── integration/        # Integration tests (@SpringBootTest + TestContainers)
│   ├── BaseIntegrationTest.java
│   ├── CacheIntegrationTest.java
│   ├── CurrencyFlowIntegrationTest.java
│   └── ExternalProviderWireMockTest.java
└── service/            # Service unit tests
```

## Key Features Implemented

### Phase 1-2: Core Infrastructure ✅
- Spring Boot 3.4.1 project with Java 21
- PostgreSQL database with Docker Compose
- Liquibase migrations (currencies, exchange_rates, users, roles tables)
- JPA entities (Currency, ExchangeRate, User, Role)
- Spring Data JPA repositories

### Phase 3: External API Integration ✅
- ExchangeRateProvider interface
- FixerIoProvider implementation (with error handling)
- ExchangeratesApiProvider implementation
- RestTemplate with timeouts (10s connect, 30s read)
- API response DTOs with error handling

### Phase 4: Redis Cache Integration ✅
- Redis 7 Docker container
- ExchangeRateCacheService with 2-hour TTL
- Cache eviction strategies
- ExchangeRateRetrievalService with cache fallback

### Phase 5: Business Logic Services ✅
- CurrencyService (CRUD operations with ISO 4217 validation)
- ExchangeRateService (conversion with same-currency handling)
- RateAggregationService (concurrent provider fetching)
- TrendAnalysisService (percentage change calculation with switch expressions)

### Phase 6: Scheduled Tasks ✅
- ExchangeRateScheduler with hourly cron (`0 0 * * * *`)
- Manual refresh endpoint for admin users
- Execution time tracking and logging

### Phase 7: DTOs and MapStruct Mappers ✅
- Record-based DTOs (CurrencyDto, ExchangeRateDto, ConversionRequestDto/ResponseDto)
- MapStruct 1.6.3 mappers (CurrencyMapper, ExchangeRateMapper)
- ErrorResponseDto with validation error support

### Phase 8: REST Controllers ✅
- CurrencyController (GET all, POST add with @PreAuthorize)
- ExchangeRateController (GET conversion, POST refresh)
- TrendController (GET trend analysis with period validation)
- Comprehensive Swagger/OpenAPI annotations
- JSON examples in API documentation

### Phase 9: Spring Security ✅
- SecurityConfig with stateless sessions
- BCrypt password encoding (strength 12)
- UserDetailsServiceImpl with custom user loading
- Role-based access control (ROLE_USER, ROLE_ADMIN)
- Method-level security with @PreAuthorize

### Phase 10: Exception Handling ✅
- Custom exceptions (CurrencyNotFoundException, ExchangeRateNotFoundException, etc.)
- GlobalExceptionHandler with @RestControllerAdvice
- Structured ErrorResponseDto responses
- Security exception handling (401, 403)
- Validation error mapping

### Phase 11: Validation ✅
- Custom @ValidCurrency annotation with ISO 4217 validation
- Custom @ValidPeriod annotation for trend periods (12H, 7D, 3M, 1Y)
- Jakarta Bean Validation (@NotBlank, @Positive, etc.)
- Validation messages in messages.properties

### Phase 12: Docker Configuration ✅
- Multi-stage Dockerfile (Maven build + Java 21 runtime)
- Updated docker-compose.yml with app, PostgreSQL, Redis
- Health checks with Spring Actuator
- Environment variables with .env support
- DOCKER.md comprehensive deployment guide

### Phase 13: Unit Tests ✅
- CurrencyServiceTest (20 tests - CRUD, validation, ISO 4217)
- TrendAnalysisServiceTest (28 tests - parameterized period validation)
- CurrencyControllerTest (9 tests - MockMvc, security)
- H2 in-memory database for tests
- TestSecurityConfig for @WebMvcTest
- **Total Unit Tests**: 106/106 passing (100%)

### Phase 14: Integration Tests ✅
- BaseIntegrationTest with PostgreSQL TestContainer
- CacheIntegrationTest (8 tests - Redis integration)
- CurrencyFlowIntegrationTest (6 tests - end-to-end workflow)
- ExternalProviderWireMockTest (9 tests - HTTP mocking, error scenarios)
- Static container initialization pattern
- TestContainers 1.20.4 with PostgreSQL 17-alpine and Redis 7-alpine
- **Total Integration Tests**: 23/23 passing (100%)
- **Total All Tests**: 129/129 passing (100%)

## Test Coverage Summary
- **Unit Tests**: 106 tests (Services, Controllers)
- **Integration Tests**: 23 tests (Cache, Workflow, WireMock)
- **Total Tests**: 129 tests - 100% passing
- **Requirements**: Docker Desktop must be running for TestContainer tests

## Next Implementation Steps

### Phase 15: Code Quality & Analysis (Next)
When you provide additional instructions, you can implement:
- [ ] Add Checkstyle plugin to pom.xml
- [ ] Configure checkstyle.xml (Google or Sun checks)
- [ ] Add PMD plugin for static analysis
- [ ] Add JaCoCo plugin for code coverage reporting
- [ ] Set coverage thresholds (e.g., 80% line coverage)
- [ ] Run code quality checks and fix violations

### Future Phases (Phases 16+)
- Performance optimization
- API rate limiting
- Advanced caching strategies
- Metrics and monitoring with Micrometer
- Additional exchange rate providers
- GraphQL API support
- Production deployment configuration

## Current Project Status
✅ **Phases 1-14 Complete**: All core features, security, validation, Docker, and comprehensive testing implemented  
📊 **Test Coverage**: 129/129 tests passing (106 unit + 23 integration)  
🚀 **Production Ready**: Application fully functional with Redis caching, scheduled updates, and security  
📝 **Next Phase**: Code Quality & Analysis (Checkstyle, PMD, JaCoCo)
