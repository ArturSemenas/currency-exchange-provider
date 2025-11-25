package com.currencyexchange.provider.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExchangeRateCacheService.
 *
 * @author Artur Semenas
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateCacheService Unit Tests")
class ExchangeRateCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    @InjectMocks
    private ExchangeRateCacheService cacheService;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    @Captor
    private ArgumentCaptor<String> fieldCaptor;

    @Captor
    private ArgumentCaptor<BigDecimal> valueCaptor;

    private static final String BASE_CURRENCY = "USD";
    private static final String TARGET_CURRENCY = "EUR";
    private static final BigDecimal RATE = new BigDecimal("0.85");

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("storeRates should store rates in Redis with expiration")
    void storeRates_ShouldStoreRatesWithExpiration() {
        // Arrange
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put(TARGET_CURRENCY, RATE);
        rates.put("GBP", new BigDecimal("0.75"));

        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // Act
        cacheService.storeRates(BASE_CURRENCY, rates);

        // Assert
        verify(hashOperations, times(2)).put(eq("rates:USD"), anyString(), any(BigDecimal.class));
        verify(redisTemplate).expire(eq("rates:USD"), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("storeRates should handle empty rates map")
    void storeRates_ShouldHandleEmptyRatesMap() {
        // Arrange
        Map<String, BigDecimal> emptyRates = new HashMap<>();
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // Act
        cacheService.storeRates(BASE_CURRENCY, emptyRates);

        // Assert
        verify(hashOperations, never()).put(anyString(), anyString(), any());
        verify(redisTemplate).expire(eq("rates:USD"), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("storeRates should handle Redis exception gracefully")
    void storeRates_ShouldHandleRedisException() {
        // Arrange
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put(TARGET_CURRENCY, RATE);

        doThrow(new RuntimeException("Redis error"))
                .when(hashOperations).put(anyString(), anyString(), any());

        // Act & Assert - should not throw exception
        cacheService.storeRates(BASE_CURRENCY, rates);

        // Verify that put was attempted
        verify(hashOperations).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("storeBestRates should store multiple base currencies")
    void storeBestRates_ShouldStoreMultipleBaseCurrencies() {
        // Arrange
        Map<String, Map<String, BigDecimal>> bestRates = new HashMap<>();
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        Map<String, BigDecimal> eurRates = new HashMap<>();
        eurRates.put("USD", new BigDecimal("1.18"));

        bestRates.put("USD", usdRates);
        bestRates.put("EUR", eurRates);

        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // Act
        cacheService.storeBestRates(bestRates);

        // Assert
        verify(hashOperations, times(2)).put(anyString(), anyString(), any(BigDecimal.class));
        verify(redisTemplate, times(2)).expire(anyString(), eq(2L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("storeBestRates should handle empty map")
    void storeBestRates_ShouldHandleEmptyMap() {
        // Arrange
        Map<String, Map<String, BigDecimal>> emptyBestRates = new HashMap<>();

        // Act
        cacheService.storeBestRates(emptyBestRates);

        // Assert
        verify(hashOperations, never()).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("storeBestRates should handle exception gracefully")
    void storeBestRates_ShouldHandleException() {
        // Arrange
        Map<String, Map<String, BigDecimal>> bestRates = new HashMap<>();
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        bestRates.put("USD", usdRates);

        doThrow(new RuntimeException("Redis error"))
                .when(hashOperations).put(anyString(), anyString(), any());

        // Act & Assert - should not throw exception
        cacheService.storeBestRates(bestRates);
    }

    @Test
    @DisplayName("getRate should return rate from cache when exists")
    void getRate_ShouldReturnRateWhenExists() {
        // Arrange
        when(hashOperations.get("rates:USD", TARGET_CURRENCY)).thenReturn(RATE);

        // Act
        Optional<BigDecimal> result = cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(RATE);
        verify(hashOperations).get("rates:USD", TARGET_CURRENCY);
    }

    @Test
    @DisplayName("getRate should return rate from String representation")
    void getRate_ShouldConvertStringToBigDecimal() {
        // Arrange
        when(hashOperations.get("rates:USD", TARGET_CURRENCY)).thenReturn("0.85");

        // Act
        Optional<BigDecimal> result = cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(RATE);
    }

    @Test
    @DisplayName("getRate should return empty when rate not in cache")
    void getRate_ShouldReturnEmptyWhenNotFound() {
        // Arrange
        when(hashOperations.get("rates:USD", TARGET_CURRENCY)).thenReturn(null);

        // Act
        Optional<BigDecimal> result = cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getRate should handle Redis exception and return empty")
    void getRate_ShouldHandleExceptionAndReturnEmpty() {
        // Arrange
        when(hashOperations.get(anyString(), anyString()))
                .thenThrow(new RuntimeException("Redis error"));

        // Act
        Optional<BigDecimal> result = cacheService.getRate(BASE_CURRENCY, TARGET_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllRates should return all rates for base currency")
    void getAllRates_ShouldReturnAllRatesForBaseCurrency() {
        // Arrange
        Map<Object, Object> entries = new HashMap<>();
        entries.put("EUR", new BigDecimal("0.85"));
        entries.put("GBP", "0.75");

        when(hashOperations.entries("rates:USD")).thenReturn(entries);

        // Act
        Map<String, BigDecimal> result = cacheService.getAllRates(BASE_CURRENCY);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get("EUR")).isEqualByComparingTo("0.85");
        assertThat(result.get("GBP")).isEqualByComparingTo("0.75");
    }

    @Test
    @DisplayName("getAllRates should return empty map when no rates cached")
    void getAllRates_ShouldReturnEmptyMapWhenNoRates() {
        // Arrange
        when(hashOperations.entries("rates:USD")).thenReturn(new HashMap<>());

        // Act
        Map<String, BigDecimal> result = cacheService.getAllRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllRates should handle exception and return empty map")
    void getAllRates_ShouldHandleExceptionAndReturnEmpty() {
        // Arrange
        when(hashOperations.entries(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

        // Act
        Map<String, BigDecimal> result = cacheService.getAllRates(BASE_CURRENCY);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllCachedRates should return rates for all base currencies")
    void getAllCachedRates_ShouldReturnAllBaseCurrencies() {
        // Arrange
        Set<String> keys = Set.of("rates:USD", "rates:EUR");
        when(redisTemplate.keys("rates:*")).thenReturn(keys);

        Map<Object, Object> usdEntries = new HashMap<>();
        usdEntries.put("EUR", new BigDecimal("0.85"));
        when(hashOperations.entries("rates:USD")).thenReturn(usdEntries);

        Map<Object, Object> eurEntries = new HashMap<>();
        eurEntries.put("USD", new BigDecimal("1.18"));
        when(hashOperations.entries("rates:EUR")).thenReturn(eurEntries);

        // Act
        Map<String, Map<String, BigDecimal>> result = cacheService.getAllCachedRates();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get("USD")).containsKey("EUR");
        assertThat(result.get("EUR")).containsKey("USD");
    }

    @Test
    @DisplayName("getAllCachedRates should handle null keys")
    void getAllCachedRates_ShouldHandleNullKeys() {
        // Arrange
        when(redisTemplate.keys("rates:*")).thenReturn(null);

        // Act
        Map<String, Map<String, BigDecimal>> result = cacheService.getAllCachedRates();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllCachedRates should skip empty rate maps")
    void getAllCachedRates_ShouldSkipEmptyRateMaps() {
        // Arrange
        Set<String> keys = Set.of("rates:USD", "rates:EUR");
        when(redisTemplate.keys("rates:*")).thenReturn(keys);

        Map<Object, Object> usdEntries = new HashMap<>();
        usdEntries.put("GBP", new BigDecimal("0.75"));
        when(hashOperations.entries("rates:USD")).thenReturn(usdEntries);
        when(hashOperations.entries("rates:EUR")).thenReturn(new HashMap<>());

        // Act
        Map<String, Map<String, BigDecimal>> result = cacheService.getAllCachedRates();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsKey("USD");
        assertThat(result).doesNotContainKey("EUR");
    }

    @Test
    @DisplayName("getAllCachedRates should handle exception and return empty map")
    void getAllCachedRates_ShouldHandleExceptionAndReturnEmpty() {
        // Arrange
        when(redisTemplate.keys(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

        // Act
        Map<String, Map<String, BigDecimal>> result = cacheService.getAllCachedRates();

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("evictAll should delete all rate keys")
    void evictAll_ShouldDeleteAllKeys() {
        // Arrange
        Set<String> keys = Set.of("rates:USD", "rates:EUR", "rates:GBP");
        when(redisTemplate.keys("rates:*")).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(3L);

        // Act
        cacheService.evictAll();

        // Assert
        verify(redisTemplate).keys("rates:*");
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("evictAll should handle null keys")
    void evictAll_ShouldHandleNullKeys() {
        // Arrange
        when(redisTemplate.keys("rates:*")).thenReturn(null);

        // Act
        cacheService.evictAll();

        // Assert
        verify(redisTemplate).keys("rates:*");
        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    @Test
    @DisplayName("evictAll should handle empty keys")
    void evictAll_ShouldHandleEmptyKeys() {
        // Arrange
        when(redisTemplate.keys("rates:*")).thenReturn(Set.of());

        // Act
        cacheService.evictAll();

        // Assert
        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    @Test
    @DisplayName("evictAll should handle exception gracefully")
    void evictAll_ShouldHandleException() {
        // Arrange
        when(redisTemplate.keys(anyString()))
                .thenThrow(new RuntimeException("Redis error"));

        // Act & Assert - should not throw exception
        cacheService.evictAll();
    }

    @Test
    @DisplayName("evictRates should delete rates for specific base currency")
    void evictRates_ShouldDeleteSpecificCurrency() {
        // Arrange
        when(redisTemplate.delete(any(String.class))).thenReturn(true);

        // Act
        cacheService.evictRates(BASE_CURRENCY);

        // Assert
        verify(redisTemplate).delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("rates:USD");
    }

    @Test
    @DisplayName("evictRates should handle key not existing")
    void evictRates_ShouldHandleKeyNotExisting() {
        // Arrange
        when(redisTemplate.delete(any(String.class))).thenReturn(false);

        // Act
        cacheService.evictRates(BASE_CURRENCY);

        // Assert
        verify(redisTemplate).delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("rates:USD");
    }

    @Test
    @DisplayName("evictRates should handle null return from delete")
    void evictRates_ShouldHandleNullReturnFromDelete() {
        // Arrange
        when(redisTemplate.delete(any(String.class))).thenReturn(null);

        // Act
        cacheService.evictRates(BASE_CURRENCY);

        // Assert
        verify(redisTemplate).delete(keyCaptor.capture());
        assertThat(keyCaptor.getValue()).isEqualTo("rates:USD");
    }

    @Test
    @DisplayName("evictRates should handle exception gracefully")
    void evictRates_ShouldHandleException() {
        // Arrange
        when(redisTemplate.delete(any(String.class)))
                .thenThrow(new RuntimeException("Redis error"));

        // Act & Assert - should not throw exception
        cacheService.evictRates(BASE_CURRENCY);
    }

    @Test
    @DisplayName("isAvailable should return true when Redis is reachable")
    void isAvailable_ShouldReturnTrueWhenReachable() {
        // Arrange
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        // Act
        boolean result = cacheService.isAvailable();

        // Assert
        assertThat(result).isTrue();
        verify(redisConnection).ping();
    }

    @Test
    @DisplayName("isAvailable should return false when Redis is not reachable")
    void isAvailable_ShouldReturnFalseWhenNotReachable() {
        // Arrange
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection())
                .thenThrow(new RuntimeException("Connection failed"));

        // Act
        boolean result = cacheService.isAvailable();

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isAvailable should return false when ping fails")
    void isAvailable_ShouldReturnFalseWhenPingFails() {
        // Arrange
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenThrow(new RuntimeException("Ping failed"));

        // Act
        boolean result = cacheService.isAvailable();

        // Assert
        assertThat(result).isFalse();
    }
}
