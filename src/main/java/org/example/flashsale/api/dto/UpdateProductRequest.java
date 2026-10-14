package org.example.flashsale.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateProductRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255)
        String name,

        @Positive(message = "Price must be positive")
        double regularPrice,

        @Size(max = 500)
        String imageUrl
) {}
