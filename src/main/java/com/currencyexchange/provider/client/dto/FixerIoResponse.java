package com.currencyexchange.provider.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for Fixer.io API response
 * Example response:
 * {
 *   "success": true,
 *   "timestamp": 1519296206,
 *   "base": "EUR",
 *   "date": "2023-03-01",
 *   "rates": {
 *     "USD": 1.23396,
 *     "GBP": 0.89282
 *   }
 * }
 */
@Data
public class FixerIoResponse {
    
    private boolean success;
    
    private Long timestamp;
    
    private String base;
    
    private String date;
    
    private Map<String, BigDecimal> rates;
    
    @JsonProperty("error")
    private ErrorInfo error;
    
    @Data
    public static class ErrorInfo {
        private Integer code;
        private String type;
        private String info;
    }
}
