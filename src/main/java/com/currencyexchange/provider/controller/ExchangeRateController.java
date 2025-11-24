package com.currencyexchange.provider.controller;

import com.currencyexchange.provider.dto.ConversionResponseDto;
import com.currencyexchange.provider.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * REST Controller for exchange rate operations
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "Endpoints for currency exchange rate operations")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/exchange-rates")
    @Operation(
            summary = "Convert currency amount",
            description = "Calculates the converted amount from one currency to another using current exchange rates. No authentication required.",
            parameters = {
                    @Parameter(name = "amount", description = "Amount to convert (must be positive)", example = "100.00", required = true),
                    @Parameter(name = "from", description = "Source currency code (ISO 4217)", example = "USD", required = true),
                    @Parameter(name = "to", description = "Target currency code (ISO 4217)", example = "EUR", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully converted currency",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ConversionResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Conversion example",
                                    value = """
                                            {
                                              "from": "USD",
                                              "to": "EUR",
                                              "amount": 100.00,
                                              "convertedAmount": 85.50,
                                              "rate": 0.855,
                                              "timestamp": "2024-01-15T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid parameters (invalid currency code, negative amount, etc.)"),
            @ApiResponse(responseCode = "404", description = "Exchange rate not found for the specified currency pair"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ConversionResponseDto> convertCurrency(
            @RequestParam
            @NotNull(message = "Amount cannot be null")
            @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
            BigDecimal amount,
            
            @RequestParam
            @NotBlank(message = "Source currency code cannot be blank")
            @Size(min = 3, max = 3, message = "Source currency code must be exactly 3 characters")
            @Pattern(regexp = "^[A-Z]{3}$", message = "Source currency code must be 3 uppercase letters")
            String from,
            
            @RequestParam
            @NotBlank(message = "Target currency code cannot be blank")
            @Size(min = 3, max = 3, message = "Target currency code must be exactly 3 characters")
            @Pattern(regexp = "^[A-Z]{3}$", message = "Target currency code must be 3 uppercase letters")
            String to) {
        
        log.info("GET /api/v1/currencies/exchange-rates?amount={}&from={}&to={}", amount, from, to);
        
        Optional<BigDecimal> convertedAmount = exchangeRateService.getExchangeRate(amount, from, to);
        
        if (convertedAmount.isEmpty()) {
            log.warn("Exchange rate not found for {} -> {}", from, to);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        // Calculate the rate used
        BigDecimal rate = convertedAmount.get().divide(amount, 6, java.math.RoundingMode.HALF_UP);
        
        ConversionResponseDto response = new ConversionResponseDto(
                from,
                to,
                amount,
                convertedAmount.get(),
                rate,
                LocalDateTime.now()
        );
        
        log.info("Successfully converted {} {} to {} {}", amount, from, convertedAmount.get(), to);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Manually refresh exchange rates",
            description = "Triggers a manual refresh of exchange rates from all providers. Requires ADMIN role. " +
                         "This will fetch the latest rates, aggregate them, and update both database and cache.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Exchange rates successfully refreshed",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success response",
                                    value = """
                                            {
                                              "message": "Successfully refreshed 48 exchange rates",
                                              "updatedCount": 48,
                                              "timestamp": "2024-01-15T10:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error - failed to refresh rates")
    })
    public ResponseEntity<RefreshResponse> refreshExchangeRates() {
        log.info("POST /api/v1/currencies/refresh - Manual refresh triggered");
        
        try {
            int updatedCount = exchangeRateService.refreshRates();
            
            RefreshResponse response = new RefreshResponse(
                    "Successfully refreshed " + updatedCount + " exchange rates",
                    updatedCount,
                    LocalDateTime.now()
            );
            
            log.info("Manual refresh completed: {} rates updated", updatedCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to refresh exchange rates: {}", e.getMessage(), e);
            
            RefreshResponse errorResponse = new RefreshResponse(
                    "Failed to refresh exchange rates: " + e.getMessage(),
                    0,
                    LocalDateTime.now()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Response DTO for refresh operation
     */
    @Schema(description = "Response for manual refresh operation")
    public record RefreshResponse(
            @Schema(description = "Status message", example = "Successfully refreshed 48 exchange rates")
            String message,
            
            @Schema(description = "Number of rates updated", example = "48")
            int updatedCount,
            
            @Schema(description = "Timestamp of the refresh", example = "2024-01-15T10:30:00")
            LocalDateTime timestamp
    ) {
    }
}
