package com.currencyexchange.provider.controller;

import com.currencyexchange.provider.model.Currency;
import com.currencyexchange.provider.service.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
@Tag(name = "Currency Management", description = "Endpoints for managing supported currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    @Operation(summary = "Get all supported currencies")
    public ResponseEntity<List<Currency>> getAllCurrencies() {
        return ResponseEntity.ok(currencyService.getAllCurrencies());
    }

    @PostMapping
    @Operation(summary = "Add a new currency")
    public ResponseEntity<Currency> addCurrency(@RequestParam String code) {
        Currency currency = currencyService.addCurrency(code);
        return ResponseEntity.ok(currency);
    }
}
