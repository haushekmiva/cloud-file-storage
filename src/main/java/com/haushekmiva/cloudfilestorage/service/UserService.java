package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.dto.RegisterResponse;

public interface UserService {

    public RegisterResponse register(String username, String password);

}
