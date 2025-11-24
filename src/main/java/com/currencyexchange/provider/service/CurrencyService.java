package com.currencyexchange.provider.service;

import com.currencyexchange.provider.model.Currency;
import com.currencyexchange.provider.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public List<Currency> getAllCurrencies() {
        log.info("Fetching all currencies");
        return currencyRepository.findAll();
    }

    @Transactional
    public Currency addCurrency(String code, String name) {
        log.info("Adding new currency: {} - {}", code, name);
        
        if (currencyRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Currency with code " + code + " already exists");
        }

        Currency currency = Currency.builder()
                .code(code)
                .name(name)
                .build();

        return currencyRepository.save(currency);
    }
}
