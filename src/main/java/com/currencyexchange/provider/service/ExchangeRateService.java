package com.currencyexchange.provider.service;

import com.currencyexchange.provider.client.ExchangeRateProvider;
import com.currencyexchange.provider.model.ExchangeRate;
import com.currencyexchange.provider.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing exchange rates
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateCacheService cacheService;
    private final ExchangeRateRetrievalService retrievalService;
    private final RateAggregationService aggregationService;
    
    /**
     * Get exchange rate and calculate converted amount
     * 
     * @param amount the amount to convert
     * @param fromCurrency the source currency code
     * @param toCurrency the target currency code
     * @return the converted amount
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getExchangeRate(BigDecimal amount, String fromCurrency, String toCurrency) {
        log.debug("Getting exchange rate: {} {} to {}", amount, fromCurrency, toCurrency);
        
        // Handle same currency conversion
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return Optional.of(amount);
        }
        
        // Get rate from cache or database
        Optional<BigDecimal> rate = retrievalService.getRate(fromCurrency, toCurrency);
        
        return rate.map(r -> {
            BigDecimal convertedAmount = amount.multiply(r).setScale(2, RoundingMode.HALF_UP);
            log.info("Converted {} {} to {} {} using rate {}", 
                    amount, fromCurrency, convertedAmount, toCurrency, r);
            return convertedAmount;
        });
    }
    
    /**
     * Refresh exchange rates from all providers
     * Fetches rates, aggregates best rates, stores in database and cache
     * 
     * @return number of rates updated
     */
    @Transactional
    public int refreshRates() {
        log.info("Starting exchange rates refresh");
        
        try {
            // Get best rates from all providers
            Map<String, Map<String, BigDecimal>> bestRates = aggregationService.aggregateBestRates();
            
            if (bestRates.isEmpty()) {
                log.warn("No rates received from providers");
                return 0;
            }
            
            // Save rates to database
            int savedCount = saveRatesToDatabase(bestRates);
            
            // Update cache
            cacheService.evictAll();
            cacheService.storeBestRates(bestRates);
            
            log.info("Successfully refreshed {} exchange rates", savedCount);
            return savedCount;
            
        } catch (Exception e) {
            log.error("Error refreshing exchange rates: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to refresh exchange rates", e);
        }
    }
    
    /**
     * Get the best rate for a currency pair from multiple providers
     * 
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return Optional containing the best rate
     */
    public Optional<BigDecimal> getBestRate(String baseCurrency, String targetCurrency) {
        log.debug("Getting best rate: {} -> {}", baseCurrency, targetCurrency);
        
        Map<String, BigDecimal> providerRates = aggregationService.getRatesForPair(baseCurrency, targetCurrency);
        
        if (providerRates.isEmpty()) {
            log.warn("No rates available from any provider for {} -> {}", baseCurrency, targetCurrency);
            return Optional.empty();
        }
        
        // Find the best (highest) rate using Stream API
        Optional<BigDecimal> bestRate = providerRates.values().stream()
                .max(Comparator.naturalOrder());
        
        bestRate.ifPresent(rate -> 
                log.info("Best rate for {} -> {} is {}", baseCurrency, targetCurrency, rate));
        
        return bestRate;
    }
    
    /**
     * Get historical rates for a time period
     * 
     * @param baseCurrency the base currency
     * @param targetCurrency the target currency
     * @param startDate start of period
     * @param endDate end of period
     * @return list of historical exchange rates
     */
    @Transactional(readOnly = true)
    public List<ExchangeRate> getHistoricalRates(String baseCurrency, String targetCurrency, 
                                                   LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Retrieving historical rates: {} -> {} from {} to {}", 
                baseCurrency, targetCurrency, startDate, endDate);
        
        return exchangeRateRepository.findRatesByPeriod(baseCurrency, targetCurrency, startDate, endDate);
    }
    
    /**
     * Save rates to database
     */
    private int saveRatesToDatabase(Map<String, Map<String, BigDecimal>> bestRates) {
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        
        for (Map.Entry<String, Map<String, BigDecimal>> entry : bestRates.entrySet()) {
            String baseCurrency = entry.getKey();
            Map<String, BigDecimal> rates = entry.getValue();
            
            for (Map.Entry<String, BigDecimal> rateEntry : rates.entrySet()) {
                String targetCurrency = rateEntry.getKey();
                BigDecimal rate = rateEntry.getValue();
                
                ExchangeRate exchangeRate = ExchangeRate.builder()
                        .baseCurrency(baseCurrency)
                        .targetCurrency(targetCurrency)
                        .rate(rate)
                        .timestamp(now)
                        .provider("aggregated")
                        .build();
                
                exchangeRateRepository.save(exchangeRate);
                count++;
            }
        }
        
        log.info("Saved {} exchange rates to database", count);
        return count;
    }
}
