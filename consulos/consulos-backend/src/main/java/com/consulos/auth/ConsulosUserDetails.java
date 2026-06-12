package com.consulos.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class ConsulosUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final UUID organisationId;
    private final String role;
    private final boolean actif;

    public ConsulosUserDetails(UUID userId,
                               String email,
                               String password,
                               UUID organisationId,
                               String role,
                               boolean actif) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.organisationId = organisationId;
        this.role = role;
        this.actif = actif;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return actif;
    }
}
