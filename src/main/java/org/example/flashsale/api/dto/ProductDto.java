package org.example.flashsale.api.dto;

public record ProductDto(
        long id,
        String name,
        double regularPrice
) {}