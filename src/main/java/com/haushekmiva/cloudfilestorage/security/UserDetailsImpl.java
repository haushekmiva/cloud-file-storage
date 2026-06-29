package com.haushekmiva.cloudfilestorage.security;

import com.haushekmiva.cloudfilestorage.model.User;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record UserDetailsImpl(User user) implements UserDetails {

    @NonNull
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @NonNull
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

}
