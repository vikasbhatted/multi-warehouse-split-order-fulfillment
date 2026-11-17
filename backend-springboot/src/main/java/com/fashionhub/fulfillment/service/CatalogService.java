package com.fashionhub.fulfillment.service;

import com.fashionhub.fulfillment.domain.WarehouseStock;
import com.fashionhub.fulfillment.dto.CatalogViews.ProductView;
import com.fashionhub.fulfillment.dto.CatalogViews.WarehouseView;
import com.fashionhub.fulfillment.repository.ProductRepository;
import com.fashionhub.fulfillment.repository.WarehouseRepository;
import com.fashionhub.fulfillment.repository.WarehouseStockRepository;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private final ProductRepository products;
    private final WarehouseRepository warehouses;
    private final WarehouseStockRepository stocks;

    public CatalogService(ProductRepository products, WarehouseRepository warehouses,
                          WarehouseStockRepository stocks) {
        this.products = products;
        this.warehouses = warehouses;
        this.stocks = stocks;
    }

    public List<ProductView> catalog() {
        Map<String, List<WarehouseStock>> bySku = stocks.findAll().stream()
                .collect(Collectors.groupingBy(WarehouseStock::getSku));
        return products.findAll().stream().sorted((a, b) -> a.getSku().compareTo(b.getSku())).map(p -> {
            Map<String, Integer> perWh = new TreeMap<>();
            bySku.getOrDefault(p.getSku(), List.of()).forEach(s -> perWh.put(s.getWarehouseId(), s.getQuantity()));
            int total = perWh.values().stream().mapToInt(Integer::intValue).sum();
            return new ProductView(p.getSku(), p.getName(), p.getCategory(), p.getPrice(), p.getSubstituteSku(),
                    perWh, total);
        }).toList();
    }

    public List<WarehouseView> warehouses() {
        return warehouses.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(w -> new WarehouseView(w.getId(), w.getName(), w.getCity())).toList();
    }
}
