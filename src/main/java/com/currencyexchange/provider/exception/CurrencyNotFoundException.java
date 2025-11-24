package com.currencyexchange.provider.exception;

/**
 * Exception thrown when a requested currency is not found in the system
 */
public class CurrencyNotFoundException extends RuntimeException {

    public CurrencyNotFoundException(String currencyCode) {
        super("Currency not found: " + currencyCode);
    }

    public CurrencyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
