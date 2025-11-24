package com.currencyexchange.provider.controller;

import com.currencyexchange.provider.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ExchangeRateController using MockMvc
 * Tests REST endpoints for exchange rate conversion and refresh
 */
@WebMvcTest(ExchangeRateController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ExchangeRateController Unit Tests")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should convert currency successfully")
    void convertCurrency_ShouldReturnConvertedAmount() throws Exception {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal convertedAmount = new BigDecimal("85.50");
        
        when(exchangeRateService.getExchangeRate(any(BigDecimal.class), eq("USD"), eq("EUR")))
                .thenReturn(Optional.of(convertedAmount));

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100.00")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from", is("USD")))
                .andExpect(jsonPath("$.to", is("EUR")))
                .andExpect(jsonPath("$.amount", is(100.00)))
                .andExpect(jsonPath("$.convertedAmount", is(85.50)))
                .andExpect(jsonPath("$.rate").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(exchangeRateService, times(1)).getExchangeRate(any(BigDecimal.class), eq("USD"), eq("EUR"));
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should return 404 when rate not found")
    void convertCurrency_ShouldReturn404_WhenRateNotFound() throws Exception {
        // Arrange - Use valid currency GBP that has no rate data
        when(exchangeRateService.getExchangeRate(any(BigDecimal.class), eq("USD"), eq("GBP")))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100.00")
                        .param("from", "USD")
                        .param("to", "GBP"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(exchangeRateService, times(1)).getExchangeRate(any(BigDecimal.class), eq("USD"), eq("GBP"));
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should return 400 for invalid currency code")
    void convertCurrency_ShouldReturn400_WhenInvalidCurrencyCode() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100.00")
                        .param("from", "INVALID")
                        .param("to", "EUR"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(exchangeRateService, never()).getExchangeRate(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should return 400 for negative amount")
    void convertCurrency_ShouldReturn400_WhenNegativeAmount() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "-100.00")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(exchangeRateService, never()).getExchangeRate(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should return 400 for zero amount")
    void convertCurrency_ShouldReturn400_WhenZeroAmount() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "0.00")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(exchangeRateService, never()).getExchangeRate(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should return 400 for blank 'from' currency")
    void convertCurrency_ShouldReturn400_WhenFromCurrencyBlank() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100.00")
                        .param("from", "")
                        .param("to", "EUR"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(exchangeRateService, never()).getExchangeRate(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should return 400 for blank 'to' currency")
    void convertCurrency_ShouldReturn400_WhenToCurrencyBlank() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100.00")
                        .param("from", "USD")
                        .param("to", ""))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(exchangeRateService, never()).getExchangeRate(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies/refresh - Should refresh rates with ADMIN role")
    void refreshExchangeRates_ShouldRefreshRates_WhenUserIsAdmin() throws Exception {
        // Arrange
        when(exchangeRateService.refreshRates()).thenReturn(48);

        // Act & Assert
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Successfully refreshed")))
                .andExpect(jsonPath("$.message", containsString("48")))
                .andExpect(jsonPath("$.updatedCount", is(48)))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/currencies/refresh - Should return 403 without ADMIN role")
    void refreshExchangeRates_ShouldReturn403_WhenUserIsNotAdmin() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(exchangeRateService, never()).refreshRates();
    }

    @Test
    @DisplayName("POST /api/v1/currencies/refresh - Should return 401 without authentication")
    void refreshExchangeRates_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        // Act & Assert - Spring Security returns 401 for unauthenticated requests
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(exchangeRateService, never()).refreshRates();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies/refresh - Should return 500 when refresh fails")
    void refreshExchangeRates_ShouldReturn500_WhenRefreshFails() throws Exception {
        // Arrange
        when(exchangeRateService.refreshRates())
                .thenThrow(new RuntimeException("Provider unavailable"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/currencies/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("Failed to refresh")))
                .andExpect(jsonPath("$.updatedCount", is(0)));

        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should handle same currency conversion")
    void convertCurrency_ShouldHandleSameCurrency() throws Exception {
        // Arrange
        BigDecimal amount = new BigDecimal("100.00");
        
        when(exchangeRateService.getExchangeRate(any(BigDecimal.class), eq("USD"), eq("USD")))
                .thenReturn(Optional.of(amount));

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100.00")
                        .param("from", "USD")
                        .param("to", "USD"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedAmount", is(100.00)));
    }

    @Test
    @DisplayName("GET /api/v1/currencies/exchange-rates - Should handle decimal amounts")
    void convertCurrency_ShouldHandleDecimalAmounts() throws Exception {
        // Arrange
        when(exchangeRateService.getExchangeRate(any(BigDecimal.class), eq("USD"), eq("EUR")))
                .thenReturn(Optional.of(new BigDecimal("85.50")));

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/exchange-rates")
                        .param("amount", "100.50")
                        .param("from", "USD")
                        .param("to", "EUR"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount", is(100.50)));
    }
}
