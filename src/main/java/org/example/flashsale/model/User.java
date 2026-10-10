package org.example.flashsale.model;

import java.time.LocalDateTime;

public class User {
    private Long id;
    private final String email;
    private final String passwordHash;
    private final LocalDateTime createdAt;

    public User(Long id, String email, String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }   // ← ADD THIS

    @Override
    public String toString() {
        return "User{id=" + id + ", email=" + email + "}";
    }
}