package com.challenge.aitools.taskmanagement.application.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challenge.aitools.taskmanagement.application.shared.port.out.Clock;
import com.challenge.aitools.taskmanagement.application.task.command.UpdateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.domain.task.exception.InvalidTaskException;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

@ExtendWith(MockitoExtension.class)
class UpdateTaskServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-01T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-05");
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private UpdateTaskService updateTaskService;

    @Test
    void shouldReplaceEveryEditableFieldWhenTaskBelongsToTheUser() {
        // given
        final var stored = storedTask(TODAY.plusDays(1), TaskStatus.TODO);
        final var command = new UpdateTaskCommand(OWNER_ID, 7L, "New title", "New description", TaskStatus.DONE,
                TODAY.plusDays(2));
        when(taskRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(stored));
        when(clock.now()).thenReturn(NOW);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        final var result = updateTaskService.handle(command);

        // then
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.title()).isEqualTo("New title");
        assertThat(result.description()).isEqualTo("New description");
        assertThat(result.status()).isEqualTo(TaskStatus.DONE);
        assertThat(result.dueDate()).isEqualTo(TODAY.plusDays(2));
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldKeepOverdueDueDateWhenDueDateIsUnchanged() {
        // given
        final var overdue = TODAY.minusDays(3);
        final var stored = storedTask(overdue, TaskStatus.TODO);
        final var command = new UpdateTaskCommand(OWNER_ID, 7L, "New title", null, TaskStatus.DONE, overdue);
        when(taskRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(stored));
        when(clock.now()).thenReturn(NOW);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        final var result = updateTaskService.handle(command);

        // then
        assertThat(result.dueDate()).isEqualTo(overdue);
        assertThat(result.status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void shouldRejectUpdateWhenNewDueDateIsInThePast() {
        // given
        final var stored = storedTask(TODAY.plusDays(1), TaskStatus.TODO);
        final var command = new UpdateTaskCommand(OWNER_ID, 7L, "New title", null, TaskStatus.TODO, TODAY.minusDays(1));
        when(taskRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(stored));
        when(clock.now()).thenReturn(NOW);

        // when & then
        assertThatThrownBy(() -> updateTaskService.handle(command))
                .isInstanceOf(InvalidTaskException.class);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldRejectUpdateWhenTaskDoesNotExist() {
        // given
        final var command = new UpdateTaskCommand(OWNER_ID, 99L, "New title", null, TaskStatus.TODO, TODAY);
        when(taskRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateTaskService.handle(command))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldRejectUpdateWhenTaskBelongsToAnotherUser() {
        // given
        final var command = new UpdateTaskCommand(OTHER_OWNER_ID, 7L, "New title", null, TaskStatus.TODO, TODAY);
        when(taskRepository.findByIdAndOwnerId(7L, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateTaskService.handle(command))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldKeepTheOwnerWhenUpdating() {
        // given
        final var stored = storedTask(TODAY.plusDays(1), TaskStatus.TODO);
        final var command = new UpdateTaskCommand(OWNER_ID, 7L, "New title", null, TaskStatus.TODO, TODAY.plusDays(1));
        when(taskRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(stored));
        when(clock.now()).thenReturn(NOW);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        updateTaskService.handle(command);

        // then
        verify(taskRepository).save(stored.replace("New title", null, TaskStatus.TODO, TODAY.plusDays(1), NOW));
    }

    private static Task storedTask(final LocalDate dueDate, final TaskStatus status) {
        return new Task(7L, "Write the plan", "Phase 1", status, dueDate, OWNER_ID, CREATED_AT, CREATED_AT);
    }
}
