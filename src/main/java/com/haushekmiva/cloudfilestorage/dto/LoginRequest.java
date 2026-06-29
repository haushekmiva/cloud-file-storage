package com.haushekmiva.cloudfilestorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.haushekmiva.cloudfilestorage.config.SecurityConfig.PASSWORD_ENCODER_MAX_LENGTH;

public record LoginRequest(
        @NotBlank(message = "{validation.login.username.not-blank}")
        @Size(max = 20, message = "{validation.login.username.size}")
        String username,

        @NotBlank(message = "{validation.login.password.not-blank}")
        @Size(max = PASSWORD_ENCODER_MAX_LENGTH, message = "{validation.login.password.size}")
        String password
) {
}
