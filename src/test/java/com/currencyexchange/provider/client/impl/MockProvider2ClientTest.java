package com.currencyexchange.provider.client.impl;

import com.currencyexchange.provider.client.dto.ExchangeratesApiResponse;
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
 * Unit tests for MockProvider2Client.
 * Tests all scenarios including success, errors, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class MockProvider2ClientTest {

    @Mock
    private RestTemplate restTemplate;

    private MockProvider2Client mockProvider2Client;

    private static final String BASE_URL = "http://localhost:8092";

    @BeforeEach
    void setUp() {
        mockProvider2Client = new MockProvider2Client(restTemplate, BASE_URL);
    }

    @Test
    @DisplayName("getProviderName should return 'mock-provider-2'")
    void getProviderName_ShouldReturnCorrectName() {
        // When
        String providerName = mockProvider2Client.getProviderName();

        // Then
        assertThat(providerName).isEqualTo("mock-provider-2");
    }

    @Test
    @DisplayName("fetchLatestRates should return rates when API responds successfully")
    void fetchLatestRates_ShouldReturnRates_WhenSuccessful() {
        // Given
        String baseCurrency = "USD";
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(true);
        mockResponse.setBase("USD");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.86"));
        rates.put("GBP", new BigDecimal("0.74"));
        rates.put("JPY", new BigDecimal("110.25"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        Map<String, BigDecimal> result = mockProvider2Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(3);
        assertThat(result.get("EUR")).isEqualByComparingTo("0.86");
        assertThat(result.get("GBP")).isEqualByComparingTo("0.74");
        assertThat(result.get("JPY")).isEqualByComparingTo("110.25");
        verify(restTemplate).getForObject(anyString(), eq(ExchangeratesApiResponse.class));
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when response is null")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenResponseIsNull() {
        // Given
        String baseCurrency = "EUR";
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(null);

        // When
        Map<String, BigDecimal> result = mockProvider2Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when success is false")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenSuccessIsFalse() {
        // Given
        String baseCurrency = "GBP";
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(false);
        mockResponse.setRates(new HashMap<>());

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        Map<String, BigDecimal> result = mockProvider2Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map on HTTP 401 error")
    void fetchLatestRates_ShouldReturnEmptyMap_OnUnauthorized() {
        // Given
        String baseCurrency = "USD";
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // When
        Map<String, BigDecimal> result = mockProvider2Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map on HTTP 500 error")
    void fetchLatestRates_ShouldReturnEmptyMap_OnServerError() {
        // Given
        String baseCurrency = "CHF";
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenThrow(HttpServerErrorException.InternalServerError.create(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // When
        Map<String, BigDecimal> result = mockProvider2Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map on connection refused")
    void fetchLatestRates_ShouldReturnEmptyMap_OnConnectionRefused() {
        // Given
        String baseCurrency = "JPY";
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        // When
        Map<String, BigDecimal> result = mockProvider2Client.fetchLatestRates(baseCurrency);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return rate when API responds successfully")
    void fetchHistoricalRate_ShouldReturnRate_WhenSuccessful() {
        // Given
        String baseCurrency = "EUR";
        String targetCurrency = "USD";
        LocalDate date = LocalDate.of(2023, 6, 20);
        
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(true);
        mockResponse.setBase("EUR");
        mockResponse.setDate("2023-06-20");
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1.09"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider2Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualByComparingTo("1.09");
        verify(restTemplate).getForObject(anyString(), eq(ExchangeratesApiResponse.class));
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when response is null")
    void fetchHistoricalRate_ShouldReturnNull_WhenResponseIsNull() {
        // Given
        String baseCurrency = "GBP";
        String targetCurrency = "USD";
        LocalDate date = LocalDate.of(2023, 6, 20);
        
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(null);

        // When
        BigDecimal result = mockProvider2Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when success is false")
    void fetchHistoricalRate_ShouldReturnNull_WhenSuccessIsFalse() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "CHF";
        LocalDate date = LocalDate.of(2023, 6, 20);
        
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(false);

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider2Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when rates map is null")
    void fetchHistoricalRate_ShouldReturnNull_WhenRatesMapIsNull() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 6, 20);
        
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(true);
        mockResponse.setRates(null);

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider2Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when target currency not in rates")
    void fetchHistoricalRate_ShouldReturnNull_WhenTargetCurrencyNotFound() {
        // Given
        String baseCurrency = "EUR";
        String targetCurrency = "AUD";
        LocalDate date = LocalDate.of(2023, 6, 20);
        
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1.09"));
        rates.put("GBP", new BigDecimal("0.86"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        BigDecimal result = mockProvider2Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null on HTTP 404 error")
    void fetchHistoricalRate_ShouldReturnNull_OnNotFound() {
        // Given
        String baseCurrency = "USD";
        String targetCurrency = "EUR";
        LocalDate date = LocalDate.of(2023, 6, 20);
        
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Not Found",
                        org.springframework.http.HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));

        // When
        BigDecimal result = mockProvider2Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null on timeout")
    void fetchHistoricalRate_ShouldReturnNull_OnTimeout() {
        // Given
        String baseCurrency = "JPY";
        String targetCurrency = "USD";
        LocalDate date = LocalDate.of(2023, 6, 20);
        
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenThrow(new ResourceAccessException("Request timeout"));

        // When
        BigDecimal result = mockProvider2Client.fetchHistoricalRate(baseCurrency, targetCurrency, date);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("isAvailable should return true when provider returns rates")
    void isAvailable_ShouldReturnTrue_WhenProviderReturnsRates() {
        // Given
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.86"));
        mockResponse.setRates(rates);

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        boolean result = mockProvider2Client.isAvailable();

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAvailable should return false when provider returns empty rates")
    void isAvailable_ShouldReturnFalse_WhenProviderReturnsEmptyRates() {
        // Given
        ExchangeratesApiResponse mockResponse = new ExchangeratesApiResponse();
        mockResponse.setSuccess(false);
        mockResponse.setRates(new HashMap<>());

        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(mockResponse);

        // When
        boolean result = mockProvider2Client.isAvailable();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAvailable should return false on exception")
    void isAvailable_ShouldReturnFalse_OnException() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        boolean result = mockProvider2Client.isAvailable();

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAvailable should return false when response is null")
    void isAvailable_ShouldReturnFalse_WhenResponseIsNull() {
        // Given
        when(restTemplate.getForObject(anyString(), eq(ExchangeratesApiResponse.class)))
                .thenReturn(null);

        // When
        boolean result = mockProvider2Client.isAvailable();

        // Then
        assertThat(result).isFalse();
    }
}
