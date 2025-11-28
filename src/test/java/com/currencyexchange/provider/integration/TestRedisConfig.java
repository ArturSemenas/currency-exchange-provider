package com.currencyexchange.provider.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

/**
 * Test configuration for Redis dependencies.
 * Provides mock RedisTemplate when Redis autoconfiguration is excluded.
 *
 * @see BaseIntegrationTest
 */
@TestConfiguration
public class TestRedisConfig {

    /**
     * Creates a mock RedisTemplate bean for testing.
     * This prevents dependency injection failures when Redis autoconfiguration is excluded.
     *
     * @return mock RedisTemplate instance
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> mock = (RedisTemplate<String, Object>) mock(RedisTemplate.class);
        return mock;
    }
}
