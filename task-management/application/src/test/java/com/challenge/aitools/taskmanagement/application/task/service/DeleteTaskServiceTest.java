package com.challenge.aitools.taskmanagement.application.task.service;

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

import com.challenge.aitools.taskmanagement.application.task.command.DeleteTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

@ExtendWith(MockitoExtension.class)
class DeleteTaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Task TASK = new Task(7L, "Write the plan", null, TaskStatus.TODO,
            LocalDate.parse("2026-10-31"), OWNER_ID, NOW, NOW);

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private DeleteTaskService deleteTaskService;

    @Test
    void shouldDeleteTaskWhenItBelongsToTheUser() {
        // given
        when(taskRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(TASK));

        // when
        deleteTaskService.handle(new DeleteTaskCommand(OWNER_ID, 7L));

        // then
        verify(taskRepository).delete(TASK);
    }

    @Test
    void shouldRejectDeletionWhenTaskDoesNotExist() {
        // given
        when(taskRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deleteTaskService.handle(new DeleteTaskCommand(OWNER_ID, 99L)))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void shouldRejectDeletionWhenTaskBelongsToAnotherUser() {
        // given
        when(taskRepository.findByIdAndOwnerId(7L, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deleteTaskService.handle(new DeleteTaskCommand(OTHER_OWNER_ID, 7L)))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository, never()).delete(any(Task.class));
    }
}
