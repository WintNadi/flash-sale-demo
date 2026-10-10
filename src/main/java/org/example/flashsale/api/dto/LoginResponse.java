package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record LoginResponse(
        String token,
        LocalDateTime expiresAt,
        UserDto user
) {}
