package com.challenge.aitools.taskmanagement.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank(message = "must not be blank")
        String email,

        @NotBlank(message = "must not be blank")
        String password) {
}
