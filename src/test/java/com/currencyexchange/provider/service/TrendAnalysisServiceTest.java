package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.ExchangeRate;
import com.currencyexchange.provider.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TrendAnalysisService
 * Tests period parsing, trend calculations, and edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrendAnalysisService Unit Tests")
class TrendAnalysisServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private TrendAnalysisService trendAnalysisService;

    private ExchangeRate oldRate;
    private ExchangeRate newRate;
    private List<ExchangeRate> rateHistory;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        
        // Old rate: 1.10 (from 7 days ago)
        oldRate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("EUR")
                .targetCurrency("USD")
                .rate(new BigDecimal("1.10"))
                .timestamp(now.minusDays(7))
                .provider("test")
                .build();

        // New rate: 1.20 (recent)
        newRate = ExchangeRate.builder()
                .id(2L)
                .baseCurrency("EUR")
                .targetCurrency("USD")
                .rate(new BigDecimal("1.20"))
                .timestamp(now.minusHours(1))
                .provider("test")
                .build();

        rateHistory = Arrays.asList(oldRate, newRate);
    }

    @Test
    @DisplayName("Should calculate positive trend when rate increases")
    void calculateTrend_ShouldReturnPositiveTrend_WhenRateIncreases() {
        // Arrange: Rate goes from 1.10 to 1.20 (9.09% increase)
        when(exchangeRateRepository.findRatesByPeriod(eq("EUR"), eq("USD"), any(), any()))
                .thenReturn(rateHistory);

        // Act
        BigDecimal trend = trendAnalysisService.calculateTrend("EUR", "USD", "7D");

        // Assert
        assertThat(trend).isGreaterThan(BigDecimal.ZERO);
        assertThat(trend).isEqualByComparingTo("9.09"); // (1.20 - 1.10) / 1.10 * 100 = 9.09%
        verify(exchangeRateRepository, times(1)).findRatesByPeriod(eq("EUR"), eq("USD"), any(), any());
    }

    @Test
    @DisplayName("Should calculate negative trend when rate decreases")
    void calculateTrend_ShouldReturnNegativeTrend_WhenRateDecreases() {
        // Arrange: Rate goes from 1.20 to 1.10 (-8.33% decrease)
        ExchangeRate highRate = oldRate.toBuilder().rate(new BigDecimal("1.20")).build();
        ExchangeRate lowRate = newRate.toBuilder().rate(new BigDecimal("1.10")).build();
        List<ExchangeRate> decreasingRates = Arrays.asList(highRate, lowRate);
        
        when(exchangeRateRepository.findRatesByPeriod(eq("EUR"), eq("USD"), any(), any()))
                .thenReturn(decreasingRates);

        // Act
        BigDecimal trend = trendAnalysisService.calculateTrend("EUR", "USD", "7D");

        // Assert
        assertThat(trend).isLessThan(BigDecimal.ZERO);
        assertThat(trend).isEqualByComparingTo("-8.33"); // (1.10 - 1.20) / 1.20 * 100 = -8.33%
    }

    @Test
    @DisplayName("Should calculate zero trend when rate unchanged")
    void calculateTrend_ShouldReturnZeroTrend_WhenRateUnchanged() {
        // Arrange: Rate stays at 1.15
        ExchangeRate sameRate1 = oldRate.toBuilder().rate(new BigDecimal("1.15")).build();
        ExchangeRate sameRate2 = newRate.toBuilder().rate(new BigDecimal("1.15")).build();
        List<ExchangeRate> unchangedRates = Arrays.asList(sameRate1, sameRate2);
        
        when(exchangeRateRepository.findRatesByPeriod(eq("EUR"), eq("USD"), any(), any()))
                .thenReturn(unchangedRates);

        // Act
        BigDecimal trend = trendAnalysisService.calculateTrend("EUR", "USD", "7D");

        // Assert
        assertThat(trend).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @CsvSource({
            "12H",  // 12 hours
            "24H",  // 24 hours
            "7D",   // 7 days
            "30D",  // 30 days
            "3M",   // 3 months
            "6M",   // 6 months
            "1Y",   // 1 year
            "2Y"    // 2 years
    })
    @DisplayName("Should accept valid period formats")
    void calculateTrend_ShouldAcceptValidPeriodFormats(String period) {
        // Arrange
        when(exchangeRateRepository.findRatesByPeriod(any(), any(), any(), any()))
                .thenReturn(rateHistory);

        // Act & Assert - Should not throw
        assertThat(trendAnalysisService.calculateTrend("EUR", "USD", period))
                .isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "ABC", "123", "H12", "D", "10", "10X", "10DM"})
    @DisplayName("Should throw exception for invalid period formats")
    void calculateTrend_ShouldThrowException_WhenPeriodFormatInvalid(String invalidPeriod) {
        // Act & Assert
        assertThatThrownBy(() -> trendAnalysisService.calculateTrend("EUR", "USD", invalidPeriod))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid period format");

        verify(exchangeRateRepository, never()).findRatesByPeriod(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when period is null")
    void calculateTrend_ShouldThrowException_WhenPeriodIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> trendAnalysisService.calculateTrend("EUR", "USD", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Period cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when period is empty string")
    void calculateTrend_ShouldThrowException_WhenPeriodIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> trendAnalysisService.calculateTrend("EUR", "USD", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Period cannot be null or empty");
    }

    @Test
    @DisplayName("Should throw exception when hours less than minimum")
    void calculateTrend_ShouldThrowException_WhenHoursLessThanMinimum() {
        // Act & Assert
        assertThatThrownBy(() -> trendAnalysisService.calculateTrend("EUR", "USD", "6H"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Hours must be at least 12");
    }

    @Test
    @DisplayName("Should throw exception when no historical data available")
    void calculateTrend_ShouldThrowException_WhenNoData() {
        // Arrange
        when(exchangeRateRepository.findRatesByPeriod(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThatThrownBy(() -> trendAnalysisService.calculateTrend("EUR", "USD", "7D"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient historical data");
    }

    @Test
    @DisplayName("Should handle multiple data points correctly")
    void calculateTrend_ShouldUseOldestAndNewest_WhenMultipleDataPoints() {
        // Arrange: Create 5 data points, should use oldest and newest
        LocalDateTime now = LocalDateTime.now();
        List<ExchangeRate> multipleRates = Arrays.asList(
                oldRate.toBuilder().rate(new BigDecimal("1.10")).timestamp(now.minusDays(7)).build(),
                oldRate.toBuilder().rate(new BigDecimal("1.12")).timestamp(now.minusDays(5)).build(),
                oldRate.toBuilder().rate(new BigDecimal("1.15")).timestamp(now.minusDays(3)).build(),
                oldRate.toBuilder().rate(new BigDecimal("1.18")).timestamp(now.minusDays(1)).build(),
                newRate.toBuilder().rate(new BigDecimal("1.20")).timestamp(now).build()
        );
        
        when(exchangeRateRepository.findRatesByPeriod(any(), any(), any(), any()))
                .thenReturn(multipleRates);

        // Act
        BigDecimal trend = trendAnalysisService.calculateTrend("EUR", "USD", "7D");

        // Assert: Should use 1.10 (oldest) and 1.20 (newest) for calculation
        assertThat(trend).isEqualByComparingTo("9.09");
    }

    @Test
    @DisplayName("Should validate valid period format")
    void isValidPeriod_ShouldReturnTrue_WhenPeriodValid() {
        // Act & Assert
        assertThat(trendAnalysisService.isValidPeriod("12H")).isTrue();
        assertThat(trendAnalysisService.isValidPeriod("7D")).isTrue();
        assertThat(trendAnalysisService.isValidPeriod("3M")).isTrue();
        assertThat(trendAnalysisService.isValidPeriod("1Y")).isTrue();
    }

    @Test
    @DisplayName("Should return false for invalid period format")
    void isValidPeriod_ShouldReturnFalse_WhenPeriodInvalid() {
        // Act & Assert
        assertThat(trendAnalysisService.isValidPeriod("invalid")).isFalse();
        assertThat(trendAnalysisService.isValidPeriod("6H")).isFalse(); // Less than minimum
        assertThat(trendAnalysisService.isValidPeriod("")).isFalse();
        assertThat(trendAnalysisService.isValidPeriod(null)).isFalse();
    }

    @Test
    @DisplayName("Should accept lowercase period format")
    void calculateTrend_ShouldAcceptLowercaseFormat() {
        // Arrange
        when(exchangeRateRepository.findRatesByPeriod(any(), any(), any(), any()))
                .thenReturn(rateHistory);

        // Act
        BigDecimal trend = trendAnalysisService.calculateTrend("EUR", "USD", "7d");

        // Assert
        assertThat(trend).isNotNull();
        verify(exchangeRateRepository, times(1)).findRatesByPeriod(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should handle period with whitespace")
    void calculateTrend_ShouldTrimWhitespace() {
        // Arrange
        when(exchangeRateRepository.findRatesByPeriod(any(), any(), any(), any()))
                .thenReturn(rateHistory);

        // Act
        BigDecimal trend = trendAnalysisService.calculateTrend("EUR", "USD", "  7D  ");

        // Assert
        assertThat(trend).isNotNull();
    }
}
