package com.currencyexchange.provider.integration;

import com.currencyexchange.provider.model.Currency;
import com.currencyexchange.provider.model.ExchangeRate;
import com.currencyexchange.provider.repository.CurrencyRepository;
import com.currencyexchange.provider.repository.ExchangeRateRepository;
import com.currencyexchange.provider.service.CurrencyService;
import com.currencyexchange.provider.service.ExchangeRateService;
import com.currencyexchange.provider.service.TrendAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for complete currency exchange flow
 * Tests end-to-end scenarios with real database
 */
@Transactional
class CurrencyFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private TrendAnalysisService trendAnalysisService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    private Optional<BigDecimal> exchangeRate;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        exchangeRateRepository.deleteAll();
        currencyRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete flow: add currency → store rate → convert currency")
    void testCompleteCurrencyFlow() {
        // Step 1: Add currencies
        Currency usd = currencyService.addCurrency("USD");
        Currency eur = currencyService.addCurrency("EUR");

        assertThat(usd.getCode()).isEqualTo("USD");
        assertThat(eur.getCode()).isEqualTo("EUR");

        // Step 2: Manually store an exchange rate
        ExchangeRate rate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.85"))
                .timestamp(LocalDateTime.now())
                .provider("Test")
                .build();
        
        exchangeRateRepository.save(rate);

        // Step 3: Convert currency
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(
                new BigDecimal("100.00"),
                "USD",
                "EUR"
        );

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("85.00");
    }

    @Test
    @DisplayName("Same currency conversion returns original amount")
    void testSameCurrencyConversion() {
        // Add currency
        currencyService.addCurrency("USD");

        // Convert USD to USD
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(
                new BigDecimal("100.00"),
                "USD",
                "USD"
        );

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Missing exchange rate returns empty")
    void testMissingExchangeRate() {
        // Add currencies but no rate
        currencyService.addCurrency("USD");
        currencyService.addCurrency("GBP");

        // Try to convert without rate
        Optional<BigDecimal> result = exchangeRate;

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Currency persistence across transactions")
    void testCurrencyPersistence() {
        // Add currencies
        currencyService.addCurrency("USD");
        currencyService.addCurrency("EUR");
        currencyService.addCurrency("GBP");

        // Verify persistence
        assertThat(currencyRepository.count()).isEqualTo(3);
        assertThat(currencyRepository.findByCode("USD")).isPresent();
        assertThat(currencyRepository.findByCode("EUR")).isPresent();
        assertThat(currencyRepository.findByCode("GBP")).isPresent();
    }

    @Test
    @DisplayName("Multiple rates for same pair - retrieves latest")
    void testMultipleRatesForSamePair() {
        // Add currencies
        currencyService.addCurrency("USD");
        currencyService.addCurrency("EUR");

        // Add older rate
        ExchangeRate oldRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.80"))
                .timestamp(LocalDateTime.now().minusHours(2))
                .provider("Test")
                .build();
        exchangeRateRepository.save(oldRate);

        // Add newer rate
        ExchangeRate newRate = ExchangeRate.builder()
                .baseCurrency("USD")
                .targetCurrency("EUR")
                .rate(new BigDecimal("0.85"))
                .timestamp(LocalDateTime.now())
                .provider("Test")
                .build();
        exchangeRateRepository.save(newRate);

        // Convert should use the latest rate (0.85)
        Optional<BigDecimal> result = exchangeRateService.getExchangeRate(
                new BigDecimal("100.00"),
                "USD",
                "EUR"
        );

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo("85.00");
    }

    @Test
    @DisplayName("Trend calculation with historical data")
    void testTrendCalculation() {
        // Add currencies
        currencyService.addCurrency("USD");
        currencyService.addCurrency("EUR");

        // Add historical rates (7 days ago to now, increasing trend)
        LocalDateTime now = LocalDateTime.now();
        for (int i = 7; i >= 0; i--) {
            double rateValue = 0.80 + ((7 - i) * 0.01); // Day 7: 0.80, Day 0: 0.87 (increasing)
            ExchangeRate rate = ExchangeRate.builder()
                    .baseCurrency("USD")
                    .targetCurrency("EUR")
                    .rate(BigDecimal.valueOf(rateValue))
                    .timestamp(now.minusDays(i))
                    .provider("Test")
                    .build();
            exchangeRateRepository.save(rate);
        }

        // Calculate 7-day trend
        BigDecimal trend = trendAnalysisService.calculateTrend("USD", "EUR", "7D");

        // Trend should be positive (rate increased from 0.80 to 0.87)
        assertThat(trend).isGreaterThan(BigDecimal.ZERO);
    }
}
