package com.tech.challenge.infrastructure.pokemon.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tech.challenge.application.shared.pagination.PageResult;
import com.tech.challenge.domain.pokemon.exception.PokemonAlreadySyncedException;
import com.tech.challenge.domain.pokemon.model.LocalPokemon;
import com.tech.challenge.infrastructure.pokemon.persistence.mapper.LocalPokemonPersistenceMapperImpl;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import({ LocalPokemonPersistenceAdapter.class, LocalPokemonPersistenceMapperImpl.class })
class LocalPokemonPersistenceAdapterTest {

    private static final BigDecimal WEIGHT = new BigDecimal("6.0");
    private static final long ASH = 1001L;
    private static final long MISTY = 1002L;

    private final LocalPokemonPersistenceAdapter adapter;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    LocalPokemonPersistenceAdapterTest(final LocalPokemonPersistenceAdapter adapter, final EntityManager entityManager,
            final JdbcTemplate jdbcTemplate) {
        this.adapter = adapter;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void createUsers() {
        for (final long id : List.of(ASH, MISTY)) {
            jdbcTemplate.update("MERGE INTO users (id, name, email, password_hash) KEY (id) VALUES (?, ?, ?, ?)", id,
                    "user" + id, "user" + id + "@example.com", "hash");
        }
    }

    private static LocalPokemon pokemon(final int pokeApiId, final String name) {
        return new LocalPokemon(null, pokeApiId, name, null, null, WEIGHT, List.of("ability"), null, null, null);
    }

    private static LocalPokemon pikachu(final List<String> internalTags) {
        return new LocalPokemon(null, 25, "pikachu", "https://img/25.png", "Mouse Pokémon", WEIGHT,
                List.of("static", "lightning-rod"), "ピカチュウ", "Kanto", internalTags);
    }

    @Test
    void shouldAssignIdAndKeepEveryFieldWhenPokemonIsSaved() {
        // when
        final var saved = adapter.save(ASH, pikachu(List.of("starter", "electric")));

        // then
        assertThat(saved.id()).isPositive();
        assertThat(saved).isEqualTo(new LocalPokemon(saved.id(), 25, "pikachu", "https://img/25.png", "Mouse Pokémon",
                WEIGHT, List.of("static", "lightning-rod"), "ピカチュウ", "Kanto", List.of("starter", "electric")));
    }

    @Test
    void shouldStoreEveryColumnAndListOrderWhenPokemonIsSaved() {
        // given
        final var saved = adapter.save(ASH, pikachu(List.of("zeta", "alpha", "mid")));
        entityManager.clear();

        // when
        final var row = jdbcTemplate.queryForMap("SELECT * FROM local_pokemon WHERE id = ?", saved.id());
        final var abilities = jdbcTemplate.queryForList(
                "SELECT ability FROM local_pokemon_abilities WHERE local_pokemon_id = ? ORDER BY position", String.class,
                saved.id());
        final var tags = jdbcTemplate.queryForList(
                "SELECT tag FROM local_pokemon_internal_tags WHERE local_pokemon_id = ? ORDER BY position", String.class,
                saved.id());

        // then
        assertThat(row).containsEntry("POKE_API_ID", 25)
                .containsEntry("NAME", "pikachu")
                .containsEntry("SPRITE_URL", "https://img/25.png")
                .containsEntry("CATEGORY", "Mouse Pokémon")
                .containsEntry("WEIGHT_KG", WEIGHT)
                .containsEntry("LOCALIZED_NAME", "ピカチュウ")
                .containsEntry("REGION", "Kanto");
        assertThat(abilities).containsExactly("static", "lightning-rod");
        assertThat(tags).containsExactly("zeta", "alpha", "mid");
    }

    @Test
    void shouldSaveNullOptionalFieldsAndNoTagsWhenTheyAreAbsent() {
        // when
        final var saved = adapter.save(ASH, new LocalPokemon(null, 25, "pikachu", null, null, WEIGHT, List.of("static"),
                null, null, null));

        // then
        assertThat(saved.spriteUrl()).isNull();
        assertThat(saved.category()).isNull();
        assertThat(saved.localizedName()).isNull();
        assertThat(saved.region()).isNull();
        assertThat(saved.internalTags()).isEmpty();
    }

    @Test
    void shouldThrowAlreadySyncedWhenTheUserAlreadyHasThatPokeApiId() {
        // given
        adapter.save(ASH, pikachu(List.of()));

        // when & then
        assertThatThrownBy(() -> adapter.save(ASH, pikachu(List.of("other"))))
                .isInstanceOf(PokemonAlreadySyncedException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldReturnEveryFieldAndListOrderWhenRecordIsFoundById() {
        // given
        final var saved = adapter.save(ASH, new LocalPokemon(null, 26, "raichu", "https://img/26.png", "Mouse Pokémon",
                WEIGHT, List.of("static", "lightning-rod"), "ライチュウ", "Johto", List.of("zeta", "alpha")));

        // when
        final Optional<LocalPokemon> found;
        try {
            found = adapter.findById(ASH, saved.id());
        } finally {
            deleteCommitted(saved.id());
        }

        // then
        assertThat(found).contains(new LocalPokemon(saved.id(), 26, "raichu", "https://img/26.png", "Mouse Pokémon",
                WEIGHT, List.of("static", "lightning-rod"), "ライチュウ", "Johto", List.of("zeta", "alpha")));
    }

    @Test
    void shouldReturnEmptyWhenNoRecordHasThatId() {
        // when & then
        assertThat(adapter.findById(ASH, 9999L)).isEmpty();
    }

    @Test
    void shouldReplaceEveryColumnAndBothListsWhenStoredPokemonIsSavedAgain() {
        // given
        final var saved = adapter.save(ASH, pikachu(List.of("starter", "electric")));
        entityManager.clear();

        // when
        final var updated = adapter.save(ASH, new LocalPokemon(saved.id(), 25, "raichu", "https://img/26.png",
                "Thunder", new BigDecimal("30.0"), List.of("static"), "ライチュウ", "Johto",
                List.of("mascot", "beta", "alpha")));
        entityManager.clear();

        // then
        assertThat(updated.id()).isEqualTo(saved.id());
        final var row = jdbcTemplate.queryForMap("SELECT * FROM local_pokemon WHERE id = ?", saved.id());
        assertThat(row).containsEntry("POKE_API_ID", 25)
                .containsEntry("NAME", "raichu")
                .containsEntry("SPRITE_URL", "https://img/26.png")
                .containsEntry("CATEGORY", "Thunder")
                .containsEntry("WEIGHT_KG", new BigDecimal("30.0"))
                .containsEntry("LOCALIZED_NAME", "ライチュウ")
                .containsEntry("REGION", "Johto");
        assertThat(abilitiesOf(saved.id())).containsExactly("static");
        assertThat(tagsOf(saved.id())).containsExactly("mascot", "beta", "alpha");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM local_pokemon WHERE poke_api_id = 25",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldClearOptionalFieldsAndTagsWhenTheyAreAbsentFromTheUpdate() {
        // given
        final var saved = adapter.save(ASH, pikachu(List.of("starter", "electric")));
        entityManager.clear();

        // when
        final var updated = adapter.save(ASH, new LocalPokemon(saved.id(), 25, "pikachu", null, null, WEIGHT,
                List.of("static", "lightning-rod", "run-away"), null, null, null));
        entityManager.clear();

        // then
        assertThat(updated.spriteUrl()).isNull();
        assertThat(updated.category()).isNull();
        assertThat(updated.localizedName()).isNull();
        assertThat(updated.region()).isNull();
        assertThat(updated.internalTags()).isEmpty();
        assertThat(abilitiesOf(saved.id())).containsExactly("static", "lightning-rod", "run-away");
        assertThat(tagsOf(saved.id())).isEmpty();
    }

    @Test
    void shouldReturnPageSortedByIdWithTotalsWhenPokemonAreStored() {
        // given
        final var first = adapter.save(ASH, pokemon(25, "pikachu"));
        final var second = adapter.save(ASH, pokemon(1, "bulbasaur"));
        final var third = adapter.save(ASH, pokemon(133, "eevee"));

        // when
        final var firstPage = adapter.findPage(ASH, 0, 2);
        final var secondPage = adapter.findPage(ASH, 1, 2);

        // then
        assertThat(firstPage.content()).extracting(LocalPokemon::id).containsExactly(first.id(), second.id());
        assertThat(secondPage.content()).extracting(LocalPokemon::id).containsExactly(third.id());
        assertThat(firstPage.page()).isZero();
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyContentWithRealTotalsWhenPageIsPastTheEnd() {
        // given
        adapter.save(ASH, pokemon(25, "pikachu"));

        // when
        final var page = adapter.findPage(ASH, 50, 20);

        // then
        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(50);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldLoadAbilitiesAndTagsWhenPageIsRead() {
        // given
        final var saved = adapter.save(ASH, new LocalPokemon(null, 27, "sandshrew", null, null, WEIGHT,
                List.of("sand-veil", "sand-rush"), null, null, List.of("ground", "kanto")));

        // when
        final PageResult<LocalPokemon> page;
        try {
            page = adapter.findPage(ASH, 0, 100);
        } finally {
            deleteCommitted(saved.id());
        }

        // then
        assertThat(page.content()).filteredOn(pokemon -> saved.id().equals(pokemon.id()))
                .singleElement()
                .satisfies(pokemon -> {
                    assertThat(pokemon.abilities()).containsExactly("sand-veil", "sand-rush");
                    assertThat(pokemon.internalTags()).containsExactly("ground", "kanto");
                });
    }

    @Test
    void shouldRemoveRowAbilitiesAndTagsWhenStoredPokemonIsDeleted() {
        // given
        final var saved = adapter.save(ASH, pikachu(List.of("starter", "electric")));

        // when
        final var deleted = adapter.deleteById(ASH, saved.id());
        entityManager.flush();

        // then
        assertThat(deleted).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM local_pokemon WHERE id = ?", Integer.class,
                saved.id())).isZero();
        assertThat(abilitiesOf(saved.id())).isEmpty();
        assertThat(tagsOf(saved.id())).isEmpty();
    }

    @Test
    void shouldReturnFalseWhenDeletingAnUnknownId() {
        // when
        final var deleted = adapter.deleteById(ASH, 9999L);

        // then
        assertThat(deleted).isFalse();
    }

    @Test
    void shouldAcceptTheSamePokeApiIdAgainWhenItWasDeleted() {
        // given
        final var saved = adapter.save(ASH, pikachu(List.of("starter")));
        adapter.deleteById(ASH, saved.id());
        entityManager.flush();

        // when
        final var savedAgain = adapter.save(ASH, pikachu(List.of("starter")));

        // then
        assertThat(savedAgain.id()).isNotEqualTo(saved.id());
        assertThat(savedAgain.pokeApiId()).isEqualTo(25);
    }

    @Test
    void shouldStoreTheOwnerWhenPokemonIsSaved() {
        // when
        final var saved = adapter.save(ASH, pikachu(List.of()));

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM local_pokemon WHERE id = ?", Long.class,
                saved.id())).isEqualTo(ASH);
    }

    @Test
    void shouldAcceptTheSamePokeApiIdWhenAnotherUserSavesIt() {
        // given
        final var ashCopy = adapter.save(ASH, pikachu(List.of()));

        // when
        final var mistyCopy = adapter.save(MISTY, pikachu(List.of()));

        // then
        assertThat(mistyCopy.id()).isNotEqualTo(ashCopy.id());
        assertThat(mistyCopy.pokeApiId()).isEqualTo(25);
    }

    @Test
    void shouldReturnEmptyWhenTheIdBelongsToAnotherUser() {
        // given
        final var saved = adapter.save(ASH, pikachu(List.of()));

        // when & then
        assertThat(adapter.findById(MISTY, saved.id())).isEmpty();
    }

    @Test
    void shouldListOnlyTheUsersPokemonWhenPageIsRead() {
        // given
        final var first = adapter.save(ASH, pokemon(25, "pikachu"));
        adapter.save(MISTY, pokemon(120, "staryu"));
        final var second = adapter.save(ASH, pokemon(1, "bulbasaur"));

        // when
        final var page = adapter.findPage(ASH, 0, 20);

        // then
        assertThat(page.content()).extracting(LocalPokemon::id).containsExactly(first.id(), second.id());
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldKeepThePokemonWhenAnotherUserTriesToDeleteIt() {
        // given
        final var saved = adapter.save(ASH, pikachu(List.of()));

        // when
        final var deleted = adapter.deleteById(MISTY, saved.id());
        entityManager.flush();

        // then
        assertThat(deleted).isFalse();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM local_pokemon WHERE id = ?", Integer.class,
                saved.id())).isEqualTo(1);
    }

    private void deleteCommitted(final long id) {
        jdbcTemplate.update("DELETE FROM local_pokemon_abilities WHERE local_pokemon_id = ?", id);
        jdbcTemplate.update("DELETE FROM local_pokemon_internal_tags WHERE local_pokemon_id = ?", id);
        jdbcTemplate.update("DELETE FROM local_pokemon WHERE id = ?", id);
    }

    private List<String> abilitiesOf(final long id) {
        return jdbcTemplate.queryForList(
                "SELECT ability FROM local_pokemon_abilities WHERE local_pokemon_id = ? ORDER BY position", String.class,
                id);
    }

    private List<String> tagsOf(final long id) {
        return jdbcTemplate.queryForList(
                "SELECT tag FROM local_pokemon_internal_tags WHERE local_pokemon_id = ? ORDER BY position", String.class,
                id);
    }
}
