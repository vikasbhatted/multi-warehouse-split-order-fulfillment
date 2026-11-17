package com.fashionhub.fulfillment.worker;

import com.fashionhub.fulfillment.dto.DamagedItem;
import com.fashionhub.fulfillment.dto.LabelResult;
import com.fashionhub.fulfillment.dto.ShipmentResult;
import com.fashionhub.fulfillment.dto.Substitute;
import com.fashionhub.fulfillment.dto.WarehouseGroup;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Typed views on the process variables each worker needs. The Camunda starter fetches only the
 * variables named by these record components (principle of least data).
 */
public final class WorkerVariables {
    private WorkerVariables() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Validate(String orderId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Register(String orderId, WarehouseGroup warehouseGroup) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FindSubstitutes(String orderId, String warehouseId, List<DamagedItem> damagedItems) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplySubstitution(String orderId, List<Substitute> substitutes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Refund(String orderId, String warehouseId, DamagedItem refundItem, Boolean simulateRefundFailure) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AutoRefund(String orderId, String warehouseId, List<DamagedItem> damagedItems) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MarkShipped(String orderId, String warehouseId, LabelResult labelResult, String resolution) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Consolidate(String orderId, List<ShipmentResult> shipmentResults) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Complete(String orderId) {
    }
}
