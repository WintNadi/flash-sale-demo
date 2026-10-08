package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record ReserveResponse(
        long orderId,
        long userId,
        long flashSaleId,
        int quantity,
        double totalPrice,
        int remainingStock,
        LocalDateTime createdAt
) {}