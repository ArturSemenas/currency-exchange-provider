package com.currencyexchange.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CurrencyExchangeProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyExchangeProviderApplication.class, args);
    }
}
