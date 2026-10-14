package org.example.flashsale.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectApplicationRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(min = 5, max = 500)
        String reason
) {}
