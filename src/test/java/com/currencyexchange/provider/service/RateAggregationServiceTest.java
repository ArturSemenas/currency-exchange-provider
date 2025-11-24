package com.currencyexchange.provider.service;

import com.currencyexchange.provider.client.ExchangeRateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateAggregationService
 * Tests rate aggregation from multiple providers with mocked providers
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateAggregationService Unit Tests")
class RateAggregationServiceTest {

    @Mock
    private ExchangeRateProvider provider1;

    @Mock
    private ExchangeRateProvider provider2;

    @Mock
    private ExchangeRateProvider provider3;

    private RateAggregationService rateAggregationService;

    @BeforeEach
    void setUp() {
        List<ExchangeRateProvider> providers = Arrays.asList(provider1, provider2, provider3);
        rateAggregationService = new RateAggregationService(providers);
    }

    @Test
    @DisplayName("Should get rates for specific currency pair from all providers")
    void getRatesForPair_ShouldReturnRatesFromAllProviders() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider1.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.85"));

        when(provider2.isAvailable()).thenReturn(true);
        when(provider2.getProviderName()).thenReturn("Provider2");
        when(provider2.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.87"));

        when(provider3.isAvailable()).thenReturn(true);
        when(provider3.getProviderName()).thenReturn("Provider3");
        when(provider3.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.86"));

        // Act
        Map<String, BigDecimal> result = rateAggregationService.getRatesForPair("USD", "EUR");

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result.get("Provider1")).isEqualByComparingTo("0.85");
        assertThat(result.get("Provider2")).isEqualByComparingTo("0.87");
        assertThat(result.get("Provider3")).isEqualByComparingTo("0.86");
        
        verify(provider1, times(1)).fetchLatestRates("USD");
        verify(provider2, times(1)).fetchLatestRates("USD");
        verify(provider3, times(1)).fetchLatestRates("USD");
    }

    @Test
    @DisplayName("Should skip unavailable providers")
    void getRatesForPair_ShouldSkipUnavailableProviders() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider1.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.85"));

        when(provider2.isAvailable()).thenReturn(false); // Unavailable

        when(provider3.isAvailable()).thenReturn(true);
        when(provider3.getProviderName()).thenReturn("Provider3");
        when(provider3.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.86"));

        // Act
        Map<String, BigDecimal> result = rateAggregationService.getRatesForPair("USD", "EUR");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsKeys("Provider1", "Provider3");
        assertThat(result).doesNotContainKey("Provider2");
        
        verify(provider1, times(1)).fetchLatestRates("USD");
        verify(provider2, never()).fetchLatestRates(anyString());
        verify(provider3, times(1)).fetchLatestRates("USD");
    }

    @Test
    @DisplayName("Should handle provider errors gracefully")
    void getRatesForPair_ShouldHandleProviderErrors() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider1.fetchLatestRates("USD"))
                .thenThrow(new RuntimeException("Provider error"));

        when(provider2.isAvailable()).thenReturn(true);
        when(provider2.getProviderName()).thenReturn("Provider2");
        when(provider2.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.87"));

        // Act
        Map<String, BigDecimal> result = rateAggregationService.getRatesForPair("USD", "EUR");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey("Provider2");
        assertThat(result.get("Provider2")).isEqualByComparingTo("0.87");
    }

    @Test
    @DisplayName("Should return empty map when target currency not found")
    void getRatesForPair_ShouldReturnEmpty_WhenTargetCurrencyNotFound() {
        // Arrange - Providers return rates but not for the target currency
        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.85"));

        when(provider2.isAvailable()).thenReturn(true);
        when(provider2.fetchLatestRates("USD")).thenReturn(createRates("USD", "GBP", "0.75"));

        when(provider3.isAvailable()).thenReturn(false);

        // Act - Looking for JPY which doesn't exist in the returned rates
        Map<String, BigDecimal> result = rateAggregationService.getRatesForPair("USD", "JPY");

        // Assert
        assertThat(result).isEmpty();
        verify(provider1).fetchLatestRates("USD");
        verify(provider2).fetchLatestRates("USD");
    }

    @Test
    @DisplayName("Should return empty map when no providers available")
    void getRatesForPair_ShouldReturnEmpty_WhenNoProvidersAvailable() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(false);
        when(provider2.isAvailable()).thenReturn(false);
        when(provider3.isAvailable()).thenReturn(false);

        // Act
        Map<String, BigDecimal> result = rateAggregationService.getRatesForPair("USD", "EUR");

        // Assert
        assertThat(result).isEmpty();
        verify(provider1, never()).fetchLatestRates(anyString());
        verify(provider2, never()).fetchLatestRates(anyString());
        verify(provider3, never()).fetchLatestRates(anyString());
    }

    @Test
    @DisplayName("Should count available providers")
    void getAvailableProvidersCount_ShouldReturnCorrectCount() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(true);
        when(provider2.isAvailable()).thenReturn(false);
        when(provider3.isAvailable()).thenReturn(true);

        // Act
        long count = rateAggregationService.getAvailableProvidersCount();

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero when no providers available for count")
    void getAvailableProvidersCount_ShouldReturnZero_WhenNoProvidersAvailable() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(false);
        when(provider2.isAvailable()).thenReturn(false);
        when(provider3.isAvailable()).thenReturn(false);

        // Act
        long count = rateAggregationService.getAvailableProvidersCount();

        // Assert
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should handle provider returning null rates")
    void getRatesForPair_ShouldSkipProvider_WhenReturnsNull() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider1.fetchLatestRates("USD")).thenReturn(null);

        when(provider2.isAvailable()).thenReturn(true);
        when(provider2.getProviderName()).thenReturn("Provider2");
        when(provider2.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.87"));

        // Act
        Map<String, BigDecimal> result = rateAggregationService.getRatesForPair("USD", "EUR");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey("Provider2");
    }

    @Test
    @DisplayName("Should handle provider returning empty rates map")
    void getRatesForPair_ShouldHandleEmptyRates() {
        // Arrange
        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.fetchLatestRates("USD")).thenReturn(Collections.emptyMap());

        when(provider2.isAvailable()).thenReturn(true);
        when(provider2.getProviderName()).thenReturn("Provider2");
        when(provider2.fetchLatestRates("USD")).thenReturn(createRates("USD", "EUR", "0.87"));

        when(provider3.isAvailable()).thenReturn(false);

        // Act
        Map<String, BigDecimal> result = rateAggregationService.getRatesForPair("USD", "EUR");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get("Provider2")).isEqualByComparingTo("0.87");
        verify(provider1).fetchLatestRates("USD");
        verify(provider2).fetchLatestRates("USD");
    }

    @Test
    @DisplayName("Should handle multiple currency pairs from same provider")
    void getRatesForPair_ShouldWorkWithMultipleCurrencyPairs() {
        // Arrange
        Map<String, BigDecimal> provider1Rates = new HashMap<>();
        provider1Rates.put("EUR", new BigDecimal("0.85"));
        provider1Rates.put("GBP", new BigDecimal("0.75"));
        provider1Rates.put("JPY", new BigDecimal("110.50"));

        when(provider1.isAvailable()).thenReturn(true);
        when(provider1.getProviderName()).thenReturn("Provider1");
        when(provider1.fetchLatestRates("USD")).thenReturn(provider1Rates);

        // Act
        Map<String, BigDecimal> eurResult = rateAggregationService.getRatesForPair("USD", "EUR");
        Map<String, BigDecimal> gbpResult = rateAggregationService.getRatesForPair("USD", "GBP");
        Map<String, BigDecimal> jpyResult = rateAggregationService.getRatesForPair("USD", "JPY");

        // Assert
        assertThat(eurResult.get("Provider1")).isEqualByComparingTo("0.85");
        assertThat(gbpResult.get("Provider1")).isEqualByComparingTo("0.75");
        assertThat(jpyResult.get("Provider1")).isEqualByComparingTo("110.50");
    }

    /**
     * Helper method to create a rates map
     */
    private Map<String, BigDecimal> createRates(String base, String target, String rate) {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put(target, new BigDecimal(rate));
        return rates;
    }
}
