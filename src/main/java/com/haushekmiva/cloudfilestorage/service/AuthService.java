package com.haushekmiva.cloudfilestorage.service;

import org.springframework.security.core.Authentication;

public interface AuthService {
    Authentication authenticateUser(String username, String password);
}
