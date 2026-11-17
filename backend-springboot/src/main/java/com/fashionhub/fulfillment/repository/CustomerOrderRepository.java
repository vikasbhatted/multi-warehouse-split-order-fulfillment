package com.fashionhub.fulfillment.repository;

import com.fashionhub.fulfillment.domain.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, String> {
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    Optional<CustomerOrder> findByProcessInstanceKey(String processInstanceKey);

    /** Single-column update, so it cannot be lost to a concurrent worker transaction on the same order. */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CustomerOrder o set o.processInstanceKey = :key where o.id = :id")
    int setProcessInstanceKey(@Param("id") String id, @Param("key") String key);
}
