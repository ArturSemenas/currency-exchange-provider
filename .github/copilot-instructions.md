# Currency Exchange Rates Provider Service - Workspace Instructions

## Project Overview
Spring Boot 3.4.1 application that provides up-to-date currency exchange rates from **4 providers** (Fixer.io, ExchangeRatesAPI, and 2 mock providers). Supports dynamic currency list management via REST API with hourly scheduled rate updates, Redis caching, multi-provider rate aggregation, and comprehensive testing.

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
   This starts PostgreSQL, Redis, pgAdmin, and 2 mock exchange rate providers

2. **Set API Keys** (optional - mock providers work without keys):
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
   - **User**: username: `user`, password: `admin123` (USER)
   - **Premium User**: username: `premium`, password: `admin123` (PREMIUM_USER)
   - **Admin**: username: `admin`, password: `admin123` (ADMIN)

## Project Structure

```
src/main/java/com/currencyexchange/provider/
├── client/             # External API clients
│   ├── ExchangeRateProvider.java (interface)
│   ├── impl/
│   │   ├── FixerIoProvider.java
│   │   ├── ExchangeratesApiProvider.java
│   │   ├── MockProvider1Client.java
│   │   └── MockProvider2Client.java
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
- MockProvider1Client implementation (port 8091, /api/v1 endpoints)
- MockProvider2Client implementation (port 8092, /v1 endpoints)
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
- Simple authority-based security model (no ROLE_ prefix)
  - Authorities stored in database: USER, PREMIUM_USER, ADMIN
  - SecurityFilterChain uses `.hasAuthority("ADMIN")` and `.hasAnyAuthority("ADMIN", "PREMIUM_USER")`
  - Method-level security removed (no @PreAuthorize annotations)
- BCrypt password encoding (strength 12)
- UserDetailsServiceImpl with custom user loading
- Authority-based access control:
  - POST /api/v1/currencies → ADMIN only
  - POST /api/v1/currencies/refresh → ADMIN only
  - GET /api/v1/currencies/trends → ADMIN and PREMIUM_USER
  - GET /api/v1/currencies → Public access
  - GET /api/v1/currencies/exchange-rates → Public access
- Entity relationship fix: Removed bidirectional @ManyToMany from Role entity
- User entity: Added @ToString.Exclude on roles field, changed to FetchType.EAGER

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

### Phase 15: Code Quality & Analysis ✅
- **JaCoCo 0.8.12**: Code coverage with 80% line, 70% branch thresholds
- **Checkstyle 10.20.2**: Google Java Style Guide enforcement (Maven plugin 3.5.0)
- **PMD 7.8.0**: Static code analysis with quickstart ruleset (Maven plugin 3.25.0)
- **checkstyle.xml**: Comprehensive ruleset (127 lines) with Spring Boot compatibility
- **Code Quality Improvements**:
  - Fixed 145 of 146 Checkstyle violations (99.3% reduction)
  - Added comprehensive Javadoc with @param/@return tags
  - Fixed unused imports, star imports, whitespace issues
  - Fixed line length violations (120 char max)
  - All DTOs, repositories, handlers, and services documented
- **Known Exception**: 1 HideUtilityClassConstructor violation on main application class (cannot fix - Spring Boot requires public constructor)

## Test Coverage Summary
- **Unit Tests**: 106 tests (Services, Controllers)
- **Integration Tests**: 23 tests (Cache, Workflow, WireMock)
- **Total Tests**: 129/129 passing (100%)
- **Requirements**: Docker Desktop must be running for TestContainer tests

## Code Quality Standards

### Checkstyle Rules (Google Java Style)
- **Line length**: Maximum 120 characters
- **Imports**: No star imports (e.g., `import java.util.*`), no unused imports
- **Javadoc**: Required for all public classes, methods, and record components
  - `@param` for all method parameters
  - `@return` for all non-void methods
  - `@param` for all record components
- **Whitespace**: Operators on new line when wrapping, spaces in empty braces `{ }`
- **Naming**: CamelCase for classes, camelCase for methods/variables
- **Design**: Utility classes should have private constructors (except Spring Boot main class)

### Important Notes for Development
- **Spring Boot Main Class**: Cannot be `final` and must have public no-args constructor
  - Spring needs to create proxies and instantiate the application context
  - FinalClass check disabled in checkstyle.xml for compatibility
- **Record Documentation**: Always add `@param` tags in class-level Javadoc for all record components
- **Exception Handlers**: Document both `@param ex` and `@param request` parameters, plus `@return`
- **Repository Methods**: Document all custom query methods with parameter descriptions
- **Multi-line Strings**: Use `+` concatenation for long annotation descriptions (Swagger, etc.)
- **Properties Files**: Use backslash `\` for line continuation when exceeding 120 chars

### Running Quality Checks
```bash
# Run Checkstyle only
mvn checkstyle:check

# Run PMD only
mvn pmd:check

# Run JaCoCo coverage with verification
mvn clean verify

# Run all quality checks
mvn clean verify checkstyle:check pmd:check
```

## Next Implementation Steps

### Future Phases (Phases 16+)
- Performance optimization
- API rate limiting
- Advanced caching strategies
- Metrics and monitoring with Micrometer
- Additional exchange rate providers
- GraphQL API support
- Production deployment configuration

## Current Project Status
✅ **Phases 1-15 Complete**: All core features, security, validation, Docker, testing, and code quality implemented  
📊 **Test Coverage**: 129/129 tests passing (106 unit + 23 integration)  
📋 **Code Quality**: 145/146 Checkstyle violations fixed (99.3%), JaCoCo coverage tracking enabled  
🔐 **Security Model**: Simple authority-based model (USER, PREMIUM_USER, ADMIN) without ROLE_ prefix  
 **Production Ready**: Application fully functional with Redis caching, scheduled updates, security, and quality gates  
🔌 **4-Provider Integration**: Successfully aggregating rates from Fixer.io, ExchangeRatesAPI, mock-provider-1, and mock-provider-2  
📝 **Next Phase**: Performance optimization and monitoring (Phases 16+)
