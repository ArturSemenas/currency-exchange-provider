package com.currencyexchange.provider.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for period format used in trend analysis
 * Validates format: <number><unit> where unit is H (hours), D (days), M (months), or Y (years)
 * Example valid periods: 12H, 7D, 3M, 1Y
 */
public class PeriodValidator implements ConstraintValidator<ValidPeriod, String> {

    private static final Pattern PERIOD_PATTERN = Pattern.compile("^(\\d+)([HDMY])$");
    private int minHours;

    @Override
    public void initialize(ValidPeriod constraintAnnotation) {
        this.minHours = constraintAnnotation.minHours();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null or empty values should be handled by @NotBlank
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        Matcher matcher = PERIOD_PATTERN.matcher(value.trim());
        
        if (!matcher.matches()) {
            return false;
        }

        // Extract number and unit
        int number = Integer.parseInt(matcher.group(1));
        char unit = matcher.group(2).charAt(0);

        // Validate minimum hours if unit is H
        if (unit == 'H' && number < minHours) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Period in hours must be at least %d. Use format: %dH, 10D, 3M, or 1Y", 
                            minHours, minHours)
            ).addConstraintViolation();
            return false;
        }

        // Validate that number is positive
        return number > 0;
    }
}
