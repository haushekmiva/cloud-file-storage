package com.haushekmiva.cloudfilestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 6, max = 20) @Pattern(regexp = "^[a-zA-Z0-9]$") String username,
        @NotBlank @Size(min = 6, max = 64) String password
) {}
