package com.currencyexchange.provider.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple health check test for TestContainers.
 * Tests basic Docker connectivity and container startup.
 */
class TestContainersHealthCheckTest {

    /**
     * Test that PostgreSQL container can start successfully.
     */
    @Test
    void testPostgreSQLContainerStarts() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")) {
            
            // Start container
            postgres.start();
            
            // Verify container is running
            assertTrue(postgres.isRunning(), "PostgreSQL container should be running");
            assertNotNull(postgres.getJdbcUrl(), "JDBC URL should not be null");
            assertTrue(postgres.getJdbcUrl().contains("testdb"), "JDBC URL should contain database name");
            
            System.out.println("✅ PostgreSQL container started successfully!");
            System.out.println("JDBC URL: " + postgres.getJdbcUrl());
            System.out.println("Username: " + postgres.getUsername());
        }
    }

    /**
     * Test that Redis container can start successfully.
     */
    @Test
    void testRedisContainerStarts() {
        try (GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)) {
            
            // Start container
            redis.start();
            
            // Verify container is running
            assertTrue(redis.isRunning(), "Redis container should be running");
            assertNotNull(redis.getHost(), "Host should not be null");
            assertTrue(redis.getMappedPort(6379) > 0, "Mapped port should be positive");
            
            System.out.println("✅ Redis container started successfully!");
            System.out.println("Host: " + redis.getHost());
            System.out.println("Port: " + redis.getMappedPort(6379));
        }
    }

    /**
     * Test that multiple containers can run simultaneously.
     */
    @Test
    void testMultipleContainersCanRun() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
             GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)) {
            
            // Start both containers
            postgres.start();
            redis.start();
            
            // Verify both are running
            assertTrue(postgres.isRunning(), "PostgreSQL should be running");
            assertTrue(redis.isRunning(), "Redis should be running");
            
            System.out.println("✅ Multiple containers running simultaneously!");
            System.out.println("PostgreSQL JDBC: " + postgres.getJdbcUrl());
            System.out.println("Redis: " + redis.getHost() + ":" + redis.getMappedPort(6379));
        }
    }
}
