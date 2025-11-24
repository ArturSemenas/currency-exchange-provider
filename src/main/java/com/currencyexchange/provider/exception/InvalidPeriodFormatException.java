package com.currencyexchange.provider.exception;

/**
 * Exception thrown when period format is invalid for trend analysis
 * Valid formats: 12H, 10D, 3M, 1Y
 */
public class InvalidPeriodFormatException extends RuntimeException {

    public InvalidPeriodFormatException(String period) {
        super(String.format("Invalid period format: '%s'. Expected format: <number><unit> "
                + "where unit is H (hours, min 12), D (days), M (months), or Y (years). "
                + "Examples: 12H, 7D, 3M, 1Y", period));
    }

    public InvalidPeriodFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
