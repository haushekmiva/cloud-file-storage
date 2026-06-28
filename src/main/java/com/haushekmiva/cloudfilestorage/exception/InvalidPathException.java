package com.haushekmiva.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class InvalidPathException extends AppException {

    private final String path;

    public InvalidPathException(String path) {
        super("Invalid path was entered: path = %s".formatted(path));
        this.path = path;
    }
}
