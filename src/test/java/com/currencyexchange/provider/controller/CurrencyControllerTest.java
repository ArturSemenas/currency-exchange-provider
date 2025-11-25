package com.currencyexchange.provider.controller;

import com.currencyexchange.provider.dto.CurrencyDto;
import com.currencyexchange.provider.mapper.CurrencyMapper;
import com.currencyexchange.provider.model.Currency;
import com.currencyexchange.provider.service.CurrencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for CurrencyController using MockMvc
 * Tests REST endpoints, validation, and security
 */
@WebMvcTest(CurrencyController.class)
@Import(TestSecurityConfig.class)
@DisplayName("CurrencyController Unit Tests")
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private CurrencyMapper currencyMapper;

    private Currency usdCurrency;
    private Currency eurCurrency;
    private CurrencyDto usdDto;
    private CurrencyDto eurDto;

    @BeforeEach
    void setUp() {
        usdCurrency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .build();

        eurCurrency = Currency.builder()
                .id(2L)
                .code("EUR")
                .name("Euro")
                .build();

        usdDto = new CurrencyDto("USD", "US Dollar");
        eurDto = new CurrencyDto("EUR", "Euro");
    }

    @Test
    @DisplayName("GET /api/v1/currencies - Should return all currencies")
    void getAllCurrencies_ShouldReturnListOfCurrencies() throws Exception {
        // Arrange
        List<Currency> currencies = Arrays.asList(usdCurrency, eurCurrency);
        List<CurrencyDto> dtos = Arrays.asList(usdDto, eurDto);
        
        when(currencyService.getAllCurrencies()).thenReturn(currencies);
        when(currencyMapper.toDtoList(currencies)).thenReturn(dtos);

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code", is("USD")))
                .andExpect(jsonPath("$[0].name", is("US Dollar")))
                .andExpect(jsonPath("$[1].code", is("EUR")))
                .andExpect(jsonPath("$[1].name", is("Euro")));

        verify(currencyService, times(1)).getAllCurrencies();
        verify(currencyMapper, times(1)).toDtoList(currencies);
    }

    @Test
    @DisplayName("GET /api/v1/currencies - Should return empty list when no currencies")
    void getAllCurrencies_ShouldReturnEmptyList_WhenNoCurrencies() throws Exception {
        // Arrange
        when(currencyService.getAllCurrencies()).thenReturn(List.of());
        when(currencyMapper.toDtoList(any())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(currencyService, times(1)).getAllCurrencies();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies - Should add currency with ADMIN role")
    void addCurrency_ShouldAddCurrency_WhenUserIsAdmin() throws Exception {
        // Arrange
        when(currencyService.addCurrency("GBP")).thenReturn(
                Currency.builder().id(3L).code("GBP").name("British Pound").build());
        when(currencyMapper.toDto(any(Currency.class))).thenReturn(
                new CurrencyDto("GBP", "British Pound"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/currencies")
                        .with(csrf())
                        .param("currency", "GBP"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("GBP")))
                .andExpect(jsonPath("$.name", is("British Pound")));

        verify(currencyService, times(1)).addCurrency("GBP");
        verify(currencyMapper, times(1)).toDto(any(Currency.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/v1/currencies - Should return 403 when user is not ADMIN")
    void addCurrency_ShouldReturnForbidden_WhenUserIsNotAdmin() throws Exception {
        // Act & Assert
        // User with ROLE_USER should get 403 Forbidden
        mockMvc.perform(post("/api/v1/currencies")
                        .with(csrf())
                        .param("currency", "GBP"))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(currencyService, never()).addCurrency(any());
    }

    @Test
    @DisplayName("POST /api/v1/currencies - Should return 401 without authentication")
    void addCurrency_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        // Act & Assert
        // Without authentication, should return 401 Unauthorized (not 403)
        mockMvc.perform(post("/api/v1/currencies")
                        .with(csrf())
                        .param("currency", "GBP"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(currencyService, never()).addCurrency(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies - Should return 400 for invalid currency code")
    void addCurrency_ShouldReturnBadRequest_WhenInvalidCurrencyCode() throws Exception {
        // Act & Assert
        // Note: The @ValidCurrency annotation validates at the controller level,
        // so the service is never called for invalid codes
        mockMvc.perform(post("/api/v1/currencies")
                        .with(csrf())
                        .param("currency", "XYZ"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        // Verify service was NOT called due to validation failure
        verify(currencyService, never()).addCurrency(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies - Should return 400 when currency already exists")
    void addCurrency_ShouldReturnBadRequest_WhenCurrencyAlreadyExists() throws Exception {
        // Arrange
        when(currencyService.addCurrency("USD"))
                .thenThrow(new IllegalArgumentException("Currency already exists: USD"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/currencies")
                        .with(csrf())
                        .param("currency", "USD"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(currencyService, times(1)).addCurrency("USD");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies - Should return 400 for blank currency code")
    void addCurrency_ShouldReturnBadRequest_WhenCurrencyCodeIsBlank() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/currencies")
                        .with(csrf())
                        .param("currency", ""))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(currencyService, never()).addCurrency(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/v1/currencies - Should accept lowercase currency code")
    void addCurrency_ShouldAcceptLowercaseCode() throws Exception {
        // Arrange
        when(currencyService.addCurrency("gbp")).thenReturn(
                Currency.builder().id(3L).code("GBP").name("British Pound").build());
        when(currencyMapper.toDto(any(Currency.class))).thenReturn(
                new CurrencyDto("GBP", "British Pound"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/currencies")
                        .with(csrf())
                        .param("currency", "gbp"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code", is("GBP")));

        verify(currencyService, times(1)).addCurrency("gbp");
    }
}
