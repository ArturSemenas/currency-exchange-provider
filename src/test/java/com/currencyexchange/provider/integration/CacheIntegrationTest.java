package com.currencyexchange.provider.integration;

import com.currencyexchange.provider.service.ExchangeRateCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Redis cache with TestContainers
 * Tests cache operations with real Redis instance
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class CacheIntegrationTest extends BaseIntegrationTest {

    static GenericContainer<?> redisContainer;

    static {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
        redisContainer.start();
    }

    @Autowired
    private ExchangeRateCacheService cacheService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Override base class properties to enable Redis
     */
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.cache.type", () -> "redis");
        registry.add("management.health.redis.enabled", () -> "true");
    }

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        }
    }

    @Test
    @DisplayName("Store and retrieve exchange rate from cache")
    void testCacheStoreAndRetrieve() {
        // Arrange
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        rates.put("GBP", new BigDecimal("0.75"));
        rates.put("JPY", new BigDecimal("110.50"));

        // Act - Store in cache
        cacheService.storeRates("USD", rates);

        // Assert - Retrieve from cache
        Optional<BigDecimal> eurRate = cacheService.getRate("USD", "EUR");
        Optional<BigDecimal> gbpRate = cacheService.getRate("USD", "GBP");
        Optional<BigDecimal> jpyRate = cacheService.getRate("USD", "JPY");

        assertThat(eurRate).isPresent();
        assertThat(eurRate.get()).isEqualByComparingTo("0.85");
        
        assertThat(gbpRate).isPresent();
        assertThat(gbpRate.get()).isEqualByComparingTo("0.75");
        
        assertThat(jpyRate).isPresent();
        assertThat(jpyRate.get()).isEqualByComparingTo("110.50");
    }

    @Test
    @DisplayName("Cache miss returns empty Optional")
    void testCacheMiss() {
        // Act - Try to get non-existent rate
        Optional<BigDecimal> rate = cacheService.getRate("USD", "XYZ");

        // Assert
        assertThat(rate).isEmpty();
    }

    @Test
    @DisplayName("Get all rates for base currency")
    void testGetAllRatesForBase() {
        // Arrange
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        rates.put("GBP", new BigDecimal("0.75"));
        rates.put("JPY", new BigDecimal("110.50"));

        // Act - Store and retrieve
        cacheService.storeRates("USD", rates);
        Map<String, BigDecimal> cachedRates = cacheService.getAllRates("USD");

        // Assert
        assertThat(cachedRates).hasSize(3);
        assertThat(cachedRates).containsKeys("EUR", "GBP", "JPY");
        assertThat(cachedRates.get("EUR")).isEqualByComparingTo("0.85");
    }

    @Test
    @DisplayName("Evict cache for specific base currency")
    void testCacheEviction() {
        // Arrange
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        cacheService.storeRates("USD", usdRates);

        Map<String, BigDecimal> eurRates = new HashMap<>();
        eurRates.put("USD", new BigDecimal("1.18"));
        cacheService.storeRates("EUR", eurRates);

        // Act - Evict USD rates
        cacheService.evictRates("USD");

        // Assert - USD rates should be gone, EUR rates should remain
        Optional<BigDecimal> usdEur = cacheService.getRate("USD", "EUR");
        Optional<BigDecimal> eurUsd = cacheService.getRate("EUR", "USD");

        assertThat(usdEur).isEmpty();
        assertThat(eurUsd).isPresent();
    }

    @Test
    @DisplayName("Evict all cache entries")
    void testEvictAllRates() {
        // Arrange
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        cacheService.storeRates("USD", usdRates);

        Map<String, BigDecimal> eurRates = new HashMap<>();
        eurRates.put("USD", new BigDecimal("1.18"));
        cacheService.storeRates("EUR", eurRates);

        // Act - Evict all
        cacheService.evictAll();

        // Assert - All rates should be gone
        Optional<BigDecimal> usdEur = cacheService.getRate("USD", "EUR");
        Optional<BigDecimal> eurUsd = cacheService.getRate("EUR", "USD");

        assertThat(usdEur).isEmpty();
        assertThat(eurUsd).isEmpty();
    }

    @Test
    @DisplayName("Redis container is available")
    void testRedisAvailability() {
        assertThat(redisContainer.isRunning()).isTrue();
        assertThat(redisTemplate).isNotNull();
    }

    @Test
    @DisplayName("Store best rates for multiple base currencies")
    void testStoreBestRates() {
        // Arrange
        Map<String, BigDecimal> usdRates = new HashMap<>();
        usdRates.put("EUR", new BigDecimal("0.85"));
        usdRates.put("GBP", new BigDecimal("0.75"));

        Map<String, BigDecimal> eurRates = new HashMap<>();
        eurRates.put("USD", new BigDecimal("1.18"));
        eurRates.put("GBP", new BigDecimal("0.88"));

        // Act
        cacheService.storeRates("USD", usdRates);
        cacheService.storeRates("EUR", eurRates);

        // Assert - All rates should be retrievable
        assertThat(cacheService.getRate("USD", "EUR")).isPresent();
        assertThat(cacheService.getRate("USD", "GBP")).isPresent();
        assertThat(cacheService.getRate("EUR", "USD")).isPresent();
        assertThat(cacheService.getRate("EUR", "GBP")).isPresent();
    }

    @Test
    @DisplayName("Cache survives across test methods")
    void testCachePersistence() {
        // This test verifies the cache is working
        // Cache is cleared in @BeforeEach, so this tests clean state

        // Arrange & Act
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.85"));
        cacheService.storeRates("USD", rates);

        // Assert
        Optional<BigDecimal> rate = cacheService.getRate("USD", "EUR");
        assertThat(rate).isPresent();
    }
}
