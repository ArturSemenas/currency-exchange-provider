package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.ExchangeRate;
import com.currencyexchange.provider.repository.ExchangeRateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExchangeRateRetrievalService.
 *
 * @author Artur Semenas
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateRetrievalService Unit Tests")
class ExchangeRateRetrievalServiceTest {

    @Mock
    private ExchangeRateCacheService cacheService;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateRetrievalService retrievalService;

    @Captor
    private ArgumentCaptor<Map<String, BigDecimal>> ratesCaptor;

    private static final String BASE_CURRENCY = "USD";
    private static final String TARGET_CURRENCY = "EUR";
    private static final BigDecimal RATE = new BigDecimal("0.85");

    @Test
    @DisplayName("getRate should return rate from cache when available")
    void getRate_ShouldReturnFromCache_WhenAvailable() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.of(RATE));

        // Act
        Optional<BigDecimal> result = retrievalService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(RATE);
        verify(cacheService).getRate(BASE_CURRENCY, TARGET_CURRENCY);
        verify(exchangeRateRepository, never()).findLatestRate(anyString(), anyString());
    }

    @Test
    @DisplayName("getRate should fallback to database when not in cache")
    void getRate_ShouldFallbackToDatabase_WhenNotInCache() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());
        
        ExchangeRate exchangeRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, RATE);
        when(exchangeRateRepository.findLatestRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.of(exchangeRate));
        
        when(cacheService.isAvailable()).thenReturn(true);

        // Act
        Optional<BigDecimal> result = retrievalService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(RATE);
        verify(cacheService).getRate(BASE_CURRENCY, TARGET_CURRENCY);
        verify(exchangeRateRepository).findLatestRate(BASE_CURRENCY, TARGET_CURRENCY);
        verify(cacheService).storeRates(eq(BASE_CURRENCY), any());
    }

    @Test
    @DisplayName("getRate should return empty when not found in cache or database")
    void getRate_ShouldReturnEmpty_WhenNotFound() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());

        // Act
        Optional<BigDecimal> result = retrievalService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
        verify(cacheService).getRate(BASE_CURRENCY, TARGET_CURRENCY);
        verify(exchangeRateRepository).findLatestRate(BASE_CURRENCY, TARGET_CURRENCY);
        verify(cacheService, never()).storeRates(anyString(), any());
    }

    @Test
    @DisplayName("getRate should not cache when Redis is unavailable")
    void getRate_ShouldNotCache_WhenRedisUnavailable() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());
        
        ExchangeRate exchangeRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, RATE);
        when(exchangeRateRepository.findLatestRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.of(exchangeRate));
        
        when(cacheService.isAvailable()).thenReturn(false);

        // Act
        Optional<BigDecimal> result = retrievalService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(RATE);
        verify(cacheService).isAvailable();
        verify(cacheService, never()).storeRates(anyString(), any());
    }

    @Test
    @DisplayName("getRate should cache rate from database when Redis is available")
    void getRate_ShouldCacheRateFromDatabase_WhenRedisAvailable() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());
        
        ExchangeRate exchangeRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, RATE);
        when(exchangeRateRepository.findLatestRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.of(exchangeRate));
        
        when(cacheService.isAvailable()).thenReturn(true);

        // Act
        retrievalService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        verify(cacheService).storeRates(eq(BASE_CURRENCY), ratesCaptor.capture());
        Map<String, BigDecimal> cachedRates = ratesCaptor.getValue();
        assertThat(cachedRates).containsEntry(TARGET_CURRENCY, RATE);
    }

    @Test
    @DisplayName("getAllRates should return from cache when available")
    void getAllRates_ShouldReturnFromCache_WhenAvailable() {
        // Arrange
        Map<String, BigDecimal> cachedRates = new HashMap<>();
        cachedRates.put("EUR", new BigDecimal("0.85"));
        cachedRates.put("GBP", new BigDecimal("0.75"));
        
        when(cacheService.getAllRates(BASE_CURRENCY)).thenReturn(cachedRates);

        // Act
        Map<String, BigDecimal> result = retrievalService.getAllRates(BASE_CURRENCY);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("EUR", "GBP");
        verify(cacheService).getAllRates(BASE_CURRENCY);
        verify(exchangeRateRepository, never()).findAllLatestRates();
    }

    @Test
    @DisplayName("getAllRates should fallback to database when cache is empty")
    void getAllRates_ShouldFallbackToDatabase_WhenCacheEmpty() {
        // Arrange
        when(cacheService.getAllRates(BASE_CURRENCY)).thenReturn(new HashMap<>());
        
        List<ExchangeRate> dbRates = new ArrayList<>();
        dbRates.add(createExchangeRate(BASE_CURRENCY, "EUR", new BigDecimal("0.85")));
        dbRates.add(createExchangeRate(BASE_CURRENCY, "GBP", new BigDecimal("0.75")));
        dbRates.add(createExchangeRate("EUR", "USD", new BigDecimal("1.18"))); // Different base
        
        when(exchangeRateRepository.findAllLatestRates()).thenReturn(dbRates);
        when(cacheService.isAvailable()).thenReturn(true);

        // Act
        Map<String, BigDecimal> result = retrievalService.getAllRates(BASE_CURRENCY);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("EUR", "GBP");
        assertThat(result).doesNotContainKey("USD");
        verify(exchangeRateRepository).findAllLatestRates();
        verify(cacheService).storeRates(eq(BASE_CURRENCY), any());
    }

    @Test
    @DisplayName("getAllRates should return empty map when no rates in database")
    void getAllRates_ShouldReturnEmptyMap_WhenNoRatesInDatabase() {
        // Arrange
        when(cacheService.getAllRates(BASE_CURRENCY)).thenReturn(new HashMap<>());
        when(exchangeRateRepository.findAllLatestRates()).thenReturn(new ArrayList<>());

        // Act
        Map<String, BigDecimal> result = retrievalService.getAllRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
        verify(cacheService, never()).storeRates(anyString(), any());
    }

    @Test
    @DisplayName("getAllRates should not cache when Redis is unavailable")
    void getAllRates_ShouldNotCache_WhenRedisUnavailable() {
        // Arrange
        when(cacheService.getAllRates(BASE_CURRENCY)).thenReturn(new HashMap<>());
        
        List<ExchangeRate> dbRates = new ArrayList<>();
        dbRates.add(createExchangeRate(BASE_CURRENCY, "EUR", new BigDecimal("0.85")));
        
        when(exchangeRateRepository.findAllLatestRates()).thenReturn(dbRates);
        when(cacheService.isAvailable()).thenReturn(false);

        // Act
        Map<String, BigDecimal> result = retrievalService.getAllRates(BASE_CURRENCY);

        // Assert
        assertThat(result).hasSize(1);
        verify(cacheService).isAvailable();
        verify(cacheService, never()).storeRates(anyString(), any());
    }

    @Test
    @DisplayName("getAllRates should filter only matching base currency")
    void getAllRates_ShouldFilterByBaseCurrency() {
        // Arrange
        when(cacheService.getAllRates(BASE_CURRENCY)).thenReturn(new HashMap<>());
        
        List<ExchangeRate> dbRates = new ArrayList<>();
        dbRates.add(createExchangeRate("USD", "EUR", new BigDecimal("0.85")));
        dbRates.add(createExchangeRate("USD", "GBP", new BigDecimal("0.75")));
        dbRates.add(createExchangeRate("EUR", "USD", new BigDecimal("1.18")));
        dbRates.add(createExchangeRate("EUR", "GBP", new BigDecimal("0.88")));
        
        when(exchangeRateRepository.findAllLatestRates()).thenReturn(dbRates);
        when(cacheService.isAvailable()).thenReturn(true);

        // Act
        Map<String, BigDecimal> result = retrievalService.getAllRates("USD");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsOnlyKeys("EUR", "GBP");
        assertThat(result.get("EUR")).isEqualByComparingTo("0.85");
        assertThat(result.get("GBP")).isEqualByComparingTo("0.75");
    }

    @Test
    @DisplayName("getAllRates should cache rates from database when Redis is available")
    void getAllRates_ShouldCacheRatesFromDatabase() {
        // Arrange
        when(cacheService.getAllRates(BASE_CURRENCY)).thenReturn(new HashMap<>());
        
        List<ExchangeRate> dbRates = new ArrayList<>();
        dbRates.add(createExchangeRate(BASE_CURRENCY, "EUR", new BigDecimal("0.85")));
        dbRates.add(createExchangeRate(BASE_CURRENCY, "GBP", new BigDecimal("0.75")));
        
        when(exchangeRateRepository.findAllLatestRates()).thenReturn(dbRates);
        when(cacheService.isAvailable()).thenReturn(true);

        // Act
        retrievalService.getAllRates(BASE_CURRENCY);

        // Assert
        verify(cacheService).storeRates(eq(BASE_CURRENCY), ratesCaptor.capture());
        Map<String, BigDecimal> cachedRates = ratesCaptor.getValue();
        assertThat(cachedRates).hasSize(2);
        assertThat(cachedRates).containsKeys("EUR", "GBP");
    }

    @Test
    @DisplayName("isRateAvailable should return true when rate exists in cache")
    void isRateAvailable_ShouldReturnTrue_WhenInCache() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.of(RATE));

        // Act
        boolean result = retrievalService.isRateAvailable(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isTrue();
        verify(cacheService).getRate(BASE_CURRENCY, TARGET_CURRENCY);
    }

    @Test
    @DisplayName("isRateAvailable should return true when rate exists in database")
    void isRateAvailable_ShouldReturnTrue_WhenInDatabase() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());
        
        ExchangeRate exchangeRate = createExchangeRate(BASE_CURRENCY, TARGET_CURRENCY, RATE);
        when(exchangeRateRepository.findLatestRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.of(exchangeRate));
        
        when(cacheService.isAvailable()).thenReturn(true);

        // Act
        boolean result = retrievalService.isRateAvailable(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isTrue();
        verify(exchangeRateRepository).findLatestRate(BASE_CURRENCY, TARGET_CURRENCY);
    }

    @Test
    @DisplayName("isRateAvailable should return false when rate does not exist")
    void isRateAvailable_ShouldReturnFalse_WhenNotFound() {
        // Arrange
        when(cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findLatestRate(BASE_CURRENCY, TARGET_CURRENCY))
                .thenReturn(Optional.empty());

        // Act
        boolean result = retrievalService.isRateAvailable(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isFalse();
    }

    /**
     * Helper method to create an ExchangeRate entity.
     *
     * @param base the base currency code
     * @param target the target currency code
     * @param rate the exchange rate
     * @return the created ExchangeRate entity
     */
    private ExchangeRate createExchangeRate(String base, String target, BigDecimal rate) {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setBaseCurrency(base);
        exchangeRate.setTargetCurrency(target);
        exchangeRate.setRate(rate);
        exchangeRate.setTimestamp(LocalDateTime.now());
        return exchangeRate;
    }
}
