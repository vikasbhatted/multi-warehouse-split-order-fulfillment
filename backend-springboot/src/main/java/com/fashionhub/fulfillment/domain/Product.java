package com.fashionhub.fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id
    private String sku;
    private String name;
    private String category;
    @Column(precision = 12, scale = 2)
    private BigDecimal price;
    /** SKU offered when this product is damaged / unavailable (may be null). */
    private String substituteSku;

    public Product(String sku, String name, String category, BigDecimal price, String substituteSku) {
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.substituteSku = substituteSku;
    }
}
