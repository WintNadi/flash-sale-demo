package org.example.flashsale.service;

import org.example.flashsale.model.User;
import org.example.flashsale.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /** Register a new user. Throws IllegalArgumentException if email taken. */
    public User register(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();

        if (userRepo.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hash = passwordEncoder.encode(rawPassword);
        long id = userRepo.insert(normalizedEmail, hash);

        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("User not found after insert"));
    }

    /**
     * Authenticate a user.
     * Throws IllegalArgumentException on bad credentials — with a GENERIC message.
     * Never reveal whether the email exists or the password was wrong.
     */
    public User login(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new org.example.flashsale.exception.BadCredentialsException(
                        "Invalid email or password"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new org.example.flashsale.exception.BadCredentialsException(
                    "Invalid email or password");
        }

        return user;
    }

    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }
}