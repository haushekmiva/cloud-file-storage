package com.haushekmiva.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends AppException {

    final private String path;

    public ResourceNotFoundException(String path) {
        super("Resource \"%s\" not found.".formatted(path));
        this.path = path;
    }
}
