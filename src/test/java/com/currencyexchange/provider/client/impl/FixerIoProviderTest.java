package com.currencyexchange.provider.client.impl;

import com.currencyexchange.provider.client.dto.FixerIoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FixerIoProvider.
 *
 * @author Artur Semenas
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FixerIoProvider Unit Tests")
class FixerIoProviderTest {

    @Mock
    private RestTemplate restTemplate;

    private FixerIoProvider provider;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    private static final String API_URL = "http://data.fixer.io/api";
    private static final String API_KEY = "test-api-key";
    private static final String BASE_CURRENCY = "USD";
    private static final String TARGET_CURRENCY = "EUR";

    @BeforeEach
    void setUp() {
        provider = new FixerIoProvider(restTemplate, API_URL, API_KEY);
    }

    @Test
    @DisplayName("fetchLatestRates should return rates when API response is successful")
    void fetchLatestRates_ShouldReturnRates_WhenResponseIsSuccessful() {
        // Arrange
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        rates.put("GBP", new BigDecimal("0.73"));
        response.setRates(rates);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        Map<String, BigDecimal> result = provider.fetchLatestRates(BASE_CURRENCY);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get("EUR")).isEqualByComparingTo("0.85");
        assertThat(result.get("GBP")).isEqualByComparingTo("0.73");
        
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(FixerIoResponse.class));
        assertThat(urlCaptor.getValue())
                .contains(API_URL + "/latest")
                .contains("access_key=" + API_KEY)
                .contains("base=" + BASE_CURRENCY);
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when response is null")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenResponseIsNull() {
        // Arrange
        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(null);

        // Act
        Map<String, BigDecimal> result = provider.fetchLatestRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when response success is false")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenSuccessIsFalse() {
        // Arrange
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(false);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        Map<String, BigDecimal> result = provider.fetchLatestRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map and log error when API returns error")
    void fetchLatestRates_ShouldReturnEmptyMapAndLogError_WhenApiReturnsError() {
        // Arrange
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(false);
        FixerIoResponse.ErrorInfo errorInfo = new FixerIoResponse.ErrorInfo();
        errorInfo.setCode(101);
        errorInfo.setType("invalid_access_key");
        errorInfo.setInfo("You have not supplied a valid API Access Key.");
        response.setError(errorInfo);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        Map<String, BigDecimal> result = provider.fetchLatestRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when RestTemplate throws exception")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenRestTemplateThrowsException() {
        // Arrange
        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        // Act
        Map<String, BigDecimal> result = provider.fetchLatestRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchLatestRates should return empty map when rates are empty")
    void fetchLatestRates_ShouldReturnEmptyMap_WhenRatesAreEmpty() {
        // Arrange
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        response.setRates(new HashMap<>());

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        Map<String, BigDecimal> result = provider.fetchLatestRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return rate when API response is successful")
    void fetchHistoricalRate_ShouldReturnRate_WhenResponseIsSuccessful() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 3, 1);
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put(TARGET_CURRENCY, new BigDecimal("0.85"));
        response.setRates(rates);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        BigDecimal result = provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        assertThat(result).isEqualByComparingTo("0.85");
        
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(FixerIoResponse.class));
        assertThat(urlCaptor.getValue())
                .contains("/2023-03-01")
                .contains("access_key=" + API_KEY)
                .contains("base=" + BASE_CURRENCY)
                .contains("symbols=" + TARGET_CURRENCY);
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when response is null")
    void fetchHistoricalRate_ShouldReturnNull_WhenResponseIsNull() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 3, 1);
        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(null);

        // Act
        BigDecimal result = provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when response success is false")
    void fetchHistoricalRate_ShouldReturnNull_WhenSuccessIsFalse() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 3, 1);
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(false);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        BigDecimal result = provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when rates are null")
    void fetchHistoricalRate_ShouldReturnNull_WhenRatesAreNull() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 3, 1);
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        response.setRates(null);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        BigDecimal result = provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when target currency not in rates")
    void fetchHistoricalRate_ShouldReturnNull_WhenTargetCurrencyNotInRates() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 3, 1);
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("GBP", new BigDecimal("0.73"));
        response.setRates(rates);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        BigDecimal result = provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null and log error when API returns error")
    void fetchHistoricalRate_ShouldReturnNullAndLogError_WhenApiReturnsError() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 3, 1);
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(false);
        FixerIoResponse.ErrorInfo errorInfo = new FixerIoResponse.ErrorInfo();
        errorInfo.setCode(301);
        errorInfo.setType("invalid_date");
        errorInfo.setInfo("You have entered an invalid date.");
        response.setError(errorInfo);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        BigDecimal result = provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("fetchHistoricalRate should return null when RestTemplate throws exception")
    void fetchHistoricalRate_ShouldReturnNull_WhenRestTemplateThrowsException() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 3, 1);
        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        // Act
        BigDecimal result = provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getProviderName should return correct provider name")
    void getProviderName_ShouldReturnCorrectName() {
        // Act
        String result = provider.getProviderName();

        // Assert
        assertThat(result).isEqualTo("fixer.io");
    }

    @Test
    @DisplayName("isAvailable should return true when provider can fetch rates")
    void isAvailable_ShouldReturnTrue_WhenProviderCanFetchRates() {
        // Arrange
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", new BigDecimal("1.18"));
        response.setRates(rates);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        boolean result = provider.isAvailable();

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isAvailable should return false when rates are empty")
    void isAvailable_ShouldReturnFalse_WhenRatesAreEmpty() {
        // Arrange
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        response.setRates(new HashMap<>());

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        boolean result = provider.isAvailable();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAvailable should return false when exception is thrown")
    void isAvailable_ShouldReturnFalse_WhenExceptionIsThrown() {
        // Arrange
        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        // Act
        boolean result = provider.isAvailable();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("fetchHistoricalRate should format date correctly")
    void fetchHistoricalRate_ShouldFormatDateCorrectly() {
        // Arrange
        LocalDate date = LocalDate.of(2023, 12, 25);
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put(TARGET_CURRENCY, new BigDecimal("0.92"));
        response.setRates(rates);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        provider.fetchHistoricalRate(BASE_CURRENCY, TARGET_CURRENCY, date);

        // Assert
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(FixerIoResponse.class));
        assertThat(urlCaptor.getValue()).contains("/2023-12-25");
    }

    @Test
    @DisplayName("fetchLatestRates should handle large number of rates")
    void fetchLatestRates_ShouldHandleLargeNumberOfRates() {
        // Arrange
        FixerIoResponse response = new FixerIoResponse();
        response.setSuccess(true);
        Map<String, BigDecimal> rates = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            rates.put("CUR" + i, new BigDecimal(i));
        }
        response.setRates(rates);

        when(restTemplate.getForObject(any(String.class), eq(FixerIoResponse.class)))
                .thenReturn(response);

        // Act
        Map<String, BigDecimal> result = provider.fetchLatestRates(BASE_CURRENCY);

        // Assert
        assertThat(result).hasSize(100);
    }
}
