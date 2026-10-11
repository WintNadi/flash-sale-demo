package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record ReservationDto(
        long orderId,
        long userId,
        long flashSaleId,
        int quantity,
        LocalDateTime reservedAt
) {}