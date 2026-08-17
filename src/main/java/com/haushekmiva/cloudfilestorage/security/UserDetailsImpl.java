package com.haushekmiva.cloudfilestorage.security;

import com.haushekmiva.cloudfilestorage.model.User;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record UserDetailsImpl(
        Long id,
        String username,
        String password
) implements UserDetails {

    public static UserDetailsImpl fromUser(User user) {
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash()
        );
    }

    @NonNull
    @Override
    public String getUsername() {
        return username;
    }

    @NonNull
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
