package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.exception.UserAuthenticationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;

    @Override
    public Authentication authenticateUser(String username, String password) {
        var authToken = UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        try {
            Authentication authentication = authManager.authenticate(authToken);
            log.info("User authenticated: username = {}.", username);
            return authentication;
        } catch (AuthenticationException e) {
            throw new UserAuthenticationException(username, e);
        }
    }
}
