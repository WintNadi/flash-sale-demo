package org.example.flashsale.api.dto;

public record ProductDto(
        long id,
        long sellerId,
        String name,
        double regularPrice,
        String imageUrl
) {}