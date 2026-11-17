package com.fashionhub.fulfillment.dto;

import java.math.BigDecimal;
import java.util.Map;

public final class CatalogViews {
    private CatalogViews() {}

    public record ProductView(String sku, String name, String category, BigDecimal price, String substituteSku,
                              Map<String, Integer> stockByWarehouse, int totalStock) {
    }

    public record WarehouseView(String id, String name, String city) {
    }
}
