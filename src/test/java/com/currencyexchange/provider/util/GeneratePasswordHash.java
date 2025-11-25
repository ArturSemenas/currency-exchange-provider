package com.currencyexchange.provider.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hash.
 * Run this class to generate a new hash for "admin123".
 */
public class GeneratePasswordHash {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String password = "admin123";
        
        System.out.println("=".repeat(60));
        System.out.println("BCrypt Password Hash Generator");
        System.out.println("=".repeat(60));
        System.out.println("Password: " + password);
        System.out.println("Strength: 12");
        System.out.println();
        
        // Generate hash
        String hash = encoder.encode(password);
        System.out.println("Generated BCrypt Hash:");
        System.out.println(hash);
        System.out.println();
        
        // Verify it works
        boolean matches = encoder.matches(password, hash);
        System.out.println("Verification: " + (matches ? "✓ SUCCESS" : "✗ FAILED"));
        System.out.println();
        
        // Show SQL update statement
        System.out.println("=".repeat(60));
        System.out.println("Use this hash in Liquibase changeset:");
        System.out.println("=".repeat(60));
        System.out.println("<column name=\"password\" value=\"" + hash + "\"/>");
        System.out.println("=".repeat(60));
    }
}
