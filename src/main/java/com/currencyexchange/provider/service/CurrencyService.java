package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.Currency;
import com.currencyexchange.provider.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing currencies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {
    
    private final CurrencyRepository currencyRepository;
    
    /**
     * Get all supported currencies
     * 
     * @return list of all currencies
     */
    @Transactional(readOnly = true)
    public List<Currency> getAllCurrencies() {
        log.debug("Retrieving all currencies");
        List<Currency> currencies = currencyRepository.findAll();
        log.info("Retrieved {} currencies", currencies.size());
        return currencies;
    }
    
    /**
     * Add a new currency with validation against ISO 4217
     * 
     * @param code the ISO 4217 currency code (e.g., "USD", "EUR")
     * @return the created currency
     * @throws IllegalArgumentException if currency code is invalid or already exists
     */
    @Transactional
    public Currency addCurrency(String code) {
        log.debug("Attempting to add currency: {}", code);
        
        // Validate currency code
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        
        String upperCode = code.trim().toUpperCase();
        
        // Validate against java.util.Currency (ISO 4217)
        Optional<java.util.Currency> javaCurrency = getJavaCurrency(upperCode);
        if (javaCurrency.isEmpty()) {
            log.warn("Invalid currency code: {}", upperCode);
            throw new IllegalArgumentException(
                    "Invalid currency code: " + upperCode + ". Must be a valid ISO 4217 code.");
        }
        
        // Check if currency already exists
        Optional<Currency> existing = currencyRepository.findByCode(upperCode);
        if (existing.isPresent()) {
            log.warn("Currency already exists: {}", upperCode);
            throw new IllegalArgumentException("Currency already exists: " + upperCode);
        }
        
        // Create and save new currency
        Currency currency = Currency.builder()
                .code(upperCode)
                .name(javaCurrency.get().getDisplayName())
                .build();
        
        Currency saved = currencyRepository.save(currency);
        log.info("Successfully added currency: {} - {}", saved.getCode(), saved.getName());
        
        return saved;
    }
    
    /**
     * Get currency by code
     * 
     * @param code the currency code
     * @return Optional containing the currency if found
     */
    @Transactional(readOnly = true)
    public Optional<Currency> getCurrencyByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        return currencyRepository.findByCode(code.trim().toUpperCase());
    }
    
    /**
     * Check if currency exists
     * 
     * @param code the currency code
     * @return true if currency exists
     */
    @Transactional(readOnly = true)
    public boolean currencyExists(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return currencyRepository.findByCode(code.trim().toUpperCase()).isPresent();
    }
    
    /**
     * Validate currency code against ISO 4217 standard
     * 
     * @param code the currency code to validate
     * @return true if valid ISO 4217 code
     */
    public boolean isValidCurrencyCode(String code) {
        return getJavaCurrency(code).isPresent();
    }
    
    /**
     * Helper method to get java.util.Currency safely
     */
    private Optional<java.util.Currency> getJavaCurrency(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(java.util.Currency.getInstance(code));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
