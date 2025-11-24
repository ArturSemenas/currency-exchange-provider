package com.currencyexchange.mock;

import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/v1")
public class MockExchangeRateController {
    
    private final Random random = new Random();
    
    @GetMapping("/latest")
    public MockResponse getLatestRates(@RequestParam(required = false) String base,
                                       @RequestParam(required = false) String access_key) {
        String baseCurrency = base != null ? base : "USD";
        
        MockResponse response = new MockResponse();
        response.setSuccess(true);
        response.setBase(baseCurrency);
        response.setDate(LocalDate.now().toString());
        response.setRates(generateMockRates(baseCurrency));
        
        return response;
    }
    
    @GetMapping("/{date}")
    public MockResponse getHistoricalRates(@PathVariable String date,
                                           @RequestParam(required = false) String base,
                                           @RequestParam(required = false) String symbols,
                                           @RequestParam(required = false) String access_key) {
        String baseCurrency = base != null ? base : "USD";
        
        MockResponse response = new MockResponse();
        response.setSuccess(true);
        response.setBase(baseCurrency);
        response.setDate(date);
        
        if (symbols != null) {
            Map<String, BigDecimal> rates = new HashMap<>();
            rates.put(symbols, generateRandomRate());
            response.setRates(rates);
        } else {
            response.setRates(generateMockRates(baseCurrency));
        }
        
        return response;
    }
    
    private Map<String, BigDecimal> generateMockRates(String baseCurrency) {
        Map<String, BigDecimal> rates = new HashMap<>();
        
        // Common currencies with different set than mock-1
        String[] currencies = {"EUR", "GBP", "JPY", "CNY", "INR", "KRW", "MXN", "BRL"};
        
        for (String currency : currencies) {
            if (!currency.equals(baseCurrency)) {
                rates.put(currency, generateRandomRate());
            }
        }
        
        return rates;
    }
    
    private BigDecimal generateRandomRate() {
        // Generate rate between 0.3 and 3.0 (wider range than mock-1)
        double rate = 0.3 + (2.7 * random.nextDouble());
        return BigDecimal.valueOf(rate).setScale(6, RoundingMode.HALF_UP);
    }
    
    @Data
    public static class MockResponse {
        private boolean success;
        private String base;
        private String date;
        private Map<String, BigDecimal> rates;
    }
}
