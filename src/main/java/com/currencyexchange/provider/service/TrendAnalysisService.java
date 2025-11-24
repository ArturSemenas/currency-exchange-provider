package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.ExchangeRate;
import com.currencyexchange.provider.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for analyzing exchange rate trends over time periods
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendAnalysisService {
    
    private static final Pattern PERIOD_PATTERN = Pattern.compile("(\\d+)([HDMY])");
    private static final int MINIMUM_HOURS = 12;
    
    private final ExchangeRateRepository exchangeRateRepository;
    
    /**
     * Calculate trend percentage for a currency pair over a specified period
     * 
     * @param baseCurrency the base currency code
     * @param targetCurrency the target currency code
     * @param period the period string (e.g., "12H", "10D", "3M", "1Y")
     * @return the trend percentage (positive = appreciation, negative = depreciation)
     * @throws IllegalArgumentException if period format is invalid
     * @throws IllegalStateException if insufficient data available
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTrend(String baseCurrency, String targetCurrency, String period) {
        log.debug("Calculating trend for {} -> {} over period {}", 
                baseCurrency, targetCurrency, period);
        
        // Parse period
        PeriodInfo periodInfo = parsePeriod(period);
        
        // Calculate start date
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = calculateStartDate(endDate, periodInfo);
        
        log.debug("Analyzing trend from {} to {}", startDate, endDate);
        
        // Fetch historical rates
        List<ExchangeRate> rates = exchangeRateRepository.findRatesByPeriod(
                baseCurrency, targetCurrency, startDate, endDate);
        
        if (rates.isEmpty()) {
            log.warn("No historical data available for {} -> {} in period {}", 
                    baseCurrency, targetCurrency, period);
            throw new IllegalStateException(
                    "Insufficient historical data for " + baseCurrency + " -> " + targetCurrency);
        }
        
        // Get oldest and latest rates using Stream API
        Optional<ExchangeRate> oldestRate = rates.stream()
                .min(Comparator.comparing(ExchangeRate::getTimestamp));
        
        Optional<ExchangeRate> latestRate = rates.stream()
                .max(Comparator.comparing(ExchangeRate::getTimestamp));
        
        if (oldestRate.isEmpty() || latestRate.isEmpty()) {
            throw new IllegalStateException("Unable to determine trend - insufficient data points");
        }
        
        // Calculate percentage change: ((latest - oldest) / oldest) * 100
        BigDecimal oldRate = oldestRate.get().getRate();
        BigDecimal newRate = latestRate.get().getRate();
        
        BigDecimal change = newRate.subtract(oldRate);
        BigDecimal percentageChange = change
                .divide(oldRate, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        
        log.info("Trend for {} -> {} over {}: {}% (from {} to {} using {} data points)", 
                baseCurrency, targetCurrency, period, percentageChange, 
                oldRate, newRate, rates.size());
        
        return percentageChange;
    }
    
    /**
     * Parse period string using regex
     * Format: \d+[HDMY] where H=hours, D=days, M=months, Y=years
     * 
     * @param period the period string
     * @return PeriodInfo containing amount and unit
     * @throws IllegalArgumentException if format is invalid
     */
    private PeriodInfo parsePeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            throw new IllegalArgumentException("Period cannot be null or empty");
        }
        
        Matcher matcher = PERIOD_PATTERN.matcher(period.trim().toUpperCase());
        
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid period format: " + period + ". Use format: 12H, 10D, 3M, or 1Y");
        }
        
        int amount = Integer.parseInt(matcher.group(1));
        char unit = matcher.group(2).charAt(0);
        
        // Validate minimum hours
        if (unit == 'H' && amount < MINIMUM_HOURS) {
            throw new IllegalArgumentException(
                    "Hours must be at least " + MINIMUM_HOURS + ". Got: " + amount);
        }
        
        return new PeriodInfo(amount, unit);
    }
    
    /**
     * Calculate start date based on period
     */
    private LocalDateTime calculateStartDate(LocalDateTime endDate, PeriodInfo periodInfo) {
        return switch (periodInfo.unit()) {
            case 'H' -> endDate.minus(periodInfo.amount(), ChronoUnit.HOURS);
            case 'D' -> endDate.minus(periodInfo.amount(), ChronoUnit.DAYS);
            case 'M' -> endDate.minus(periodInfo.amount(), ChronoUnit.MONTHS);
            case 'Y' -> endDate.minus(periodInfo.amount(), ChronoUnit.YEARS);
            default -> throw new IllegalArgumentException("Unsupported period unit: " + periodInfo.unit());
        };
    }
    
    /**
     * Record for storing parsed period information
     */
    private record PeriodInfo(int amount, char unit) {}
    
    /**
     * Validate period format without calculating trend
     * 
     * @param period the period string to validate
     * @return true if period format is valid
     */
    public boolean isValidPeriod(String period) {
        try {
            parsePeriod(period);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
