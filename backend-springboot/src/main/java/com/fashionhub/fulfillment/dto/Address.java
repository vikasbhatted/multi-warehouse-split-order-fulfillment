package com.fashionhub.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Shipping address - used both as REST payload and as the `shippingAddress` process variable. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Address(
        @NotBlank @Size(max = 120) String line1,
        @NotBlank @Size(max = 80) String city,
        @NotBlank @Size(max = 12) String postalCode,
        @NotBlank @Size(min = 2, max = 2) String country) {
}
