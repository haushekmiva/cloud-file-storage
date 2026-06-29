package com.haushekmiva.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class UserAuthenticationException extends AppException {

    private final String username;

    public UserAuthenticationException(String username, Throwable cause) {
        super("Incorrect login or password while authenticating username = %s".formatted(username));
        this.username = username;
    }
}
