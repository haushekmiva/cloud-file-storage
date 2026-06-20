package com.haushekmiva.cloudfilestorage.service;

import org.springframework.security.core.Authentication;

public interface AuthService {
    public Authentication authenticateUser(String username, String password);
}
