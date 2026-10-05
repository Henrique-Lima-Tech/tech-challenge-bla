package com.challenge.aitools.taskmanagement.web.task.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.challenge.aitools.taskmanagement.application.shared.pagination.Page;
import com.challenge.aitools.taskmanagement.application.task.command.CreateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.command.DeleteTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.command.GetTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.command.ListTasksCommand;
import com.challenge.aitools.taskmanagement.application.task.command.UpdateTaskCommand;
import com.challenge.aitools.taskmanagement.application.task.port.in.CreateTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.DeleteTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.GetTask;
import com.challenge.aitools.taskmanagement.application.task.port.in.ListTasks;
import com.challenge.aitools.taskmanagement.application.task.port.in.UpdateTask;
import com.challenge.aitools.taskmanagement.application.task.result.TaskResult;
import com.challenge.aitools.taskmanagement.domain.task.exception.InvalidTaskException;
import com.challenge.aitools.taskmanagement.domain.task.exception.TaskNotFoundException;
import com.challenge.aitools.taskmanagement.domain.task.model.TaskStatus;
import com.challenge.aitools.taskmanagement.web.shared.security.SecurityConfig;
import com.challenge.aitools.taskmanagement.web.task.mapper.TaskMapperImpl;

import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(TaskController.class)
@Import({ TaskMapperImpl.class, SecurityConfig.class })
class TaskControllerTest {

    private static final String TASKS = "/api/v1/tasks";
    private static final Long OWNER_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");
    private static final LocalDate DUE_DATE = LocalDate.parse("2026-10-31");
    private static final TaskResult TASK = new TaskResult(7L, "Write the plan", "Phase 1", TaskStatus.TODO, DUE_DATE,
            NOW, NOW);

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private CreateTask createTask;

    @MockitoBean
    private ListTasks listTasks;

    @MockitoBean
    private GetTask getTask;

    @MockitoBean
    private UpdateTask updateTask;

    @MockitoBean
    private DeleteTask deleteTask;

    @Autowired
    TaskControllerTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldCreateTaskWhenRequestIsValid() throws Exception {
        // given
        when(createTask.handle(any(CreateTaskCommand.class))).thenReturn(TASK);

        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan", "description", "Phase 1", "status", "TODO",
                                "dueDate", "2026-10-31")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tasks/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Write the plan"))
                .andExpect(jsonPath("$.description").value("Phase 1"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2026-10-31"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldTakeTheOwnerFromTheTokenWhenCreating() throws Exception {
        // given
        when(createTask.handle(any(CreateTaskCommand.class))).thenReturn(TASK);

        // when
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan", "dueDate", "2026-10-31")))
                .andExpect(status().isCreated());

        // then
        verify(createTask).handle(new CreateTaskCommand(OWNER_ID, "Write the plan", null, null, DUE_DATE));
    }

    @Test
    void shouldNotExposeTheOwnerWhenReturningATask() throws Exception {
        // given
        when(createTask.handle(any(CreateTaskCommand.class))).thenReturn(TASK);

        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan", "dueDate", "2026-10-31")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").doesNotExist());
    }

    @Test
    void shouldRejectCreationWhenTitleIsBlank() throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "   ", "dueDate", "2026-10-31")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.instance").value(TASKS))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must not be blank")));
        verifyNoInteractions(createTask);
    }

    @Test
    void shouldRejectCreationWhenTitleExceedsMaxLength() throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "a".repeat(121), "dueDate", "2026-10-31")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be at most 120 characters")));
        verifyNoInteractions(createTask);
    }

    @Test
    void shouldRejectCreationWhenDueDateIsMissing() throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("dueDate")));
        verifyNoInteractions(createTask);
    }

    @Test
    void shouldRejectCreationWhenDueDateIsInThePast() throws Exception {
        // given
        when(createTask.handle(any(CreateTaskCommand.class)))
                .thenThrow(new InvalidTaskException("dueDate", "must not be in the past"));

        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan", "dueDate", "2020-01-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("dueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be in the past"));
    }

    @Test
    void shouldRejectCreationWhenStatusIsUnknown() throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan", "status", "ARCHIVED", "dueDate", "2026-10-31")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("status")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be one of TODO, IN_PROGRESS, DONE")));
        verifyNoInteractions(createTask);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "ownerId", "createdAt", "updatedAt"})
    void shouldRejectCreationWhenAReadOnlyFieldIsSent(final String field) throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan", "dueDate", "2026-10-31", field, "7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value("must not be sent"));
        verifyNoInteractions(createTask);
    }

    @Test
    void shouldRejectCreationWhenBodyIsMalformed() throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
        verifyNoInteractions(createTask);
    }

    @Test
    void shouldRejectCreationWhenBodyIsEmpty() throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON).content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
        verifyNoInteractions(createTask);
    }

    @Test
    void shouldRejectCreationWhenDueDateIsNotADate() throws Exception {
        // when & then
        mockMvc.perform(authenticated(post(TASKS)).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "Write the plan", "dueDate", "31/10/2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
        verifyNoInteractions(createTask);
    }

    @Test
    void shouldListTasksWhenTheUserIsAuthenticated() throws Exception {
        // given
        when(listTasks.handle(any(ListTasksCommand.class)))
                .thenReturn(new Page<>(List.of(TASK), 0, 20, 1L, 1));

        // when & then
        mockMvc.perform(authenticated(get(TASKS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].title").value("Write the plan"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldUseTheDefaultPageAndSizeWhenNotGiven() throws Exception {
        // given
        when(listTasks.handle(any(ListTasksCommand.class))).thenReturn(new Page<>(List.of(), 0, 20, 0L, 0));

        // when
        mockMvc.perform(authenticated(get(TASKS))).andExpect(status().isOk());

        // then
        verify(listTasks).handle(new ListTasksCommand(OWNER_ID, null, 0, 20));
    }

    @Test
    void shouldPassTheStatusFilterWhenGiven() throws Exception {
        // given
        when(listTasks.handle(any(ListTasksCommand.class))).thenReturn(new Page<>(List.of(), 0, 20, 0L, 0));

        // when
        mockMvc.perform(authenticated(get(TASKS)).param("status", "DONE")).andExpect(status().isOk());

        // then
        verify(listTasks).handle(new ListTasksCommand(OWNER_ID, TaskStatus.DONE, 0, 20));
    }

    @Test
    void shouldReturnEmptyContentWithRealTotalsWhenPageIsPastTheEnd() throws Exception {
        // given
        when(listTasks.handle(any(ListTasksCommand.class))).thenReturn(new Page<>(List.of(), 9, 20, 3L, 1));

        // when & then
        mockMvc.perform(authenticated(get(TASKS)).param("page", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(9))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldRejectListingWhenSizeIsAboveTheMaximum() throws Exception {
        // when & then
        mockMvc.perform(authenticated(get(TASKS)).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("size")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be at most 100")));
        verifyNoInteractions(listTasks);
    }

    @Test
    void shouldRejectListingWhenSizeIsBelowTheMinimum() throws Exception {
        // when & then
        mockMvc.perform(authenticated(get(TASKS)).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("size")));
        verifyNoInteractions(listTasks);
    }

    @Test
    void shouldRejectListingWhenPageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(authenticated(get(TASKS)).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("page")));
        verifyNoInteractions(listTasks);
    }

    @Test
    void shouldRejectListingWhenPageIsNotAnInteger() throws Exception {
        // when & then
        mockMvc.perform(authenticated(get(TASKS)).param("page", "first"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("page")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be an integer")));
        verifyNoInteractions(listTasks);
    }

    @Test
    void shouldRejectListingWhenStatusIsUnknown() throws Exception {
        // when & then
        mockMvc.perform(authenticated(get(TASKS)).param("status", "ARCHIVED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("status")))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be one of TODO, IN_PROGRESS, DONE")));
        verifyNoInteractions(listTasks);
    }

    @Test
    void shouldReturnTaskWhenItBelongsToTheUser() throws Exception {
        // given
        when(getTask.handle(new GetTaskCommand(OWNER_ID, 7L))).thenReturn(TASK);

        // when & then
        mockMvc.perform(authenticated(get(TASKS + "/7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void shouldReturnNotFoundWhenTaskBelongsToAnotherUser() throws Exception {
        // given
        when(getTask.handle(any(GetTaskCommand.class))).thenThrow(new TaskNotFoundException());

        // when & then
        mockMvc.perform(authenticated(get(TASKS + "/3")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Task not found"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void shouldRejectReadingWhenIdIsNotAnInteger() throws Exception {
        // when & then
        mockMvc.perform(authenticated(get(TASKS + "/abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be an integer")));
        verifyNoInteractions(getTask);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void shouldRejectReadingWhenIdIsBelowOne(final String id) throws Exception {
        // when & then
        mockMvc.perform(authenticated(get(TASKS + "/" + id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].message").value(hasItem("must be at least 1")));
        verifyNoInteractions(getTask);
    }

    @Test
    void shouldReplaceTaskWhenRequestIsValid() throws Exception {
        // given
        when(updateTask.handle(any(UpdateTaskCommand.class))).thenReturn(TASK);

        // when & then
        mockMvc.perform(authenticated(put(TASKS + "/7")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "New title", "description", "New description", "status", "DONE",
                                "dueDate", "2026-10-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
        verify(updateTask).handle(new UpdateTaskCommand(OWNER_ID, 7L, "New title", "New description", TaskStatus.DONE,
                DUE_DATE));
    }

    @Test
    void shouldReturnNotFoundWhenReplacingATaskOfAnotherUser() throws Exception {
        // given
        when(updateTask.handle(any(UpdateTaskCommand.class))).thenThrow(new TaskNotFoundException());

        // when & then
        mockMvc.perform(authenticated(put(TASKS + "/3")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "New title", "dueDate", "2026-10-31")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task not found"));
    }

    @Test
    void shouldRejectReplacementWhenTitleIsBlank() throws Exception {
        // when & then
        mockMvc.perform(authenticated(put(TASKS + "/7")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "", "dueDate", "2026-10-31")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(hasItem("title")));
        verifyNoInteractions(updateTask);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "ownerId", "createdAt", "updatedAt"})
    void shouldRejectReplacementWhenAReadOnlyFieldIsSent(final String field) throws Exception {
        // when & then
        mockMvc.perform(authenticated(put(TASKS + "/7")).contentType(MediaType.APPLICATION_JSON)
                        .content(body("title", "New title", "dueDate", "2026-10-31", field, "7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(jsonPath("$.errors[0].message").value("must not be sent"));
        verifyNoInteractions(updateTask);
    }

    @Test
    void shouldDeleteTaskWhenItBelongsToTheUser() throws Exception {
        // when & then
        mockMvc.perform(authenticated(delete(TASKS + "/7")))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());
        verify(deleteTask).handle(new DeleteTaskCommand(OWNER_ID, 7L));
    }

    @Test
    void shouldReturnNotFoundWhenDeletingATaskOfAnotherUser() throws Exception {
        // given
        org.mockito.Mockito.doThrow(new TaskNotFoundException()).when(deleteTask).handle(any(DeleteTaskCommand.class));

        // when & then
        mockMvc.perform(authenticated(delete(TASKS + "/3")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task not found"));
    }

    @Test
    void shouldRejectEveryTaskRouteWhenTokenIsMissing() throws Exception {
        // when & then
        mockMvc.perform(get(TASKS)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Authentication required"))
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mockMvc.perform(get(TASKS + "/7")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON)
                .content(body("title", "Write the plan", "dueDate", "2026-10-31"))).andExpect(status().isUnauthorized());
        mockMvc.perform(put(TASKS + "/7").contentType(MediaType.APPLICATION_JSON)
                .content(body("title", "Write the plan", "dueDate", "2026-10-31"))).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(TASKS + "/7")).andExpect(status().isUnauthorized());
        verifyNoInteractions(createTask, listTasks, getTask, updateTask, deleteTask);
    }

    @Test
    void shouldRejectTaskRouteWhenTokenIsInvalid() throws Exception {
        // when & then
        mockMvc.perform(get(TASKS).header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
        verifyNoInteractions(listTasks);
    }

    private static MockHttpServletRequestBuilder authenticated(final MockHttpServletRequestBuilder request) {
        return request.with(jwt().jwt(builder -> builder.subject(String.valueOf(OWNER_ID))));
    }

    private String body(final String... keysAndValues) {
        final Map<String, String> content = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            content.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return jsonMapper.writeValueAsString(content);
    }
}
