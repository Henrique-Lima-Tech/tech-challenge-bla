package com.tech.challenge.web.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed email address")
        @Size(max = 254, message = "size must be at most 254") String email,
        @NotBlank(message = "must not be blank") String password) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "LoginRequest[]";
    }
}
