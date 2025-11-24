package com.currencyexchange.provider.exception;

/**
 * Exception thrown when an exchange rate is not found for a currency pair
 */
public class ExchangeRateNotFoundException extends RuntimeException {

    public ExchangeRateNotFoundException(String baseCurrency, String targetCurrency) {
        super(String.format("Exchange rate not found for currency pair: %s -> %s", 
                baseCurrency, targetCurrency));
    }

    public ExchangeRateNotFoundException(String message) {
        super(message);
    }

    public ExchangeRateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
