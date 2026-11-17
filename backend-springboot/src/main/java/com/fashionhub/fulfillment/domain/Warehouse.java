package com.fashionhub.fulfillment.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "warehouses")
@Getter @Setter @NoArgsConstructor
public class Warehouse {
    @Id
    private String id;
    private String name;
    private String city;

    public Warehouse(String id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }
}
