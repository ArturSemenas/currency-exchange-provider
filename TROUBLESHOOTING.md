# Troubleshooting Guide - Currency Exchange Provider

Comprehensive guide for diagnosing and resolving common issues.

## Table of Contents

1. [Application Startup Issues](#application-startup-issues)
2. [Database Problems](#database-problems)
3. [Redis Cache Issues](#redis-cache-issues)
4. [Docker Container Issues](#docker-container-issues)
5. [Authentication & Security](#authentication--security)
6. [External API Integration](#external-api-integration)
7. [Testing Issues](#testing-issues)
8. [Build & Compilation](#build--compilation)
9. [Performance Issues](#performance-issues)
10. [Debugging Tips](#debugging-tips)

---

## Application Startup Issues

### Issue: Application fails to start with database connection error

**Symptoms:**
```
com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool
Caused by: org.postgresql.util.PSQLException: Connection to localhost:5432 refused
```

**Solution:**

1. **Check if PostgreSQL is running:**
   ```bash
   docker ps | grep postgres
   ```

2. **Start PostgreSQL if not running:**
   ```bash
   docker-compose up -d postgres
   ```

3. **Wait for database to be ready:**
   ```bash
   # Check logs for "database system is ready to accept connections"
   docker logs currency-exchange-db --tail 20
   ```

4. **Verify database exists:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -c "\l"
   ```

5. **Test connection manually:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db -c "SELECT 1;"
   ```

**Prevention:**
- Always start Docker containers before running the application
- Use `docker-compose up -d` to start all infrastructure

---

### Issue: Port 8080 already in use

**Symptoms:**
```
Web server failed to start. Port 8080 was already in use.
```

**Solution:**

1. **Find process using port 8080:**
   
   **Windows (PowerShell):**
   ```powershell
   netstat -ano | findstr :8080
   ```
   
   **Linux/Mac:**
   ```bash
   lsof -i :8080
   ```

2. **Stop the conflicting process:**
   
   **Windows:**
   ```powershell
   # Find PID from netstat output, then:
   Stop-Process -Id <PID> -Force
   ```
   
   **Linux/Mac:**
   ```bash
   kill -9 <PID>
   ```

3. **Or change application port:**
   
   Edit `application.properties`:
   ```properties
   server.port=8081
   ```
   
   Or run with different port:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
   ```

---

### Issue: Liquibase migration fails

**Symptoms:**
```
liquibase.exception.DatabaseException: 
ERROR: relation "currencies" already exists
```

**Solution:**

1. **Check migration status:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;"
   ```

2. **Drop database and recreate (DEVELOPMENT ONLY):**
   ```bash
   docker-compose down -v
   docker-compose up -d postgres
   # Wait 10 seconds
   mvn spring-boot:run
   ```

3. **Manual cleanup (if needed):**
   ```bash
   docker exec -it currency-exchange-db psql -U postgres -d currency_exchange_db
   # Then in psql:
   DROP TABLE IF EXISTS currencies CASCADE;
   DROP TABLE IF EXISTS exchange_rates CASCADE;
   DROP TABLE IF EXISTS users CASCADE;
   DROP TABLE IF EXISTS roles CASCADE;
   DROP TABLE IF EXISTS user_roles CASCADE;
   DROP TABLE IF EXISTS databasechangelog CASCADE;
   DROP TABLE IF EXISTS databasechangeloglock CASCADE;
   \q
   ```

---

## Database Problems

### Issue: Cannot connect to pgAdmin

**Symptoms:**
- pgAdmin page not loading at http://localhost:5050
- "Unable to connect" error

**Solution:**

1. **Check pgAdmin container status:**
   ```bash
   docker ps | grep pgadmin
   ```

2. **Restart pgAdmin:**
   ```bash
   docker-compose restart pgadmin
   ```

3. **Check pgAdmin logs:**
   ```bash
   docker logs pgadmin
   ```

4. **Verify port binding:**
   ```bash
   netstat -ano | findstr :5050  # Windows
   lsof -i :5050                 # Linux/Mac
   ```

5. **Access pgAdmin:**
   - URL: http://localhost:5050
   - Email: `admin@admin.com`
   - Password: `admin`

---

### Issue: "Currency not found" after adding it

**Symptoms:**
- POST `/api/v1/currencies?currency=JPY` returns 201
- GET `/api/v1/currencies` doesn't show JPY

**Solution:**

1. **Check database directly:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "SELECT * FROM currencies;"
   ```

2. **Check application logs:**
   ```bash
   docker logs currency-exchange-app --tail 50 | grep -i "currency"
   ```

3. **Verify transaction committed:**
   - Check for rollback in logs
   - Ensure no validation errors
   - Confirm service method is @Transactional

4. **Clear cache and retry:**
   ```bash
   docker exec currency-exchange-redis redis-cli FLUSHALL
   ```

---

### Issue: Exchange rates not updating

**Symptoms:**
- Manual refresh returns success but rates are old
- Hourly scheduled job not working

**Solution:**

1. **Check scheduler is enabled:**
   
   Verify `@EnableScheduling` in main application class:
   ```java
   @SpringBootApplication
   @EnableScheduling  // Must be present
   public class CurrencyExchangeProviderApplication
   ```

2. **Check cron expression:**
   
   In `application.properties`:
   ```properties
   exchange.rates.update.cron=0 0 * * * *  # Every hour at :00
   ```

3. **Manually trigger refresh:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/currencies/refresh -u admin:admin123
   ```

4. **Check application logs for scheduler execution:**
   ```bash
   docker logs currency-exchange-app | grep -i "scheduled\|refresh\|aggregat"
   ```

5. **Verify providers are accessible:**
   ```bash
   # Test mock providers
   curl http://localhost:8091/api/v1/latest
   curl http://localhost:8092/v1/latest
   ```

---

## Redis Cache Issues

### Issue: Redis connection refused

**Symptoms:**
```
org.springframework.data.redis.RedisConnectionFailureException: 
Unable to connect to Redis; nested exception is 
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**Solution:**

1. **Check Redis container status:**
   ```bash
   docker ps | grep redis
   ```

2. **Start Redis:**
   ```bash
   docker-compose up -d redis
   ```

3. **Test Redis connection:**
   ```bash
   docker exec currency-exchange-redis redis-cli ping
   # Should return: PONG
   ```

4. **Check Redis logs:**
   ```bash
   docker logs currency-exchange-redis --tail 30
   ```

5. **Verify Redis configuration:**
   
   In `application.properties`:
   ```properties
   spring.data.redis.host=localhost  # or redis for Docker
   spring.data.redis.port=6379
   spring.data.redis.timeout=60000
   ```

---

### Issue: Cache not working (always querying database)

**Symptoms:**
- Slow response times
- Database queries on every request
- Logs show "Cache miss" every time

**Solution:**

1. **Check Redis is running and healthy:**
   ```bash
   docker ps | grep redis
   docker exec currency-exchange-redis redis-cli ping
   ```

2. **Verify cache keys exist:**
   ```bash
   docker exec currency-exchange-redis redis-cli KEYS "rates:*"
   ```

3. **Check cache TTL:**
   ```bash
   docker exec currency-exchange-redis redis-cli TTL "rates:USD"
   # Should return positive number (seconds remaining)
   ```

4. **Inspect cached data:**
   ```bash
   docker exec currency-exchange-redis redis-cli HGETALL "rates:USD"
   ```

5. **Clear cache and trigger refresh:**
   ```bash
   docker exec currency-exchange-redis redis-cli FLUSHALL
   curl -X POST http://localhost:8080/api/v1/currencies/refresh -u admin:admin123
   ```

6. **Check application logs for cache operations:**
   ```bash
   docker logs currency-exchange-app | grep -i "cache\|redis"
   ```

---

### Issue: Cache eviction not working

**Symptoms:**
- Old rates still returned after refresh
- Cache shows expired data

**Solution:**

1. **Manual cache eviction:**
   ```bash
   docker exec currency-exchange-redis redis-cli FLUSHALL
   ```

2. **Check TTL configuration:**
   
   In `application.properties`:
   ```properties
   cache.ttl.exchange-rates=7200000  # 2 hours in milliseconds
   ```

3. **Verify eviction in code:**
   - Check `ExchangeRateCacheService.evictAll()` is called after refresh
   - Ensure `@CacheEvict` annotations are present if using Spring Cache

4. **Monitor cache size:**
   ```bash
   docker exec currency-exchange-redis redis-cli INFO memory
   ```

---

## Docker Container Issues

### Issue: Container exits immediately after starting

**Symptoms:**
```bash
docker ps  # Shows container missing
docker ps -a  # Shows container with "Exited (1)" status
```

**Solution:**

1. **Check container logs:**
   ```bash
   docker logs currency-exchange-app
   docker logs currency-exchange-db
   docker logs currency-exchange-redis
   ```

2. **Check exit code:**
   ```bash
   docker inspect currency-exchange-app | grep -i exitcode
   ```

3. **Common causes and fixes:**
   
   **Exit Code 1 (Application Error):**
   - Check application logs for Java exceptions
   - Verify database connection
   - Check environment variables

   **Exit Code 137 (OOM Killed):**
   - Increase Docker memory limit
   - Add JVM memory settings:
     ```dockerfile
     ENV JAVA_OPTS="-Xmx512m -Xms256m"
     ```

   **Exit Code 139 (Segmentation Fault):**
   - Rebuild Docker image
   - Check Java version compatibility

4. **Restart container:**
   ```bash
   docker-compose restart currency-exchange-app
   ```

---

### Issue: Health check failing

**Symptoms:**
```bash
docker ps
# Shows "unhealthy" status
```

**Solution:**

1. **Check health check endpoint:**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

2. **View health check logs:**
   ```bash
   docker inspect currency-exchange-app | grep -i health -A 10
   ```

3. **Common health check failures:**
   
   **Database Down:**
   ```json
   {
     "status": "DOWN",
     "components": {
       "db": {
         "status": "DOWN",
         "details": "Cannot get connection"
       }
     }
   }
   ```
   - Solution: Start PostgreSQL container

   **Redis Down:**
   ```json
   {
     "status": "DOWN",
     "components": {
       "redis": {
         "status": "DOWN"
       }
     }
   }
   ```
   - Solution: Start Redis container

4. **Disable health check temporarily:**
   
   Edit `docker-compose.yml`:
   ```yaml
   healthcheck:
     disable: true  # Temporary debugging only
   ```

---

### Issue: Mock providers not accessible

**Symptoms:**
- Rate aggregation fails with connection timeout
- `curl http://localhost:8091/api/v1/latest` fails

**Solution:**

1. **Check mock provider containers:**
   ```bash
   docker ps | grep mock-provider
   ```

2. **Start mock providers:**
   ```bash
   docker-compose up -d mock-provider-1 mock-provider-2
   ```

3. **Test endpoints:**
   ```bash
   curl http://localhost:8091/api/v1/latest
   curl http://localhost:8092/v1/latest
   ```

4. **Check mock provider logs:**
   ```bash
   docker logs mock-provider-1 --tail 30
   docker logs mock-provider-2 --tail 30
   ```

5. **Rebuild mock providers:**
   ```bash
   docker-compose build mock-provider-1 mock-provider-2
   docker-compose up -d mock-provider-1 mock-provider-2
   ```

6. **Verify network connectivity:**
   ```bash
   docker exec currency-exchange-app wget -q -O - http://mock-provider-1:8091/api/v1/latest
   ```

---

## Authentication & Security

### Issue: 401 Unauthorized on all requests

**Symptoms:**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required"
}
```

**Solution:**

1. **Verify credentials are provided:**
   
   **curl:**
   ```bash
   curl -u admin:admin123 http://localhost:8080/api/v1/currencies
   ```
   
   **PowerShell:**
   ```powershell
   $headers = @{ 'Authorization' = 'Basic YWRtaW46YWRtaW4xMjM=' }
   Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/currencies' -Headers $headers
   ```

2. **Check Base64 encoding:**
   ```bash
   echo -n 'admin:admin123' | base64
   # Should output: YWRtaW46YWRtaW4xMjM=
   ```

3. **Verify users exist in database:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "SELECT username, enabled FROM users;"
   ```

4. **Check password encryption:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "SELECT username, password FROM users WHERE username='admin';"
   # Password should start with $2a$ (BCrypt)
   ```

---

### Issue: 403 Forbidden - Access Denied

**Symptoms:**
```json
{
  "timestamp": "2025-11-26T15:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

**Solution:**

1. **Check user has required authority:**
   
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db -c "
   SELECT u.username, r.name as authority
   FROM users u
   JOIN user_roles ur ON u.id = ur.user_id
   JOIN roles r ON ur.role_id = r.id
   WHERE u.username = 'user';"
   ```

2. **Verify endpoint access requirements:**
   
   | Endpoint | Required Authority |
   |----------|-------------------|
   | POST `/api/v1/currencies` | ADMIN |
   | POST `/api/v1/currencies/refresh` | ADMIN |
   | GET `/api/v1/currencies/trends` | ADMIN or PREMIUM_USER |

3. **Use correct credentials:**
   
   ```bash
   # For ADMIN endpoints:
   curl -u admin:admin123 <endpoint>
   
   # For PREMIUM endpoints:
   curl -u premium:admin123 <endpoint>
   ```

4. **Check SecurityConfig:**
   - Verify authority checks in SecurityFilterChain
   - Confirm no ROLE_ prefix (using simple authorities)

5. **Application logs:**
   ```bash
   docker logs currency-exchange-app | grep -i "access denied\|403\|forbidden"
   ```

---

### Issue: User cannot log in with correct password

**Symptoms:**
- Correct username and password but get 401
- Works for other users

**Solution:**

1. **Check if user is enabled:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "SELECT username, enabled FROM users WHERE username='user';"
   ```

2. **Enable user if disabled:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "UPDATE users SET enabled = true WHERE username='user';"
   ```

3. **Verify password hash:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "SELECT username, password FROM users WHERE username='user';"
   # Should start with $2a$12$ (BCrypt with strength 12)
   ```

4. **Reset password (if needed):**
   
   Generate new BCrypt hash (strength 12):
   ```java
   BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
   String hash = encoder.encode("admin123");
   System.out.println(hash);
   ```
   
   Update in database:
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db \
     -c "UPDATE users SET password='$2a$12$NEW_HASH_HERE' WHERE username='user';"
   ```

---

## External API Integration

### Issue: External API calls timing out

**Symptoms:**
```
java.net.SocketTimeoutException: Read timed out
```

**Solution:**

1. **Check network connectivity:**
   ```bash
   curl -v https://data.fixer.io/api/latest?access_key=test
   ```

2. **Verify API keys are set:**
   
   **Windows:**
   ```powershell
   $env:FIXER_API_KEY
   $env:EXCHANGERATESAPI_KEY
   ```
   
   **Linux/Mac:**
   ```bash
   echo $FIXER_API_KEY
   echo $EXCHANGERATESAPI_KEY
   ```

3. **Check timeout configuration:**
   
   In `RestClientConfig`:
   ```java
   @Bean
   public RestTemplate restTemplate() {
       SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
       factory.setConnectTimeout(10000);  // 10 seconds
       factory.setReadTimeout(30000);     // 30 seconds
       return new RestTemplate(factory);
   }
   ```

4. **Test with mock providers instead:**
   - Mock providers work without API keys
   - Faster response times
   - See [MOCK_PROVIDERS.md](MOCK_PROVIDERS.md)

5. **Increase timeout (temporary):**
   ```java
   factory.setReadTimeout(60000);  // 60 seconds
   ```

---

### Issue: External API returns error 401 Unauthorized

**Symptoms:**
```
External API error from fixer.io: 401 Unauthorized
```

**Solution:**

1. **Verify API key validity:**
   - Log in to https://fixer.io/dashboard
   - Check subscription status
   - Verify key hasn't expired

2. **Test API key manually:**
   ```bash
   curl "https://data.fixer.io/api/latest?access_key=YOUR_KEY"
   ```

3. **Check API key format:**
   - Should be alphanumeric string
   - No extra spaces or quotes
   - Properly set in environment variable

4. **Application uses test key if not set:**
   ```properties
   api.fixer.key=${FIXER_API_KEY:test-key}
   # Falls back to "test-key" which will fail
   ```

5. **Fallback to mock providers:**
   - Application automatically uses mock providers if external APIs fail
   - Check logs for "Using mock provider" messages

---

### Issue: Rate aggregation selects wrong rate

**Symptoms:**
- Conversion uses unexpectedly low rate
- Different rate than shown by individual providers

**Solution:**

1. **Understand rate selection:**
   - System selects HIGHEST rate (best for customer)
   - Compares rates from all 4 providers
   - Uses `Stream.max()` with natural order

2. **Check provider responses:**
   ```bash
   curl http://localhost:8091/api/v1/latest
   curl http://localhost:8092/v1/latest
   curl "https://data.fixer.io/api/latest?access_key=YOUR_KEY"
   ```

3. **Verify aggregation logic:**
   
   In `RateAggregationService`:
   ```java
   BigDecimal bestRate = rates.stream()
       .map(r -> r.getRate())
       .max(Comparator.naturalOrder())
       .orElse(BigDecimal.ZERO);
   ```

4. **Check database for stored rates:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db -c "
   SELECT base_currency, target_currency, rate, provider, timestamp
   FROM exchange_rates
   WHERE base_currency='USD' AND target_currency='EUR'
   ORDER BY timestamp DESC LIMIT 5;"
   ```

5. **Manually trigger refresh and compare:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/currencies/refresh -u admin:admin123
   ```

---

## Testing Issues

### Issue: Integration tests fail with "Could not find Docker environment"

**Symptoms:**
```
org.testcontainers.DockerClientException: 
Could not find a valid Docker environment
```

**Solution:**

1. **Ensure Docker Desktop is running:**
   
   **Windows:**
   - Check system tray for Docker icon
   - Should show "Docker Desktop is running"

   **Linux:**
   ```bash
   sudo systemctl status docker
   ```

2. **Verify Docker daemon is accessible:**
   ```bash
   docker ps
   # Should list running containers, not error
   ```

3. **Check Docker socket permissions (Linux):**
   ```bash
   sudo usermod -aG docker $USER
   # Log out and back in
   ```

4. **Set DOCKER_HOST environment variable (if needed):**
   ```bash
   export DOCKER_HOST=unix:///var/run/docker.sock
   ```

5. **Run tests:**
   ```bash
   mvn test
   ```

---

### Issue: Tests fail with H2 database errors

**Symptoms:**
```
org.h2.jdbc.JdbcSQLSyntaxErrorException: 
Table "CURRENCIES" not found
```

**Solution:**

1. **Check test configuration:**
   
   In `src/test/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:h2:mem:testdb
   spring.datasource.driver-class-name=org.h2.Driver
   spring.jpa.hibernate.ddl-auto=create-drop
   ```

2. **Verify H2 dependency:**
   
   In `pom.xml`:
   ```xml
   <dependency>
       <groupId>com.h2database</groupId>
       <artifactId>h2</artifactId>
       <scope>test</scope>
   </dependency>
   ```

3. **Disable Liquibase for tests:**
   ```properties
   spring.liquibase.enabled=false
   ```

4. **Enable JPA DDL auto for tests:**
   ```properties
   spring.jpa.hibernate.ddl-auto=create-drop
   ```

---

### Issue: WireMock tests fail with connection refused

**Symptoms:**
```
java.net.ConnectException: Connection refused
```

**Solution:**

1. **Check WireMock server is started:**
   
   In test class:
   ```java
   @BeforeEach
   void setup() {
       wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
       wireMockServer.start();
       WireMock.configureFor("localhost", wireMockServer.port());
   }
   ```

2. **Verify test uses WireMock port:**
   ```java
   String baseUrl = "http://localhost:" + wireMockServer.port();
   ```

3. **Check WireMock stub is configured:**
   ```java
   stubFor(get(urlEqualTo("/api/v1/latest"))
       .willReturn(aResponse()
           .withStatus(200)
           .withBody(jsonResponse)));
   ```

4. **Stop server in @AfterEach:**
   ```java
   @AfterEach
   void teardown() {
       if (wireMockServer != null) {
           wireMockServer.stop();
       }
   }
   ```

---

## Build & Compilation

### Issue: Maven build fails with compiler errors

**Symptoms:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin
```

**Solution:**

1. **Verify Java version:**
   ```bash
   java -version
   # Should show Java 21
   ```

2. **Set JAVA_HOME:**
   
   **Windows:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
   ```
   
   **Linux/Mac:**
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
   ```

3. **Clean and rebuild:**
   ```bash
   mvn clean install -U
   ```

4. **Check Maven version:**
   ```bash
   mvn -version
   # Should be 3.9 or higher
   ```

---

### Issue: Lombok annotations not working

**Symptoms:**
- "Cannot resolve symbol" for generated methods
- `@Data` doesn't generate getters/setters

**Solution:**

1. **Verify Lombok dependency:**
   
   In `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.projectlombok</groupId>
       <artifactId>lombok</artifactId>
       <scope>provided</scope>
   </dependency>
   ```

2. **Enable annotation processing in IDE:**
   
   **IntelliJ IDEA:**
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"

   **Eclipse:**
   - Install Lombok from https://projectlombok.org/download
   - Run `java -jar lombok.jar`

3. **Clean and rebuild:**
   ```bash
   mvn clean compile
   ```

---

### Issue: MapStruct mappers not generated

**Symptoms:**
- `CurrencyMapperImpl` not found
- "Cannot resolve symbol" for mapper implementation

**Solution:**

1. **Verify MapStruct dependencies:**
   
   In `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.mapstruct</groupId>
       <artifactId>mapstruct</artifactId>
       <version>1.6.3</version>
   </dependency>
   <dependency>
       <groupId>org.mapstruct</groupId>
       <artifactId>mapstruct-processor</artifactId>
       <version>1.6.3</version>
       <scope>provided</scope>
   </dependency>
   ```

2. **Check annotation processor configuration:**
   ```xml
   <annotationProcessorPaths>
       <path>
           <groupId>org.projectlombok</groupId>
           <artifactId>lombok</artifactId>
           <version>${lombok.version}</version>
       </path>
       <path>
           <groupId>org.mapstruct</groupId>
           <artifactId>mapstruct-processor</artifactId>
           <version>1.6.3</version>
       </path>
   </annotationProcessorPaths>
   ```

3. **Compile project:**
   ```bash
   mvn clean compile
   ```

4. **Check generated sources:**
   - Look in `target/generated-sources/annotations/`
   - Should contain `CurrencyMapperImpl.java`

---

### Issue: Checkstyle violations failing build

**Symptoms:**
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-checkstyle-plugin
```

**Solution:**

1. **View violations:**
   ```bash
   mvn checkstyle:check
   # Check target/checkstyle-result.xml
   ```

2. **Common violations and fixes:**
   
   **Missing Javadoc:**
   ```java
   /**
    * Retrieves all currencies.
    *
    * @return list of all currencies
    */
   public List<Currency> getAllCurrencies() {
   ```

   **Star imports:**
   ```java
   // Bad:
   import java.util.*;
   
   // Good:
   import java.util.List;
   import java.util.Optional;
   ```

   **Line too long (>120 chars):**
   - Break into multiple lines
   - Use string concatenation

3. **Gradual adoption mode:**
   
   In `pom.xml`:
   ```xml
   <failOnViolation>false</failOnViolation>
   <!-- Build succeeds with warnings -->
   ```

4. **Skip Checkstyle temporarily:**
   ```bash
   mvn clean install -Dcheckstyle.skip=true
   ```

---

## Performance Issues

### Issue: Slow API response times

**Symptoms:**
- Requests take >5 seconds
- Timeout errors under load

**Solution:**

1. **Check if Redis cache is working:**
   ```bash
   docker exec currency-exchange-redis redis-cli KEYS "rates:*"
   # Should show cached rates
   ```

2. **Monitor database queries:**
   
   Enable SQL logging in `application.properties`:
   ```properties
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   logging.level.org.hibernate.SQL=DEBUG
   ```

3. **Check database indexes:**
   ```bash
   docker exec currency-exchange-db psql -U postgres -d currency_exchange_db -c "
   SELECT tablename, indexname
   FROM pg_indexes
   WHERE schemaname = 'public'
   ORDER BY tablename, indexname;"
   ```

4. **Enable connection pooling:**
   
   In `application-prod.properties`:
   ```properties
   spring.datasource.hikari.maximum-pool-size=10
   spring.datasource.hikari.minimum-idle=5
   spring.datasource.hikari.connection-timeout=20000
   ```

5. **Profile application:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.jvmArguments="-XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=recording.jfr"
   ```

---

### Issue: High memory usage

**Symptoms:**
- Application using >1GB RAM
- OutOfMemoryError

**Solution:**

1. **Set JVM memory limits:**
   
   In `Dockerfile`:
   ```dockerfile
   ENV JAVA_OPTS="-Xmx512m -Xms256m"
   ```

2. **Enable GC logging:**
   ```properties
   -Xlog:gc*:file=gc.log
   ```

3. **Check for memory leaks:**
   ```bash
   jmap -heap <PID>
   jmap -histo <PID> | head -20
   ```

4. **Monitor with Docker:**
   ```bash
   docker stats currency-exchange-app
   ```

5. **Optimize cache size:**
   
   Set max entries in Redis:
   ```bash
   docker exec currency-exchange-redis redis-cli CONFIG SET maxmemory 256mb
   docker exec currency-exchange-redis redis-cli CONFIG SET maxmemory-policy allkeys-lru
   ```

---

## Debugging Tips

### Enable Debug Logging

**application.properties:**
```properties
# Root level
logging.level.root=INFO

# Application package
logging.level.com.currencyexchange=DEBUG

# Spring components
logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.security=DEBUG

# Database
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Redis
logging.level.org.springframework.data.redis=DEBUG
```

### View Live Logs

**Docker:**
```bash
# Follow logs
docker logs -f currency-exchange-app

# Last 100 lines
docker logs --tail 100 currency-exchange-app

# Since timestamp
docker logs --since 2025-11-26T15:00:00 currency-exchange-app

# Search logs
docker logs currency-exchange-app | grep ERROR
```

### Database Debugging

**View table contents:**
```bash
docker exec currency-exchange-db psql -U postgres -d currency_exchange_db -c "
SELECT * FROM currencies;
SELECT * FROM exchange_rates ORDER BY timestamp DESC LIMIT 10;
SELECT * FROM users;
SELECT * FROM roles;
"
```

**Check table structure:**
```bash
docker exec currency-exchange-db psql -U postgres -d currency_exchange_db -c "\d currencies"
```

### Network Debugging

**Test container connectivity:**
```bash
# From main app to database
docker exec currency-exchange-app ping -c 3 postgres

# From main app to Redis
docker exec currency-exchange-app ping -c 3 redis

# From main app to mock provider
docker exec currency-exchange-app wget -q -O - http://mock-provider-1:8091/api/v1/latest
```

### Performance Profiling

**Enable actuator metrics:**

`application.properties`:
```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

**View metrics:**
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## Getting Additional Help

### Resources

- **Documentation**: Check [README.md](README.md), [DOCKER.md](DOCKER.md), [MOCK_PROVIDERS.md](MOCK_PROVIDERS.md)
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Logs**: Always start with application logs
- **Database**: Use pgAdmin for visual inspection

### Reporting Issues

When reporting issues, include:

1. **Error message** (full stack trace)
2. **Steps to reproduce**
3. **Environment info**:
   ```bash
   java -version
   mvn -version
   docker -v
   docker-compose -v
   ```
4. **Application logs** (last 50 lines)
5. **Docker status**: `docker ps -a`
6. **Configuration** (sanitized, no secrets)

### Quick Diagnostic Script

**PowerShell:**
```powershell
Write-Host "=== Environment ===" -ForegroundColor Cyan
java -version
mvn -version
docker -v

Write-Host "`n=== Docker Status ===" -ForegroundColor Cyan
docker ps

Write-Host "`n=== Application Health ===" -ForegroundColor Cyan
try { Invoke-RestMethod http://localhost:8080/actuator/health } catch { Write-Host "App not running" }

Write-Host "`n=== Redis Status ===" -ForegroundColor Cyan
docker exec currency-exchange-redis redis-cli ping

Write-Host "`n=== Database Status ===" -ForegroundColor Cyan
docker exec currency-exchange-db psql -U postgres -c "SELECT 1;"

Write-Host "`n=== Mock Providers ===" -ForegroundColor Cyan
try { Invoke-RestMethod http://localhost:8091/api/v1/latest } catch { Write-Host "Mock 1 not running" }
try { Invoke-RestMethod http://localhost:8092/v1/latest } catch { Write-Host "Mock 2 not running" }
```

---

**Still having issues?** Check the application logs first - they usually contain detailed error messages that point to the root cause.
