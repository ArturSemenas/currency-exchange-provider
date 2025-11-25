package com.currencyexchange.provider.mapper;

import com.currencyexchange.provider.dto.ExchangeRateDto;
import com.currencyexchange.provider.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ExchangeRateMapper.
 * Tests MapStruct-generated mapping logic using the Mappers factory.
 */
@DisplayName("ExchangeRateMapper Unit Tests")
class ExchangeRateMapperTest {

    private ExchangeRateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ExchangeRateMapper.class);
    }

    // ==================== toDto Tests ====================

    @Test
    @DisplayName("toDto should map ExchangeRate entity to ExchangeRateDto")
    void toDto_ShouldMapEntityToDto() {
        // Given
        LocalDateTime timestamp = LocalDateTime.of(2024, 11, 25, 10, 30);
        ExchangeRate rate = ExchangeRate.builder()
                .id(1L)
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.85"))
                .timestamp(timestamp)
                .provider("fixer.io")
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.baseCurrency()).isEqualTo("USD");
        assertThat(dto.targetCurrency()).isEqualTo("EUR");
        assertThat(dto.rate()).isEqualByComparingTo(new BigDecimal("0.85"));
        assertThat(dto.provider()).isEqualTo("fixer.io");
        assertThat(dto.lastUpdated()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("toDto should map timestamp field to lastUpdated")
    void toDto_ShouldMapTimestampToLastUpdated() {
        // Given
        LocalDateTime specificTime = LocalDateTime.of(2024, 12, 1, 15, 45, 30);
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("GBP")
                .targetCurrency("JPY")
                .rate(new BigDecimal("188.50"))
                .timestamp(specificTime)
                .provider("exchangeratesapi")
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.lastUpdated()).isEqualTo(specificTime);
    }

    @Test
    @DisplayName("toDto should return null when ExchangeRate is null")
    void toDto_ShouldReturnNull_WhenEntityIsNull() {
        // When
        ExchangeRateDto dto = mapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("toDto should handle entity with minimal data")
    void toDto_ShouldHandleEntityWithMinimalData() {
        // Given
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("CAD")
                .rate(new BigDecimal("1.35"))
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.baseCurrency()).isEqualTo("USD");
        assertThat(dto.targetCurrency()).isEqualTo("CAD");
        assertThat(dto.rate()).isEqualByComparingTo(new BigDecimal("1.35"));
    }

    @Test
    @DisplayName("toDto should handle high precision rate values")
    void toDto_ShouldHandleHighPrecisionRateValues() {
        // Given
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("BTC")
                .targetCurrency("USD")
                .rate(new BigDecimal("1.234567"))
                .provider("crypto-api")
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.rate()).isEqualByComparingTo(new BigDecimal("1.234567"));
        assertThat(dto.rate().scale()).isEqualTo(6);
    }

    @Test
    @DisplayName("toDto should handle very small rate values")
    void toDto_ShouldHandleVerySmallRateValues() {
        // Given
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("JPY")
                .rate(new BigDecimal("0.000001"))
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.rate()).isEqualByComparingTo(new BigDecimal("0.000001"));
    }

    @Test
    @DisplayName("toDto should handle very large rate values")
    void toDto_ShouldHandleVeryLargeRateValues() {
        // Given
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("IDR")
                .targetCurrency("USD")
                .rate(new BigDecimal("150000.50"))
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.rate()).isEqualByComparingTo(new BigDecimal("150000.50"));
    }

    @Test
    @DisplayName("toDto should handle rate with zero value")
    void toDto_ShouldHandleRateWithZeroValue() {
        // Given
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("XXX")
                .targetCurrency("YYY")
                .rate(BigDecimal.ZERO)
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.rate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("toDto should handle same base and target currency")
    void toDto_ShouldHandleSameBaseAndTargetCurrency() {
        // Given
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("USD")
                .rate(BigDecimal.ONE)
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.baseCurrency()).isEqualTo("USD");
        assertThat(dto.targetCurrency()).isEqualTo("USD");
        assertThat(dto.rate()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("toDto should handle timestamp at start of day")
    void toDto_ShouldHandleTimestampAtStartOfDay() {
        // Given
        LocalDateTime startOfDay = LocalDateTime.of(2024, 11, 25, 0, 0, 0);
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("EUR")
                .targetCurrency("USD")
                .rate(new BigDecimal("1.10"))
                .timestamp(startOfDay)
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.lastUpdated()).isEqualTo(startOfDay);
    }

    @Test
    @DisplayName("toDto should handle timestamp at end of day")
    void toDto_ShouldHandleTimestampAtEndOfDay() {
        // Given
        LocalDateTime endOfDay = LocalDateTime.of(2024, 11, 25, 23, 59, 59);
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("GBP")
                .targetCurrency("EUR")
                .rate(new BigDecimal("1.17"))
                .timestamp(endOfDay)
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.lastUpdated()).isEqualTo(endOfDay);
    }

    // ==================== toDtoList Tests ====================

    @Test
    @DisplayName("toDtoList should map list of ExchangeRate entities to ExchangeRateDto list")
    void toDtoList_ShouldMapEntityListToDtoList() {
        // Given
        List<ExchangeRate> rates = Arrays.asList(
                createExchangeRate(1L, "USD", "EUR", "0.85"),
                createExchangeRate(2L, "GBP", "JPY", "188.50"),
                createExchangeRate(3L, "CAD", "USD", "0.74")
        );

        // When
        List<ExchangeRateDto> dtos = mapper.toDtoList(rates);

        // Then
        assertThat(dtos).hasSize(3);
        assertThat(dtos.get(0).baseCurrency()).isEqualTo("USD");
        assertThat(dtos.get(1).baseCurrency()).isEqualTo("GBP");
        assertThat(dtos.get(2).baseCurrency()).isEqualTo("CAD");
    }

    @Test
    @DisplayName("toDtoList should return empty list when input list is empty")
    void toDtoList_ShouldReturnEmptyList_WhenInputIsEmpty() {
        // When
        List<ExchangeRateDto> dtos = mapper.toDtoList(Collections.emptyList());

        // Then
        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("toDtoList should return null when input list is null")
    void toDtoList_ShouldReturnNull_WhenInputIsNull() {
        // When
        List<ExchangeRateDto> dtos = mapper.toDtoList(null);

        // Then
        assertThat(dtos).isNull();
    }

    @Test
    @DisplayName("toDtoList should map single element list")
    void toDtoList_ShouldMapListWithSingleElement() {
        // Given
        List<ExchangeRate> rates = Collections.singletonList(
                createExchangeRate(1L, "CHF", "EUR", "0.95")
        );

        // When
        List<ExchangeRateDto> dtos = mapper.toDtoList(rates);

        // Then
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).baseCurrency()).isEqualTo("CHF");
    }

    @Test
    @DisplayName("toDtoList should handle list with null elements")
    void toDtoList_ShouldHandleListWithNullElements() {
        // Given
        List<ExchangeRate> rates = Arrays.asList(
                createExchangeRate(1L, "USD", "EUR", "0.85"),
                null,
                createExchangeRate(2L, "GBP", "JPY", "188.50")
        );

        // When
        List<ExchangeRateDto> dtos = mapper.toDtoList(rates);

        // Then
        assertThat(dtos).hasSize(3);
        assertThat(dtos.get(0)).isNotNull();
        assertThat(dtos.get(1)).isNull();
        assertThat(dtos.get(2)).isNotNull();
    }

    @Test
    @DisplayName("toDtoList should map large list efficiently")
    void toDtoList_ShouldMapLargeListEfficiently() {
        // Given
        List<ExchangeRate> rates = Arrays.asList(
                createExchangeRate(1L, "USD", "EUR", "0.85"),
                createExchangeRate(2L, "EUR", "GBP", "0.87"),
                createExchangeRate(3L, "GBP", "JPY", "188.50"),
                createExchangeRate(4L, "JPY", "CNY", "0.048"),
                createExchangeRate(5L, "CNY", "USD", "0.14"),
                createExchangeRate(6L, "CAD", "USD", "0.74"),
                createExchangeRate(7L, "AUD", "USD", "0.65"),
                createExchangeRate(8L, "CHF", "EUR", "0.95"),
                createExchangeRate(9L, "SEK", "NOK", "0.97"),
                createExchangeRate(10L, "DKK", "SEK", "1.37")
        );

        // When
        List<ExchangeRateDto> dtos = mapper.toDtoList(rates);

        // Then
        assertThat(dtos).hasSize(10);
        assertThat(dtos).allMatch(dto -> dto != null);
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("toDto should handle different provider names")
    void toDto_ShouldHandleDifferentProviderNames() {
        // Given
        ExchangeRate rate1 = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.85"))
                .provider("fixer.io")
                .build();

        ExchangeRate rate2 = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.86"))
                .provider("exchangeratesapi")
                .build();

        // When
        ExchangeRateDto dto1 = mapper.toDto(rate1);
        ExchangeRateDto dto2 = mapper.toDto(rate2);

        // Then
        assertThat(dto1.provider()).isEqualTo("fixer.io");
        assertThat(dto2.provider()).isEqualTo("exchangeratesapi");
    }

    @Test
    @DisplayName("toDto should preserve all decimal places in rate")
    void toDto_ShouldPreserveAllDecimalPlacesInRate() {
        // Given
        BigDecimal preciseRate = new BigDecimal("1.234567");
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("EUR")
                .targetCurrency("GBP")
                .rate(preciseRate)
                .build();

        // When
        ExchangeRateDto dto = mapper.toDto(rate);

        // Then
        assertThat(dto.rate()).isEqualByComparingTo(preciseRate);
        assertThat(dto.rate().scale()).isEqualTo(6);
    }

    // ==================== Helper Methods ====================

    /**
     * Helper method to create ExchangeRate test data.
     *
     * @param id the exchange rate ID
     * @param base the base currency code
     * @param target the target currency code
     * @param rateValue the exchange rate value as string
     * @return ExchangeRate entity for testing
     */
    private ExchangeRate createExchangeRate(Long id, String base, String target, String rateValue) {
        return ExchangeRate.builder()
                .id(id)
                .baseCurrency(base)
                .targetCurrency(target)
                .rate(new BigDecimal(rateValue))
                .timestamp(LocalDateTime.now())
                .provider("test-provider")
                .build();
    }
}
