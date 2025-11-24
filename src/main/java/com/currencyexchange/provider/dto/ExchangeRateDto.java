package com.currencyexchange.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Exchange Rate information.
 *
 * @param id             the unique identifier
 * @param baseCurrency   the base currency code
 * @param targetCurrency the target currency code
 * @param rate           the exchange rate value
 * @param provider       the provider name
 * @param lastUpdated    the last update timestamp
 */
@Schema(description = "Exchange rate information")
public record ExchangeRateDto(
        
        @Schema(description = "Unique identifier", example = "1")
        Long id,
        
        @Schema(description = "Base currency code", example = "USD")
        String baseCurrency,
        
        @Schema(description = "Target currency code", example = "EUR")
        String targetCurrency,
        
        @Schema(description = "Exchange rate value", example = "0.85")
        BigDecimal rate,
        
        @Schema(description = "Provider name", example = "fixer.io")
        String provider,
        
        @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime lastUpdated
) {
}
