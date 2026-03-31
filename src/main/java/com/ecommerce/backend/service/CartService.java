package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.CartItemRequest;
import com.ecommerce.backend.dto.response.CartItemResponse;
import com.ecommerce.backend.dto.response.CartResponse;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.InsufficientStockException;
import com.ecommerce.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ── Get or Create Cart for User ───────────────────────────────────────
    private Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    // ── Get Cart ─────────────────
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        Cart cart = getOrCreateCart(email);
        return mapToResponse(cart);
    }

    // ── Add Item to Cart ─────────
    @Transactional
    public CartResponse addItem(String email, CartItemRequest request) {

        Cart cart = getOrCreateCart(email);

        // 1. Find product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found with id: " + request.getProductId()));

        // 2. Validate stock
        if (product.getStock() < request.getQuantity()) {
    throw new InsufficientStockException(product.getName(), product.getStock());
        }

        // 3. Check if product already in cart
        var existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            // Validate total quantity against stock
            if (product.getStock() < newQuantity) {
              throw new InsufficientStockException(product.getName(), product.getStock());
            }
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        } else {
            // Add new cart item
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        // Refresh cart
        Cart updatedCart = cartRepository.findById(cart.getId()).get();
        return mapToResponse(updatedCart);
    }

    // ── Update Item Quantity ─────
    @Transactional
    public CartResponse updateItem(String email,
                                   Long productId,
                                   CartItemRequest request) {

        Cart cart = getOrCreateCart(email);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Validate stock
        if (product.getStock() < request.getQuantity()) {
          throw new InsufficientStockException(product.getName(), product.getStock());
        }

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() ->
                        new RuntimeException("Item not found in cart"));

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        Cart updatedCart = cartRepository.findById(cart.getId()).get();
        return mapToResponse(updatedCart);
    }

    // ── Remove Item from Cart ────
    @Transactional
    public CartResponse removeItem(String email, Long productId) {
        Cart cart = getOrCreateCart(email);

        cartItemRepository.deleteByCartIdAndProductId(
                cart.getId(), productId);

        Cart updatedCart = cartRepository.findById(cart.getId()).get();
        return mapToResponse(updatedCart);
    }

    // ── Clear Cart ───────────────
    @Transactional
    public void clearCart(String email) {
        Cart cart = getOrCreateCart(email);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // ── Map Entity to Response ───
    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems()
                .stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        BigDecimal totalPrice = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .totalItems(totalItems)
                .build();
    }

    private CartItemResponse mapItemToResponse(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(item.getProduct().getImageUrl())
                .productPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}