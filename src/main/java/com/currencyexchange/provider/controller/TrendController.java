package com.currencyexchange.provider.controller;

import com.currencyexchange.provider.service.TrendAnalysisService;
import com.currencyexchange.provider.validation.ValidCurrency;
import com.currencyexchange.provider.validation.ValidPeriod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST Controller for currency trend analysis
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/currencies/trends")
@RequiredArgsConstructor
@Tag(name = "Trend Analysis", description = "Endpoints for analyzing currency exchange rate trends")
public class TrendController {

    private final TrendAnalysisService trendAnalysisService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PREMIUM_USER')")
    @Operation(
            summary = "Analyze exchange rate trend",
            description = """
                    Calculates the percentage change in exchange rate over a specified time period.
                    Requires ADMIN or PREMIUM_USER role.
                    
                    Period format: <number><unit> where unit is:
                    - H: Hours (minimum 12)
                    - D: Days
                    - M: Months
                    - Y: Years
                    
                    Examples: 12H, 7D, 3M, 1Y
                    
                    Returns positive percentage for appreciation, negative for depreciation.
                    """,
            security = @SecurityRequirement(name = "basicAuth"),
            parameters = {
                    @Parameter(name = "from", description = "Base currency code (ISO 4217)", example = "USD", required = true),
                    @Parameter(name = "to", description = "Target currency code (ISO 4217)", example = "EUR", required = true),
                    @Parameter(name = "period", description = "Time period (e.g., 12H, 7D, 3M, 1Y)", example = "7D", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully calculated trend",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TrendResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Trend example",
                                    value = """
                                            {
                                              "baseCurrency": "USD",
                                              "targetCurrency": "EUR",
                                              "period": "7D",
                                              "trendPercentage": 2.35,
                                              "description": "USD appreciated by 2.35% against EUR over the last 7 days"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid parameters (invalid currency code, invalid period format)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN or PREMIUM_USER role required"),
            @ApiResponse(responseCode = "404", description = "Insufficient historical data for the specified period"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TrendResponseDto> analyzeTrend(
            @RequestParam
            @NotBlank(message = "Base currency code cannot be blank")
            @ValidCurrency
            String from,
            
            @RequestParam
            @NotBlank(message = "Target currency code cannot be blank")
            @ValidCurrency
            String to,
            
            @RequestParam
            @NotBlank(message = "Period cannot be blank")
            @ValidPeriod
            String period) {
        
        log.info("GET /api/v1/currencies/trends?from={}&to={}&period={}", from, to, period);
        
        try {
            BigDecimal trendPercentage = trendAnalysisService.calculateTrend(from, to, period);
            
            // Create description based on trend direction
            String description = createTrendDescription(from, to, period, trendPercentage);
            
            TrendResponseDto response = new TrendResponseDto(
                    from,
                    to,
                    period,
                    trendPercentage,
                    description
            );
            
            log.info("Trend analysis completed: {} -> {} over {} = {}%", from, to, period, trendPercentage);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.warn("Insufficient data: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Create a human-readable description of the trend
     */
    private String createTrendDescription(String from, String to, String period, BigDecimal trendPercentage) {
        String direction = trendPercentage.compareTo(BigDecimal.ZERO) >= 0 ? "appreciated" : "depreciated";
        String absPercentage = trendPercentage.abs().toPlainString();
        String periodDescription = formatPeriodDescription(period);
        
        return String.format("%s %s by %s%% against %s over the last %s",
                from, direction, absPercentage, to, periodDescription);
    }

    /**
     * Format period for human-readable description
     */
    private String formatPeriodDescription(String period) {
        String number = period.replaceAll("[HDMY]", "");
        char unit = period.charAt(period.length() - 1);
        
        return switch (unit) {
            case 'H' -> number + " hour" + (number.equals("1") ? "" : "s");
            case 'D' -> number + " day" + (number.equals("1") ? "" : "s");
            case 'M' -> number + " month" + (number.equals("1") ? "" : "s");
            case 'Y' -> number + " year" + (number.equals("1") ? "" : "s");
            default -> period;
        };
    }

    /**
     * Response DTO for trend analysis
     */
    @Schema(description = "Exchange rate trend analysis result")
    public record TrendResponseDto(
            @Schema(description = "Base currency code", example = "USD")
            String baseCurrency,
            
            @Schema(description = "Target currency code", example = "EUR")
            String targetCurrency,
            
            @Schema(description = "Analysis period", example = "7D")
            String period,
            
            @Schema(description = "Trend percentage (positive = appreciation, negative = depreciation)", example = "2.35")
            BigDecimal trendPercentage,
            
            @Schema(description = "Human-readable description of the trend", 
                    example = "USD appreciated by 2.35% against EUR over the last 7 days")
            String description
    ) {
    }
}
