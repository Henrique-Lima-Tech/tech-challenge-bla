package com.challenge.aitools.taskmanagement.application.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.challenge.aitools.taskmanagement.application.task.command.GetTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

@ExtendWith(MockitoExtension.class)
class GetTaskServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_OWNER_ID = 2L;
    private static final Task TASK = new Task(7L, "Write the plan", "Phase 1", TaskStatus.TODO,
            LocalDate.parse("2026-10-31"), OWNER_ID, NOW, NOW);

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private GetTaskService getTaskService;

    @Test
    void shouldReturnTaskWhenItBelongsToTheUser() {
        // given
        when(taskRepository.findByIdAndOwnerId(7L, OWNER_ID)).thenReturn(Optional.of(TASK));

        // when
        final var result = getTaskService.handle(new GetTaskCommand(OWNER_ID, 7L));

        // then
        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.title()).isEqualTo("Write the plan");
    }

    @Test
    void shouldRejectWhenTaskDoesNotExist() {
        // given
        when(taskRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getTaskService.handle(new GetTaskCommand(OWNER_ID, 99L)))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    void shouldRejectWhenTaskBelongsToAnotherUser() {
        // given
        when(taskRepository.findByIdAndOwnerId(7L, OTHER_OWNER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getTaskService.handle(new GetTaskCommand(OTHER_OWNER_ID, 7L)))
                .isInstanceOf(TaskNotFoundException.class);
        verify(taskRepository).findByIdAndOwnerId(7L, OTHER_OWNER_ID);
    }
}
