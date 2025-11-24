package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.Currency;
import com.currencyexchange.provider.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CurrencyService
 * Tests all business logic with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CurrencyService Unit Tests")
class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyService currencyService;

    private Currency usdCurrency;
    private Currency eurCurrency;

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
    }

    @Test
    @DisplayName("Should return all currencies")
    void getAllCurrencies_ShouldReturnAllCurrencies() {
        // Arrange
        List<Currency> currencies = Arrays.asList(usdCurrency, eurCurrency);
        when(currencyRepository.findAll()).thenReturn(currencies);

        // Act
        List<Currency> result = currencyService.getAllCurrencies();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder(usdCurrency, eurCurrency);
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no currencies exist")
    void getAllCurrencies_ShouldReturnEmptyList_WhenNoCurrencies() {
        // Arrange
        when(currencyRepository.findAll()).thenReturn(List.of());

        // Act
        List<Currency> result = currencyService.getAllCurrencies();

        // Assert
        assertThat(result).isEmpty();
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should add valid currency successfully")
    void addCurrency_ShouldAddCurrency_WhenValidCode() {
        // Arrange
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.empty());
        when(currencyRepository.save(any(Currency.class))).thenReturn(usdCurrency);

        // Act
        Currency result = currencyService.addCurrency("USD");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("USD");
        assertThat(result.getName()).isEqualTo("US Dollar");
        verify(currencyRepository, times(1)).findByCode("USD");
        verify(currencyRepository, times(1)).save(any(Currency.class));
    }

    @Test
    @DisplayName("Should convert lowercase currency code to uppercase")
    void addCurrency_ShouldConvertToUpperCase_WhenLowercaseCode() {
        // Arrange
        when(currencyRepository.findByCode("EUR")).thenReturn(Optional.empty());
        when(currencyRepository.save(any(Currency.class))).thenReturn(eurCurrency);

        // Act
        Currency result = currencyService.addCurrency("eur");

        // Assert
        assertThat(result.getCode()).isEqualTo("EUR");
        verify(currencyRepository, times(1)).findByCode("EUR");
    }

    @Test
    @DisplayName("Should trim whitespace from currency code")
    void addCurrency_ShouldTrimWhitespace_WhenCodeHasSpaces() {
        // Arrange
        when(currencyRepository.findByCode("GBP")).thenReturn(Optional.empty());
        Currency gbpCurrency = Currency.builder()
                .id(3L)
                .code("GBP")
                .name("British Pound")
                .build();
        when(currencyRepository.save(any(Currency.class))).thenReturn(gbpCurrency);

        // Act
        Currency result = currencyService.addCurrency("  GBP  ");

        // Assert
        assertThat(result.getCode()).isEqualTo("GBP");
        verify(currencyRepository, times(1)).findByCode("GBP");
    }

    @Test
    @DisplayName("Should throw exception when currency code is null")
    void addCurrency_ShouldThrowException_WhenCodeIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> currencyService.addCurrency(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency code cannot be null or empty");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when currency code is empty")
    void addCurrency_ShouldThrowException_WhenCodeIsEmpty() {
        // Act & Assert
        assertThatThrownBy(() -> currencyService.addCurrency(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency code cannot be null or empty");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when currency code is blank")
    void addCurrency_ShouldThrowException_WhenCodeIsBlank() {
        // Act & Assert
        assertThatThrownBy(() -> currencyService.addCurrency("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency code cannot be null or empty");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when currency code is invalid")
    void addCurrency_ShouldThrowException_WhenCodeIsInvalid() {
        // Act & Assert
        assertThatThrownBy(() -> currencyService.addCurrency("XYZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid currency code");

        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when currency already exists")
    void addCurrency_ShouldThrowException_WhenCurrencyAlreadyExists() {
        // Arrange
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

        // Act & Assert
        assertThatThrownBy(() -> currencyService.addCurrency("USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency already exists: USD");

        verify(currencyRepository, times(1)).findByCode("USD");
        verify(currencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get currency by code successfully")
    void getCurrencyByCode_ShouldReturnCurrency_WhenExists() {
        // Arrange
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

        // Act
        Optional<Currency> result = currencyService.getCurrencyByCode("USD");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("USD");
        verify(currencyRepository, times(1)).findByCode("USD");
    }

    @Test
    @DisplayName("Should return empty when currency not found")
    void getCurrencyByCode_ShouldReturnEmpty_WhenNotFound() {
        // Arrange
        when(currencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        // Act
        Optional<Currency> result = currencyService.getCurrencyByCode("XYZ");

        // Assert
        assertThat(result).isEmpty();
        verify(currencyRepository, times(1)).findByCode("XYZ");
    }

    @Test
    @DisplayName("Should return empty when code is null")
    void getCurrencyByCode_ShouldReturnEmpty_WhenCodeIsNull() {
        // Act
        Optional<Currency> result = currencyService.getCurrencyByCode(null);

        // Assert
        assertThat(result).isEmpty();
        verify(currencyRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("Should return empty when code is empty")
    void getCurrencyByCode_ShouldReturnEmpty_WhenCodeIsEmpty() {
        // Act
        Optional<Currency> result = currencyService.getCurrencyByCode("");

        // Assert
        assertThat(result).isEmpty();
        verify(currencyRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("Should check if currency exists")
    void currencyExists_ShouldReturnTrue_WhenCurrencyExists() {
        // Arrange
        when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

        // Act
        boolean result = currencyService.currencyExists("USD");

        // Assert
        assertThat(result).isTrue();
        verify(currencyRepository, times(1)).findByCode("USD");
    }

    @Test
    @DisplayName("Should return false when currency does not exist")
    void currencyExists_ShouldReturnFalse_WhenCurrencyDoesNotExist() {
        // Arrange
        when(currencyRepository.findByCode("XYZ")).thenReturn(Optional.empty());

        // Act
        boolean result = currencyService.currencyExists("XYZ");

        // Assert
        assertThat(result).isFalse();
        verify(currencyRepository, times(1)).findByCode("XYZ");
    }

    @Test
    @DisplayName("Should return false when code is null for exists check")
    void currencyExists_ShouldReturnFalse_WhenCodeIsNull() {
        // Act
        boolean result = currencyService.currencyExists(null);

        // Assert
        assertThat(result).isFalse();
        verify(currencyRepository, never()).findByCode(any());
    }

    @Test
    @DisplayName("Should validate valid ISO 4217 currency code")
    void isValidCurrencyCode_ShouldReturnTrue_WhenValidCode() {
        // Act
        boolean result = currencyService.isValidCurrencyCode("USD");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for invalid currency code")
    void isValidCurrencyCode_ShouldReturnFalse_WhenInvalidCode() {
        // Act
        boolean result = currencyService.isValidCurrencyCode("INVALID");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for null currency code validation")
    void isValidCurrencyCode_ShouldReturnFalse_WhenCodeIsNull() {
        // Act
        boolean result = currencyService.isValidCurrencyCode(null);

        // Assert
        assertThat(result).isFalse();
    }
}
