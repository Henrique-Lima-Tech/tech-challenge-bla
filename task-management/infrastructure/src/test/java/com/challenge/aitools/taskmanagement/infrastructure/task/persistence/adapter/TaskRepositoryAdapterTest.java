package com.challenge.aitools.taskmanagement.infrastructure.task.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;
import com.challenge.aitools.taskmanagement.domain.user.model.User;
import com.challenge.aitools.taskmanagement.infrastructure.task.persistence.mapper.TaskPersistenceMapperImpl;
import com.challenge.aitools.taskmanagement.infrastructure.user.persistence.adapter.UserRepositoryAdapter;
import com.challenge.aitools.taskmanagement.infrastructure.user.persistence.mapper.UserPersistenceMapperImpl;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import({ TaskRepositoryAdapter.class, TaskPersistenceMapperImpl.class, UserRepositoryAdapter.class,
        UserPersistenceMapperImpl.class })
class TaskRepositoryAdapterTest {

    private static final String HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BWX4Z3Pc5lYxOZpSmVnZf2C1Q/4K";
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T08:30:15Z");
    private static final LocalDate DUE_DATE = LocalDate.parse("2026-10-31");

    private final TaskRepositoryAdapter adapter;
    private final UserRepositoryAdapter userRepositoryAdapter;

    private Long ownerId;
    private Long otherOwnerId;

    @Autowired
    TaskRepositoryAdapterTest(final TaskRepositoryAdapter adapter, final UserRepositoryAdapter userRepositoryAdapter) {
        this.adapter = adapter;
        this.userRepositoryAdapter = userRepositoryAdapter;
    }

    @BeforeEach
    void saveOwners() {
        ownerId = userRepositoryAdapter.save(new User(null, "Demo User", "demo@example.com", HASH)).id();
        otherOwnerId = userRepositoryAdapter.save(new User(null, "Other User", "other@example.com", HASH)).id();
    }

    @Test
    void shouldAssignIdWhenTaskIsSaved() {
        // when
        final var saved = adapter.save(task("Write the plan", DUE_DATE, TaskStatus.TODO, ownerId));

        // then
        assertThat(saved.id()).isPositive();
    }

    @Test
    void shouldKeepEveryFieldWhenRoundTrippingATask() {
        // given
        final var updatedAt = CREATED_AT.plusSeconds(3600);
        final var saved = adapter.save(new Task(null, "Write the plan", "Phase 1", TaskStatus.IN_PROGRESS, DUE_DATE,
                ownerId, CREATED_AT, updatedAt));

        // when
        final var found = adapter.findByIdAndOwnerId(saved.id(), ownerId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().title()).isEqualTo("Write the plan");
        assertThat(found.get().description()).isEqualTo("Phase 1");
        assertThat(found.get().status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(found.get().dueDate()).isEqualTo(DUE_DATE);
        assertThat(found.get().ownerId()).isEqualTo(ownerId);
        assertThat(found.get().createdAt()).isEqualTo(CREATED_AT);
        assertThat(found.get().updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldKeepNullDescriptionWhenTaskHasNone() {
        // given
        final var saved = adapter.save(task("Write the plan", DUE_DATE, TaskStatus.TODO, ownerId));

        // when
        final var found = adapter.findByIdAndOwnerId(saved.id(), ownerId);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().description()).isNull();
    }

    @Test
    void shouldFindNothingWhenTaskBelongsToAnotherUser() {
        // given
        final var saved = adapter.save(task("Write the plan", DUE_DATE, TaskStatus.TODO, ownerId));

        // when & then
        assertThat(adapter.findByIdAndOwnerId(saved.id(), otherOwnerId)).isEmpty();
    }

    @Test
    void shouldFindNothingWhenTaskDoesNotExist() {
        // when & then
        assertThat(adapter.findByIdAndOwnerId(9999L, ownerId)).isEmpty();
    }

    @Test
    void shouldOrderTasksByDueDateThenIdWhenListing() {
        // given
        final var later = adapter.save(task("Later", DUE_DATE.plusDays(1), TaskStatus.TODO, ownerId));
        final var firstOfTheDay = adapter.save(task("First of the day", DUE_DATE, TaskStatus.TODO, ownerId));
        final var secondOfTheDay = adapter.save(task("Second of the day", DUE_DATE, TaskStatus.TODO, ownerId));

        // when
        final var found = adapter.findByOwnerId(ownerId, null, 0, 20);

        // then
        assertThat(found.content()).extracting(Task::id)
                .containsExactly(firstOfTheDay.id(), secondOfTheDay.id(), later.id());
    }

    @Test
    void shouldReturnOnlyTheOwnerTasksWhenListing() {
        // given
        adapter.save(task("Mine", DUE_DATE, TaskStatus.TODO, ownerId));
        adapter.save(task("Theirs", DUE_DATE, TaskStatus.TODO, otherOwnerId));

        // when
        final var found = adapter.findByOwnerId(ownerId, null, 0, 20);

        // then
        assertThat(found.content()).extracting(Task::title).containsExactly("Mine");
        assertThat(found.totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldFilterByStatusWhenStatusIsGiven() {
        // given
        adapter.save(task("Open", DUE_DATE, TaskStatus.TODO, ownerId));
        adapter.save(task("Finished", DUE_DATE, TaskStatus.DONE, ownerId));

        // when
        final var found = adapter.findByOwnerId(ownerId, TaskStatus.DONE, 0, 20);

        // then
        assertThat(found.content()).extracting(Task::title).containsExactly("Finished");
        assertThat(found.totalElements()).isEqualTo(1L);
    }

    @Test
    void shouldReturnEveryStatusWhenStatusIsNull() {
        // given
        adapter.save(task("Open", DUE_DATE, TaskStatus.TODO, ownerId));
        adapter.save(task("Finished", DUE_DATE, TaskStatus.DONE, ownerId));

        // when
        final var found = adapter.findByOwnerId(ownerId, null, 0, 20);

        // then
        assertThat(found.totalElements()).isEqualTo(2L);
    }

    @Test
    void shouldPaginateWhenMoreTasksThanThePageSize() {
        // given
        adapter.save(task("First", DUE_DATE, TaskStatus.TODO, ownerId));
        adapter.save(task("Second", DUE_DATE.plusDays(1), TaskStatus.TODO, ownerId));
        adapter.save(task("Third", DUE_DATE.plusDays(2), TaskStatus.TODO, ownerId));

        // when
        final var found = adapter.findByOwnerId(ownerId, null, 1, 2);

        // then
        assertThat(found.content()).extracting(Task::title).containsExactly("Third");
        assertThat(found.page()).isEqualTo(1);
        assertThat(found.size()).isEqualTo(2);
        assertThat(found.totalElements()).isEqualTo(3L);
        assertThat(found.totalPages()).isEqualTo(2);
    }

    @Test
    void shouldReturnEmptyContentWithRealTotalsWhenPageIsPastTheEnd() {
        // given
        adapter.save(task("First", DUE_DATE, TaskStatus.TODO, ownerId));
        adapter.save(task("Second", DUE_DATE.plusDays(1), TaskStatus.TODO, ownerId));

        // when
        final var found = adapter.findByOwnerId(ownerId, null, 9, 20);

        // then
        assertThat(found.content()).isEmpty();
        assertThat(found.page()).isEqualTo(9);
        assertThat(found.totalElements()).isEqualTo(2L);
        assertThat(found.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldDeleteTaskWhenAsked() {
        // given
        final var saved = adapter.save(task("Write the plan", DUE_DATE, TaskStatus.TODO, ownerId));

        // when
        adapter.delete(saved);

        // then
        assertThat(adapter.findByIdAndOwnerId(saved.id(), ownerId)).isEmpty();
    }

    @Test
    void shouldStoreAnOverdueDueDateWhenRebuildingATask() {
        // given
        final var overdue = LocalDate.parse("2020-01-01");

        // when
        final var saved = adapter.save(task("Overdue", overdue, TaskStatus.TODO, ownerId));

        // then
        assertThat(adapter.findByIdAndOwnerId(saved.id(), ownerId)).get()
                .extracting(Task::dueDate).isEqualTo(overdue);
    }

    private static Task task(final String title, final LocalDate dueDate, final TaskStatus status, final Long ownerId) {
        return new Task(null, title, null, status, dueDate, ownerId, CREATED_AT, CREATED_AT);
    }
}
