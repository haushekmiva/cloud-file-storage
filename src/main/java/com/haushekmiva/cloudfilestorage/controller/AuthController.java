package com.haushekmiva.cloudfilestorage.controller;


import com.haushekmiva.cloudfilestorage.dto.LoginRequest;
import com.haushekmiva.cloudfilestorage.dto.RegisterRequest;
import com.haushekmiva.cloudfilestorage.dto.AuthResponse;
import com.haushekmiva.cloudfilestorage.service.AuthService;
import com.haushekmiva.cloudfilestorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        userService.register(registerRequest.username(), registerRequest.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(registerRequest.username()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response) {

        Authentication authentication = authService.authenticateUser(loginRequest.username(), loginRequest.password());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(new AuthResponse(loginRequest.username()));
    }

}
