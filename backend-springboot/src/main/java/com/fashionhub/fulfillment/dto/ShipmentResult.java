package com.fashionhub.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** Element of the multi-instance output collection `shipmentResults`. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShipmentResult(String warehouseId, String warehouseName, String trackingNumber, String carrier,
                             String labelUrl, String status, int itemsShipped, int itemsRefunded,
                             String resolution, BigDecimal refundTotal) {
}
