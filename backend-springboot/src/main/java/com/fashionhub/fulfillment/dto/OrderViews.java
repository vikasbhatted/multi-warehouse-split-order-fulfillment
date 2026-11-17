package com.fashionhub.fulfillment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Read models returned by the REST API. */
public final class OrderViews {
    private OrderViews() {}

    public record OrderSummary(String id, String customerName, String status, BigDecimal totalAmount,
                               BigDecimal refundedAmount, int itemCount, String processInstanceKey,
                               Instant createdAt) {
    }

    public record ItemView(Long id, String sku, String name, int quantity, BigDecimal unitPrice, String warehouseId,
                           String status, String proposedSubstituteSku, String damageReason) {
    }

    public record ShipmentView(Long id, String warehouseId, String warehouseName, String processInstanceKey,
                               String status, String resolution, String trackingNumber, String carrier,
                               String labelUrl, double parcelWeightKg, Instant shippedAt) {
    }

    public record RefundView(String sku, BigDecimal amount, String type, String reference, String reason,
                             Instant createdAt) {
    }

    public record OrderDetail(String id, String customerName, String customerEmail, String status,
                              BigDecimal totalAmount, BigDecimal refundedAmount, String processInstanceKey,
                              String rejectionReason, boolean simulateRefundFailure, Address shippingAddress,
                              Instant createdAt, Instant updatedAt, List<ItemView> items,
                              List<ShipmentView> shipments, List<RefundView> refunds) {
    }
}
