package com.challenge.aitools.taskmanagement.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Only emptiness is checked here: anything else about the credentials must come back as the same
 * generic 401, never as a hint that the email itself was malformed.
 */
public record LoginRequest(

        @NotBlank(message = "must not be blank")
        String email,

        @NotBlank(message = "must not be blank")
        String password) {
}
