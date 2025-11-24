package com.currencyexchange.provider.mapper;

import com.currencyexchange.provider.dto.CurrencyDto;
import com.currencyexchange.provider.model.Currency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for Currency entity and DTO conversions.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CurrencyMapper {
    
    /**
     * Convert Currency entity to DTO.
     *
     * @param currency the entity to convert
     * @return the DTO representation
     */
    CurrencyDto toDto(Currency currency);
    
    /**
     * Convert CurrencyDto to entity.
     *
     * @param dto the DTO to convert
     * @return the entity representation
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Currency toEntity(CurrencyDto dto);
    
    /**
     * Convert list of Currency entities to list of DTOs.
     *
     * @param currencies the list of entities to convert
     * @return the list of DTOs
     */
    List<CurrencyDto> toDtoList(List<Currency> currencies);
    
    /**
     * Update existing Currency entity from DTO.
     * Only updates non-null values from DTO.
     *
     * @param dto the DTO with updated values
     * @param currency the entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(CurrencyDto dto, @MappingTarget Currency currency);
}
