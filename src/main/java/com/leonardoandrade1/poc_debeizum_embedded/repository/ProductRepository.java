package com.leonardoandrade1.poc_debeizum_embedded.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.leonardoandrade1.poc_debeizum_embedded.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
