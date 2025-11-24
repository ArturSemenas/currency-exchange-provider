package com.currencyexchange.provider.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Interface for external exchange rate providers
 */
public interface ExchangeRateProvider {
    
    /**
     * Fetch latest exchange rates for a base currency
     * 
     * @param baseCurrency the base currency code (e.g., "USD")
     * @return Map of target currency codes to exchange rates
     */
    Map<String, BigDecimal> fetchLatestRates(String baseCurrency);
    
    /**
     * Fetch historical exchange rate for a specific date
     * 
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @param date the date for historical rate
     * @return the exchange rate, or null if not available
     */
    BigDecimal fetchHistoricalRate(String baseCurrency, String targetCurrency, LocalDate date);
    
    /**
     * Get the name of this provider
     * 
     * @return provider name (e.g., "fixer.io", "exchangeratesapi.io")
     */
    String getProviderName();
    
    /**
     * Check if provider is currently available
     * 
     * @return true if provider is reachable and operational
     */
    boolean isAvailable();
}
