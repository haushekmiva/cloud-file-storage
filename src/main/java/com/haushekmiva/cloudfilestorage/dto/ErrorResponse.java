package com.haushekmiva.cloudfilestorage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String message, List<ValidationError> validationErrors) {

    public ErrorResponse(String message) {
        this(message, null);
    }

}
