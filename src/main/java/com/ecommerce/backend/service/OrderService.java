package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.OrderRequest;
import com.ecommerce.backend.dto.response.OrderItemResponse;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.exception.InsufficientStockException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

        private final OrderRepository orderRepository;
        private final OrderItemRepository orderItemRepository;
        private final CartRepository cartRepository;
        private final UserRepository userRepository;
        private final ProductRepository productRepository;

        // ── Create Order from Cart ───
        @Transactional
        public OrderResponse createOrder(String email, OrderRequest request) {

                // 1. Get user
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // 2. Get user's cart
                Cart cart = cartRepository.findByUser(user)
                                .orElseThrow(() -> new RuntimeException("Cart not found"));

                // 3. Validate cart is not empty
                if (cart.getItems().isEmpty()) {
                        throw new RuntimeException(
                                        "Cannot create order from empty cart");
                }

                // 4. Build order
                Order order = Order.builder()
                                .user(user)
                                .shippingAddress(request.getShippingAddress())
                                .status(OrderStatus.CREATED)
                                .totalPrice(BigDecimal.ZERO)
                                .build();

                Order savedOrder = orderRepository.save(order);

                // 5. Create order items from cart items
                BigDecimal totalPrice = BigDecimal.ZERO;

                for (CartItem cartItem : cart.getItems()) {
                        Product product = cartItem.getProduct();

                        // Validate stock again at order time
                        if (product.getStock() < cartItem.getQuantity()) {
                               throw new InsufficientStockException(product.getName(), product.getStock());

                        }

                        BigDecimal subtotal = product.getPrice()
                                        .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

                        OrderItem orderItem = OrderItem.builder()
                                        .order(savedOrder)
                                        .product(product)
                                        .quantity(cartItem.getQuantity())
                                        .unitPrice(product.getPrice())
                                        .subtotal(subtotal)
                                        .build();

                        orderItemRepository.save(orderItem);

                        // Deduct stock
                        product.setStock(product.getStock() - cartItem.getQuantity());
                        productRepository.save(product);

                        totalPrice = totalPrice.add(subtotal);
                }

                // 6. Update order total
                savedOrder.setTotalPrice(totalPrice);
                orderRepository.save(savedOrder);

                // 7. Clear the cart after order
                cart.getItems().clear();
                cartRepository.save(cart);

                // 8. Return response
                return getOrderById(email, savedOrder.getId());
        }

        // ── Get All Orders for User ───────────────────────────────────────
        @Transactional(readOnly = true)
        public List<OrderResponse> getUserOrders(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return orderRepository.findByUserId(user.getId())
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        // ── Get Order by ID ──────
        @Transactional(readOnly = true)
        public OrderResponse getOrderById(String email, Long orderId) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Order order = orderRepository.findById(orderId)
                               .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

                if (!order.getUser().getId().equals(user.getId())) {
                        throw new RuntimeException("You are not authorized to view this order");
                }

                return mapToResponse(order);
        }

        // ── Get All Orders (ADMIN) ────────────────────────────────────────
        @Transactional(readOnly = true)
        public List<OrderResponse> getAllOrders() {
                return orderRepository.findAll()
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        // ── Update Order Status (ADMIN) ───────────────────────────────────────
        @Transactional
        public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new RuntimeException("Order not found with id: "
                                                + orderId));
                order.setStatus(status);
                return mapToResponse(orderRepository.save(order));
        }

        // ── Map Entity to Response ───
        public OrderResponse mapToResponse(Order order) {
                List<OrderItemResponse> itemResponses = order.getOrderItems()
                                .stream()
                                .map(this::mapItemToResponse)
                                .collect(Collectors.toList());

                return OrderResponse.builder()
                                .id(order.getId())
                                .userId(order.getUser().getId())
                                .orderItems(itemResponses)
                                .totalPrice(order.getTotalPrice())
                                .status(order.getStatus().name())
                                .shippingAddress(order.getShippingAddress())
                                .stripePaymentIntentId(order.getStripePaymentIntentId())
                                .createdAt(order.getCreatedAt())
                                .updatedAt(order.getUpdatedAt())
                                .build();
        }

        private OrderItemResponse mapItemToResponse(OrderItem item) {
                return OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProduct().getName())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .subtotal(item.getSubtotal())
                                .build();
        }
}