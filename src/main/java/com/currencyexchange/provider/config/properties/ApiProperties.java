package com.currencyexchange.provider.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for external API providers.
 * Binds properties with prefix "api".
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiProperties {

    /**
     * Fixer.io API configuration.
     */
    private FixerApi fixer = new FixerApi();

    /**
     * ExchangeRatesAPI.io configuration.
     */
    private ExchangeratesApi exchangeratesapi = new ExchangeratesApi();

    /**
     * Fixer.io API settings.
     */
    @Data
    public static class FixerApi {
        /**
         * Base URL for Fixer.io API.
         */
        @NotBlank(message = "Fixer API URL cannot be blank")
        private String url;

        /**
         * API key for Fixer.io authentication.
         */
        @NotBlank(message = "Fixer API key cannot be blank")
        private String key;
    }

    /**
     * ExchangeRatesAPI.io settings.
     */
    @Data
    public static class ExchangeratesApi {
        /**
         * Base URL for ExchangeRatesAPI.io.
         */
        @NotBlank(message = "ExchangeRatesAPI URL cannot be blank")
        private String url;

        /**
         * API key for ExchangeRatesAPI.io authentication.
         */
        @NotBlank(message = "ExchangeRatesAPI key cannot be blank")
        private String key;
    }
}
