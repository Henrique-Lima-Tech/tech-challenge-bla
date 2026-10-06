package com.tech.challenge.web.pokemon.dto.request;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.validator.constraints.UniqueElements;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public record UpdateLocalPokemonRequest(
        @Null(message = "must not be sent") Long id,
        @Null(message = "must not be sent") Integer pokeApiId,
        @NotBlank(message = "must not be blank")
        @Size(max = 50, message = "size must be at most 50") String name,
        @HttpUrl(message = "must be a valid http or https URL")
        @Size(max = 500, message = "size must be at most 500") String spriteUrl,
        @Size(max = 50, message = "size must be at most 50") String category,
        @NotNull(message = "must not be null")
        @DecimalMin(value = "0", message = "must be greater than or equal to 0")
        @DecimalMax(value = "9999.9", message = "must be less than or equal to 9999.9")
        @Digits(integer = 4, fraction = 1, message = "must have at most 1 decimal place") BigDecimal weightKg,
        @NotNull(message = "must not be null")
        @Size(min = 1, max = 10, message = "size must be between 1 and 10")
        @UniqueElements(message = "must not contain duplicates")
        List<@NotBlank(message = "must not be blank") @Size(max = 50, message = "size must be at most 50") String> abilities,
        @Size(max = 100, message = "size must be at most 100") String localizedName,
        @Size(max = 100, message = "size must be at most 100") String region,
        @Size(max = 20, message = "size must be at most 20")
        @UniqueIgnoringCase(message = "must not contain duplicates")
        List<@NotBlank(message = "must not be blank") @Size(max = 30, message = "size must be at most 30") String> internalTags) {

    public UpdateLocalPokemonRequest {
        name = trim(name);
        spriteUrl = trim(spriteUrl);
        category = trim(category);
        abilities = trimAll(abilities);
        localizedName = trim(localizedName);
        region = trim(region);
        internalTags = trimAll(internalTags);
    }

    private static List<String> trimAll(final List<String> texts) {
        return texts == null ? null : texts.stream().map(UpdateLocalPokemonRequest::trim).toList();
    }

    private static String trim(final String text) {
        return text == null ? null : text.trim();
    }
}
