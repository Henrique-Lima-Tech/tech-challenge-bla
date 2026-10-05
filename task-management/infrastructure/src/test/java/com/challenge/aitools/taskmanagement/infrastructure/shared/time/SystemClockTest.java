package com.challenge.aitools.taskmanagement.infrastructure.shared.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class SystemClockTest {

    private final SystemClock clock = new SystemClock();

    @Test
    void shouldReturnTheCurrentInstantWhenAsked() {
        // given
        final var before = Instant.now();

        // when
        final var now = clock.now();

        // then
        assertThat(now).isBetween(before.minusSeconds(1), before.plus(Duration.ofSeconds(5)));
    }
}
