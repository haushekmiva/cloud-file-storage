package com.haushekmiva.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class ResourceAlreadyExistsException extends AppException {

    private final String path;

    public ResourceAlreadyExistsException(String path) {
        super("Resource \"%s\" already exists.".formatted(path));
        this.path = path;
    }
}
