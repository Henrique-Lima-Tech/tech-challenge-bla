package com.challenge.aitools.taskmanagement.domain.task.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.challenge.aitools.taskmanagement.domain.task.exception.InvalidTaskException;

class TaskTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-05");
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final Long OWNER_ID = 1L;

    @Test
    void shouldCreateTaskWhenDataIsValid() {
        // when
        final var task = Task.create("Write the plan", "Phase 1", TaskStatus.IN_PROGRESS, TOMORROW, OWNER_ID, NOW);

        // then
        assertThat(task.id()).isNull();
        assertThat(task.title()).isEqualTo("Write the plan");
        assertThat(task.description()).isEqualTo("Phase 1");
        assertThat(task.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.dueDate()).isEqualTo(TOMORROW);
        assertThat(task.ownerId()).isEqualTo(OWNER_ID);
        assertThat(task.createdAt()).isEqualTo(NOW);
        assertThat(task.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldDefaultStatusToTodoWhenStatusIsNotProvided() {
        // when
        final var task = Task.create("Write the plan", null, null, TOMORROW, OWNER_ID, NOW);

        // then
        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void shouldTrimTitleWhenTaskIsCreated() {
        // when
        final var task = Task.create("  Write the plan  ", null, null, TOMORROW, OWNER_ID, NOW);

        // then
        assertThat(task.title()).isEqualTo("Write the plan");
    }

    @Test
    void shouldTrimDescriptionWhenTaskIsCreated() {
        // when
        final var task = Task.create("Write the plan", "  Phase 1  ", null, TOMORROW, OWNER_ID, NOW);

        // then
        assertThat(task.description()).isEqualTo("Phase 1");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldStoreNullDescriptionWhenDescriptionIsBlank(final String description) {
        // when
        final var task = Task.create("Write the plan", description, null, TOMORROW, OWNER_ID, NOW);

        // then
        assertThat(task.description()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void shouldRejectTaskWhenTitleIsBlank(final String title) {
        // when & then
        assertThatThrownBy(() -> Task.create(title, null, TaskStatus.TODO, TOMORROW, OWNER_ID, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldRejectTaskWhenTitleExceedsMaxLength() {
        // given
        final var title = "a".repeat(Task.TITLE_MAX_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> Task.create(title, null, TaskStatus.TODO, TOMORROW, OWNER_ID, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldAcceptTaskWhenTitleIsAtMaxLength() {
        // given
        final var title = "a".repeat(Task.TITLE_MAX_LENGTH);

        // when
        final var task = Task.create(title, null, TaskStatus.TODO, TOMORROW, OWNER_ID, NOW);

        // then
        assertThat(task.title()).hasSize(Task.TITLE_MAX_LENGTH);
    }

    @Test
    void shouldRejectTaskWhenDescriptionExceedsMaxLength() {
        // given
        final var description = "a".repeat(Task.DESCRIPTION_MAX_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> Task.create("Write the plan", description, TaskStatus.TODO, TOMORROW, OWNER_ID, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldRejectTaskWhenDueDateIsMissing() {
        // when & then
        assertThatThrownBy(() -> Task.create("Write the plan", null, TaskStatus.TODO, null, OWNER_ID, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldRejectTaskWhenDueDateIsInThePast() {
        // when & then
        assertThatThrownBy(() -> Task.create("Write the plan", null, TaskStatus.TODO, YESTERDAY, OWNER_ID, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldAcceptTaskWhenDueDateIsToday() {
        // when
        final var task = Task.create("Write the plan", null, TaskStatus.TODO, TODAY, OWNER_ID, NOW);

        // then
        assertThat(task.dueDate()).isEqualTo(TODAY);
    }

    @Test
    void shouldRejectTaskWhenOwnerIsMissing() {
        // when & then
        assertThatThrownBy(() -> Task.create("Write the plan", null, TaskStatus.TODO, TOMORROW, null, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectTaskWhenOwnerIsNotPositive(final long ownerId) {
        // when & then
        assertThatThrownBy(() -> Task.create("Write the plan", null, TaskStatus.TODO, TOMORROW, ownerId, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldRejectTaskWhenCreationInstantIsMissing() {
        // when & then
        assertThatThrownBy(() -> Task.create("Write the plan", null, TaskStatus.TODO, TOMORROW, OWNER_ID, null))
                .isInstanceOf(InvalidTaskException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void shouldRejectTaskWhenIdIsNotPositive(final long id) {
        // when & then
        assertThatThrownBy(() -> new Task(id, "Write the plan", null, TaskStatus.TODO, TOMORROW, OWNER_ID, NOW, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldRejectTaskWhenStatusIsMissingOnTheCanonicalConstructor() {
        // when & then
        assertThatThrownBy(() -> new Task(1L, "Write the plan", null, null, TOMORROW, OWNER_ID, NOW, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldRebuildOverdueTaskWhenUsingTheCanonicalConstructor() {
        // when
        final var task = new Task(1L, "Write the plan", null, TaskStatus.TODO, YESTERDAY, OWNER_ID, NOW, NOW);

        // then
        assertThat(task.dueDate()).isEqualTo(YESTERDAY);
    }

    @Test
    void shouldReplaceEveryEditableFieldWhenUpdated() {
        // given
        final var task = storedTask(TOMORROW, TaskStatus.TODO);
        final var later = NOW.plusSeconds(60);

        // when
        final var updated = task.replace("New title", "New description", TaskStatus.DONE, TOMORROW.plusDays(1), later);

        // then
        assertThat(updated.title()).isEqualTo("New title");
        assertThat(updated.description()).isEqualTo("New description");
        assertThat(updated.status()).isEqualTo(TaskStatus.DONE);
        assertThat(updated.dueDate()).isEqualTo(TOMORROW.plusDays(1));
        assertThat(updated.updatedAt()).isEqualTo(later);
    }

    @Test
    void shouldKeepIdentityAndCreationInstantWhenUpdated() {
        // given
        final var task = storedTask(TOMORROW, TaskStatus.TODO);

        // when
        final var updated = task.replace("New title", null, TaskStatus.DONE, TOMORROW, NOW.plusSeconds(60));

        // then
        assertThat(updated.id()).isEqualTo(task.id());
        assertThat(updated.ownerId()).isEqualTo(task.ownerId());
        assertThat(updated.createdAt()).isEqualTo(task.createdAt());
    }

    @Test
    void shouldKeepOverdueDueDateWhenDueDateIsUnchanged() {
        // given
        final var task = storedTask(YESTERDAY, TaskStatus.TODO);

        // when
        final var updated = task.replace("New title", null, TaskStatus.DONE, YESTERDAY, NOW);

        // then
        assertThat(updated.dueDate()).isEqualTo(YESTERDAY);
    }

    @Test
    void shouldRejectUpdateWhenNewDueDateIsInThePast() {
        // given
        final var task = storedTask(TOMORROW, TaskStatus.TODO);

        // when & then
        assertThatThrownBy(() -> task.replace("New title", null, TaskStatus.TODO, YESTERDAY, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void shouldAllowEveryStatusTransitionWhenUpdated(final TaskStatus target) {
        // given
        final var task = storedTask(TOMORROW, TaskStatus.DONE);

        // when
        final var updated = task.replace("New title", null, target, TOMORROW, NOW);

        // then
        assertThat(updated.status()).isEqualTo(target);
    }

    @Test
    void shouldDefaultStatusToTodoWhenUpdatedWithoutStatus() {
        // given
        final var task = storedTask(TOMORROW, TaskStatus.DONE);

        // when
        final var updated = task.replace("New title", null, null, TOMORROW, NOW);

        // then
        assertThat(updated.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void shouldRejectUpdateWhenTitleIsBlank() {
        // given
        final var task = storedTask(TOMORROW, TaskStatus.TODO);

        // when & then
        assertThatThrownBy(() -> task.replace("   ", null, TaskStatus.TODO, TOMORROW, NOW))
                .isInstanceOf(InvalidTaskException.class);
    }

    @Test
    void shouldRejectUpdateWhenUpdateInstantIsMissing() {
        // given
        final var task = storedTask(TOMORROW, TaskStatus.TODO);

        // when & then
        assertThatThrownBy(() -> task.replace("New title", null, TaskStatus.TODO, TOMORROW, null))
                .isInstanceOf(InvalidTaskException.class);
    }

    private static Task storedTask(final LocalDate dueDate, final TaskStatus status) {
        return new Task(7L, "Write the plan", "Phase 1", status, dueDate, OWNER_ID, NOW, NOW);
    }
}
