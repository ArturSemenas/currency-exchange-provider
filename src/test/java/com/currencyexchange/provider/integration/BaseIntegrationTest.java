package com.currencyexchange.provider.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests with TestContainers
 * Provides PostgreSQL container for all integration tests
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
    },
    classes = {
        com.currencyexchange.provider.CurrencyExchangeProviderApplication.class
    }
)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    static PostgreSQLContainer<?> postgresContainer;

    static {
        postgresContainer = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL configuration
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // Disable Liquibase for tests (we'll use JPA to create schema)
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // API provider configuration (test values)
        registry.add("api.fixer.url", () -> "https://data.fixer.io/api");
        registry.add("api.fixer.key", () -> "test-key");
        registry.add("api.exchangeratesapi.url", () -> "https://v6.exchangerate-api.com/v6");
        registry.add("api.exchangeratesapi.key", () -> "test-key");
        registry.add("api.mock.provider1.url", () -> "http://localhost:8091");
        registry.add("api.mock.provider2.url", () -> "http://localhost:8092");
        
        // Scheduling configuration
        registry.add("exchange.rates.update.cron", () -> "0 0 * * * *"); // Dummy cron (every hour)
        registry.add("spring.task.scheduling.enabled", () -> "false");
        
        // Disable Redis cache for tests by default (can be overridden in specific tests)
        registry.add("spring.cache.type", () -> "none");
        
        // Disable actuator health checks
        registry.add("management.health.redis.enabled", () -> "false");
    }
}
