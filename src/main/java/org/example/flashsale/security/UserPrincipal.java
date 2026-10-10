package org.example.flashsale.security;

import java.security.Principal;

public class UserPrincipal implements Principal {

    private final Long userId;
    private final String email;

    public UserPrincipal(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }

    @Override
    public String getName() {
        return email;
    }
}