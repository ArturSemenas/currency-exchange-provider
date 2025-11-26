package com.currencyexchange.provider.client.impl;

import com.currencyexchange.provider.client.ExchangeRateProvider;
import com.currencyexchange.provider.client.dto.ExchangeratesApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for Mock Provider 2 (ExchangeRatesAPI simulation).
 * Fetches exchange rates from standalone mock service on port 8092.
 * Used for testing and development without requiring real API keys.
 */
@Slf4j
@Component
public class MockProvider2Client implements ExchangeRateProvider {
    
    private static final String LATEST_ENDPOINT = "/v1/latest";
    private static final String HISTORICAL_ENDPOINT = "/v1/{date}";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    /**
     * Constructor with dependency injection.
     *
     * @param restTemplate the REST template for HTTP calls
     * @param baseUrl the base URL of mock provider 2
     */
    public MockProvider2Client(RestTemplate restTemplate,
                               @Value("${api.mock.provider2.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    
    @Override
    public Map<String, BigDecimal> fetchLatestRates(String baseCurrency) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + LATEST_ENDPOINT)
                    .queryParam("base", baseCurrency)
                    .toUriString();
            
            log.debug("Fetching latest rates from Mock Provider 2 for base: {}", baseCurrency);
            ExchangeratesApiResponse response = restTemplate.getForObject(url, 
                    ExchangeratesApiResponse.class);
            
            if (response != null && response.isSuccess()) {
                log.info("Successfully fetched {} rates from Mock Provider 2 for base {}", 
                        response.getRates().size(), baseCurrency);
                return response.getRates();
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Error fetching latest rates from Mock Provider 2: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    @Override
    public BigDecimal fetchHistoricalRate(String baseCurrency, String targetCurrency, LocalDate date) {
        try {
            String dateStr = date.format(DATE_FORMATTER);
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + HISTORICAL_ENDPOINT)
                    .queryParam("base", baseCurrency)
                    .queryParam("symbols", targetCurrency)
                    .buildAndExpand(dateStr)
                    .toUriString();
            
            log.debug("Fetching historical rate from Mock Provider 2: {} to {} on {}", 
                    baseCurrency, targetCurrency, dateStr);
            ExchangeratesApiResponse response = restTemplate.getForObject(url, 
                    ExchangeratesApiResponse.class);
            
            if (response != null && response.isSuccess() && response.getRates() != null) {
                BigDecimal rate = response.getRates().get(targetCurrency);
                log.info("Successfully fetched historical rate from Mock Provider 2: {} -> {} = {}", 
                        baseCurrency, targetCurrency, rate);
                return rate;
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error fetching historical rate from Mock Provider 2: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public String getProviderName() {
        return "mock-provider-2";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Simple health check - try to fetch USD rates
            Map<String, BigDecimal> rates = fetchLatestRates("USD");
            boolean available = !rates.isEmpty();
            if (!available) {
                log.debug("Mock Provider 2 returned empty rates");
            }
            return available;
        } catch (Exception e) {
            log.warn("Mock Provider 2 is not available: {}", e.getMessage());
            return false;
        }
    }
}
