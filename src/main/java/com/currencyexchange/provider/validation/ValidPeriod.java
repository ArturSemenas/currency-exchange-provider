package com.currencyexchange.provider.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for period format in trend analysis
 * Valid formats: 12H (minimum 12 hours), 7D (days), 3M (months), 1Y (years)
 */
@Documented
@Constraint(validatedBy = PeriodValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPeriod {
    
    String message() default "{period.invalid}";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Minimum hours allowed for hour-based periods
     */
    int minHours() default 12;
}
