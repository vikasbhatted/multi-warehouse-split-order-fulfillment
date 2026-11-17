package com.fashionhub.fulfillment.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "customer_orders")
// Only changed columns are written: parallel job workers / task completions update different
// columns of the same row and must not overwrite each other with stale values.
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
public class CustomerOrder {
    @Id
    @Column(length = 40)
    private String id;
    @Column(nullable = false)
    private String customerName;
    @Column(nullable = false)
    private String customerEmail;
    private String shippingLine1;
    private String shippingCity;
    private String shippingPostalCode;
    private String shippingCountry;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;
    private String processInstanceKey;
    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private boolean simulateRefundFailure;
    @Column(length = 500)
    private String rejectionReason;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
