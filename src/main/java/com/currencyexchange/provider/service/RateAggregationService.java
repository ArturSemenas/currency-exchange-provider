package com.currencyexchange.provider.service;

import com.currencyexchange.provider.client.ExchangeRateProvider;
import com.currencyexchange.provider.model.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Service for aggregating exchange rates from multiple providers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateAggregationService {
    
    private final List<ExchangeRateProvider> providers;
    private final CurrencyService currencyService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    /**
     * Aggregate best rates from all providers
     * Fetches rates concurrently from all providers and selects the best (highest) rate for each currency pair
     * Only fetches rates for currencies that are registered in the database
     * 
     * @return map of base currency to map of target currencies to best rates
     */
    public Map<String, Map<String, BigDecimal>> aggregateBestRates() {
        log.info("Aggregating rates from {} providers", providers.size());
        
        // Get supported currencies from database
        List<String> baseCurrencies = currencyService.getAllCurrencies().stream()
                .map(Currency::getCode)
                .collect(Collectors.toList());
        
        if (baseCurrencies.isEmpty()) {
            log.warn("No currencies found in database. Cannot fetch exchange rates.");
            return new HashMap<>();
        }
        
        log.info("Fetching rates for {} supported currencies: {}", 
                baseCurrencies.size(), baseCurrencies);
        
        // Fetch rates concurrently from all providers for all base currencies
        Map<String, Map<String, Map<String, BigDecimal>>> allProviderRates = 
                fetchRatesFromAllProviders(baseCurrencies);
        
        // Aggregate best rates using Stream API
        Map<String, Map<String, BigDecimal>> bestRates = new HashMap<>();
        
        for (String baseCurrency : baseCurrencies) {
            Map<String, BigDecimal> currencyBestRates = aggregateBestRatesForBase(
                    baseCurrency, allProviderRates);
            
            if (!currencyBestRates.isEmpty()) {
                bestRates.put(baseCurrency, currencyBestRates);
            }
        }
        
        log.info("Aggregated best rates for {} base currencies with total {} currency pairs", 
                bestRates.size(), 
                bestRates.values().stream().mapToInt(Map::size).sum());
        
        return bestRates;
    }
    
    /**
     * Get rates for a specific currency pair from all providers
     * 
     * @param baseCurrency the base currency
     * @param targetCurrency the target currency
     * @return map of provider names to rates
     */
    public Map<String, BigDecimal> getRatesForPair(String baseCurrency, String targetCurrency) {
        log.debug("Fetching rates for {} -> {} from all providers", baseCurrency, targetCurrency);
        
        return providers.stream()
                .filter(ExchangeRateProvider::isAvailable)
                .map(provider -> {
                    try {
                        Map<String, BigDecimal> rates = provider.fetchLatestRates(baseCurrency);
                        BigDecimal rate = rates.get(targetCurrency);
                        return rate != null
                                ? Map.entry(provider.getProviderName(), rate) : null;
                    } catch (Exception e) {
                        log.error("Error fetching rate from {}: {}", 
                                provider.getProviderName(), e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Fetch rates from all providers concurrently
     */
    private Map<String, Map<String, Map<String, BigDecimal>>> fetchRatesFromAllProviders(
            List<String> baseCurrencies) {
        
        Map<String, Map<String, Map<String, BigDecimal>>> result = new HashMap<>();
        
        // Create futures for concurrent fetching
        List<CompletableFuture<Map.Entry<String, Map<String, BigDecimal>>>> futures = 
                providers.stream()
                        .filter(ExchangeRateProvider::isAvailable)
                        .flatMap(provider -> baseCurrencies.stream()
                                .map(baseCurrency -> CompletableFuture.supplyAsync(() -> 
                                        fetchFromProvider(provider, baseCurrency), executorService)))
                        .collect(Collectors.toList());
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Collect results using Stream API
        futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .forEach(entry -> {
                    String key = entry.getKey();
                    String[] parts = key.split(":");
                    String providerName = parts[0];
                    String baseCurrency = parts[1];
                    
                    result.computeIfAbsent(providerName, k -> new HashMap<>())
                           .put(baseCurrency, entry.getValue());
                });
        
        return result;
    }
    
    /**
     * Fetch rates from a single provider
     */
    private Map.Entry<String, Map<String, BigDecimal>> fetchFromProvider(
            ExchangeRateProvider provider, String baseCurrency) {
        
        try {
            Map<String, BigDecimal> rates = provider.fetchLatestRates(baseCurrency);
            String key = provider.getProviderName() + ":" + baseCurrency;
            log.debug("Fetched {} rates from {} for {}", 
                    rates.size(), provider.getProviderName(), baseCurrency);
            return Map.entry(key, rates);
        } catch (Exception e) {
            log.error("Error fetching rates from {} for {}: {}", 
                    provider.getProviderName(), baseCurrency, e.getMessage());
            return null;
        }
    }
    
    /**
     * Aggregate best rates for a specific base currency
     * Only includes target currencies that are registered in the database
     */
    private Map<String, BigDecimal> aggregateBestRatesForBase(
            String baseCurrency, 
            Map<String, Map<String, Map<String, BigDecimal>>> allProviderRates) {
        
        // Get list of supported currency codes for filtering
        List<String> supportedCurrencies = currencyService.getAllCurrencies().stream()
                .map(Currency::getCode)
                .collect(Collectors.toList());
        
        // Collect all rates for this base currency from all providers
        Map<String, List<BigDecimal>> ratesByTarget = new HashMap<>();
        
        allProviderRates.values().stream()
                .map(providerRates -> providerRates.get(baseCurrency))
                .filter(Objects::nonNull)
                .forEach(rates -> rates.forEach((target, rate) -> {
                    // Only include rates for supported currencies
                    if (supportedCurrencies.contains(target)) {
                        ratesByTarget.computeIfAbsent(target, k -> new ArrayList<>()).add(rate);
                    } else {
                        log.debug("Skipping unsupported currency: {}", target);
                    }
                }));
        
        // Select best (max) rate for each target currency using Stream API
        return ratesByTarget.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .max(Comparator.naturalOrder())
                                .orElse(BigDecimal.ZERO)
                ));
    }
    
    /**
     * Get count of available providers
     * 
     * @return number of available providers
     */
    public long getAvailableProvidersCount() {
        return providers.stream()
                .filter(ExchangeRateProvider::isAvailable)
                .count();
    }
}
