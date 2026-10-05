package com.tech.challenge.application.user.result;

/**
 * The access token and the name of the user who signed in, so the front end can show it (D-32).
 */
public record LoginResult(AccessTokenResult accessToken, String name) {
}
