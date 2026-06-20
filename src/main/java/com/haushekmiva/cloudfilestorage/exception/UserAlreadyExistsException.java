package com.haushekmiva.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExistsException extends AppException {

    private final String username;

    public UserAlreadyExistsException(String username) {
        super("User with this username %s already exists.".formatted(username));
        this.username = username;
    }
}
