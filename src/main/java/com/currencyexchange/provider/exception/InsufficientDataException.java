package com.currencyexchange.provider.exception;

/**
 * Exception thrown when insufficient historical data is available for trend analysis
 */
public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }

    public InsufficientDataException(String baseCurrency, String targetCurrency, String period) {
        super(String.format("Insufficient historical data for %s -> %s over period %s", 
                baseCurrency, targetCurrency, period));
    }

    public InsufficientDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
