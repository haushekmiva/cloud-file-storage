package com.haushekmiva.cloudfilestorage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceInfoResponse(
        String path,
        String name,
        ResourceType type,
        Long size
) {
    public ResourceInfoResponse(String path, String name, ResourceType resourceType) {
        this(path, name, resourceType, null);
    }
}
