package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.exception.UserAlreadyExistsException;
import com.haushekmiva.cloudfilestorage.model.User;
import com.haushekmiva.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(String username, String password) {

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException(username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        log.info("User registered: username = {}.", username);
    }
}
