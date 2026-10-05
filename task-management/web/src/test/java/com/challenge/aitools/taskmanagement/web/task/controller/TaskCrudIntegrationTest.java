package com.challenge.aitools.taskmanagement.web.task.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

/**
 * The whole task CRUD through the real chain, plus the two answers that must never leak ownership: a task
 * of another user, and a request without a token.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TaskCrudIntegrationTest {

    private static final String TASKS = "/api/v1/tasks";

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Autowired
    TaskCrudIntegrationTest(final MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldWalkTheWholeCrudWhenTheUserOwnsTheTask() throws Exception {
        // given
        final var token = registerAndLogin("crud-user@example.com");
        final var dueDate = LocalDate.now().plusDays(7);

        // when
        final var created = mockMvc.perform(post(TASKS).headers(bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Write the plan","description":"Phase 1","status":"TODO","dueDate":"%s"}"""
                                .formatted(dueDate)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andReturn().getResponse().getContentAsString();
        final var id = jsonMapper.readTree(created).get("id").asLong();

        // then
        mockMvc.perform(get(TASKS).headers(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get(TASKS + "/" + id).headers(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write the plan"));

        mockMvc.perform(put(TASKS + "/" + id).headers(bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Write the plan v2","description":null,"status":"DONE","dueDate":"%s"}"""
                                .formatted(dueDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write the plan v2"))
                .andExpect(jsonPath("$.description").value((String) null))
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(delete(TASKS + "/" + id).headers(bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TASKS + "/" + id).headers(bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task not found"));
    }

    @Test
    void shouldReturnNotFoundWhenTheTaskBelongsToAnotherUser() throws Exception {
        // given
        final var ownerToken = registerAndLogin("owner-user@example.com");
        final var intruderToken = registerAndLogin("intruder-user@example.com");
        final var created = mockMvc.perform(post(TASKS).headers(bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Private task","dueDate":"%s"}""".formatted(LocalDate.now().plusDays(3))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        final var id = jsonMapper.readTree(created).get("id").asLong();

        // when & then
        mockMvc.perform(get(TASKS + "/" + id).headers(bearer(intruderToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Task not found"));
        mockMvc.perform(put(TASKS + "/" + id).headers(bearer(intruderToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Stolen","dueDate":"%s"}""".formatted(LocalDate.now().plusDays(3))))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(TASKS + "/" + id).headers(bearer(intruderToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(TASKS).headers(bearer(intruderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectCreationWhenDueDateIsInThePast() throws Exception {
        // given
        final var token = registerAndLogin("past-due-user@example.com");

        // when & then
        mockMvc.perform(post(TASKS).headers(bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Too late","dueDate":"%s"}""".formatted(LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("dueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be in the past"));
    }

    @Test
    void shouldKeepAnOverdueDueDateWhenItIsNotChangedOnUpdate() throws Exception {
        // given
        final var token = registerAndLogin("overdue-user@example.com");
        final var today = LocalDate.now();
        final var created = mockMvc.perform(post(TASKS).headers(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Due today","dueDate":"%s"}""".formatted(today)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        final var id = jsonMapper.readTree(created).get("id").asLong();

        // when & then
        mockMvc.perform(put(TASKS + "/" + id).headers(bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Due today, now done","status":"DONE","dueDate":"%s"}""".formatted(today)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.dueDate").value(today.toString()));
    }

    @Test
    void shouldRejectEveryTaskRouteWhenTokenIsMissing() throws Exception {
        // when & then
        mockMvc.perform(get(TASKS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    private HttpHeaders bearer(final String token) {
        final var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String registerAndLogin(final String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Integration User","email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isCreated());
        final var response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response).get("accessToken").asString();
    }
}
