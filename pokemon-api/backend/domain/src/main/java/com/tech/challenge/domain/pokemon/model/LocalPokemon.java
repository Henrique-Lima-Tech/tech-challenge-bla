package com.tech.challenge.domain.pokemon.model;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A Pokémon replicated into the local database (REQ-US03): the PokéAPI data plus the proprietary fields
 * {@code localizedName}, {@code region} and {@code internalTags}. {@code id} is {@code null} until it is saved.
 * Limits from D-27.
 */
public record LocalPokemon(
        Long id,
        int pokeApiId,
        String name,
        String spriteUrl,
        String category,
        BigDecimal weightKg,
        List<String> abilities,
        String localizedName,
        String region,
        List<String> internalTags) {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("9999.9");

    public LocalPokemon {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("Local Pokemon id must be positive");
        }
        if (pokeApiId <= 0) {
            throw new IllegalArgumentException("PokeAPI id must be positive");
        }
        requireText(name, 50, "Pokemon name");
        if (spriteUrl != null) {
            requireHttpUrl(spriteUrl);
        }
        requireMaxLength(category, 50, "Pokemon category");
        requireWeight(weightKg);
        abilities = copyOfTexts(abilities, 50, "Ability");
        if (abilities.isEmpty() || abilities.size() > 10) {
            throw new IllegalArgumentException("Pokemon must have 1 to 10 abilities");
        }
        if (new HashSet<>(abilities).size() != abilities.size()) {
            throw new IllegalArgumentException("Abilities must not contain duplicates");
        }
        requireMaxLength(localizedName, 100, "Localized name");
        requireMaxLength(region, 100, "Region");
        internalTags = copyOfTexts(internalTags, 30, "Internal tag");
        if (internalTags.size() > 20) {
            throw new IllegalArgumentException("Pokemon must have at most 20 internal tags");
        }
        final Set<String> lowercaseTags = new HashSet<>();
        internalTags.forEach(tag -> lowercaseTags.add(tag.toLowerCase(Locale.ROOT)));
        if (lowercaseTags.size() != internalTags.size()) {
            throw new IllegalArgumentException("Internal tags must not contain duplicates (case-insensitive)");
        }
    }

    /** {@code List.copyOf} would throw a {@code NullPointerException} on a null element: check them first. */
    private static List<String> copyOfTexts(final List<String> values, final int maxLength, final String field) {
        if (values == null) {
            return List.of();
        }
        values.forEach(value -> requireText(value, maxLength, field));
        return List.copyOf(values);
    }

    private static void requireText(final String value, final int maxLength, final String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        requireMaxLength(value, maxLength, field);
    }

    private static void requireMaxLength(final String value, final int maxLength, final String field) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(field + " must have at most " + maxLength + " characters");
        }
    }

    private static void requireHttpUrl(final String spriteUrl) {
        requireMaxLength(spriteUrl, 500, "Sprite URL");
        try {
            final var uri = new URI(spriteUrl);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Sprite URL must be an http or https URL");
            }
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Sprite URL must be an http or https URL", e);
        }
    }

    private static void requireWeight(final BigDecimal weightKg) {
        if (weightKg == null || weightKg.signum() < 0 || weightKg.compareTo(MAX_WEIGHT_KG) > 0) {
            throw new IllegalArgumentException("Pokemon weight must be between 0 and 9999.9 kg");
        }
        if (weightKg.stripTrailingZeros().scale() > 1) {
            throw new IllegalArgumentException("Pokemon weight must have at most 1 decimal place");
        }
    }
}
