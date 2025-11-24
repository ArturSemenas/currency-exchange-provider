package com.currencyexchange.provider.controller;

import com.currencyexchange.provider.dto.CurrencyDto;
import com.currencyexchange.provider.mapper.CurrencyMapper;
import com.currencyexchange.provider.model.Currency;
import com.currencyexchange.provider.service.CurrencyService;
import com.currencyexchange.provider.validation.ValidCurrency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for currency management operations
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Tag(name = "Currency Management", description = "Endpoints for managing supported currencies")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final CurrencyMapper currencyMapper;

    @GetMapping
    @Operation(
            summary = "Get all supported currencies",
            description = "Retrieves a list of all currencies supported by the system. No authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of currencies"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
        log.info("GET /api/v1/currencies - Fetching all currencies");
        List<Currency> currencies = currencyService.getAllCurrencies();
        List<CurrencyDto> dtos = currencyMapper.toDtoList(currencies);
        log.info("Returning {} currencies", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Add a new currency",
            description = "Adds a new currency to the system. Requires ADMIN role. Currency code must be a valid ISO 4217 code.",
            security = @SecurityRequirement(name = "basicAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Currency successfully added"),
            @ApiResponse(responseCode = "400", description = "Invalid currency code or currency already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CurrencyDto> addCurrency(
            @Parameter(description = "ISO 4217 currency code (3 uppercase letters)", example = "USD", required = true)
            @RequestParam
            @NotBlank(message = "Currency code cannot be blank")
            @ValidCurrency
            String currency) {
        
        log.info("POST /api/v1/currencies?currency={} - Adding new currency", currency);
        Currency savedCurrency = currencyService.addCurrency(currency);
        CurrencyDto dto = currencyMapper.toDto(savedCurrency);
        log.info("Successfully added currency: {}", currency);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
