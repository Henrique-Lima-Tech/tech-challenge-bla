package com.challenge.aitools.taskmanagement.web.user.dto.request;

import com.challenge.aitools.taskmanagement.domain.user.model.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "must not be blank")
        @Size(max = User.NAME_MAX_LENGTH, message = "must be at most " + User.NAME_MAX_LENGTH + " characters")
        String name,

        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed email address")
        @Size(max = User.EMAIL_MAX_LENGTH, message = "must be at most " + User.EMAIL_MAX_LENGTH + " characters")
        String email,

        @NotBlank(message = "must not be blank")
        @Size(min = RegisterRequest.PASSWORD_MIN_LENGTH,
                message = "must be at least " + RegisterRequest.PASSWORD_MIN_LENGTH + " characters")
        @MaxUtf8Bytes(value = RegisterRequest.PASSWORD_MAX_BYTES,
                message = "must be at most " + RegisterRequest.PASSWORD_MAX_BYTES + " bytes")
        String password) {

    public static final int PASSWORD_MIN_LENGTH = 8;

    public static final int PASSWORD_MAX_BYTES = 72;
}
