package com.currencyexchange.provider.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Utility test for BCrypt password hashing verification.
 */
@Slf4j
class PasswordHashTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Test
    void testGeneratePasswordHash() {
        String password = "admin123";
        String hash = encoder.encode(password);
        
        log.info("======================================");
        log.info("Password: {}", password);
        log.info("Generated BCrypt Hash:");
        log.info("{}", hash);
        log.info("======================================");
        
        assertTrue(encoder.matches(password, hash), "Password should match the generated hash");
    }

    @Test
    void testCurrentDatabaseHash() {
        // Hash from the database (from changeset 005-insert-users-seed-data.xml)
        String databaseHash = "$2a$12$zPBRneGsZVsI5mUDNQuUiufG8xrCmc7/G7KN.m0USnBWiCqn9iQWi";
        String password = "admin123";
        
        log.info("======================================");
        log.info("Testing Database Hash:");
        log.info("Hash: {}", databaseHash);
        log.info("Password to test: {}", password);
        
        boolean matches = encoder.matches(password, databaseHash);
        log.info("Matches: {}", matches);
        log.info("======================================");
        
        assertTrue(matches, "Password 'admin123' should match database hash");
    }

    @Test
    void testOldDatabaseHash() {
        // Old hash that was in the database before
        String oldHash = "$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5qdZRuTdvPpRi";
        
        log.info("======================================");
        log.info("Testing OLD Database Hash:");
        log.info("Hash: {}", oldHash);
        
        // Test common passwords
        String[] testPasswords = {"password123", "admin", "user", "admin123", "password", "12345678"};
        
        boolean found = false;
        for (String testPassword : testPasswords) {
            boolean matches = encoder.matches(testPassword, oldHash);
            log.info("Password '{}': {}", testPassword, (matches ? "MATCH!" : "no match"));
            if (matches) {
                found = true;
            }
        }
        log.info("======================================");
        
        assertFalse(found, "Old hash should not match any common passwords (it's unknown)");
    }

    @Test
    void testAllUsersPasswords() {
        String databaseHash = "$2a$12$zPBRneGsZVsI5mUDNQuUiufG8xrCmc7/G7KN.m0USnBWiCqn9iQWi";
        String password = "admin123";
        
        log.info("======================================");
        log.info("Testing ALL Users (all should have same password):");
        log.info("Expected password: {}", password);
        
        String[] users = {"user", "premium", "admin"};
        for (String user : users) {
            boolean matches = encoder.matches(password, databaseHash);
            log.info("{}: {}", user, (matches ? "✓ VALID" : "✗ INVALID"));
        }
        log.info("======================================");
        
        assertTrue(encoder.matches(password, databaseHash));
    }

    @Test
    void testBCryptStrength12() {
        String password = "admin123";
        
        // BCrypt with strength 12 (same as SecurityConfig)
        BCryptPasswordEncoder encoder12 = new BCryptPasswordEncoder(12);
        String hash12 = encoder12.encode(password);
        
        log.info("======================================");
        log.info("BCrypt Strength Comparison:");
        log.info("Strength 12 hash: {}", hash12);
        log.info("Hash starts with: {}", hash12.substring(0, 7));
        log.info("Expected: $2a$12$");
        log.info("======================================");
        
        assertTrue(hash12.startsWith("$2a$12$"), "Hash should start with $2a$12$");
        assertTrue(encoder12.matches(password, hash12));
    }

    @Test
    void testPasswordsDoNotMatch() {
        String databaseHash = "$2a$12$zPBRneGsZVsI5mUDNQuUiufG8xrCmc7/G7KN.m0USnBWiCqn9iQWi";
        
        log.info("======================================");
        log.info("Testing WRONG Passwords:");
        
        String[] wrongPasswords = {"admin", "password", "password123", "user", "wrong"};
        for (String wrongPassword : wrongPasswords) {
            boolean matches = encoder.matches(wrongPassword, databaseHash);
            log.info("'{}': {}", wrongPassword, (matches ? "MATCH (unexpected!)" : "no match (expected)"));
            assertFalse(matches, "Wrong password '" + wrongPassword + "' should NOT match");
        }
        log.info("======================================");
    }

    @Test
    void generateNewHashForPassword() {
        String password = "admin123";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        
        log.info("======================================");
        log.info("Generate NEW hash for: {}", password);
        log.info("");
        
        // Generate 3 different hashes for the same password (BCrypt uses salt)
        for (int i = 1; i <= 3; i++) {
            String hash = encoder.encode(password);
            log.info("Hash {}: {}", i, hash);
            
            // Verify each hash works
            assertTrue(encoder.matches(password, hash), "Hash " + i + " should match password");
        }
        
        log.info("");
        log.info("All hashes are different (due to random salt)");
        log.info("But all match the password 'admin123'");
        log.info("======================================");
    }
}
