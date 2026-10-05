package com.challenge.aitools.taskmanagement.application.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challenge.aitools.taskmanagement.application.shared.port.out.Clock;
import com.challenge.aitools.taskmanagement.application.task.command.CreateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.domain.task.exception.InvalidTaskException;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

@ExtendWith(MockitoExtension.class)
class CreateTaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-05");
    private static final Long OWNER_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private CreateTaskService createTaskService;

    @Test
    void shouldCreateTaskWhenDataIsValid() {
        // given
        final var command = new CreateTaskCommand(OWNER_ID, "Write the plan", "Phase 1", TaskStatus.IN_PROGRESS,
                TODAY.plusDays(1));
        when(clock.now()).thenReturn(NOW);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            final Task task = invocation.getArgument(0);
            return new Task(7L, task.title(), task.description(), task.status(), task.dueDate(), task.ownerId(),
                    task.createdAt(), task.updatedAt());
        });

        // when
        final var result = createTaskService.handle(command);

        // then
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.title()).isEqualTo("Write the plan");
        assertThat(result.description()).isEqualTo("Phase 1");
        assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.dueDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldOwnTheTaskByTheGivenUserWhenCreating() {
        // given
        final var command = new CreateTaskCommand(OWNER_ID, "Write the plan", null, null, TODAY);
        when(clock.now()).thenReturn(NOW);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        createTaskService.handle(command);

        // then
        final var saved = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(saved.capture());
        assertThat(saved.getValue().ownerId()).isEqualTo(OWNER_ID);
        assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
    }

    @Test
    void shouldDefaultStatusToTodoWhenStatusIsNotProvided() {
        // given
        final var command = new CreateTaskCommand(OWNER_ID, "Write the plan", null, null, TODAY);
        when(clock.now()).thenReturn(NOW);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        final var result = createTaskService.handle(command);

        // then
        assertThat(result.status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void shouldRejectCreationWhenDueDateIsInThePast() {
        // given
        final var command = new CreateTaskCommand(OWNER_ID, "Write the plan", null, TaskStatus.TODO, TODAY.minusDays(1));
        when(clock.now()).thenReturn(NOW);

        // when & then
        assertThatThrownBy(() -> createTaskService.handle(command))
                .isInstanceOf(InvalidTaskException.class);
        verify(taskRepository, never()).save(any(Task.class));
    }
}
