package com.currencyexchange.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for Currency information.
 *
 * @param code the currency code (ISO 4217)
 * @param name the currency name
 */
@Schema(description = "Currency information")
public record CurrencyDto(
        
        @Schema(description = "Currency code (ISO 4217)", example = "USD")
        @NotBlank(message = "Currency code cannot be blank")
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be 3 uppercase letters")
        String code,
        
        @Schema(description = "Currency name", example = "US Dollar")
        @NotBlank(message = "Currency name cannot be blank")
        @Size(max = 100, message = "Currency name must not exceed 100 characters")
        String name
) {
}
