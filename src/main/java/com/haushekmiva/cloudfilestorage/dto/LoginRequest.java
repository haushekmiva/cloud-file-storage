package com.haushekmiva.cloudfilestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 20) String username,
        @NotBlank @Size(max = BCRYPT_MAX_LENGTH) @Pattern(regexp = "^[\\x20-\\x7E]+$") String password
) {
    public static final int BCRYPT_MAX_LENGTH = 72;
}
