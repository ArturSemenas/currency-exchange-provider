package com.currencyexchange.provider.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test using external Docker containers.
 * Run `docker-compose -f docker-compose.test.yml up -d` before running this test.
 *
 * <p>This is a workaround for Docker Desktop 29.x compatibility issues with TestContainers.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "spring.cache.type=none"
    }
)
class ExternalContainersIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Use external containers started via docker-compose.test.yml
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://localhost:5433/testdb");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Disable Liquibase for tests
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // API provider configuration
        registry.add("api.fixer.url", () -> "https://data.fixer.io/api");
        registry.add("api.fixer.key", () -> "test-key");
        registry.add("api.exchangeratesapi.url", () -> "https://v6.exchangerate-api.com/v6");
        registry.add("api.exchangeratesapi.key", () -> "test-key");
        registry.add("api.mock.provider1.url", () -> "http://localhost:8091");
        registry.add("api.mock.provider2.url", () -> "http://localhost:8092");

        // Disable scheduling
        registry.add("spring.task.scheduling.enabled", () -> "false");
    }

    /**
     * Test that PostgreSQL container is accessible.
     */
    @Test
    void testPostgresConnection() {
        assertNotNull(jdbcTemplate, "JdbcTemplate should be injected");

        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result, "PostgreSQL should return 1");

        System.out.println("✅ PostgreSQL connection successful!");
    }

    /**
     * Test that database schema is created.
     */
    @Test
    void testDatabaseSchemaCreated() {
        String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                     "WHERE table_schema = 'public' AND table_name = 'currencies'";
        
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        assertEquals(1, count, "Currencies table should exist");

        System.out.println("✅ Database schema created successfully!");
    }

    /**
     * Test that application context loads successfully.
     */
    @Test
    void testApplicationContextLoads() {
        assertNotNull(jdbcTemplate);
        System.out.println("✅ Spring application context loaded successfully!");
    }
}
