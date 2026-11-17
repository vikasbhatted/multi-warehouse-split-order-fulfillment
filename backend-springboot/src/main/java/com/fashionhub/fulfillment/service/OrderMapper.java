package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.domain.CustomerOrder;
import com.fashionhub.fulfillment.domain.OrderItem;
import com.fashionhub.fulfillment.domain.RefundRecord;
import com.fashionhub.fulfillment.domain.Shipment;
import com.fashionhub.fulfillment.dto.Address;
import com.fashionhub.fulfillment.dto.OrderViews.ItemView;
import com.fashionhub.fulfillment.dto.OrderViews.OrderDetail;
import com.fashionhub.fulfillment.dto.OrderViews.OrderSummary;
import com.fashionhub.fulfillment.dto.OrderViews.RefundView;
import com.fashionhub.fulfillment.dto.OrderViews.ShipmentView;
import com.fashionhub.fulfillment.repository.RefundRepository;
import com.fashionhub.fulfillment.repository.ShipmentRepository;
import org.springframework.stereotype.Component;

/** Entity -> read-model mapping. Must be called inside a transaction (lazy item collection). */
@Component
public class OrderMapper {

    private final ShipmentRepository shipments;
    private final RefundRepository refunds;

    public OrderMapper(ShipmentRepository shipments, RefundRepository refunds) {
        this.shipments = shipments;
        this.refunds = refunds;
    }

    public OrderSummary toSummary(CustomerOrder o) {
        return new OrderSummary(o.getId(), o.getCustomerName(), o.getStatus().name(), o.getTotalAmount(),
                o.getRefundedAmount(), o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                o.getProcessInstanceKey(), o.getCreatedAt());
    }

    public OrderDetail toDetail(CustomerOrder o) {
        return new OrderDetail(o.getId(), o.getCustomerName(), o.getCustomerEmail(), o.getStatus().name(),
                o.getTotalAmount(), o.getRefundedAmount(), o.getProcessInstanceKey(), o.getRejectionReason(),
                o.isSimulateRefundFailure(),
                new Address(o.getShippingLine1(), o.getShippingCity(), o.getShippingPostalCode(),
                        o.getShippingCountry()),
                o.getCreatedAt(), o.getUpdatedAt(),
                o.getItems().stream().map(this::toItem).toList(),
                shipments.findByOrderIdOrderByIdAsc(o.getId()).stream().map(this::toShipment).toList(),
                refunds.findByOrderIdOrderByIdAsc(o.getId()).stream().map(this::toRefund).toList());
    }

    public ItemView toItem(OrderItem i) {
        return new ItemView(i.getId(), i.getSku(), i.getName(), i.getQuantity(), i.getUnitPrice(),
                i.getWarehouseId(), i.getStatus().name(), i.getProposedSubstituteSku(), i.getDamageReason());
    }

    public ShipmentView toShipment(Shipment s) {
        return new ShipmentView(s.getId(), s.getWarehouseId(), s.getWarehouseName(), s.getProcessInstanceKey(),
                s.getStatus().name(), s.getResolution(), s.getTrackingNumber(), s.getCarrier(), s.getLabelUrl(),
                s.getParcelWeightKg(), s.getShippedAt());
    }

    public RefundView toRefund(RefundRecord r) {
        return new RefundView(r.getSku(), r.getAmount(), r.getType(), r.getReference(), r.getReason(),
                r.getCreatedAt());
    }
}
