package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.ExchangeRate;
import com.currencyexchange.provider.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateService
 * Tests exchange rate retrieval, conversion, and refresh operations
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService Unit Tests")
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private ExchangeRateCacheService cacheService;

    @Mock
    private ExchangeRateRetrievalService retrievalService;

    @Mock
    private RateAggregationService aggregationService;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRate eurToUsdRate;

    @BeforeEach
    void setUp() {
        eurToUsdRate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("EUR")
                .targetCurrency("USD")
                .rate(new BigDecimal("1.20"))
                .timestamp(LocalDateTime.now())
                .provider("test")
                .build();
    }

    @Test
    @DisplayName("Should convert amount using exchange rate")
    void getExchangeRate_ShouldConvertAmount_WhenRateExists() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        when(retrievalService.getRate("EUR", "USD"))
                .thenReturn(Optional.of(new BigDecimal("1.20")));

        // Act
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(amount, "EUR", "USD");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("120.00");
        verify(retrievalService, times(1)).getRate("EUR", "USD");
    }

    @Test
    @DisplayName("Should return same amount when currencies are the same")
    void getExchangeRate_ShouldReturnSameAmount_WhenSameCurrency() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(amount, "USD", "USD");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("100.00");
        verify(retrievalService, never()).getRate(any(), any());
    }

    @Test
    @DisplayName("Should return empty when rate not found")
    void getExchangeRate_ShouldReturnEmpty_WhenRateNotFound() {
        // Arrange
        when(retrievalService.getRate("EUR", "XYZ"))
                .thenReturn(Optional.empty());

        // Act
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(
                new BigDecimal("100"), "EUR", "XYZ");

        // Assert
        assertThat(result).isEmpty();
        verify(retrievalService, times(1)).getRate("EUR", "XYZ");
    }

    @Test
    @DisplayName("Should round converted amount to 2 decimal places")
    void getExchangeRate_ShouldRoundToTwoDecimals() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        when(retrievalService.getRate("EUR", "USD"))
                .thenReturn(Optional.of(new BigDecimal("1.234567")));

        // Act
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(amount, "EUR", "USD");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("123.46"); // Rounded up
        assertThat(result.get().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle case-insensitive currency comparison")
    void getExchangeRate_ShouldBeCaseInsensitive_WhenSameCurrency() {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");

        // Act
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(amount, "usd", "USD");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Should refresh rates from all providers successfully")
    void refreshRates_ShouldUpdateRates_WhenProvidersReturnData() {
        // Arrange
        Map<String, Map<String, BigDecimal>> bestRates = new HashMap<>();
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        usdRates.put("GBP", new BigDecimal("0.75"));
        bestRates.put("USD", usdRates);

        when(aggregationService.aggregateBestRates()).thenReturn(bestRates);
        when(exchangeRateRepository.save(any(ExchangeRate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int result = exchangeRateService.refreshRates();

        // Assert
        assertThat(result).isEqualTo(2); // 2 rates saved
        verify(aggregationService, times(1)).aggregateBestRates();
        verify(exchangeRateRepository, times(2)).save(any(ExchangeRate.class));
        verify(cacheService, times(1)).evictAll();
        verify(cacheService, times(1)).storeBestRates(bestRates);
    }

    @Test
    @DisplayName("Should return zero when no rates available from providers")
    void refreshRates_ShouldReturnZero_WhenNoRatesAvailable() {
        // Arrange
        when(aggregationService.aggregateBestRates()).thenReturn(Collections.emptyMap());

        // Act
        int result = exchangeRateService.refreshRates();

        // Assert
        assertThat(result).isZero();
        verify(aggregationService, times(1)).aggregateBestRates();
        verify(exchangeRateRepository, never()).save(any());
        verify(cacheService, never()).evictAll();
    }

    @Test
    @DisplayName("Should throw exception when refresh fails")
    void refreshRates_ShouldThrowException_WhenAggregationFails() {
        // Arrange
        when(aggregationService.aggregateBestRates())
                .thenThrow(new RuntimeException("Provider error"));

        // Act & Assert
        assertThatThrownBy(() -> exchangeRateService.refreshRates())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to refresh exchange rates");

        verify(aggregationService, times(1)).aggregateBestRates();
        verify(exchangeRateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get best rate from multiple providers")
    void getBestRate_ShouldReturnHighestRate_WhenMultipleProvidersHaveRates() {
        // Arrange
        Map<String, BigDecimal> providerRates = new HashMap<>();
        providerRates.put("provider1", new BigDecimal("1.18"));
        providerRates.put("provider2", new BigDecimal("1.22"));
        providerRates.put("provider3", new BigDecimal("1.20"));

        when(aggregationService.getRatesForPair("EUR", "USD")).thenReturn(providerRates);

        // Act
        Optional<BigDecimal> result = exchangeRateService.getBestRate("EUR", "USD");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("1.22"); // Highest rate
        verify(aggregationService, times(1)).getRatesForPair("EUR", "USD");
    }

    @Test
    @DisplayName("Should return empty when no provider rates available")
    void getBestRate_ShouldReturnEmpty_WhenNoProviderRates() {
        // Arrange
        when(aggregationService.getRatesForPair("EUR", "XYZ"))
                .thenReturn(Collections.emptyMap());

        // Act
        Optional<BigDecimal> result = exchangeRateService.getBestRate("EUR", "XYZ");

        // Assert
        assertThat(result).isEmpty();
        verify(aggregationService, times(1)).getRatesForPair("EUR", "XYZ");
    }

    @Test
    @DisplayName("Should get historical rates for time period")
    void getHistoricalRates_ShouldReturnRates_WhenPeriodSpecified() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        List<ExchangeRate> historicalRates = Arrays.asList(
                eurToUsdRate,
                eurToUsdRate.toBuilder().rate(new BigDecimal("1.25")).build()
        );

        when(exchangeRateRepository.findRatesByPeriod("EUR", "USD", start, end))
                .thenReturn(historicalRates);

        // Act
        List<ExchangeRate> result = exchangeRateService.getHistoricalRates("EUR", "USD", start, end);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(historicalRates);
        verify(exchangeRateRepository, times(1)).findRatesByPeriod("EUR", "USD", start, end);
    }

    @Test
    @DisplayName("Should return empty list when no historical data")
    void getHistoricalRates_ShouldReturnEmpty_WhenNoData() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        when(exchangeRateRepository.findRatesByPeriod(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        List<ExchangeRate> result = exchangeRateService.getHistoricalRates("EUR", "XYZ", start, end);

        // Assert
        assertThat(result).isEmpty();
        verify(exchangeRateRepository, times(1)).findRatesByPeriod(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle multiple base currencies in refresh")
    void refreshRates_ShouldSaveAllBaseCurrencies() {
        // Arrange
        Map<String, Map<String, BigDecimal>> bestRates = new HashMap<>();
        
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        bestRates.put("USD", usdRates);
        
        Map<String, BigDecimal> eurRates = new HashMap<>();
        eurRates.put("USD", new BigDecimal("1.20"));
        eurRates.put("GBP", new BigDecimal("0.88"));
        bestRates.put("EUR", eurRates);

        when(aggregationService.aggregateBestRates()).thenReturn(bestRates);
        when(exchangeRateRepository.save(any(ExchangeRate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        int result = exchangeRateService.refreshRates();

        // Assert
        assertThat(result).isEqualTo(3); // 1 USD rate + 2 EUR rates
        verify(exchangeRateRepository, times(3)).save(any(ExchangeRate.class));
    }
}
