package org.example.flashsale.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final LocalDateTime createdAt;

    public User(Long id, String email, String passwordHash, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + ", role=" + role + "}";
    }
}