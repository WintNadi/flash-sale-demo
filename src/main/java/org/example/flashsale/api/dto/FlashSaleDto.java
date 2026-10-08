package org.example.flashsale.api.dto;

import java.time.LocalDateTime;

public record FlashSaleDto(
        long id,
        ProductDto product,
        int totalStock,
        int remainingStock,
        double salePrice,
        double discountPercent,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status
) {}
