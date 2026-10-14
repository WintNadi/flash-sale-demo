package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record UserDto(
        long id,
        String email,
        String role,
        LocalDateTime createdAt
) {}