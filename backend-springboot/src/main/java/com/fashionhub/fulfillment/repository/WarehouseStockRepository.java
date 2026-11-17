package com.fashionhub.fulfillment.repository;

import com.fashionhub.fulfillment.domain.WarehouseStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    List<WarehouseStock> findBySku(String sku);

    Optional<WarehouseStock> findBySkuAndWarehouseId(String sku, String warehouseId);
}
