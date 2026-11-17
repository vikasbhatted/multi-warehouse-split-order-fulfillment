package com.fashionhub.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** `labelResult` variable produced by the REST connector's result expression. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LabelResult(String trackingNumber, String labelUrl, String carrier, String labelId,
                          BigDecimal shipmentCost) {
}
