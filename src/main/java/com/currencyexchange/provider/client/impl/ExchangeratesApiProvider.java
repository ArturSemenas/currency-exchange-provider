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
 * Implementation for Exchangeratesapi.io API
 * API Documentation: https://exchangeratesapi.io/documentation/
 */
@Slf4j
@Component
public class ExchangeratesApiProvider implements ExchangeRateProvider {
    
    private static final String LATEST_ENDPOINT = "/latest";
    private static final String HISTORICAL_ENDPOINT = "/{date}";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    
    public ExchangeratesApiProvider(RestTemplate restTemplate,
                                   @Value("${api.exchangeratesapi.url}") String apiUrl,
                                   @Value("${api.exchangeratesapi.key}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }
    
    @Override
    public Map<String, BigDecimal> fetchLatestRates(String baseCurrency) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl + LATEST_ENDPOINT)
                    .queryParam("access_key", apiKey)
                    .queryParam("base", baseCurrency)
                    .toUriString();
            
            log.debug("Fetching latest rates from Exchangeratesapi.io for base: {}", baseCurrency);
            ExchangeratesApiResponse response = restTemplate.getForObject(url, ExchangeratesApiResponse.class);
            
            if (response != null && response.isSuccess()) {
                log.info("Successfully fetched {} rates from Exchangeratesapi.io for base {}", 
                        response.getRates().size(), baseCurrency);
                return response.getRates();
            }
            
            return new HashMap<>();
        } catch (Exception e) {
            log.error("Error fetching latest rates from Exchangeratesapi.io: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    @Override
    public BigDecimal fetchHistoricalRate(String baseCurrency, String targetCurrency, LocalDate date) {
        try {
            String dateStr = date.format(DATE_FORMATTER);
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl + HISTORICAL_ENDPOINT)
                    .queryParam("access_key", apiKey)
                    .queryParam("base", baseCurrency)
                    .queryParam("symbols", targetCurrency)
                    .buildAndExpand(dateStr)
                    .toUriString();
            
            log.debug("Fetching historical rate from Exchangeratesapi.io: {} to {} on {}", 
                    baseCurrency, targetCurrency, dateStr);
            ExchangeratesApiResponse response = restTemplate.getForObject(url, ExchangeratesApiResponse.class);
            
            if (response != null && response.isSuccess() && response.getRates() != null) {
                BigDecimal rate = response.getRates().get(targetCurrency);
                log.info("Successfully fetched historical rate from Exchangeratesapi.io: {} -> {} = {}", 
                        baseCurrency, targetCurrency, rate);
                return rate;
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error fetching historical rate from Exchangeratesapi.io: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public String getProviderName() {
        return "exchangeratesapi.io";
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // Simple health check - try to fetch EUR rates
            Map<String, BigDecimal> rates = fetchLatestRates("EUR");
            return !rates.isEmpty();
        } catch (Exception e) {
            log.warn("Exchangeratesapi.io provider is not available: {}", e.getMessage());
            return false;
        }
    }
}
