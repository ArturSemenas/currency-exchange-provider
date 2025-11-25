package com.currencyexchange.provider.scheduler;

import com.currencyexchange.provider.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ExchangeRateScheduler.
 * Tests scheduled and manual refresh functionality with success and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateScheduler Tests")
class ExchangeRateSchedulerTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ExchangeRateScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(exchangeRateService);
    }

    @Test
    @DisplayName("Should successfully refresh exchange rates when scheduled")
    void refreshExchangeRates_ShouldSucceed() {
        // Arrange
        int expectedCount = 50;
        when(exchangeRateService.refreshRates()).thenReturn(expectedCount);

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should handle zero rates refreshed")
    void refreshExchangeRates_ShouldHandleZeroRates() {
        // Arrange
        when(exchangeRateService.refreshRates()).thenReturn(0);

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should handle exception during refresh and log error")
    void refreshExchangeRates_ShouldHandleException() {
        // Arrange
        String errorMessage = "Provider connection failed";
        when(exchangeRateService.refreshRates())
                .thenThrow(new RuntimeException(errorMessage));

        // Act - should not throw exception, just log it
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should handle null pointer exception during refresh")
    void refreshExchangeRates_ShouldHandleNullPointerException() {
        // Arrange
        when(exchangeRateService.refreshRates())
                .thenThrow(new NullPointerException("Service unavailable"));

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should successfully trigger manual refresh")
    void triggerManualRefresh_ShouldSucceed() {
        // Arrange
        int expectedCount = 30;
        when(exchangeRateService.refreshRates()).thenReturn(expectedCount);

        // Act
        scheduler.triggerManualRefresh();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should handle exception during manual refresh")
    void triggerManualRefresh_ShouldHandleException() {
        // Arrange
        when(exchangeRateService.refreshRates())
                .thenThrow(new RuntimeException("Manual refresh failed"));

        // Act
        scheduler.triggerManualRefresh();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should call refresh service exactly once per scheduled execution")
    void refreshExchangeRates_ShouldCallServiceOnce() {
        // Arrange
        when(exchangeRateService.refreshRates()).thenReturn(10);

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
        verifyNoMoreInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("Should handle large number of refreshed rates")
    void refreshExchangeRates_ShouldHandleLargeCount() {
        // Arrange
        int largeCount = 10000;
        when(exchangeRateService.refreshRates()).thenReturn(largeCount);

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Manual refresh should invoke scheduled refresh method")
    void triggerManualRefresh_ShouldInvokeScheduledMethod() {
        // Arrange
        when(exchangeRateService.refreshRates()).thenReturn(25);

        // Act
        scheduler.triggerManualRefresh();

        // Assert
        // Manual trigger calls the same refresh logic
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should handle IllegalStateException during refresh")
    void refreshExchangeRates_ShouldHandleIllegalStateException() {
        // Arrange
        when(exchangeRateService.refreshRates())
                .thenThrow(new IllegalStateException("Service not initialized"));

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should complete refresh successfully with typical rate count")
    void refreshExchangeRates_ShouldCompleteWithTypicalCount() {
        // Arrange
        int typicalCount = 45; // Typical number of currency pairs
        when(exchangeRateService.refreshRates()).thenReturn(typicalCount);

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should handle negative rate count gracefully")
    void refreshExchangeRates_ShouldHandleNegativeCount() {
        // Arrange
        when(exchangeRateService.refreshRates()).thenReturn(-1);

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Multiple manual refreshes should each call service")
    void triggerManualRefresh_MultipleCallsShouldEachInvokeService() {
        // Arrange
        when(exchangeRateService.refreshRates()).thenReturn(20, 25, 30);

        // Act
        scheduler.triggerManualRefresh();
        scheduler.triggerManualRefresh();
        scheduler.triggerManualRefresh();

        // Assert
        verify(exchangeRateService, times(3)).refreshRates();
    }

    @Test
    @DisplayName("Should handle exception with detailed error message")
    void refreshExchangeRates_ShouldHandleDetailedException() {
        // Arrange
        String detailedMessage = "Connection timeout: Unable to reach api.fixer.io after 30 seconds";
        when(exchangeRateService.refreshRates())
                .thenThrow(new RuntimeException(detailedMessage));

        // Act
        scheduler.refreshExchangeRates();

        // Assert
        verify(exchangeRateService, times(1)).refreshRates();
    }

    @Test
    @DisplayName("Should execute refresh independently of previous execution state")
    void refreshExchangeRates_ShouldBeStateless() {
        // Arrange
        when(exchangeRateService.refreshRates())
                .thenReturn(10)
                .thenThrow(new RuntimeException("Error"))
                .thenReturn(15);

        // Act
        scheduler.refreshExchangeRates(); // First call succeeds
        scheduler.refreshExchangeRates(); // Second call fails
        scheduler.refreshExchangeRates(); // Third call succeeds

        // Assert
        verify(exchangeRateService, times(3)).refreshRates();
    }
}
