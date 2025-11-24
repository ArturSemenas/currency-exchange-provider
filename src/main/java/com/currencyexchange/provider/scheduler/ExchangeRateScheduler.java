package com.currencyexchange.provider.scheduler;

import com.currencyexchange.provider.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Scheduler for automatic exchange rate updates
 * Runs hourly to fetch latest rates from all providers
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ExchangeRateService exchangeRateService;
    
    /**
     * Scheduled task to refresh exchange rates hourly
     * Cron expression: 0 0 * * * * = At the start of every hour (00:00, 01:00, 02:00, etc.)
     */
    @Scheduled(cron = "${exchange.rates.update.cron}")
    public void refreshExchangeRates() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("========================================");
        log.info("Starting scheduled exchange rate refresh at {}", startTime.format(FORMATTER));
        log.info("========================================");
        
        try {
            int updatedCount = exchangeRateService.refreshRates();
            
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            log.info("========================================");
            log.info("Successfully refreshed {} exchange rates", updatedCount);
            log.info("Refresh completed at {}", endTime.format(FORMATTER));
            log.info("Total duration: {} seconds", durationSeconds);
            log.info("========================================");
            
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            log.error("========================================");
            log.error("Failed to refresh exchange rates at {}", endTime.format(FORMATTER));
            log.error("Error occurred after {} seconds: {}", durationSeconds, e.getMessage(), e);
            log.error("========================================");
        }
    }
    
    /**
     * Manual trigger for exchange rate refresh (can be called via admin endpoint)
     */
    public void triggerManualRefresh() {
        log.info("Manual exchange rate refresh triggered");
        refreshExchangeRates();
    }
}
