package com.currencyexchange.provider.controller;

import com.currencyexchange.provider.service.TrendAnalysisService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TrendController using MockMvc
 * Tests REST endpoints for currency trend analysis
 */
@WebMvcTest(TrendController.class)
@Import(TestSecurityConfig.class)
@DisplayName("TrendController Unit Tests")
class TrendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TrendAnalysisService trendAnalysisService;

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should return trend with ADMIN role")
    void analyzeTrend_ShouldReturnTrend_WhenUserIsAdmin() throws Exception {
        // Arrange
        when(trendAnalysisService.calculateTrend("EUR", "USD", "7D"))
                .thenReturn(new BigDecimal("2.35"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "EUR")
                        .param("to", "USD")
                        .param("period", "7D")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency", is("EUR")))
                .andExpect(jsonPath("$.targetCurrency", is("USD")))
                .andExpect(jsonPath("$.period", is("7D")))
                .andExpect(jsonPath("$.trendPercentage", is(2.35)))
                .andExpect(jsonPath("$.description", containsString("EUR")))
                .andExpect(jsonPath("$.description", containsString("appreciated")));

        verify(trendAnalysisService, times(1)).calculateTrend("EUR", "USD", "7D");
    }

    @Test
    @WithMockUser(authorities = "PREMIUM_USER")
    @DisplayName("GET /api/v1/currencies/trends - Should return trend with PREMIUM_USER role")
    void analyzeTrend_ShouldReturnTrend_WhenUserIsPremium() throws Exception {
        // Arrange
        when(trendAnalysisService.calculateTrend("USD", "EUR", "30D"))
                .thenReturn(new BigDecimal("-1.50"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "30D"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trendPercentage", is(-1.50)))
                .andExpect(jsonPath("$.description", containsString("depreciated")));

        verify(trendAnalysisService, times(1)).calculateTrend("USD", "EUR", "30D");
    }

    @Test
    @WithMockUser(authorities = "USER")
    @DisplayName("GET /api/v1/currencies/trends - Should return 403 with USER role")
    void analyzeTrend_ShouldReturn403_WhenUserIsRegularUser() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(trendAnalysisService, never()).calculateTrend(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/currencies/trends - Should return 401 without authentication")
    void analyzeTrend_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        // Act & Assert - Spring Security returns 401 for unauthenticated requests
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(trendAnalysisService, never()).calculateTrend(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should return 400 for invalid currency code")
    void analyzeTrend_ShouldReturn400_WhenInvalidCurrencyCode() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "INVALID")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(trendAnalysisService, never()).calculateTrend(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should return 400 for invalid period format")
    void analyzeTrend_ShouldReturn400_WhenInvalidPeriodFormat() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "invalid"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(trendAnalysisService, never()).calculateTrend(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should return 400 for blank 'from' currency")
    void analyzeTrend_ShouldReturn400_WhenFromCurrencyBlank() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(trendAnalysisService, never()).calculateTrend(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should return 400 for blank 'to' currency")
    void analyzeTrend_ShouldReturn400_WhenToCurrencyBlank() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(trendAnalysisService, never()).calculateTrend(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should return 400 for blank period")
    void analyzeTrend_ShouldReturn400_WhenPeriodBlank() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", ""))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(trendAnalysisService, never()).calculateTrend(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should handle different period formats")
    void analyzeTrend_ShouldAcceptDifferentPeriodFormats() throws Exception {
        // Arrange
        when(trendAnalysisService.calculateTrend(eq("USD"), eq("EUR"), any()))
                .thenReturn(new BigDecimal("1.50"));

        // Test hours
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "24H"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", containsString("24 hours")));

        // Test days
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", containsString("7 days")));

        // Test months
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "3M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", containsString("3 months")));

        // Test years
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "1Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", containsString("1 year")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should handle zero trend")
    void analyzeTrend_ShouldHandleZeroTrend() throws Exception {
        // Arrange
        when(trendAnalysisService.calculateTrend("USD", "EUR", "7D"))
                .thenReturn(BigDecimal.ZERO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trendPercentage", is(0)))
                .andExpect(jsonPath("$.description", containsString("appreciated")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should handle negative trend")
    void analyzeTrend_ShouldHandleNegativeTrend() throws Exception {
        // Arrange
        when(trendAnalysisService.calculateTrend("USD", "EUR", "7D"))
                .thenReturn(new BigDecimal("-5.75"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "EUR")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trendPercentage", is(-5.75)))
                .andExpect(jsonPath("$.description", containsString("depreciated")))
                .andExpect(jsonPath("$.description", containsString("5.75")));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    @DisplayName("GET /api/v1/currencies/trends - Should return 409 when insufficient data")
    void analyzeTrend_ShouldReturn409_WhenInsufficientData() throws Exception {
        // Arrange - Use valid currency GBP to avoid validation error
        when(trendAnalysisService.calculateTrend("USD", "GBP", "7D"))
                .thenThrow(new IllegalStateException("Insufficient historical data"));

        // Act & Assert - GlobalExceptionHandler maps IllegalStateException to 409 CONFLICT
        mockMvc.perform(get("/api/v1/currencies/trends")
                        .param("from", "USD")
                        .param("to", "GBP")
                        .param("period", "7D"))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Insufficient historical data")));
    }
}
