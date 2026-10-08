package org.example.flashsale.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveRequest(
        @NotNull @Min(1) Long userId,
        @Min(1) int quantity
) {}