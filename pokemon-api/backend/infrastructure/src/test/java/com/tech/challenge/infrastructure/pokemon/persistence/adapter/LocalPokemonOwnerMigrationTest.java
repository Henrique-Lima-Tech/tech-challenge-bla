package com.tech.challenge.infrastructure.pokemon.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class LocalPokemonOwnerMigrationTest {

    private final DriverManagerDataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:owner-migration-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    private final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

    @Test
    void shouldGiveExistingPokemonToTheOldestUserWhenTheOwnerColumnIsAdded() {
        // given
        migrateTo("2");
        jdbcTemplate.update("INSERT INTO users (id, name, email, password_hash) VALUES "
                + "(9, 'Misty', 'misty@example.com', 'hash'), (5, 'Ash', 'ash@example.com', 'hash')");
        insertPokemon(1, 25);
        insertPokemon(2, 1);

        // when
        migrateTo("3");

        // then
        assertThat(jdbcTemplate.queryForList("SELECT user_id FROM local_pokemon ORDER BY id", Long.class))
                .containsExactly(5L, 5L);
        assertThat(count("local_pokemon_abilities")).isEqualTo(2);
        assertThat(count("local_pokemon_internal_tags")).isEqualTo(2);
    }

    @Test
    void shouldDeleteExistingPokemonWithTheirListsWhenThereIsNoUser() {
        // given
        migrateTo("2");
        insertPokemon(1, 25);

        // when
        migrateTo("3");

        // then
        assertThat(count("local_pokemon")).isZero();
        assertThat(count("local_pokemon_abilities")).isZero();
        assertThat(count("local_pokemon_internal_tags")).isZero();
    }

    private void migrateTo(final String version) {
        Flyway.configure().dataSource(dataSource).target(version).load().migrate();
    }

    private void insertPokemon(final long id, final int pokeApiId) {
        jdbcTemplate.update("INSERT INTO local_pokemon (id, poke_api_id, name, weight_kg) VALUES (?, ?, 'x', 1.0)", id,
                pokeApiId);
        jdbcTemplate.update(
                "INSERT INTO local_pokemon_abilities (local_pokemon_id, position, ability) VALUES (?, 0, 'a')", id);
        jdbcTemplate.update(
                "INSERT INTO local_pokemon_internal_tags (local_pokemon_id, position, tag) VALUES (?, 0, 't')", id);
    }

    private int count(final String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
