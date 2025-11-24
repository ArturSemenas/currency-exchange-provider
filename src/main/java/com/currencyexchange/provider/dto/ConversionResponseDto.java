package com.currencyexchange.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for currency conversion response.
 */
@Schema(description = "Currency conversion result")
public record ConversionResponseDto(
        
        @Schema(description = "Base currency code", example = "USD")
        String from,
        
        @Schema(description = "Target currency code", example = "EUR")
        String to,
        
        @Schema(description = "Original amount", example = "100.00")
        BigDecimal amount,
        
        @Schema(description = "Converted amount", example = "85.00")
        BigDecimal convertedAmount,
        
        @Schema(description = "Exchange rate used", example = "0.85")
        BigDecimal rate,
        
        @Schema(description = "Conversion timestamp", example = "2024-01-15T10:30:00")
        LocalDateTime timestamp
) {
}
