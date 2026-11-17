package com.fashionhub.fulfillment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Email @Size(max = 150) String customerEmail,
        @NotNull @Valid Address shippingAddress,
        @NotEmpty @Size(max = 10) List<@Valid @NotNull ItemRequest> items,
        boolean simulateRefundFailure) {

    public record ItemRequest(@NotBlank String sku, @Min(1) @Max(5) int quantity) {
    }
}
