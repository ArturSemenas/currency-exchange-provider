package com.currencyexchange.provider.mapper;

import com.currencyexchange.provider.dto.ExchangeRateDto;
import com.currencyexchange.provider.model.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper for ExchangeRate entity and DTO conversions.
 */
@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {
    
    /**
     * Convert ExchangeRate entity to DTO.
     *
     * @param exchangeRate the entity to convert
     * @return the DTO representation
     */
    @Mapping(source = "timestamp", target = "lastUpdated")
    ExchangeRateDto toDto(ExchangeRate exchangeRate);
    
    /**
     * Convert list of ExchangeRate entities to list of DTOs.
     *
     * @param exchangeRates the list of entities to convert
     * @return the list of DTOs
     */
    List<ExchangeRateDto> toDtoList(List<ExchangeRate> exchangeRates);
}
