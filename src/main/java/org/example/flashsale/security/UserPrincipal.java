package org.example.flashsale.security;

import org.example.flashsale.model.Role;

import java.security.Principal;

public class UserPrincipal implements Principal {

    private final Long userId;
    private final String email;
    private final Role role;

    public UserPrincipal(Long userId, String email, Role role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }

    @Override
    public String getName() {
        return email;
    }
}