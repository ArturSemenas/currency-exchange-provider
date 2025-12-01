package com.currencyexchange.provider.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal TestContainers test to debug Docker connectivity issues.
 * This test has no Spring dependencies - just pure TestContainers.
 */
@Testcontainers
class MinimalTestContainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void testContainerStarts() {
        assertTrue(postgres.isRunning(), "Container should be running");
        assertNotNull(postgres.getJdbcUrl(), "JDBC URL should not be null");
        System.out.println("✅ Container started successfully!");
        System.out.println("JDBC URL: " + postgres.getJdbcUrl());
        System.out.println("Host: " + postgres.getHost());
        System.out.println("Port: " + postgres.getMappedPort(5432));
    }

    @Test
    void testDatabaseConnection() throws Exception {
        assertTrue(postgres.isRunning(), "Container should be running");

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            assertTrue(rs.next(), "Result set should have data");
            assertEquals(1, rs.getInt(1), "Query should return 1");
            
            System.out.println("✅ Database connection successful!");
        }
    }
}
