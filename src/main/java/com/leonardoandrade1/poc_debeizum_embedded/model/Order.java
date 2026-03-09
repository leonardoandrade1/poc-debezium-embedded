package com.leonardoandrade1.poc_debeizum_embedded.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    private Long id;
    private Long productId;
    private Integer quantity;
    private String customerEmail;
    private LocalDateTime orderDate;
}
