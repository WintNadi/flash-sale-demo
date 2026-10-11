package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record OrderDto(
        long id,
        long userId,
        long flashSaleId,
        int quantity,
        double totalPrice,
        String status,
        LocalDateTime createdAt
) {}
