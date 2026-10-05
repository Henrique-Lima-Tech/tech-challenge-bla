package com.tech.challenge.application.user.result;

public record AccessTokenResult(String value, long expiresInSeconds) {

    @Override
    public String toString() {
        return "AccessTokenResult[expiresInSeconds=" + expiresInSeconds + "]";
    }
}
