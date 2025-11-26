# Currency Exchange Rates Provider Service

A production-ready Spring Boot 3.4.1 application that provides up-to-date currency exchange rates from multiple providers with Redis caching, rate aggregation, trend analysis, and comprehensive testing.

## 🚀 Features

### Core Functionality
- **Multi-Provider Rate Aggregation** - Fetches and compares rates from Fixer.io, ExchangeRatesAPI, and 2 mock providers
- **Intelligent Rate Selection** - Automatically selects the best (highest) exchange rate from all providers
- **Currency Conversion API** - Convert amounts between currencies with real-time rates
- **Trend Analysis** - Calculate currency appreciation/depreciation over configurable periods (12H, 7D, 3M, 1Y)
- **Scheduled Updates** - Hourly automatic refresh of exchange rates from all providers
- **Dynamic Currency Management** - Add/remove supported currencies via REST API

### Technical Excellence
- **Redis Caching** - 2-hour TTL cache with automatic fallback to database
- **Concurrent Rate Fetching** - CompletableFuture-based parallel provider queries
- **Role-Based Security** - 3-tier access control (USER, PREMIUM_USER, ADMIN)
- **Comprehensive Testing** - 129 tests (106 unit + 23 integration) with TestContainers and WireMock
- **Code Quality Gates** - Checkstyle, PMD, JaCoCo coverage tracking
- **Full Docker Support** - Multi-container setup with health checks and networking
- **OpenAPI Documentation** - Interactive Swagger UI with authentication support

## 📋 Technology Stack

- **Java 21** - Modern Java with Records, Pattern Matching, Switch Expressions
- **Spring Boot 3.4.1** - Core framework with Security, Data JPA, Web
- **Maven 3.9+** - Build and dependency management
- **PostgreSQL 17** - Primary database with Liquibase migrations
- **Redis 7** - Caching layer for exchange rates
- **Liquibase** - Database schema versioning and migrations
- **Spring Security** - Basic Auth with BCrypt password hashing
- **MapStruct 1.6.3** - Type-safe DTO mapping
- **Lombok** - Boilerplate reduction
- **Swagger/OpenAPI 2.8.1** - API documentation
- **TestContainers 1.20.4** - Integration testing with PostgreSQL and Redis
- **WireMock 3.10.0** - External API mocking for tests
- **JaCoCo 0.8.12** - Code coverage reporting
- **Checkstyle 10.20.2** - Google Java Style Guide enforcement
- **PMD 7.3.0** - Static code analysis
- **Docker & Docker Compose** - Container orchestration

## ⚡ Quick Start

### Prerequisites

- **Java 21** (required)
- **Maven 3.9+** (required)
- **Docker Desktop** (required for PostgreSQL, Redis, and TestContainers)
- **API Keys** (optional - test keys provided for development)
  - Fixer.io API key: Sign up at [https://fixer.io/](https://fixer.io/)
  - ExchangeRatesAPI key: Sign up at [https://exchangeratesapi.io/](https://exchangeratesapi.io/)

### 1. Clone the Repository

```bash
git clone https://github.com/ArturSemenas/currency-exchange-provider.git
cd currency-exchange-provider
```

### 2. Start Infrastructure (PostgreSQL + Redis + Mock Providers)

```bash
docker-compose up -d
```

This starts:
- **PostgreSQL 17** on port 5432 (database)
- **Redis 7** on port 6379 (cache)
- **Mock Provider 1** on port 8091 (Fixer.io simulation)
- **Mock Provider 2** on port 8092 (ExchangeRatesAPI simulation)
- **pgAdmin** on port 5050 (database management UI)

Verify all containers are healthy:
```bash
docker ps
```

### 3. Configure API Keys (Optional)

For real exchange rate data, set environment variables:

**Windows (PowerShell):**
```powershell
$env:FIXER_API_KEY="your-fixer-io-api-key"
$env:EXCHANGERATESAPI_KEY="your-exchangeratesapi-key"
```

**Linux/Mac (Bash):**
```bash
export FIXER_API_KEY=your-fixer-io-api-key
export EXCHANGERATESAPI_KEY=your-exchangeratesapi-key
```

**Note**: The application works without API keys using the 2 mock providers.

### 4. Build the Application

```bash
mvn clean install
```

This runs all 129 tests (requires Docker for TestContainers).

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`

**Startup Verification:**
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs
- Health Check: http://localhost:8080/actuator/health

### 6. Test the API

**Get all supported currencies:**
```bash
curl http://localhost:8080/api/v1/currencies
```

**Convert currency (no auth required):**
```bash
curl "http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=USD&to=EUR"
```

**Add a new currency (admin only):**
```bash
curl -X POST "http://localhost:8080/api/v1/currencies?currency=GBP" \
  -u admin:admin123
```

**Trigger rate refresh (admin only):**
```bash
curl -X POST http://localhost:8080/api/v1/currencies/refresh \
  -u admin:admin123
```

**Get trend analysis (premium+ users):**
```bash
curl "http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=7D" \
  -u premium:admin123
```

## 📚 API Documentation

### Interactive Documentation

Access the Swagger UI at: **http://localhost:8080/swagger-ui.html**

The Swagger interface provides:
- Interactive API testing with "Try it out" feature
- Request/response examples
- Authentication support (click "Authorize" button)
- Complete schema documentation

### API Endpoints Overview

| Method | Endpoint | Description | Access | 
|--------|----------|-------------|--------|
| GET | `/api/v1/currencies` | List all supported currencies | Public |
| POST | `/api/v1/currencies?currency={code}` | Add new currency | ADMIN |
| GET | `/api/v1/currencies/exchange-rates` | Convert currency amount | Public |
| POST | `/api/v1/currencies/refresh` | Manually refresh rates | ADMIN |
| GET | `/api/v1/currencies/trends` | Get trend analysis | PREMIUM_USER, ADMIN |

### Test Users

The application comes with 3 preconfigured users:

| Username | Password | Authority | Access |
|----------|----------|-----------|--------|
| `user` | `admin123` | USER | Public endpoints only |
| `premium` | `admin123` | PREMIUM_USER | Public + Trends |
| `admin` | `admin123` | ADMIN | All endpoints |

### Example API Calls

#### 1. Get All Currencies (Public)

**Request:**
```bash
curl http://localhost:8080/api/v1/currencies
```

**Response:**
```json
[
  {"code": "USD", "name": "US Dollar"},
  {"code": "EUR", "name": "Euro"},
  {"code": "GBP", "name": "British Pound"}
]
```

#### 2. Convert Currency (Public)

**Request:**
```bash
curl "http://localhost:8080/api/v1/currencies/exchange-rates?amount=100&from=USD&to=EUR"
```

**Response:**
```json
{
  "from": "USD",
  "to": "EUR",
  "amount": 100.00,
  "convertedAmount": 92.35,
  "rate": 0.923500,
  "timestamp": "2025-11-26T14:30:00"
}
```

#### 3. Add Currency (ADMIN only)

**Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/currencies?currency=JPY" \
  -u admin:admin123
```

**Response:**
```json
{
  "code": "JPY",
  "name": "Japanese Yen"
}
```

#### 4. Refresh Exchange Rates (ADMIN only)

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/currencies/refresh \
  -u admin:admin123
```

**Response:**
```json
{
  "message": "Exchange rates updated successfully",
  "updatedCount": 24,
  "timestamp": "2025-11-26T15:00:00"
}
```

#### 5. Get Trend Analysis (PREMIUM_USER or ADMIN)

**Request:**
```bash
curl "http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=7D" \
  -u premium:admin123
```

**Response:**
```json
{
  "baseCurrency": "USD",
  "targetCurrency": "EUR",
  "period": "7D",
  "trendPercentage": 2.35,
  "description": "USD appreciated by 2.35% against EUR over the last 7 days"
}
```

**Supported Periods:**
- `12H` - 12 hours (minimum)
- `7D` - 7 days
- `30D` - 30 days
- `3M` - 3 months
- `1Y` - 1 year

### Error Responses

All errors return a structured JSON response:

```json
{
  "timestamp": "2025-11-26T15:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Currency not found: XYZ",
  "path": "/api/v1/currencies"
}
```

**Common Status Codes:**
- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error

## 🏗️ Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Applications                      │
│              (Browser, Postman, curl, etc.)                  │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/REST
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot Application (Port 8080)             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Controllers  │  │   Services   │  │ Repositories │       │
│  │  (REST API)  │─▶│  (Business)  │─▶│    (JPA)     │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│         │                  │                  │              │
│         │                  ▼                  ▼              │
│         │          ┌───────────────┐  ┌──────────────┐      │
│         │          │ Rate          │  │  PostgreSQL  │      │
│         │          │ Aggregation   │  │  Database    │      │
│         │          │ Service       │  │ (Port 5432)  │      │
│         │          └───────┬───────┘  └──────────────┘      │
│         │                  │                                 │
│         ▼                  ▼                                 │
│  ┌──────────────┐  ┌────────────────┐                       │
│  │   Spring     │  │  Redis Cache   │                       │
│  │  Security    │  │  (Port 6379)   │                       │
│  └──────────────┘  └────────────────┘                       │
└─────────────────────────┬───────────────────────────────────┘
                          │
              ┌───────────┴──────────────┐
              │  External Rate Providers  │
              ├──────────────────────────┤
              │  Fixer.io API            │
              │  ExchangeRatesAPI.io     │
              │  Mock Provider 1 (8091)  │
              │  Mock Provider 2 (8092)  │
              └──────────────────────────┘
```

### Data Flow

1. **Rate Aggregation** (Hourly via @Scheduled):
   - `RateAggregationService` fetches rates from all 4 providers concurrently
   - Compares rates and selects best (highest) rate for each currency pair
   - Stores rates in PostgreSQL with timestamp and provider info
   - Updates Redis cache with 2-hour TTL

2. **Conversion Request**:
   - Check Redis cache first for rate
   - If cache miss, query PostgreSQL database
   - Store retrieved rate back in cache
   - Calculate conversion and return result

3. **Trend Analysis**:
   - Query historical rates from PostgreSQL for specified period
   - Calculate percentage change (appreciation/depreciation)
   - Return formatted trend description

### Project Structure

```
src/
├── main/
│   ├── java/com/currencyexchange/provider/
│   │   ├── client/                    # External API clients
│   │   │   ├── ExchangeRateProvider.java (interface)
│   │   │   ├── impl/
│   │   │   │   ├── FixerIoProvider.java
│   │   │   │   └── ExchangeratesApiProvider.java
│   │   │   └── dto/                   # API response DTOs
│   │   ├── config/                    # Spring configuration
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── RestClientConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/                # REST endpoints
│   │   │   ├── CurrencyController.java
│   │   │   ├── ExchangeRateController.java
│   │   │   └── TrendController.java
│   │   ├── dto/                       # Request/Response DTOs
│   │   ├── exception/                 # Custom exceptions & handler
│   │   ├── mapper/                    # MapStruct mappers
│   │   ├── model/                     # JPA entities
│   │   ├── repository/                # Spring Data JPA
│   │   ├── scheduler/                 # Scheduled tasks
│   │   ├── security/                  # Security components
│   │   ├── service/                   # Business logic
│   │   └── validation/                # Custom validators
│   └── resources/
│       ├── db/changelog/              # Liquibase migrations
│       ├── application.properties
│       └── application-prod.properties
├── test/
│   └── java/com/currencyexchange/provider/
│       ├── controller/                # Controller tests (@WebMvcTest)
│       ├── integration/               # Integration tests (TestContainers)
│       └── service/                   # Service unit tests
└── mock-services/                     # Standalone mock providers
    ├── mock-provider-1/               # Fixer.io simulation (port 8091)
    └── mock-provider-2/               # ExchangeRatesAPI simulation (port 8092)
```

## 🐳 Docker Setup

### All Services

Start all services (app + database + cache + mocks):
```bash
docker-compose up -d
```

Stop all services:
```bash
docker-compose down
```

Remove all data (including volumes):
```bash
docker-compose down -v
```

### Individual Services

**Start only infrastructure:**
```bash
docker-compose up -d postgres redis mock-provider-1 mock-provider-2
```

**Rebuild and restart application:**
```bash
docker-compose build currency-exchange-app
docker-compose up -d currency-exchange-app
```

### Docker Services

| Service | Port | Description |
|---------|------|-------------|
| `currency-exchange-app` | 8080 | Main Spring Boot application |
| `postgres` | 5432 | PostgreSQL 17 database |
| `redis` | 6379 | Redis 7 cache |
| `mock-provider-1` | 8091 | Mock Fixer.io simulation |
| `mock-provider-2` | 8092 | Mock ExchangeRatesAPI simulation |
| `pgadmin` | 5050 | PostgreSQL web UI |

### Health Checks

Check status of all containers:
```bash
docker ps
```

View logs for specific service:
```bash
docker logs currency-exchange-app
docker logs currency-exchange-db
docker logs currency-exchange-redis
```

Follow logs in real-time:
```bash
docker logs -f currency-exchange-app
```

### Database Access

**Using pgAdmin (Web UI):**
1. Navigate to http://localhost:5050
2. Login with:
   - Email: `admin@admin.com`
   - Password: `admin`
3. Create server connection:
   - Host: `postgres` (container name)
   - Port: `5432`
   - Database: `currency_exchange_db`
   - Username: `postgres`
   - Password: `postgres`

**Using DBeaver or CLI:**
```bash
Host: localhost
Port: 5432
Database: currency_exchange_db
Username: postgres
Password: postgres
```

### Mock Providers

The project includes 2 standalone mock services that simulate external exchange rate APIs:

**Mock Provider 1** (Fixer.io simulation):
```bash
# Test endpoint
curl http://localhost:8091/api/v1/latest

# Get USD-based rates
curl http://localhost:8091/api/v1/latest?base=USD
```

**Mock Provider 2** (ExchangeRatesAPI simulation):
```bash
# Test endpoint
curl http://localhost:8092/v1/latest

# Get EUR-based rates  
curl http://localhost:8092/v1/latest?base=EUR
```

See [MOCK_PROVIDERS.md](MOCK_PROVIDERS.md) for complete documentation.

### Environment Variables

Configure via `.env` file or export directly:

```bash
# Database
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=currency_exchange_db

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# External APIs (optional - mocks work without these)
FIXER_API_KEY=your-fixer-api-key
EXCHANGERATESAPI_KEY=your-exchangeratesapi-key

# Mock Providers
MOCK_PROVIDER_1_URL=http://mock-provider-1:8091
MOCK_PROVIDER_2_URL=http://mock-provider-2:8092
```

See [DOCKER.md](DOCKER.md) for detailed deployment guide.

## 🧪 Testing

### Test Coverage

The project includes comprehensive testing:

- **Unit Tests**: 106 tests
  - Service layer tests (CurrencyService, TrendAnalysisService)
  - Controller tests with @WebMvcTest (CurrencyController)
  - Mockito for dependency mocking
  
- **Integration Tests**: 23 tests
  - TestContainers with PostgreSQL 17-alpine
  - TestContainers with Redis 7-alpine
  - WireMock for external API mocking
  - End-to-end workflow tests

- **Total**: 129 tests (100% passing)

### Running Tests

**All tests:**
```bash
mvn test
```

**Unit tests only:**
```bash
mvn test -Dtest="*Test"
```

**Integration tests only:**
```bash
mvn test -Dtest="*IntegrationTest"
```

**With coverage report:**
```bash
mvn clean test jacoco:report
```

View coverage report at: `target/site/jacoco/index.html`

### Code Quality

**Run all quality checks:**
```bash
mvn clean verify checkstyle:check pmd:check
```

**Checkstyle (Google Java Style):**
```bash
mvn checkstyle:check
```

**PMD (Static Analysis):**
```bash
mvn pmd:check
```

**JaCoCo Coverage:**
```bash
mvn verify
```

**Quality Thresholds:**
- Line Coverage: 80% minimum
- Branch Coverage: 70% minimum
- Checkstyle: Google Java Style Guide
- PMD: Quickstart ruleset

### Test Requirements

- **Docker Desktop** must be running for integration tests (TestContainers)
- Tests automatically start PostgreSQL and Redis containers
- Containers are reused between test runs for performance

## ⚙️ Configuration

### Application Properties

Key configuration in `application.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/currency_exchange_db
spring.datasource.username=${POSTGRES_USER:postgres}
spring.datasource.password=${POSTGRES_PASSWORD:postgres}

# Redis Cache
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
cache.ttl.exchange-rates=7200000  # 2 hours in milliseconds

# External APIs
api.fixer.url=https://data.fixer.io/api/latest
api.fixer.key=${FIXER_API_KEY:test-key}
api.exchangeratesapi.url=https://api.exchangeratesapi.io/latest
api.exchangeratesapi.key=${EXCHANGERATESAPI_KEY:test-key}

# Mock Providers
api.mock.provider1.url=${MOCK_PROVIDER_1_URL:http://localhost:8091}
api.mock.provider2.url=${MOCK_PROVIDER_2_URL:http://localhost:8092}

# Scheduled Tasks
exchange.rates.update.cron=0 0 * * * *  # Every hour at :00 minutes

# Security
spring.security.user.name=admin
spring.security.user.password=admin123
```

### Production Configuration

See `application-prod.properties` for production-specific settings:
- HikariCP connection pooling
- Production security settings
- Performance optimizations
- Actuator endpoints configuration

Activate production profile:
```bash
java -jar target/currency-exchange-provider-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Scheduled Tasks

**Automatic Rate Updates:**
- Runs every hour at :00 minutes (configurable via `exchange.rates.update.cron`)
- Fetches rates from all 4 providers concurrently
- Selects best rate for each currency pair
- Updates database and Redis cache
- Logs execution time and results

**Manual Trigger:**
```bash
curl -X POST http://localhost:8080/api/v1/currencies/refresh \
  -u admin:admin123
```

## 🚀 Building for Production

### Maven Build

**Standard build:**
```bash
mvn clean package
```

**Skip tests:**
```bash
mvn clean package -DskipTests
```

**Run the JAR:**
```bash
java -jar target/currency-exchange-provider-0.0.1-SNAPSHOT.jar
```

### Docker Build

**Build image:**
```bash
docker-compose build currency-exchange-app
```

**Run container:**
```bash
docker-compose up -d currency-exchange-app
```

**Check logs:**
```bash
docker logs -f currency-exchange-app
```

## 🔧 Troubleshooting

### Common Issues

#### Application Won't Start

**Symptom**: Application fails to start with database connection error

**Solution**:
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Start PostgreSQL
docker-compose up -d postgres

# Wait for database to be ready
docker logs currency-exchange-db --tail 20

# Verify connection
docker exec currency-exchange-db psql -U postgres -c "\l"
```

#### Tests Failing

**Symptom**: Integration tests fail with "Could not find valid Docker environment"

**Solution**:
- Ensure Docker Desktop is running
- Check Docker daemon is accessible:
  ```bash
  docker ps
  ```
- TestContainers requires Docker to be running for integration tests

#### Redis Connection Issues

**Symptom**: Cache operations fail, logs show "Connection refused"

**Solution**:
```bash
# Check if Redis is running
docker ps | grep redis

# Start Redis
docker-compose up -d redis

# Test Redis connection
docker exec currency-exchange-redis redis-cli ping
# Should return: PONG
```

#### Mock Providers Not Working

**Symptom**: Rate aggregation fails, logs show connection timeout to mock providers

**Solution**:
```bash
# Check if mock providers are running
docker ps | grep mock-provider

# Start mock providers
docker-compose up -d mock-provider-1 mock-provider-2

# Test endpoints
curl http://localhost:8091/api/v1/latest
curl http://localhost:8092/v1/latest
```

#### 403 Forbidden Errors

**Symptom**: API returns 403 when calling secured endpoints

**Solution**:
- Verify you're using correct credentials
- Check user has required authority:
  ```bash
  # Check user authorities in database
  docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
    -c "SELECT u.username, r.name FROM users u JOIN user_roles ur ON u.id = ur.user_id JOIN roles r ON ur.role_id = r.id;"
  ```
- Admin endpoints require `admin:admin123`
- Trends endpoint requires `premium:admin123` or `admin:admin123`

#### Port Already in Use

**Symptom**: "Address already in use" error on startup

**Solution**:
```bash
# Check what's using the port
# Windows:
netstat -ano | findstr :8080

# Linux/Mac:
lsof -i :8080

# Stop conflicting service or change port in application.properties
server.port=8081
```

#### Checkstyle Violations

**Symptom**: Build warnings about code style violations

**Solution**:
- Review violations in `target/checkstyle-result.xml`
- Run Checkstyle check: `mvn checkstyle:check`
- Fix violations according to Google Java Style Guide
- Common issues:
  - Missing Javadoc: Add `@param` and `@return` tags
  - Star imports: Change `import java.util.*` to specific imports
  - Line length: Keep lines under 120 characters

#### External API Errors

**Symptom**: Rate refresh fails with "External API error"

**Solution**:
- Verify API keys are set correctly:
  ```bash
  # Windows PowerShell:
  $env:FIXER_API_KEY
  $env:EXCHANGERATESAPI_KEY
  
  # Linux/Mac:
  echo $FIXER_API_KEY
  echo $EXCHANGERATESAPI_KEY
  ```
- Check API quota hasn't been exceeded
- Verify network connectivity
- Application will fall back to mock providers if external APIs fail

### Getting Help

- **Documentation**: Check `DOCKER.md`, `MOCK_PROVIDERS.md` for detailed guides
- **Logs**: Always check application logs for detailed error messages
  ```bash
  docker logs currency-exchange-app --tail 50
  ```
- **Swagger UI**: Use http://localhost:8080/swagger-ui.html for API exploration
- **Database**: Use pgAdmin at http://localhost:5050 to inspect database state

## 📖 Additional Documentation

- **[DOCKER.md](DOCKER.md)** - Comprehensive Docker deployment guide
- **[MOCK_PROVIDERS.md](MOCK_PROVIDERS.md)** - Mock exchange rate providers documentation
- **[Swagger UI](http://localhost:8080/swagger-ui.html)** - Interactive API documentation
- **[OpenAPI Spec](http://localhost:8080/v3/api-docs)** - OpenAPI 3.0 specification

## 🎯 Key Features Implemented

✅ Multi-provider rate aggregation from 4 sources  
✅ Intelligent best-rate selection algorithm  
✅ Redis caching with 2-hour TTL and database fallback  
✅ Concurrent provider fetching with CompletableFuture  
✅ Role-based security (USER, PREMIUM_USER, ADMIN)  
✅ Trend analysis with configurable periods  
✅ Hourly scheduled updates via @Scheduled  
✅ 129 comprehensive tests (100% passing)  
✅ Docker Compose multi-container setup  
✅ Swagger/OpenAPI documentation  
✅ Code quality gates (Checkstyle, PMD, JaCoCo)  
✅ Liquibase database migrations  
✅ MapStruct type-safe DTO mapping  
✅ Global exception handling  
✅ Custom validators for currency codes and periods  

## 📝 License

This project is licensed under the MIT License.

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Make your changes
4. Run tests: `mvn test`
5. Run quality checks: `mvn checkstyle:check pmd:check`
6. Commit your changes: `git commit -m "Add your feature"`
7. Push to the branch: `git push origin feature/your-feature-name`
8. Open a Pull Request

### Code Standards

- Follow Google Java Style Guide
- Maintain test coverage above 80%
- Add Javadoc for public methods
- Use meaningful commit messages
- Update documentation for new features

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- TestContainers for integration testing support
- Fixer.io and ExchangeRatesAPI.io for exchange rate data
- All open-source contributors

---

**Built with ❤️ using Spring Boot 3.4.1 and Java 21**
