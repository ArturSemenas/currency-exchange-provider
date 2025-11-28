package com.currencyexchange.provider.client.impl;

import com.currencyexchange.provider.client.dto.FixerIoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for MockProvider1Client.
 * Tests all scenarios including success, errors, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class MockProvider1ClientTest {

    @Mock
    private RestTemplate restTemplate;

    private MockProvider1Client mockProvider1Client;

    private static final String BASE_URL = "http://localhost:8091";

    @BeforeEach
    void setUp() {
        mockProvider1Client = new MockProvider1Client(restTemplate, BASE_URL);
    }

    @Test
    @DisplayName("getProviderName should return 'mock-provider-1'")
    void getProviderName_ShouldReturnCorrectName() {
        // When
        String providerName = mockProvider1Client.getProviderName();

        // Then
        assertThat(providerName).isEqualTo("mock-provider-1");
    }

    @Test
    @DisplayName("fetchLatestRates should return rates when API responds successfully")
    void fetchLatestRates_ShouldReturnRates_WhenSuccessful() {
        // Given
        String baseCurrency = "USD";
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(true);
        mockResponse.setBase("USD");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        rates.put("GBP", new BigDecimal("0.73"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        Map<String, BigDecimal> result = mockProvider1Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(2);
        assertThat(result.get("EUR")).isEqualByComparingTo("0.85");
        assertThat(result.get("GBP")).isEqualByComparingTo("0.73");
        verify(restTemplate).getForObject(anyString(), eq(FixerIoResponse.class));
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when response is null")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenResponseIsNull() {
        // Given
        String baseCurrency = "USD";
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(null);

        // When
        Map<String, BigDecimal> result = mockProvider1Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when success is false")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenSuccessIsFalse() {
        // Given
        String baseCurrency = "USD";
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(false);
        mockResponse.setRates(new HashMap<>());

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        Map<String, BigDecimal> result = mockProvider1Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map on HTTP client error")
    void fetchLatestRates_ShouldReturnEmptyMap_OnHttpClientError() {
        // Given
        String baseCurrency = "USD";
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenThrow(HttpClientErrorException.BadRequest.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // When
        Map<String, BigDecimal> result = mockProvider1Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map on HTTP server error")
    void fetchLatestRates_ShouldReturnEmptyMap_OnHttpServerError() {
        // Given
        String baseCurrency = "EUR";
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenThrow(HttpServerErrorException.InternalServerError.create(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Server Error",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // When
        Map<String, BigDecimal> result = mockProvider1Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map on network timeout")
    void fetchLatestRates_ShouldReturnEmptyMap_OnNetworkTimeout() {
        // Given
        String baseCurrency = "GBP";
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        // When
        Map<String, BigDecimal> result = mockProvider1Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return rate when API responds successfully")
    void fetchHistoricalRate_ShouldReturnRate_WhenSuccessful() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 3, 15);
        
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(true);
        mockResponse.setBase("USD");
        mockResponse.setDate("2023-03-15");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.92"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider1Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualByComparingTo("0.92");
        verify(restTemplate).getForObject(anyString(), eq(FixerIoResponse.class));
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when response is null")
    void fetchHistoricalRate_ShouldReturnNull_WhenResponseIsNull() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 3, 15);
        
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(null);

        // When
        BigDecimal result = mockProvider1Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when success is false")
    void fetchHistoricalRate_ShouldReturnNull_WhenSuccessIsFalse() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 3, 15);
        
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(false);

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider1Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when rates map is null")
    void fetchHistoricalRate_ShouldReturnNull_WhenRatesMapIsNull() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 3, 15);
        
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(true);
        mockResponse.setRates(null);

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider1Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when target currency not in rates")
    void fetchHistoricalRate_ShouldReturnNull_WhenTargetCurrencyNotFound() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "JPY";
        LocalDate date = LocalDate.of(2023, 3, 15);
        
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.92"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider1Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null on HTTP error")
    void fetchHistoricalRate_ShouldReturnNull_OnHttpError() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 3, 15);
        
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // When
        BigDecimal result = mockProvider1Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null on network error")
    void fetchHistoricalRate_ShouldReturnNull_OnNetworkError() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 3, 15);
        
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenThrow(new ResourceAccessException("Network error"));

        // When
        BigDecimal result = mockProvider1Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("isAvailable should return true when provider returns rates")
    void isAvailable_ShouldReturnTrue_WhenProviderReturnsRates() {
        // Given
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1.00"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        boolean result = mockProvider1Client.isAvailable();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAvailable should return false when provider returns empty rates")
    void isAvailable_ShouldReturnFalse_WhenProviderReturnsEmptyRates() {
        // Given
        FixerIoResponse mockResponse = new FixerIoResponse();
        mockResponse.setSuccess(false);
        mockResponse.setRates(new HashMap<>());

        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenReturn(mockResponse);

        // When
        boolean result = mockProvider1Client.isAvailable();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAvailable should return false on exception")
    void isAvailable_ShouldReturnFalse_OnException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(FixerIoResponse.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        // When
        boolean result = mockProvider1Client.isAvailable();

        // Then
        assertThat(result).isFalse();
    }
}
