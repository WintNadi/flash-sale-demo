package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record OrderDto(
        long id,
        long userId,
        long flashSaleId,
        String productName,
        int quantity,
        double totalPrice,
        String status,
        long ttlSeconds,
        LocalDateTime createdAt
) {}