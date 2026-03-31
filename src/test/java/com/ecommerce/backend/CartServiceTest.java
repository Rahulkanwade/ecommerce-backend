package com.ecommerce.backend;

import com.ecommerce.backend.dto.request.CartItemRequest;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.enums.Role;
import com.ecommerce.backend.exception.InsufficientStockException;
import com.ecommerce.backend.repository.*;
import com.ecommerce.backend.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;
    private CartItemRequest cartItemRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@test.com")
                .password("hashedPassword")
                .role(Role.USER)
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("iPhone 15")
                .price(new BigDecimal("999.99"))
                .stock(10)
                .build();

        testCart = Cart.builder()
                .id(1L)
                .user(testUser)
                .items(new ArrayList<>())
                .build();

        cartItemRequest = new CartItemRequest();
        cartItemRequest.setProductId(1L);
        cartItemRequest.setQuantity(2);
    }

    // ── Add Item Tests ───────────

    @Test
    void addItem_Success() {
        // Arrange
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(any()))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenReturn(new CartItem());
        when(cartRepository.findById(1L))
                .thenReturn(Optional.of(testCart));

        // Act
        CartResponse response = cartService.addItem(
                "john@test.com", cartItemRequest);

        // Assert
        assertNotNull(response);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_InsufficientStock_ThrowsException() {
        // Arrange
        testProduct.setStock(1); // Only 1 in stock
        cartItemRequest.setQuantity(5); // Requesting 5

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(any()))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(testProduct));

        // Act & Assert
        assertThrows(InsufficientStockException.class,
                () -> cartService.addItem("john@test.com", cartItemRequest));

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_ProductNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(any()))
                .thenReturn(Optional.of(testCart));
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        cartItemRequest.setProductId(999L);

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> cartService.addItem("john@test.com", cartItemRequest));
    }

    @Test
    void getCart_CreatesNewCartIfNotExists() {
        // Arrange
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(testUser));
        when(cartRepository.findByUser(any()))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any())).thenReturn(testCart);

        // Act
        CartResponse response = cartService.getCart("john@test.com");

        // Assert
        assertNotNull(response);
        verify(cartRepository).save(any(Cart.class));
    }
}