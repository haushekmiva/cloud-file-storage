package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.TestcontainersConfiguration;
import com.haushekmiva.cloudfilestorage.exception.UserAlreadyExistsException;
import com.haushekmiva.cloudfilestorage.model.User;
import com.haushekmiva.cloudfilestorage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@RequiredArgsConstructor
class UserServiceIntegrationTest {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String testUsername = "userTest";
    private final String testPassword = "password123";

    @Test
    void register_savesUserInDatabase() {
        userService.register(testUsername, testPassword);
        Optional<User> user = userRepository.findByUsername(testUsername);
        assertThat(user).isPresent();
    }

    @Test
    void register_withDuplicateUsername_throwsException() {
        userService.register(testUsername, testPassword);

        assertThatThrownBy(() -> userService.register(testUsername, "anotherPassword"))
                .as("UserAlreadyExistsException should be thrown.")
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void register_savesCorrectPasswordHashInDatabase() {
        userService.register(testUsername, testPassword);
        Optional<User> user = userRepository.findByUsername(testUsername);

        String passwordHash = user.get().getPasswordHash();

        assertThat(passwordEncoder.matches(testPassword, passwordHash))
                .as("Password should be save in encrypted form.")
                .isTrue();
    }
}