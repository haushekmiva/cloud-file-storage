package com.haushekmiva.cloudfilestorage.exception;

import lombok.Getter;

@Getter
public class FileStorageException extends AppException {

    final private Throwable cause;

    public FileStorageException(String message, Throwable cause) {
        super(message);
        this.cause = cause;
    }
}
