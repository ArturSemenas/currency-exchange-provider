package com.currencyexchange.provider.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for ISO 4217 currency codes
 * Validates against java.util.Currency.getAvailableCurrencies()
 */
public class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    private static final Set<String> VALID_CURRENCY_CODES;

    static {
        // Build set of valid currency codes from Java's Currency class
        VALID_CURRENCY_CODES = Currency.getAvailableCurrencies().stream()
                .map(Currency::getCurrencyCode)
                .collect(Collectors.toSet());
    }

    @Override
    public void initialize(ValidCurrency constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null or empty values should be handled by @NotBlank
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        // Convert to uppercase for comparison
        String upperValue = value.trim().toUpperCase();

        // Check if it's a valid ISO 4217 currency code
        if (!VALID_CURRENCY_CODES.contains(upperValue)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid currency code: '" + value + "'. Must be a valid ISO 4217 code (e.g., USD, EUR, GBP)"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
