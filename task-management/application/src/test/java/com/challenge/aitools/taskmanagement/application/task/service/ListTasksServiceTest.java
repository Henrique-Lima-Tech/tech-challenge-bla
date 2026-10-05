package com.challenge.aitools.taskmanagement.application.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.challenge.aitools.taskmanagement.application.shared.pagination.Page;
import com.challenge.aitools.taskmanagement.application.task.command.ListTasksCommand;
import com.challenge.aitools.taskmanagement.application.task.port.out.TaskRepository;
import com.challenge.aitools.taskmanagement.domain.task.model.Task;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;

@ExtendWith(MockitoExtension.class)
class ListTasksServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final Long OWNER_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private ListTasksService listTasksService;

    @Test
    void shouldReturnPageOfTasksWhenTheUserHasTasks() {
        // given
        final var task = new Task(7L, "Write the plan", "Phase 1", TaskStatus.TODO, LocalDate.parse("2026-10-31"),
                OWNER_ID, NOW, NOW);
        when(taskRepository.findByOwnerId(OWNER_ID, null, 0, 20))
                .thenReturn(new Page<>(List.of(task), 0, 20, 1L, 1));

        // when
        final var result = listTasksService.handle(new ListTasksCommand(OWNER_ID, null, 0, 20));

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(7L);
        assertThat(result.content().getFirst().title()).isEqualTo("Write the plan");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(1L);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void shouldPassTheStatusFilterToTheRepositoryWhenFilterIsGiven() {
        // given
        when(taskRepository.findByOwnerId(OWNER_ID, TaskStatus.DONE, 1, 5))
                .thenReturn(new Page<>(List.of(), 1, 5, 0L, 0));

        // when
        listTasksService.handle(new ListTasksCommand(OWNER_ID, TaskStatus.DONE, 1, 5));

        // then
        verify(taskRepository).findByOwnerId(OWNER_ID, TaskStatus.DONE, 1, 5);
    }

    @Test
    void shouldReturnEmptyContentWithRealTotalsWhenPageIsPastTheEnd() {
        // given
        when(taskRepository.findByOwnerId(OWNER_ID, null, 9, 20))
                .thenReturn(new Page<>(List.of(), 9, 20, 3L, 1));

        // when
        final var result = listTasksService.handle(new ListTasksCommand(OWNER_ID, null, 9, 20));

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(9);
        assertThat(result.totalElements()).isEqualTo(3L);
        assertThat(result.totalPages()).isEqualTo(1);
    }
}
