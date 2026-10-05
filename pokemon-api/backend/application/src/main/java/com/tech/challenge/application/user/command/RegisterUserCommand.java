package com.tech.challenge.application.user.command;

public record RegisterUserCommand(String name, String email, String password) {

    @Override
    public String toString() {
        return "RegisterUserCommand[name=" + name + "]";
    }
}
