package com.fashionhub.fulfillment.repository;

import com.fashionhub.fulfillment.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
}
