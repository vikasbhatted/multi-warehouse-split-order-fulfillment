package com.fashionhub.fulfillment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouse_stock", uniqueConstraints = @UniqueConstraint(columnNames = {"sku", "warehouseId"}))
@Getter @Setter @NoArgsConstructor
public class WarehouseStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private String warehouseId;
    private int quantity;

    public WarehouseStock(String sku, String warehouseId, int quantity) {
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }
}
