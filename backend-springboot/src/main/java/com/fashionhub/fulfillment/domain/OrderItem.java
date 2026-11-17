package com.fashionhub.fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "order_items")
// Only changed columns are written: parallel job workers / task completions update different
// columns of the same row and must not overwrite each other with stale values.
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;
    private String sku;
    private String name;
    private int quantity;
    @Column(precision = 12, scale = 2)
    private BigDecimal unitPrice;
    private String warehouseId;
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.ALLOCATED;
    private String proposedSubstituteSku;
    private String damageReason;

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
