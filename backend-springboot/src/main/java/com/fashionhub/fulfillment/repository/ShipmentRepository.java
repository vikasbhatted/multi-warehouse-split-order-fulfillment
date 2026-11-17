package com.fashionhub.fulfillment.repository;

import com.fashionhub.fulfillment.domain.Shipment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByProcessInstanceKey(String processInstanceKey);

    List<Shipment> findByOrderIdOrderByIdAsc(String orderId);

    Optional<Shipment> findByOrderIdAndWarehouseId(String orderId, String warehouseId);
}
