package com.haushekmiva.cloudfilestorage.service;

import com.haushekmiva.cloudfilestorage.model.User;
import com.haushekmiva.cloudfilestorage.repository.UserRepository;
import com.haushekmiva.cloudfilestorage.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username).orElseThrow(() ->
                new UsernameNotFoundException("User not found: " + username)
        );
        return new UserDetailsImpl(user);
    }
}
