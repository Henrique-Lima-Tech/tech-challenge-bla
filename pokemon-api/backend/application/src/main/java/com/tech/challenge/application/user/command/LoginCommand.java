package com.tech.challenge.application.user.command;

public record LoginCommand(String email, String password) {

    @Override
    public String toString() {
        return "LoginCommand[]";
    }
}
