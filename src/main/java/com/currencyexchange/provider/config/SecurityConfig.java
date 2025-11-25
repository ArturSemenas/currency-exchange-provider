package com.currencyexchange.provider.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
     * Configure HTTP security filter chain.
     * Sets up endpoint authorization, HTTP Basic Auth, and stateless sessions.
     *
     * @param http the HTTP security configuration
     * @return the security filter chain
     * @throws Exception if configuration fails
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
                        
                        // Swagger/OpenAPI documentation - public access
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        
                        // Public GET endpoints - no authentication required
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/currencies",
                                "/api/v1/currencies/exchange-rates"
                        ).permitAll()
                        
                        // POST to currencies - requires ADMIN authority
                        .requestMatchers(HttpMethod.POST, "/api/v1/currencies").hasAuthority("ADMIN")
                        
                        // POST refresh - requires ADMIN authority
                        .requestMatchers(HttpMethod.POST, "/api/v1/currencies/refresh").hasAuthority("ADMIN")
                        
                        // GET trends - requires ADMIN or PREMIUM_USER authority
                        .requestMatchers(HttpMethod.GET, "/api/v1/currencies/trends").hasAnyAuthority("ADMIN", "PREMIUM_USER")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                
                // Enable HTTP Basic Authentication
                .httpBasic(basic -> { })
                
                // Set session management to STATELESS (no server-side sessions)
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * Password encoder bean using BCrypt with strength 12.
     * BCrypt is a strong hashing algorithm designed for password hashing.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication manager bean.
     * Used for authenticating users.
     *
     * @param config the authentication configuration
     * @return the authentication manager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
