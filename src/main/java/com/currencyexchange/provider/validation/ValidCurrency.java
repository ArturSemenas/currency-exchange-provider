package com.currencyexchange.provider.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for ISO 4217 currency codes
 * Validates that the currency code is a valid ISO 4217 code
 */
@Documented
@Constraint(validatedBy = CurrencyValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {
    
    String message() default "{currency.invalid}";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
