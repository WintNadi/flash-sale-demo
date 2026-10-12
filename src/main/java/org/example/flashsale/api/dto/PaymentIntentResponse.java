package org.example.flashsale.api.dto;

public record PaymentIntentResponse(
        long orderId,
        String clientSecret
) {}
