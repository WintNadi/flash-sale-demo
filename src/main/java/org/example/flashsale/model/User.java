package org.example.flashsale.model;

public class User {
    private final Long id;
    private final String email;

    public User(Long id, String email) {
        this.id = id;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }

    @Override
    public String toString() {
        return "User{" + id + ", " + email + "}";
    }
}