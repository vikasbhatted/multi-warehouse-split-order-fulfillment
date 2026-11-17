package com.fashionhub.fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refunds")
@Getter @Setter @NoArgsConstructor
public class RefundRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    private String sku;
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;
    private String reason;
    /** ITEM (sequential refund), AUTO_TIMEOUT or MANUAL. */
    private String type;
    private String reference;
    private Instant createdAt = Instant.now();
}
