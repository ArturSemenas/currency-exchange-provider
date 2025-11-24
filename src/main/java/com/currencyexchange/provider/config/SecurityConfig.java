package com.currencyexchange.provider.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application
 * Configures HTTP Basic Authentication, role-based access control, and stateless sessions
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Configure HTTP security filter chain
     * Sets up endpoint authorization, HTTP Basic Auth, and stateless sessions
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for REST API (stateless)
                .csrf(csrf -> csrf.disable())
                
                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Actuator health endpoint - allow anonymous access for Docker health checks
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/v1/currencies",
                                "/api/v1/currencies/exchange-rates"
                        ).permitAll()
                        
                        // Swagger/OpenAPI documentation - public access
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        
                        // Admin-only endpoints
                        .requestMatchers(
                                "/api/v1/currencies/refresh"
                        ).hasRole("ADMIN")
                        
                        // Trend analysis - requires ADMIN or PREMIUM_USER role
                        .requestMatchers(
                                "/api/v1/currencies/trends"
                        ).hasAnyRole("ADMIN", "PREMIUM_USER")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                // Enable HTTP Basic Authentication
                .httpBasic(basic -> {})
                
                // Set session management to STATELESS (no server-side sessions)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * Password encoder bean using BCrypt with strength 12
     * BCrypt is a strong hashing algorithm designed for password hashing
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication manager bean
     * Used for authenticating users
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
