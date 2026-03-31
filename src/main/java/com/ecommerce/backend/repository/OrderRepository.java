package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Get all orders for a specific user
    List<Order> findByUserId(Long userId);

    // Get orders by status
    List<Order> findByStatus(OrderStatus status);

    // Find order by stripe payment intent
    Optional<Order> findByStripePaymentIntentId(String paymentIntentId);
}