package com.fashionhub.fulfillment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** One element of the multi-instance collection `warehouseGroups`. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WarehouseGroup(String warehouseId, String warehouseName, List<OrderLine> items) {
}
