package com.tech.challenge.infrastructure.pokemon.pokeapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the PokéAPI responses recorded in {@code test/resources/fixtures/pokeapi}. Tests never call the real API.
 */
public final class PokeApiFixtures {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private PokeApiFixtures() {
    }

    /** @param path relative to the fixtures folder, for example {@code pokemon/1.json} */
    public static String read(final String path) {
        try (InputStream in = PokeApiFixtures.class.getResourceAsStream("/fixtures/pokeapi/" + path)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing fixture: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T read(final String path, final Class<T> type) {
        return JSON_MAPPER.readValue(read(path), type);
    }
}
