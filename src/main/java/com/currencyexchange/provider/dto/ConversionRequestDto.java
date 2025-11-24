package com.currencyexchange.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO for currency conversion requests.
 *
 * @param from   the base currency code (ISO 4217)
 * @param to     the target currency code (ISO 4217)
 * @param amount the amount to convert
 */
@Schema(description = "Request for currency conversion")
public record ConversionRequestDto(
        
        @Schema(description = "Base currency code (ISO 4217)", example = "USD", required = true)
        @NotBlank(message = "Base currency code cannot be blank")
        @Size(min = 3, max = 3, message = "Base currency code must be exactly 3 characters")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency code must be 3 uppercase letters")
        String from,
        
        @Schema(description = "Target currency code (ISO 4217)", example = "EUR", required = true)
        @NotBlank(message = "Target currency code cannot be blank")
        @Size(min = 3, max = 3, message = "Target currency code must be exactly 3 characters")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Target currency code must be 3 uppercase letters")
        String to,
        
        @Schema(description = "Amount to convert", example = "100.00", required = true)
        @NotNull(message = "Amount cannot be null")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount
) {
}
