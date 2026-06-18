package com.haushekmiva.cloudfilestorage.controller;


import com.haushekmiva.cloudfilestorage.dto.RegisterRequest;
import com.haushekmiva.cloudfilestorage.dto.RegisterResponse;
import com.haushekmiva.cloudfilestorage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse = userService.register(registerRequest.username(), registerRequest.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(registerResponse);
    }

}
