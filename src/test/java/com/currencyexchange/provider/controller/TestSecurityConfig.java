package com.currencyexchange.provider.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration for controller tests
 * Replicates the main SecurityConfig for @WebMvcTest
 */
@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public GET endpoints - match main SecurityConfig
                        .requestMatchers(HttpMethod.GET, "/api/v1/currencies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/currencies/exchange-rates").permitAll()
                        
                        // POST to currencies - requires ADMIN authority
                        .requestMatchers(HttpMethod.POST, "/api/v1/currencies").hasAuthority("ADMIN")
                        
                        // POST to refresh exchange rates - requires ADMIN authority
                        .requestMatchers(HttpMethod.POST, "/api/v1/currencies/refresh").hasAuthority("ADMIN")
                        
                        // GET trend analysis - requires ADMIN or PREMIUM_USER
                        .requestMatchers(HttpMethod.GET, "/api/v1/currencies/trends").hasAnyAuthority("ADMIN", "PREMIUM_USER")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

        return http.build();
    }
}
