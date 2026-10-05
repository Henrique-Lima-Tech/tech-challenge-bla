package com.tech.challenge.web.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 100, message = "size must be at most 100") String name,
        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed email address")
        @Size(max = 254, message = "size must be at most 254") String email,
        @NotBlank(message = "must not be blank")
        @Size(min = 8, max = 72, message = "size must be between 8 and 72")
        @MaxUtf8Bytes(value = 72, message = "size must be at most 72 bytes") String password) {

    /** Runs before Bean Validation. The password is never trimmed. */
    public RegisterRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + "]";
    }
}
