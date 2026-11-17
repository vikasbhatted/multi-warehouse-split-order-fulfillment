package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.common.ResourceNotFoundException;
import com.fashionhub.fulfillment.common.OrderRejectedException;
import com.fashionhub.fulfillment.domain.CustomerOrder;
import com.fashionhub.fulfillment.domain.ItemStatus;
import com.fashionhub.fulfillment.domain.OrderItem;
import com.fashionhub.fulfillment.domain.OrderStatus;
import com.fashionhub.fulfillment.domain.Product;
import com.fashionhub.fulfillment.domain.RefundRecord;
import com.fashionhub.fulfillment.domain.Shipment;
import com.fashionhub.fulfillment.domain.ShipmentStatus;
import com.fashionhub.fulfillment.domain.Warehouse;
import com.fashionhub.fulfillment.domain.WarehouseStock;
import com.fashionhub.fulfillment.dto.DamagedItem;
import com.fashionhub.fulfillment.dto.LabelResult;
import com.fashionhub.fulfillment.dto.OrderLine;
import com.fashionhub.fulfillment.dto.ShipmentResult;
import com.fashionhub.fulfillment.dto.Substitute;
import com.fashionhub.fulfillment.dto.WarehouseGroup;
import com.fashionhub.fulfillment.repository.CustomerOrderRepository;
import com.fashionhub.fulfillment.repository.ProductRepository;
import com.fashionhub.fulfillment.repository.RefundRepository;
import com.fashionhub.fulfillment.repository.ShipmentRepository;
import com.fashionhub.fulfillment.repository.WarehouseRepository;
import com.fashionhub.fulfillment.repository.WarehouseStockRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic behind every job worker. All methods are idempotent so that a retried job
 * (after a worker crash or timeout) never double-reserves stock or double-refunds an item.
 */
@Service
@Transactional
public class FulfillmentService {
    private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);

    public record ValidationResult(List<WarehouseGroup> groups, BigDecimal total) {
    }

    private final CustomerOrderRepository orders;
    private final ShipmentRepository shipments;
    private final RefundRepository refunds;
    private final ProductRepository products;
    private final WarehouseRepository warehouses;
    private final WarehouseStockRepository stocks;
    private final AllocationService allocation;
    private final PaymentGateway payments;

    public FulfillmentService(CustomerOrderRepository orders, ShipmentRepository shipments, RefundRepository refunds,
                              ProductRepository products, WarehouseRepository warehouses,
                              WarehouseStockRepository stocks, AllocationService allocation,
                              PaymentGateway payments) {
        this.orders = orders;
        this.shipments = shipments;
        this.refunds = refunds;
        this.products = products;
        this.warehouses = warehouses;
        this.stocks = stocks;
        this.allocation = allocation;
        this.payments = payments;
    }

    // ------------------------------------------------------------------ main process
    public ValidationResult validateAndAllocate(String orderId) {
        CustomerOrder o = orders.findById(orderId)
                .orElseThrow(() -> new OrderRejectedException("Order " + orderId + " does not exist"));
        if (o.getStatus() == OrderStatus.CREATED) {
            allocation.allocate(o);
            o.setStatus(OrderStatus.VALIDATED);
            o.touch();
        }
        return new ValidationResult(groupsOf(o), o.getTotalAmount());
    }

    private List<WarehouseGroup> groupsOf(CustomerOrder o) {
        Map<String, List<OrderLine>> byWarehouse = new LinkedHashMap<>();
        for (OrderItem it : o.getItems()) {
            byWarehouse.computeIfAbsent(it.getWarehouseId(), k -> new ArrayList<>())
                    .add(new OrderLine(it.getSku(), it.getName(), it.getQuantity(), it.getUnitPrice()));
        }
        List<WarehouseGroup> groups = new ArrayList<>();
        byWarehouse.forEach((wh, lines) -> groups.add(new WarehouseGroup(wh, warehouseName(wh), lines)));
        return groups;
    }

    private String warehouseName(String id) {
        return warehouses.findById(id).map(Warehouse::getName).orElse(id);
    }

    // ------------------------------------------------------------------ warehouse process
    public Map<String, Object> registerShipment(String orderId, WarehouseGroup group, String processInstanceKey) {
        Shipment s = shipments.findByProcessInstanceKey(processInstanceKey).orElseGet(Shipment::new);
        int qty = group.items().stream().mapToInt(OrderLine::quantity).sum();
        double weight = Math.round(Math.max(0.5, qty * 0.6) * 100.0) / 100.0;
        if (s.getId() == null) {
            s.setOrderId(orderId);
            s.setWarehouseId(group.warehouseId());
            s.setWarehouseName(group.warehouseName());
            s.setProcessInstanceKey(processInstanceKey);
            s.setParcelWeightKg(weight);
            s.setStatus(ShipmentStatus.REGISTERED);
            shipments.save(s);
        }
        CustomerOrder o = order(orderId);
        if (o.getStatus() == OrderStatus.VALIDATED) {
            o.setStatus(OrderStatus.IN_FULFILLMENT);
            o.touch();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("shipmentId", s.getId());
        out.put("parcelWeightKg", s.getParcelWeightKg());
        out.put("resolution", "NONE");
        out.put("refundTotal", BigDecimal.ZERO);
        return out;
    }

    public Map<String, Object> findSubstitutes(String orderId, String warehouseId, List<DamagedItem> damaged) {
        CustomerOrder o = order(orderId);
        List<Substitute> substitutes = new ArrayList<>();
        List<DamagedItem> toRefund = new ArrayList<>();
        for (DamagedItem d : damaged) {
            OrderItem item = item(o, d.sku(), warehouseId);
            Product p = products.findById(d.sku()).orElse(null);
            String subSku = p == null ? null : p.getSubstituteSku();
            boolean available = false;
            if (subSku != null) {
                int free = stocks.findBySkuAndWarehouseId(subSku, warehouseId)
                        .map(WarehouseStock::getQuantity).orElse(0);
                available = free >= item.getQuantity();
            }
            if (available) {
                item.setProposedSubstituteSku(subSku);
                substitutes.add(new Substitute(d.sku(), subSku,
                        products.findById(subSku).map(Product::getName).orElse(subSku)));
            } else {
                toRefund.add(d);
            }
        }
        shipments.findByOrderIdAndWarehouseId(orderId, warehouseId).ifPresent(s -> s.setStatus(ShipmentStatus.DAMAGE_HANDLING));
        log.info("Order {} / {}: {} substitute(s) proposed, {} item(s) to refund", orderId, warehouseId,
                substitutes.size(), toRefund.size());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("substitutes", substitutes);
        out.put("refundItems", toRefund);
        out.put("substitutionApproved", false);
        return out;
    }

    public void applySubstitution(String orderId, List<Substitute> substitutes) {
        CustomerOrder o = order(orderId);
        for (Substitute sub : substitutes) {
            for (OrderItem item : o.getItems()) {
                if (item.getSku().equals(sub.damagedSku()) && item.getStatus() != ItemStatus.SUBSTITUTED) {
                    stocks.findBySkuAndWarehouseId(sub.substituteSku(), item.getWarehouseId())
                            .ifPresent(st -> st.setQuantity(Math.max(0, st.getQuantity() - item.getQuantity())));
                    item.setProposedSubstituteSku(sub.substituteSku());
                    item.setStatus(ItemStatus.SUBSTITUTED);
                }
            }
        }
        o.touch();
    }

    /** Refunds one damaged item. Throws PaymentGatewayException if the gateway refuses. */
    public Map<String, Object> issueRefund(String orderId, String warehouseId, DamagedItem damaged,
                                           boolean simulateFailure) {
        CustomerOrder o = order(orderId);
        RefundRecord existing = refunds.findByOrderIdAndSku(orderId, damaged.sku()).orElse(null);
        if (existing != null) {
            return refundResult(existing);
        }
        OrderItem item = item(o, damaged.sku(), warehouseId);
        return refundResult(refundItem(o, item, damaged.reason(), "ITEM", simulateFailure));
    }

    /** Timer-boundary fallback: refund every damaged item that is not refunded yet. */
    public Map<String, Object> autoRefundDamaged(String orderId, String warehouseId, List<DamagedItem> damaged) {
        CustomerOrder o = order(orderId);
        BigDecimal total = BigDecimal.ZERO;
        for (DamagedItem d : damaged) {
            RefundRecord r = refunds.findByOrderIdAndSku(orderId, d.sku()).orElse(null);
            if (r == null) {
                r = refundItem(o, item(o, d.sku(), warehouseId), d.reason(), "AUTO_TIMEOUT", false);
            }
            total = total.add(r.getAmount());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("resolution", "AUTO_REFUND_TIMEOUT");
        out.put("refundTotal", total);
        return out;
    }

    private RefundRecord refundItem(CustomerOrder o, OrderItem item, String reason, String type, boolean simulate) {
        BigDecimal amount = item.lineTotal();
        String ref = payments.refund(o.getId(), item.getSku(), amount, simulate);
        RefundRecord r = new RefundRecord();
        r.setOrderId(o.getId());
        r.setSku(item.getSku());
        r.setAmount(amount);
        r.setReason(reason);
        r.setType(type);
        r.setReference(ref);
        refunds.save(r);
        item.setStatus(ItemStatus.REFUNDED);
        item.setDamageReason(reason);
        o.setRefundedAmount(o.getRefundedAmount().add(amount));
        o.touch();
        return r;
    }

    private Map<String, Object> refundResult(RefundRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sku", r.getSku());
        result.put("amount", r.getAmount());
        result.put("refundId", r.getReference());
        return Map.of("refundResult", result);
    }

    public ShipmentResult markShipped(String orderId, String warehouseId, LabelResult label, String resolution) {
        CustomerOrder o = order(orderId);
        Shipment s = shipments.findByOrderIdAndWarehouseId(orderId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for " + orderId + "/" + warehouseId));

        for (OrderItem it : o.getItems()) {
            if (!warehouseId.equals(it.getWarehouseId())) {
                continue;
            }
            if (it.getStatus() == ItemStatus.DAMAGED && "MANUAL_REFUND".equals(resolution)) {
                // finance refunded manually after the automatic refund failed
                RefundRecord r = new RefundRecord();
                r.setOrderId(orderId);
                r.setSku(it.getSku());
                r.setAmount(it.lineTotal());
                r.setReason(it.getDamageReason());
                r.setType("MANUAL");
                r.setReference("MANUAL-" + orderId);
                refunds.save(r);
                it.setStatus(ItemStatus.REFUNDED);
                o.setRefundedAmount(o.getRefundedAmount().add(it.lineTotal()));
            } else if (it.getStatus() == ItemStatus.ALLOCATED || it.getStatus() == ItemStatus.PICKED) {
                it.setStatus(ItemStatus.SHIPPED);
            }
        }
        int shipped = 0;
        int refunded = 0;
        BigDecimal refundTotal = BigDecimal.ZERO;
        for (OrderItem it : o.getItems()) {
            if (!warehouseId.equals(it.getWarehouseId())) {
                continue;
            }
            if (it.getStatus() == ItemStatus.SHIPPED || it.getStatus() == ItemStatus.SUBSTITUTED) {
                shipped += it.getQuantity();
            } else if (it.getStatus() == ItemStatus.REFUNDED) {
                refunded += it.getQuantity();
                refundTotal = refundTotal.add(it.lineTotal());
            }
        }
        s.setTrackingNumber(label.trackingNumber());
        s.setCarrier(label.carrier());
        s.setLabelUrl(label.labelUrl());
        s.setResolution(resolution == null ? "NONE" : resolution);
        s.setStatus(ShipmentStatus.SHIPPED);
        s.setShippedAt(Instant.now());
        o.touch();
        log.info("Order {} / {} shipped, tracking {}", orderId, warehouseId, label.trackingNumber());
        return new ShipmentResult(warehouseId, s.getWarehouseName(), label.trackingNumber(), label.carrier(),
                label.labelUrl(), "SHIPPED", shipped, refunded, s.getResolution(), refundTotal);
    }

    // ------------------------------------------------------------------ main process (tail)
    public Map<String, Object> consolidate(String orderId, List<ShipmentResult> results) {
        CustomerOrder o = order(orderId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("parcelCount", results.size());
        out.put("trackingNumbers", results.stream().map(ShipmentResult::trackingNumber).toList());
        out.put("refundedAmount", o.getRefundedAmount());
        return out;
    }

    public Map<String, Object> completeOrder(String orderId) {
        CustomerOrder o = order(orderId);
        boolean refunded = o.getRefundedAmount().compareTo(BigDecimal.ZERO) > 0;
        o.setStatus(refunded ? OrderStatus.PARTIALLY_REFUNDED : OrderStatus.COMPLETED);
        o.touch();
        return Map.of("orderStatus", o.getStatus().name());
    }

    // ------------------------------------------------------------------ helpers
    private CustomerOrder order(String id) {
        return orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderItem item(CustomerOrder o, String sku, String warehouseId) {
        return o.getItems().stream()
                .filter(i -> i.getSku().equals(sku) && warehouseId.equals(i.getWarehouseId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item " + sku + " is not part of order " + o.getId() + " in " + warehouseId));
    }
}
