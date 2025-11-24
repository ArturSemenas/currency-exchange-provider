package com.currencyexchange.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for error responses.
 *
 * @param timestamp         the timestamp of the error
 * @param status            the HTTP status code
 * @param error             the error type
 * @param message           the error message
 * @param path              the API path where error occurred
 * @param validationErrors  the list of validation errors (if applicable)
 */
@Schema(description = "Error response")
public record ErrorResponseDto(
        
        @Schema(description = "Timestamp of the error", example = "2024-01-15T10:30:00")
        LocalDateTime timestamp,
        
        @Schema(description = "HTTP status code", example = "400")
        int status,
        
        @Schema(description = "Error type", example = "Bad Request")
        String error,
        
        @Schema(description = "Error message", example = "Currency code must be 3 uppercase letters")
        String message,
        
        @Schema(description = "API path where error occurred", example = "/api/v1/exchange-rates")
        String path,
        
        @Schema(description = "Validation errors (if applicable)")
        List<ValidationError> validationErrors
) {
    /**
     * Validation error details.
     *
     * @param field         the field name
     * @param rejectedValue the rejected value
     * @param message       the error message
     */
    @Schema(description = "Validation error detail")
    public record ValidationError(
            
            @Schema(description = "Field name", example = "baseCurrency")
            String field,
            
            @Schema(description = "Rejected value", example = "us")
            Object rejectedValue,
            
            @Schema(description = "Error message", example = "Currency code must be exactly 3 characters")
            String message
    ) {
    }
}
