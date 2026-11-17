package com.fashionhub.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderLine(String sku, String name, int quantity, BigDecimal unitPrice) {
}
