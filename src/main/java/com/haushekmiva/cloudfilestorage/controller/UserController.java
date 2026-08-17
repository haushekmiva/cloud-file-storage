package com.haushekmiva.cloudfilestorage.controller;

import com.haushekmiva.cloudfilestorage.dto.UserResponse;
import com.haushekmiva.cloudfilestorage.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "User", description = "Current user information")
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    @GetMapping("/me")
    @Operation(
            summary = "Get user information",
            description = "Returns username of the current user"
    )
    @ApiResponse(responseCode = "200", description = "User information retrieved successfully")
    public ResponseEntity<UserResponse> getUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return ResponseEntity.ok(new UserResponse(userDetails.getUsername()));
    }

}
