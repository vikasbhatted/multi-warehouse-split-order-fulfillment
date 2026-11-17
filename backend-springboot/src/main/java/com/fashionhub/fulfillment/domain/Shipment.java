package com.fashionhub.fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One parcel = one warehouse sub-workflow (child process instance). */
@Entity
@Table(name = "shipments")
@Getter @Setter @NoArgsConstructor
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String orderId;
    private String warehouseId;
    private String warehouseName;
    /** Key of the warehouse_pick_pack_ship child process instance. */
    @Column(unique = true)
    private String processInstanceKey;
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status = ShipmentStatus.REGISTERED;
    private double parcelWeightKg;
    private String resolution = "NONE";
    private String trackingNumber;
    private String carrier;
    @Column(length = 500)
    private String labelUrl;
    private Instant createdAt = Instant.now();
    private Instant shippedAt;
}
