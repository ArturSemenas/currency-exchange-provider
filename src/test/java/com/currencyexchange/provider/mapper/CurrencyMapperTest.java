package com.currencyexchange.provider.mapper;

import com.currencyexchange.provider.dto.CurrencyDto;
import com.currencyexchange.provider.model.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CurrencyMapper.
 * Tests MapStruct-generated mapping logic using the Mappers factory.
 */
@DisplayName("CurrencyMapper Unit Tests")
class CurrencyMapperTest {

    private CurrencyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(CurrencyMapper.class);
    }

    // ==================== toDto Tests ====================

    @Test
    @DisplayName("toDto should map Currency entity to CurrencyDto")
    void toDto_ShouldMapEntityToDto() {
        // Given
        Currency currency = Currency.builder()
                .id(1L)
                .code("USD")
                .name("US Dollar")
                .createdAt(LocalDateTime.now())
                .build();

        // When
        CurrencyDto dto = mapper.toDto(currency);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.code()).isEqualTo("USD");
        assertThat(dto.name()).isEqualTo("US Dollar");
    }

    @Test
    @DisplayName("toDto should return null when Currency is null")
    void toDto_ShouldReturnNull_WhenEntityIsNull() {
        // When
        CurrencyDto dto = mapper.toDto(null);

        // Then
        assertThat(dto).isNull();
    }

    @Test
    @DisplayName("toDto should map Currency with minimal data")
    void toDto_ShouldMapEntityWithMinimalData() {
        // Given
        Currency currency = Currency.builder()
                .code("EUR")
                .name("Euro")
                .build();

        // When
        CurrencyDto dto = mapper.toDto(currency);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.code()).isEqualTo("EUR");
        assertThat(dto.name()).isEqualTo("Euro");
    }

    @Test
    @DisplayName("toDto should handle special characters in currency name")
    void toDto_ShouldHandleSpecialCharactersInName() {
        // Given
        Currency currency = Currency.builder()
                .code("GBP")
                .name("British Pound £")
                .build();

        // When
        CurrencyDto dto = mapper.toDto(currency);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.name()).isEqualTo("British Pound £");
    }

    // ==================== toEntity Tests ====================

    @Test
    @DisplayName("toEntity should map CurrencyDto to Currency entity")
    void toEntity_ShouldMapDtoToEntity() {
        // Given
        CurrencyDto dto = new CurrencyDto("JPY", "Japanese Yen");

        // When
        Currency entity = mapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getCode()).isEqualTo("JPY");
        assertThat(entity.getName()).isEqualTo("Japanese Yen");
    }

    @Test
    @DisplayName("toEntity should return null when CurrencyDto is null")
    void toEntity_ShouldReturnNull_WhenDtoIsNull() {
        // When
        Currency entity = mapper.toEntity(null);

        // Then
        assertThat(entity).isNull();
    }

    @Test
    @DisplayName("toEntity should not set id and createdAt fields")
    void toEntity_ShouldIgnoreIdAndCreatedAtFields() {
        // Given
        CurrencyDto dto = new CurrencyDto("CAD", "Canadian Dollar");

        // When
        Currency entity = mapper.toEntity(dto);

        // Then
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    // ==================== toDtoList Tests ====================

    @Test
    @DisplayName("toDtoList should map list of Currency entities to CurrencyDto list")
    void toDtoList_ShouldMapEntityListToDtoList() {
        // Given
        List<Currency> currencies = Arrays.asList(
                Currency.builder().code("USD").name("US Dollar").build(),
                Currency.builder().code("EUR").name("Euro").build(),
                Currency.builder().code("GBP").name("British Pound").build()
        );

        // When
        List<CurrencyDto> dtos = mapper.toDtoList(currencies);

        // Then
        assertThat(dtos).hasSize(3);
        assertThat(dtos.get(0).code()).isEqualTo("USD");
        assertThat(dtos.get(1).code()).isEqualTo("EUR");
        assertThat(dtos.get(2).code()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("toDtoList should return empty list when input list is empty")
    void toDtoList_ShouldReturnEmptyList_WhenInputIsEmpty() {
        // When
        List<CurrencyDto> dtos = mapper.toDtoList(Collections.emptyList());

        // Then
        assertThat(dtos).isEmpty();
    }

    @Test
    @DisplayName("toDtoList should return null when input list is null")
    void toDtoList_ShouldReturnNull_WhenInputIsNull() {
        // When
        List<CurrencyDto> dtos = mapper.toDtoList(null);

        // Then
        assertThat(dtos).isNull();
    }

    @Test
    @DisplayName("toDtoList should map single element list")
    void toDtoList_ShouldMapListWithSingleElement() {
        // Given
        List<Currency> currencies = Collections.singletonList(
                Currency.builder().code("CHF").name("Swiss Franc").build()
        );

        // When
        List<CurrencyDto> dtos = mapper.toDtoList(currencies);

        // Then
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).code()).isEqualTo("CHF");
    }

    @Test
    @DisplayName("toDtoList should handle list with null elements")
    void toDtoList_ShouldHandleListWithNullElements() {
        // Given
        List<Currency> currencies = Arrays.asList(
                Currency.builder().code("USD").name("US Dollar").build(),
                null,
                Currency.builder().code("EUR").name("Euro").build()
        );

        // When
        List<CurrencyDto> dtos = mapper.toDtoList(currencies);

        // Then
        assertThat(dtos).hasSize(3);
        assertThat(dtos.get(0)).isNotNull();
        assertThat(dtos.get(1)).isNull();
        assertThat(dtos.get(2)).isNotNull();
    }

    // ==================== updateEntityFromDto Tests ====================

    @Test
    @DisplayName("updateEntityFromDto should update entity with DTO values")
    void updateEntityFromDto_ShouldUpdateEntityWithDtoValues() {
        // Given
        CurrencyDto dto = new CurrencyDto("EUR", "European Euro");
        Currency existingEntity = Currency.builder()
                .id(5L)
                .code("USD")
                .name("US Dollar")
                .createdAt(LocalDateTime.of(2024, 1, 1, 12, 0))
                .build();

        // When
        mapper.updateEntityFromDto(dto, existingEntity);

        // Then
        assertThat(existingEntity.getCode()).isEqualTo("EUR");
        assertThat(existingEntity.getName()).isEqualTo("European Euro");
    }

    @Test
    @DisplayName("updateEntityFromDto should preserve id and createdAt")
    void updateEntityFromDto_ShouldPreserveIdAndCreatedAt() {
        // Given
        CurrencyDto dto = new CurrencyDto("GBP", "British Pound Sterling");
        LocalDateTime originalCreatedAt = LocalDateTime.of(2023, 6, 15, 10, 30);
        Currency existingEntity = Currency.builder()
                .id(10L)
                .code("USD")
                .name("US Dollar")
                .createdAt(originalCreatedAt)
                .build();

        // When
        mapper.updateEntityFromDto(dto, existingEntity);

        // Then
        assertThat(existingEntity.getId()).isEqualTo(10L);
        assertThat(existingEntity.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    @DisplayName("updateEntityFromDto should handle same values gracefully")
    void updateEntityFromDto_ShouldUpdateOnlyCodeWhenNameIsSame() {
        // Given
        CurrencyDto dto = new CurrencyDto("JPY", "Japanese Yen");
        Currency existingEntity = Currency.builder()
                .id(3L)
                .code("USD")
                .name("Japanese Yen")
                .build();

        // When
        mapper.updateEntityFromDto(dto, existingEntity);

        // Then
        assertThat(existingEntity.getCode()).isEqualTo("JPY");
        assertThat(existingEntity.getName()).isEqualTo("Japanese Yen");
    }

    @Test
    @DisplayName("updateEntityFromDto should handle null DTO gracefully")
    void updateEntityFromDto_ShouldHandleNullDtoGracefully() {
        // Given
        Currency existingEntity = Currency.builder()
                .id(7L)
                .code("CAD")
                .name("Canadian Dollar")
                .build();

        // When
        mapper.updateEntityFromDto(null, existingEntity);

        // Then - entity should remain unchanged
        assertThat(existingEntity.getCode()).isEqualTo("CAD");
        assertThat(existingEntity.getName()).isEqualTo("Canadian Dollar");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("mapper should handle special characters in currency name")
    void mapper_ShouldHandleSpecialCharactersInCurrencyName() {
        // Given
        Currency currency = Currency.builder()
                .code("EUR")
                .name("Euro €")
                .build();

        // When
        CurrencyDto dto = mapper.toDto(currency);
        Currency backToEntity = mapper.toEntity(dto);

        // Then
        assertThat(dto.name()).isEqualTo("Euro €");
        assertThat(backToEntity.getName()).isEqualTo("Euro €");
    }

    @Test
    @DisplayName("mapper should handle long currency names")
    void mapper_ShouldHandleLongCurrencyNames() {
        // Given
        String longName = "A".repeat(100);
        Currency currency = Currency.builder()
                .code("XXX")
                .name(longName)
                .build();

        // When
        CurrencyDto dto = mapper.toDto(currency);

        // Then
        assertThat(dto.name()).hasSize(100);
        assertThat(dto.name()).isEqualTo(longName);
    }

    @Test
    @DisplayName("bidirectional mapping should maintain data integrity")
    void bidirectionalMapping_ShouldMaintainDataIntegrity() {
        // Given
        Currency original = Currency.builder()
                .code("CHF")
                .name("Swiss Franc")
                .build();

        // When
        CurrencyDto dto = mapper.toDto(original);
        Currency roundTrip = mapper.toEntity(dto);

        // Then
        assertThat(roundTrip.getCode()).isEqualTo(original.getCode());
        assertThat(roundTrip.getName()).isEqualTo(original.getName());
    }
}
