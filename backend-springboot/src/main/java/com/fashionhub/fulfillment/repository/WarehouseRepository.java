package com.fashionhub.fulfillment.repository;

import com.fashionhub.fulfillment.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
}
