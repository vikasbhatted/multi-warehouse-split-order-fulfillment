package com.fashionhub.fulfillment.repository;

import com.fashionhub.fulfillment.domain.RefundRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<RefundRecord, Long> {
    List<RefundRecord> findByOrderIdOrderByIdAsc(String orderId);

    Optional<RefundRecord> findByOrderIdAndSku(String orderId, String sku);
}
