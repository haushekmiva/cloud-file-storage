package com.haushekmiva.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExists extends AppException {

    private final String username;

    public UserAlreadyExists(String message, String username) {
        super(message);
        this.username = username;
    }
}
