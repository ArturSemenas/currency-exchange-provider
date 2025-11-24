package com.currencyexchange.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Currency Exchange Provider.
 * Spring Boot applications require a public no-args constructor and cannot be final.
 */
@SpringBootApplication
@EnableScheduling
public class CurrencyExchangeProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyExchangeProviderApplication.class, args);
    }
}
