package com.currencyexchange.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for requesting exchange rates.
 */
@Schema(description = "Request for exchange rates between currencies")
public record ExchangeRateRequestDto(
        
        @Schema(description = "Base currency code (ISO 4217)", example = "USD", required = true)
        @NotBlank(message = "Base currency code cannot be blank")
        @Size(min = 3, max = 3, message = "Base currency code must be exactly 3 characters")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency code must be 3 uppercase letters")
        String baseCurrency,
        
        @Schema(description = "Target currency code (ISO 4217)", example = "EUR", required = true)
        @NotBlank(message = "Target currency code cannot be blank")
        @Size(min = 3, max = 3, message = "Target currency code must be exactly 3 characters")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Target currency code must be 3 uppercase letters")
        String targetCurrency
) {
}
