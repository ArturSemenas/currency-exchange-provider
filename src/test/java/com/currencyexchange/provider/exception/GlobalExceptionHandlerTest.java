package com.currencyexchange.provider.exception;

import com.currencyexchange.provider.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * @author Currency Exchange Provider Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    @DisplayName("Should handle CurrencyNotFoundException")
    void handleCurrencyNotFoundException_ShouldReturnNotFound() {
        // Given
        CurrencyNotFoundException exception = new CurrencyNotFoundException("USD");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleCurrencyNotFoundException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Currency Not Found");
        assertThat(response.getBody().message()).isEqualTo("Currency not found: USD");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().validationErrors()).isNull();
    }

    @Test
    @DisplayName("Should handle ExchangeRateNotFoundException")
    void handleExchangeRateNotFoundException_ShouldReturnNotFound() {
        // Given
        ExchangeRateNotFoundException exception = 
                new ExchangeRateNotFoundException("USD", "EUR");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleExchangeRateNotFoundException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Exchange Rate Not Found");
        assertThat(response.getBody().message())
                .isEqualTo("Exchange rate not found for currency pair: USD -> EUR");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle InvalidPeriodFormatException")
    void handleInvalidPeriodFormatException_ShouldReturnBadRequest() {
        // Given
        InvalidPeriodFormatException exception = new InvalidPeriodFormatException("5X");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleInvalidPeriodFormatException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Invalid Period Format");
        assertThat(response.getBody().message()).contains("Invalid period format: '5X'");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle ExternalApiException")
    void handleExternalApiException_ShouldReturnServiceUnavailable() {
        // Given
        ExternalApiException exception = new ExternalApiException("Fixer.io", "Connection timeout");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleExternalApiException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(503);
        assertThat(response.getBody().error()).isEqualTo("External API Error");
        assertThat(response.getBody().message())
                .isEqualTo("External API error from Fixer.io: Connection timeout");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle CurrencyAlreadyExistsException")
    void handleCurrencyAlreadyExistsException_ShouldReturnConflict() {
        // Given
        CurrencyAlreadyExistsException exception = new CurrencyAlreadyExistsException("EUR");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleCurrencyAlreadyExistsException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Currency Already Exists");
        assertThat(response.getBody().message()).isEqualTo("Currency already exists: EUR");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle InsufficientDataException")
    void handleInsufficientDataException_ShouldReturnNotFound() {
        // Given
        InsufficientDataException exception = 
                new InsufficientDataException("USD", "EUR", "7D");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleInsufficientDataException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Insufficient Data");
        assertThat(response.getBody().message())
                .isEqualTo("Insufficient historical data for USD -> EUR over period 7D");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException")
    void handleConstraintViolation_ShouldReturnBadRequestWithValidationErrors() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("convertCurrency.amount");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getInvalidValue()).thenReturn(-100.0);
        when(violation1.getMessage()).thenReturn("must be greater than 0");
        violations.add(violation1);
        
        ConstraintViolationException exception = new ConstraintViolationException(violations);

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleConstraintViolation(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Validation Failed");
        assertThat(response.getBody().message())
                .isEqualTo("Parameter validation failed. See validationErrors for details.");
        assertThat(response.getBody().validationErrors()).hasSize(1);
        assertThat(response.getBody().validationErrors().get(0).field())
                .isEqualTo("amount");
        assertThat(response.getBody().validationErrors().get(0).rejectedValue())
                .isEqualTo(-100.0);
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException")
    void handleIllegalArgument_ShouldReturnBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleIllegalArgument(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid input");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle IllegalStateException")
    void handleIllegalState_ShouldReturnConflict() {
        // Given
        IllegalStateException exception = new IllegalStateException("Invalid state");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleIllegalState(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).isEqualTo("Invalid state");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle AuthenticationException")
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        // Given
        AuthenticationException exception = new AuthenticationException("Auth failed") {};

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleAuthenticationException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("Unauthorized");
        assertThat(response.getBody().message())
                .isEqualTo("Authentication failed. Please provide valid credentials.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle BadCredentialsException")
    void handleBadCredentials_ShouldReturnUnauthorized() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleBadCredentials(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().error()).isEqualTo("Unauthorized");
        assertThat(response.getBody().message()).isEqualTo("Invalid username or password.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException")
    void handleAccessDenied_ShouldReturnForbidden() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleAccessDenied(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().error()).isEqualTo("Forbidden");
        assertThat(response.getBody().message())
                .isEqualTo("You do not have permission to access this resource.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void handleGlobalException_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new Exception("Unexpected error");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleGlobalException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message())
                .isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should extract path from WebRequest correctly")
    void handleException_ShouldExtractPathCorrectly() {
        // Given
        when(webRequest.getDescription(false)).thenReturn("uri=/api/v1/currencies");
        CurrencyNotFoundException exception = new CurrencyNotFoundException("GBP");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleCurrencyNotFoundException(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path()).isEqualTo("/api/v1/currencies");
    }

    @Test
    @DisplayName("Should include timestamp in error response")
    void handleException_ShouldIncludeTimestamp() {
        // Given
        CurrencyNotFoundException exception = new CurrencyNotFoundException("JPY");

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleCurrencyNotFoundException(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should handle ConstraintViolationException with simple property path")
    void handleConstraintViolation_ShouldHandleSimplePropertyPath() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("currency");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getInvalidValue()).thenReturn("us");
        when(violation.getMessage()).thenReturn("must be 3 characters");
        violations.add(violation);
        
        ConstraintViolationException exception = new ConstraintViolationException(violations);

        // When
        ResponseEntity<ErrorResponseDto> response = 
                exceptionHandler.handleConstraintViolation(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().validationErrors()).hasSize(1);
        assertThat(response.getBody().validationErrors().get(0).field())
                .isEqualTo("currency");
    }

}
