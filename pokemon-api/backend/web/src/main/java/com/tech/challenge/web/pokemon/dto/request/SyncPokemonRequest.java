package com.tech.challenge.web.pokemon.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SyncPokemonRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 50, message = "size must be at most 50") String pokemon,
        @Size(max = 100, message = "size must be at most 100") String localizedName,
        @Size(max = 100, message = "size must be at most 100") String region,
        @Size(max = 20, message = "size must be at most 20")
        @UniqueIgnoringCase(message = "must not contain duplicates")
        List<@NotBlank(message = "must not be blank") @Size(max = 30, message = "size must be at most 30") String> internalTags) {

    public SyncPokemonRequest {
        pokemon = trim(pokemon);
        localizedName = trim(localizedName);
        region = trim(region);
        internalTags = internalTags == null ? null : internalTags.stream().map(SyncPokemonRequest::trim).toList();
    }

    private static String trim(final String text) {
        return text == null ? null : text.trim();
    }
}
