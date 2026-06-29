package com.haushekmiva.cloudfilestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.haushekmiva.cloudfilestorage.config.SecurityConfig.PASSWORD_ENCODER_MAX_LENGTH;

public record RegisterRequest(
        @NotBlank(message = "{validation.register.username.not-blank}")
        @Size(min = 6, max = 20, message = "{validation.register.username.size}")
        @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "{validation.register.username.format}")
        String username,

        @NotBlank(message = "{validation.register.password.not-blank}")
        @Size(min = 6, max = PASSWORD_ENCODER_MAX_LENGTH, message = "{validation.register.password.size}")
        @Pattern(regexp = "^[\\x20-\\x7E]+$", message = "{validation.register.password.format}")
        String password
) {
    public RegisterRequest {
        username = username == null ? null : username.trim();
    }
}
