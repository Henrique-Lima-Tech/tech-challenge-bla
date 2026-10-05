package com.tech.challenge.application.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PageResultTest {

    @Test
    void shouldComputeTotalPagesRoundingUpWhenLastPageIsPartial() {
        // when
        final var page = PageResult.of(List.of("bulbasaur"), 0, 20, 1302);

        // then
        assertThat(page.content()).containsExactly("bulbasaur");
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(1302);
        assertThat(page.totalPages()).isEqualTo(66);
    }

    @Test
    void shouldComputeExactTotalPagesWhenTotalIsAMultipleOfSize() {
        // when
        final var page = PageResult.of(List.of(), 1, 10, 40);

        // then
        assertThat(page.totalPages()).isEqualTo(4);
    }

    @Test
    void shouldHaveNoPagesWhenThereAreNoElements() {
        // when
        final var page = PageResult.of(List.of(), 0, 20, 0);

        // then
        assertThat(page.totalPages()).isZero();
        assertThat(page.content()).isEmpty();
    }

    @Test
    void shouldRejectPageWhenPageNumberIsNegative() {
        // when & then
        assertThatThrownBy(() -> PageResult.of(List.of(), -1, 20, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectPageWhenSizeIsNotPositive() {
        // when & then
        assertThatThrownBy(() -> PageResult.of(List.of(), 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectPageWhenTotalElementsIsNegative() {
        // when & then
        assertThatThrownBy(() -> PageResult.of(List.of(), 0, 20, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUseEmptyContentWhenContentIsNull() {
        // when
        final var page = PageResult.of(null, 0, 20, 0);

        // then
        assertThat(page.content()).isEmpty();
    }

    @Test
    void shouldKeepAnImmutableCopyOfContentWhenSourceListChanges() {
        // given
        final var content = new ArrayList<>(List.of("bulbasaur"));

        // when
        final var page = PageResult.of(content, 0, 20, 1);
        content.add("ivysaur");

        // then
        assertThat(page.content()).containsExactly("bulbasaur");
        assertThatThrownBy(() -> page.content().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldRejectPageWhenTotalPagesDoesNotMatchTotalElements() {
        // when & then
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 20, 1302, 65))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResult<>(List.of(), 0, 20, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
