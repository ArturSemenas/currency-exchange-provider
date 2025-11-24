package com.currencyexchange.provider.exception;

/**
 * Exception thrown when attempting to add a currency that already exists
 */
public class CurrencyAlreadyExistsException extends RuntimeException {

    public CurrencyAlreadyExistsException(String currencyCode) {
        super("Currency already exists: " + currencyCode);
    }

    public CurrencyAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
