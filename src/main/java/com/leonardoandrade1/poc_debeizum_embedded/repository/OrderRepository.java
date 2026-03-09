package com.leonardoandrade1.poc_debeizum_embedded.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.leonardoandrade1.poc_debeizum_embedded.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
