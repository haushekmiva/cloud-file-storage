package com.haushekmiva.cloudfilestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchRequest(
        @NotBlank(message = "{validation.search-query.not-blank}")
        @Size(min=3, max=64, message="{validation.search-query.size}")
        @Pattern(regexp = "^[^/]+$", message = "{validation.search-query.format}")
        String query
) {
}
