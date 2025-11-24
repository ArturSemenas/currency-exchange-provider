package com.currencyexchange.provider.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for Exchangeratesapi.io API response
 * Example response:
 * {
 *   "success": true,
 *   "base": "EUR",
 *   "date": "2023-03-01",
 *   "rates": {
 *     "USD": 1.23396,
 *     "GBP": 0.89282
 *   }
 * }
 */
@Data
public class ExchangeratesApiResponse {
    
    private boolean success;
    
    private String base;
    
    private String date;
    
    private Map<String, BigDecimal> rates;
}
