package com.currencyexchange.provider.integration;

import com.currencyexchange.provider.client.impl.FixerIoProvider;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for external API providers using WireMock
 * Tests FixerIoProvider with mocked HTTP responses
 */
class ExternalProviderWireMockTest {

    private WireMockServer wireMockServer;
    private FixerIoProvider fixerIoProvider;

    @BeforeEach
    void setUp() {
        // Start WireMock server on dynamic port
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        // Create provider with WireMock URL
        String baseUrl = "http://localhost:" + wireMockServer.port();
        fixerIoProvider = new FixerIoProvider(
                new RestTemplate(),
                baseUrl,
                "test-api-key"
        );
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Successfully fetch rates from Fixer.io API")
    void testSuccessfulRateFetch() {
        // Arrange - Mock successful API response
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .withQueryParam("access_key", equalTo("test-api-key"))
                .withQueryParam("base", equalTo("USD"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "timestamp": 1519296206,
                                    "base": "USD",
                                    "date": "2024-11-24",
                                    "rates": {
                                        "EUR": 0.85,
                                        "GBP": 0.75,
                                        "JPY": 110.50
                                    }
                                }
                                """)));

        // Act
        Map<String, BigDecimal> rates = fixerIoProvider.fetchLatestRates("USD");

        // Assert
        assertThat(rates).isNotEmpty();
        assertThat(rates).hasSize(3);
        assertThat(rates.get("EUR")).isEqualByComparingTo("0.85");
        assertThat(rates.get("GBP")).isEqualByComparingTo("0.75");
        assertThat(rates.get("JPY")).isEqualByComparingTo("110.50");

        // Verify the request was made
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/latest"))
                .withQueryParam("access_key", equalTo("test-api-key"))
                .withQueryParam("base", equalTo("USD")));
    }

    @Test
    @DisplayName("Handle API error response (401 Unauthorized)")
    void testApiErrorResponse() {
        // Arrange - Mock 401 error
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": false,
                                    "error": {
                                        "code": 101,
                                        "type": "invalid_access_key",
                                        "info": "You have not supplied a valid API Access Key."
                                    }
                                }
                                """)));

        // Act
        Map<String, BigDecimal> rates = fixerIoProvider.fetchLatestRates("USD");

        // Assert - Should return empty map on error
        assertThat(rates).isEmpty();
    }

    @Test
    @DisplayName("Handle server error (500 Internal Server Error)")
    void testServerError() {
        // Arrange - Mock 500 error
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act
        Map<String, BigDecimal> rates = fixerIoProvider.fetchLatestRates("USD");

        // Assert - Should handle error gracefully
        assertThat(rates).isEmpty();
    }

    @Test
    @DisplayName("Handle invalid JSON response")
    void testInvalidJsonResponse() {
        // Arrange - Mock invalid JSON
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        // Act
        Map<String, BigDecimal> rates = fixerIoProvider.fetchLatestRates("USD");

        // Assert - Should handle parse error gracefully
        assertThat(rates).isEmpty();
    }

    @Test
    @DisplayName("Handle network timeout")
    void testNetworkTimeout() {
        // Arrange - Mock delayed response (35 seconds)
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(35000) // 35 seconds
                        .withBody("{}")));

        // Act
        Map<String, BigDecimal> rates = fixerIoProvider.fetchLatestRates("USD");

        // Assert - Should handle timeout gracefully
        assertThat(rates).isEmpty();
    }

    @Test
    @DisplayName("Provider name is correct")
    void testProviderName() {
        String providerName = fixerIoProvider.getProviderName();
        assertThat(providerName).isEqualTo("fixer.io");
    }

    @Test
    @DisplayName("Provider is available")
    void testProviderAvailability() {
        // Arrange - Mock successful response for availability check
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "EUR",
                                    "rates": {
                                        "USD": 1.18
                                    }
                                }
                                """)));

        // Act
        boolean isAvailable = fixerIoProvider.isAvailable();

        // Assert
        assertThat(isAvailable).isTrue();
    }

    @Test
    @DisplayName("Handle empty rates response")
    void testEmptyRatesResponse() {
        // Arrange - Mock response with empty rates
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "USD",
                                    "rates": {}
                                }
                                """)));

        // Act
        Map<String, BigDecimal> rates = fixerIoProvider.fetchLatestRates("USD");

        // Assert
        assertThat(rates).isEmpty();
    }

    @Test
    @DisplayName("Handle response with multiple currencies")
    void testMultipleCurrenciesResponse() {
        // Arrange - Mock response with many currencies
        wireMockServer.stubFor(get(urlPathEqualTo("/latest"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "success": true,
                                    "base": "USD",
                                    "rates": {
                                        "EUR": 0.85,
                                        "GBP": 0.75,
                                        "JPY": 110.50,
                                        "AUD": 1.35,
                                        "CAD": 1.25,
                                        "CHF": 0.92,
                                        "CNY": 6.45,
                                        "SEK": 8.75
                                    }
                                }
                                """)));

        // Act
        Map<String, BigDecimal> rates = fixerIoProvider.fetchLatestRates("USD");

        // Assert
        assertThat(rates).hasSize(8);
        assertThat(rates).containsKeys("EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK");
    }
}
