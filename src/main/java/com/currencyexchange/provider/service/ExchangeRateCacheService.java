package com.currencyexchange.provider.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Service for caching exchange rates in Redis
 * Storage structure: Hash with key "rates:{baseCurrency}", field "{targetCurrency}", value "{rate}"
 * Falls back gracefully if Redis is not available
 */
@Slf4j
@Service
public class ExchangeRateCacheService {
    
    private static final String RATES_KEY_PREFIX = "rates:";
    private static final long TTL_HOURS = 2;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Store exchange rates for a base currency
     * 
     * @param baseCurrency the base currency code
     * @param rates map of target currency codes to exchange rates
     */
    public void storeRates(String baseCurrency, Map<String, BigDecimal> rates) {
        if (redisTemplate == null) {
            log.debug("Redis not available, skipping cache storage for {}", baseCurrency);
            return;
        }
        
        try {
            String key = RATES_KEY_PREFIX + baseCurrency;
            
            // Store each rate as a hash field
            rates.forEach((targetCurrency, rate) -> {
                redisTemplate.opsForHash().put(key, targetCurrency, rate);
            });
            
            // Set expiration time
            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
            
            log.info("Stored {} rates for base currency {} in Redis", rates.size(), baseCurrency);
        } catch (Exception e) {
            log.error("Failed to store rates in Redis for base currency {}: {}", 
                    baseCurrency, e.getMessage(), e);
        }
    }
    
    /**
     * Store best rates from multiple providers
     * 
     * @param bestRates map of base currency to map of target currencies to rates
     */
    public void storeBestRates(Map<String, Map<String, BigDecimal>> bestRates) {
        if (redisTemplate == null) {
            log.debug("Redis not available, skipping cache storage for best rates");
            return;
        }
        
        try {
            bestRates.forEach(this::storeRates);
            log.info("Stored best rates for {} base currencies", bestRates.size());
        } catch (Exception e) {
            log.error("Failed to store best rates in Redis: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get exchange rate from cache
     * 
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @return Optional containing the rate if found in cache
     */
    public Optional<BigDecimal> getRate(String baseCurrency, String targetCurrency) {
        if (redisTemplate == null) {
            return Optional.empty();
        }
        
        try {
            String key = RATES_KEY_PREFIX + baseCurrency;
            Object rate = redisTemplate.opsForHash().get(key, targetCurrency);
            
            if (rate != null) {
                BigDecimal rateValue = rate instanceof BigDecimal
                        ? (BigDecimal) rate : new BigDecimal(rate.toString());
                log.debug("Retrieved rate from cache: {} -> {} = {}", 
                        baseCurrency, targetCurrency, rateValue);
                return Optional.of(rateValue);
            }
            
            log.debug("Rate not found in cache: {} -> {}", baseCurrency, targetCurrency);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving rate from Redis: {} -> {}: {}", 
                    baseCurrency, targetCurrency, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Get all rates for a base currency from cache
     * 
     * @param baseCurrency the base currency code
     * @return map of target currencies to rates, or empty map if not in cache
     */
    public Map<String, BigDecimal> getAllRates(String baseCurrency) {
        if (redisTemplate == null) {
            return Map.of();
        }
        
        try {
            String key = RATES_KEY_PREFIX + baseCurrency;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            
            Map<String, BigDecimal> rates = new HashMap<>();
            entries.forEach((targetCurrency, rate) -> {
                BigDecimal rateValue = rate instanceof BigDecimal
                        ? (BigDecimal) rate : new BigDecimal(rate.toString());
                rates.put(targetCurrency.toString(), rateValue);
            });
            
            if (!rates.isEmpty()) {
                log.debug("Retrieved {} rates from cache for base currency {}", 
                        rates.size(), baseCurrency);
            }
            
            return rates;
        } catch (Exception e) {
            log.error("Error retrieving all rates from Redis for base currency {}: {}", 
                    baseCurrency, e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Get all cached rates (all base currencies)
     * 
     * @return map of base currencies to map of target currencies to rates
     */
    public Map<String, Map<String, BigDecimal>> getAllCachedRates() {
        try {
            Map<String, Map<String, BigDecimal>> allRates = new HashMap<>();
            Set<String> keys = redisTemplate.keys(RATES_KEY_PREFIX + "*");
            
            if (keys != null) {
                keys.forEach(key -> {
                    String baseCurrency = key.substring(RATES_KEY_PREFIX.length());
                    Map<String, BigDecimal> rates = getAllRates(baseCurrency);
                    if (!rates.isEmpty()) {
                        allRates.put(baseCurrency, rates);
                    }
                });
            }
            
            log.debug("Retrieved cached rates for {} base currencies", allRates.size());
            return allRates;
        } catch (Exception e) {
            log.error("Error retrieving all cached rates from Redis: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Evict all cached rates
     */
    public void evictAll() {
        if (redisTemplate == null) {
            log.debug("Redis not available, skipping cache eviction");
            return;
        }
        
        try {
            Set<String> keys = redisTemplate.keys(RATES_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} rate entries from cache", keys.size());
            }
        } catch (Exception e) {
            log.error("Error evicting cache: {}", e.getMessage());
        }
    }
    
    /**
     * Evict rates for a specific base currency
     * 
     * @param baseCurrency the base currency code
     */
    public void evictRates(String baseCurrency) {
        if (redisTemplate == null) {
            log.debug("Redis not available, skipping cache eviction for {}", baseCurrency);
            return;
        }
        
        try {
            String key = RATES_KEY_PREFIX + baseCurrency;
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.info("Evicted rates for base currency {} from cache", baseCurrency);
            }
        } catch (Exception e) {
            log.error("Error evicting rates for base currency {}: {}", 
                    baseCurrency, e.getMessage());
        }
    }
    
    /**
     * Check if Redis is available
     * 
     * @return true if Redis is reachable
     */
    public boolean isAvailable() {
        if (redisTemplate == null) {
            return false;
        }
        
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.warn("Redis is not available: {}", e.getMessage());
            return false;
        }
    }
}
