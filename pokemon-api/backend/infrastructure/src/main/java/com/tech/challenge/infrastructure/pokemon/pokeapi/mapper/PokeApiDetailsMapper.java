package com.tech.challenge.infrastructure.pokemon.pokeapi.mapper;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.tech.challenge.domain.pokemon.model.EvolutionStage;
import com.tech.challenge.domain.pokemon.model.PokemonDetails;
import com.tech.challenge.domain.pokemon.model.PokemonStat;
import com.tech.challenge.domain.pokemon.model.PokemonSummary;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiEvolutionChain;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiPokemon;
import com.tech.challenge.infrastructure.pokemon.pokeapi.model.PokeApiSpecies;

@Mapper(componentModel = "spring")
public interface PokeApiDetailsMapper {

    String DESCRIPTION_LANGUAGE = "en";
    String CATEGORY_LANGUAGE = "en";
    /** D-29: the pattern {@code sprites.front_default} uses for a species' default form. */
    String SPRITE_URL = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/%s.png";
    Pattern SPECIES_ID = Pattern.compile(".*/(\\d+)/?");

    @Mapping(target = "id", source = "pokemon.id")
    @Mapping(target = "name", source = "pokemon.name")
    @Mapping(target = "imageUrl", source = "pokemon.sprites.frontDefault")
    @Mapping(target = "stats", source = "pokemon.stats")
    @Mapping(target = "description", source = "species", qualifiedByName = "description")
    @Mapping(target = "evolutionChain", source = "chain.chain")
    PokemonDetails toDetails(PokeApiPokemon pokemon, PokeApiSpecies species, PokeApiEvolutionChain chain);

    @Mapping(target = "id", source = "pokemon.id")
    @Mapping(target = "name", source = "pokemon.name")
    @Mapping(target = "spriteUrl", source = "pokemon.sprites.frontDefault")
    @Mapping(target = "category", source = "species", qualifiedByName = "category")
    @Mapping(target = "weightKg", source = "pokemon.weight", qualifiedByName = "weightKg")
    @Mapping(target = "abilities", source = "pokemon.abilities")
    PokemonSummary toSummary(PokeApiPokemon pokemon, PokeApiSpecies species);

    @Mapping(target = "name", source = "stat.name")
    PokemonStat toStat(PokeApiPokemon.StatSlot statSlot);

    @Mapping(target = "name", source = "species.name")
    @Mapping(target = "spriteUrl", source = "species.url", qualifiedByName = "spriteUrl")
    EvolutionStage toStage(PokeApiEvolutionChain.ChainLink link);

    @Named("spriteUrl")
    default String toSpriteUrl(final String speciesUrl) {
        if (speciesUrl == null) {
            return null;
        }
        final var matcher = SPECIES_ID.matcher(speciesUrl.trim());
        return matcher.matches() ? SPRITE_URL.formatted(matcher.group(1)) : null;
    }

    /**
     * First English flavor text, cleaned of the game-file characters: a soft hyphen before a line break
     * joins the split word, and every run of whitespace (line breaks, form feeds) becomes one space.
     */
    @Named("description")
    default String toDescription(final PokeApiSpecies species) {
        if (species.flavorTextEntries() == null) {
            return null;
        }
        return species.flavorTextEntries().stream()
                .filter(entry -> entry.language() != null && DESCRIPTION_LANGUAGE.equals(entry.language().name()))
                .map(entry -> entry.flavorText().replaceAll("­\\s*", "").replaceAll("\\s+", " ").trim())
                .findFirst()
                .orElse(null);
    }

    default String toAbility(final PokeApiPokemon.AbilitySlot slot) {
        return slot.ability().name();
    }

    /** First English genus, for example "Seed Pokémon". */
    @Named("category")
    default String toCategory(final PokeApiSpecies species) {
        if (species.genera() == null) {
            return null;
        }
        return species.genera().stream()
                .filter(genus -> genus.language() != null && CATEGORY_LANGUAGE.equals(genus.language().name()))
                .map(PokeApiSpecies.Genus::genus)
                .findFirst()
                .orElse(null);
    }

    /** The PokéAPI weight is in hectograms. */
    @Named("weightKg")
    default BigDecimal toWeightKg(final int hectograms) {
        return BigDecimal.valueOf(hectograms, 1);
    }
}
