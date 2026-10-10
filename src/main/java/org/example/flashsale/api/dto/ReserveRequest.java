package org.example.flashsale.api.dto;

import jakarta.validation.constraints.Min;

public record ReserveRequest(
        @Min(1) int quantity
) {}