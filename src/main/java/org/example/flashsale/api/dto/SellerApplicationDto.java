package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record SellerApplicationDto(
        long id,
        long userId,
        String userEmail,
        String businessName,
        String description,
        String status,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason,
        LocalDateTime createdAt
) {}