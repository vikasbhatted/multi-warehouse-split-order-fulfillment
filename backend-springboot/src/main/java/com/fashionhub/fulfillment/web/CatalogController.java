package com.fashionhub.fulfillment.web;

import com.fashionhub.fulfillment.dto.CatalogViews.ProductView;
import com.fashionhub.fulfillment.dto.CatalogViews.WarehouseView;
import com.fashionhub.fulfillment.service.CatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/catalog")
    public List<ProductView> catalog() {
        return catalog.catalog();
    }

    @GetMapping("/warehouses")
    public List<WarehouseView> warehouses() {
        return catalog.warehouses();
    }
}
