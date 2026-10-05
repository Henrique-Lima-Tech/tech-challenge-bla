package com.tech.challenge.web.user.dto.response;

public record TokenResponse(String accessToken, String tokenType, long expiresIn, String name) {

    @Override
    public String toString() {
        return "TokenResponse[tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}
