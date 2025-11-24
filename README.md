# Currency Exchange Rates Provider Service

A Spring Boot application that provides up-to-date currency exchange rates from multiple providers (fixer.io and others). The service supports dynamic currency list management via REST API with hourly rate updates.

## Features

- RESTful API for currency and exchange rate management
- Integration with multiple exchange rate providers (fixer.io)
- Hourly automatic update of exchange rates
- PostgreSQL database for data persistence
- Liquibase for database migrations
- Spring Security with Basic Authentication
- Swagger/OpenAPI documentation
- Docker Compose for easy database setup

## Technology Stack

- **Java 21**
- **Spring Boot 3.4.1**
- **Maven** - Build and dependency management
- **PostgreSQL** - Database
- **Liquibase** - Database migrations
- **Spring Security** - Authentication and authorization
- **MapStruct** - DTO mapping
- **Lombok** - Reduce boilerplate code
- **Swagger/OpenAPI** - API documentation
- **Docker Compose** - Container orchestration

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose
- DBeaver or any database management tool (optional)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd currency-exchange-provider
```

### 2. Set Up Environment Variables

Create a `.env` file or export the following environment variables:

```bash
export FIXER_API_KEY=your-fixer-io-api-key
```

To get a Fixer.io API key, sign up at [https://fixer.io/](https://fixer.io/)

### 3. Start PostgreSQL Database

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database on port `5432`
- pgAdmin on port `5050` (accessible at http://localhost:5050)
  - Email: admin@admin.com
  - Password: admin

### 4. Build the Application

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Documentation

Once the application is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

### Available Endpoints

#### Currency Management

- **GET** `/api/v1/currencies` - Get all supported currencies
- **POST** `/api/v1/currencies?code={code}&name={name}` - Add a new currency

## Database Access

### Using pgAdmin

1. Navigate to http://localhost:5050
2. Login with credentials (admin@admin.com / admin)
3. Create a new server connection:
   - Host: postgres
   - Port: 5432
   - Database: currency_exchange_db
   - Username: postgres
   - Password: postgres

### Using DBeaver or other tools

- Host: localhost
- Port: 5432
- Database: currency_exchange_db
- Username: postgres
- Password: postgres

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/currency_exchange_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# External API
api.fixer.url=https://data.fixer.io/api/latest
api.fixer.key=${FIXER_API_KEY}

# Exchange Rate Update Schedule (hourly)
exchange.rates.update.cron=0 0 * * * *
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/currencyexchange/provider/
│   │       ├── controller/      # REST controllers
│   │       ├── model/           # JPA entities
│   │       ├── repository/      # Data repositories
│   │       ├── service/         # Business logic
│   │       └── CurrencyExchangeProviderApplication.java
│   └── resources/
│       ├── db/changelog/        # Liquibase migrations
│       └── application.properties
└── test/
    └── java/                    # Test files
```

## Development

### Running Tests

```bash
mvn test
```

### Building for Production

```bash
mvn clean package
java -jar target/currency-exchange-provider-0.0.1-SNAPSHOT.jar
```

## Scheduled Tasks

The application includes a scheduled task that:
- Updates exchange rates every hour (configurable via `exchange.rates.update.cron`)
- Fetches data from configured providers (fixer.io, etc.)
- Stores the latest rates in the database

## Next Steps

You can now extend this application by:

1. Implementing the exchange rate provider services
2. Adding more REST endpoints for querying exchange rates
3. Implementing caching for better performance
4. Adding more external provider integrations
5. Implementing rate comparison logic across providers
6. Adding authentication and user management
7. Creating DTO classes and mappers with MapStruct

## Troubleshooting

### Database Connection Issues

If you encounter database connection errors:
1. Ensure Docker containers are running: `docker-compose ps`
2. Check database logs: `docker-compose logs postgres`
3. Verify connection settings in `application.properties`

### API Key Issues

If exchange rate updates fail:
1. Verify your Fixer.io API key is correct
2. Check the API key environment variable is set
3. Review application logs for detailed error messages

## License

This project is licensed under the MIT License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
