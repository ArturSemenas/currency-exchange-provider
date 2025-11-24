package com.currencyexchange.provider.repository;

import com.currencyexchange.provider.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    
    /**
     * Find all exchange rates for a given base currency
     */
    List<ExchangeRate> findByBaseCurrency(String baseCurrency);
    
    /**
     * Find the latest exchange rate between two currencies
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :baseCurrency " +
           "AND e.targetCurrency = :targetCurrency ORDER BY e.timestamp DESC LIMIT 1")
    Optional<ExchangeRate> findLatestRate(@Param("baseCurrency") String baseCurrency, 
                                          @Param("targetCurrency") String targetCurrency);
    
    /**
     * Find all exchange rates within a time period for trend analysis
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :baseCurrency " +
           "AND e.targetCurrency = :targetCurrency " +
           "AND e.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY e.timestamp ASC")
    List<ExchangeRate> findRatesByPeriod(@Param("baseCurrency") String baseCurrency,
                                         @Param("targetCurrency") String targetCurrency,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find all latest rates (one per base-target currency pair)
     */
    @Query("SELECT e FROM ExchangeRate e WHERE e.timestamp = " +
           "(SELECT MAX(e2.timestamp) FROM ExchangeRate e2 " +
           "WHERE e2.baseCurrency = e.baseCurrency AND e2.targetCurrency = e.targetCurrency)")
    List<ExchangeRate> findAllLatestRates();
}
