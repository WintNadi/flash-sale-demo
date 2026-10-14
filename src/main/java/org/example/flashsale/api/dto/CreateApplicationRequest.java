package org.example.flashsale.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank(message = "Business name is required")
        @Size(min = 2, max = 255, message = "Business name must be 2-255 characters")
        String businessName,

        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 2000, message = "Description must be 20-2000 characters")
        String description
) {}