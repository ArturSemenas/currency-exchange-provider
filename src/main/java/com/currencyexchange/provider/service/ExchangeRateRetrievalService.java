package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.ExchangeRate;
import com.currencyexchange.provider.repository.ExchangeRateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for retrieving exchange rates with cache fallback to database
 */
@Slf4j
@Service
public class ExchangeRateRetrievalService {
    
    private final ExchangeRateCacheService cacheService;
    private final ExchangeRateRepository exchangeRateRepository;
    
    public ExchangeRateRetrievalService(
            @Autowired(required = false) ExchangeRateCacheService cacheService,
            ExchangeRateRepository exchangeRateRepository) {
        this.cacheService = cacheService;
        this.exchangeRateRepository = exchangeRateRepository;
    }
    
    /**
     * Get exchange rate with cache fallback to database
     * First tries to get from Redis cache, if not found or Redis unavailable, queries database
     * 
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return Optional containing the rate if found
     */
    public Optional<BigDecimal> getRate(String baseCurrency, String targetCurrency) {
        // Try cache first if available
        if (cacheService != null) {
            Optional<BigDecimal> cachedRate = cacheService.getRate(baseCurrency, targetCurrency);
            if (cachedRate.isPresent()) {
                log.debug("Rate found in cache: {} -> {}", baseCurrency, targetCurrency);
                return cachedRate;
            }
        }
        
        // Fallback to database
        log.debug("Rate not in cache, querying database: {} -> {}", baseCurrency, targetCurrency);
        return exchangeRateRepository.findLatestRate(baseCurrency, targetCurrency)
                .map(ExchangeRate::getRate)
                .map(rate -> {
                    log.info("Rate retrieved from database: {} -> {} = {}", 
                            baseCurrency, targetCurrency, rate);
                    
                    // Store in cache for future requests if cache is available
                    if (cacheService != null && cacheService.isAvailable()) {
                        Map<String, BigDecimal> rateMap = new HashMap<>();
                        rateMap.put(targetCurrency, rate);
                        cacheService.storeRates(baseCurrency, rateMap);
                    }
                    
                    return rate;
                });
    }
    
    /**
     * Get all rates for a base currency with cache fallback
     * 
     * @param baseCurrency the base currency code
     * @return map of target currencies to rates
     */
    public Map<String, BigDecimal> getAllRates(String baseCurrency) {
        // Try cache first if available
        if (cacheService != null) {
            Map<String, BigDecimal> cachedRates = cacheService.getAllRates(baseCurrency);
            if (!cachedRates.isEmpty()) {
                log.debug("Rates found in cache for base currency: {}", baseCurrency);
                return cachedRates;
            }
        }
        
        // Fallback to database - get all latest rates
        log.debug("Rates not in cache, querying database for base currency: {}", baseCurrency);
        List<ExchangeRate> latestRates = exchangeRateRepository.findAllLatestRates();
        
        Map<String, BigDecimal> rates = new HashMap<>();
        latestRates.stream()
                .filter(rate -> rate.getBaseCurrency().equals(baseCurrency))
                .forEach(rate -> rates.put(rate.getTargetCurrency(), rate.getRate()));
        
        if (!rates.isEmpty()) {
            log.info("Retrieved {} rates from database for base currency {}", 
                    rates.size(), baseCurrency);
            
            // Store in cache for future requests if cache is available
            if (cacheService != null && cacheService.isAvailable()) {
                cacheService.storeRates(baseCurrency, rates);
            }
        }
        
        return rates;
    }
    
    /**
     * Check if rate data is available (either in cache or database)
     * 
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return true if rate is available
     */
    public boolean isRateAvailable(String baseCurrency, String targetCurrency) {
        return getRate(baseCurrency, targetCurrency).isPresent();
    }
}
