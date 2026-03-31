package com.ecommerce.backend;

import com.ecommerce.backend.dto.request.OrderRequest;
import com.ecommerce.backend.dto.response.OrderResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.enums.Role;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItem testCartItem;
    private Order testOrder;
    private OrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .role(Role.USER)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("iPhone 15")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .build();

        testCartItem = CartItem.builder()
                .id(1L)
                .product(testProduct)
                .quantity(2)
                .build();

        List<CartItem> items = new ArrayList<>();
        items.add(testCartItem);

        testCart = Cart.builder()
                .id(1L)
                .user(testUser)
                .items(items)
                .build();

        testCartItem.setCart(testCart);

        testOrder = Order.builder()
                .id(1L)
                .user(testUser)
                .totalPrice(new BigDecimal("1999.98"))
                .status(OrderStatus.CREATED)
                .shippingAddress("123 Main St")
                .orderItems(new ArrayList<>())
                .build();

        orderRequest = new OrderRequest();
        orderRequest.setShippingAddress("123 Main St, Mumbai");
    }

    // ── Create Order Tests ───────

    @Test
    void createOrder_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(any()))
                .thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(testOrder);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(new OrderItem());
        when(productRepository.save(any(Product.class)))
                .thenReturn(testProduct);
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(testOrder));

        // Act
        OrderResponse response = orderService.createOrder(
                "john@test.com", orderRequest);

        // Assert
        assertNotNull(response);
        assertEquals(OrderStatus.CREATED.name(), response.getStatus());
        verify(orderRepository, atLeastOnce()).save(any(Order.class));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void createOrder_EmptyCart_ThrowsException() {
        // Arrange
        testCart.setItems(new ArrayList<>()); // Empty cart

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(any()))
                .thenReturn(Optional.of(testCart));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(
                        "john@test.com", orderRequest));

        assertEquals("Cannot create order from empty cart", ex.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_InsufficientStock_ThrowsException() {
        // Arrange
        testProduct.setStock(1); // Only 1 in stock
        testCartItem.setQuantity(5); // Requesting 5

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(any()))
                .thenReturn(Optional.of(testCart));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(testOrder);

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> orderService.createOrder(
                        "john@test.com", orderRequest));
    }

    // ── Get Orders Tests ─────────

    @Test
    void getUserOrders_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(orderRepository.findByUserId(1L))
                .thenReturn(List.of(testOrder));

        // Act
        List<OrderResponse> responses = orderService.getUserOrders(
                "john@test.com");

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void getOrderById_UnauthorizedUser_ThrowsException() {
        // Arrange
        User otherUser = User.builder()
                .id(2L)
                .email("other@test.com")
                .build();

        when(userRepository.findByEmail("other@test.com"))
                .thenReturn(Optional.of(otherUser));
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(testOrder));

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.getOrderById("other@test.com", 1L));

        assertTrue(ex.getMessage().contains("not authorized"));
    }

    @Test
    void updateOrderStatus_Success() {
        // Arrange
        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class)))
                .thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.updateOrderStatus(
                1L, OrderStatus.PAID);

        // Assert
        assertNotNull(response);
        verify(orderRepository).save(any(Order.class));
    }
}