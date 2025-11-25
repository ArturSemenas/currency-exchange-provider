package com.currencyexchange.provider.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI currencyExchangeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Currency Exchange Provider API")
                        .description("""
                                RESTful API for currency exchange rates management.
                                
                                Features:
                                - Real-time exchange rate conversion
                                - Multiple provider integration (Fixer.io, ExchangeRatesAPI.io)
                                - Rate aggregation (best rates from all providers)
                                - Historical trend analysis
                                - Redis caching for performance
                                - Scheduled hourly rate updates
                                
                                Authentication:
                                - HTTP Basic Authentication required for secured endpoints
                                - Test users: user/admin123, premium/admin123, admin/admin123
                                
                                Authorities:
                                - USER: Basic access to public endpoints
                                - PREMIUM_USER: Access to trend analysis and public endpoints
                                - ADMIN: Full access including currency management and manual refresh
                                
                                Endpoint Access:
                                - GET /api/v1/currencies: Public (no authentication)
                                - GET /api/v1/currencies/exchange-rates: Public (no authentication)
                                - POST /api/v1/currencies: ADMIN only
                                - POST /api/v1/currencies/refresh: ADMIN only
                                - GET /api/v1/currencies/trends: ADMIN and PREMIUM_USER
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Currency Exchange Team")
                                .email("support@currencyexchange.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.currencyexchange.com")
                                .description("Production Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic Authentication. Use username and password from test users.")));
    }
}
