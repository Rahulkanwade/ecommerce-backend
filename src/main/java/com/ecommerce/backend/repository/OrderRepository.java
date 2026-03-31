package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    
    List<Order> findByUserId(Long userId);

    
    List<Order> findByStatus(OrderStatus status);

    
    Optional<Order> findByStripePaymentIntentId(String paymentIntentId);
}