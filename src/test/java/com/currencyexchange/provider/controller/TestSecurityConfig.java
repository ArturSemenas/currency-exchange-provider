package com.currencyexchange.provider.controller;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
                        // Public endpoints - match main SecurityConfig
                        .requestMatchers("/api/v1/currencies").permitAll()
                        .requestMatchers("/api/v1/currencies/exchange-rates").permitAll()
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});

        return http.build();
    }
}
