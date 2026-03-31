package com.ecommerce.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private String currency = "USD";

    // URL to redirect after successful payment
    private String successUrl = "http://localhost:8080/api/payments/success";

    // URL to redirect after cancelled payment
    private String cancelUrl = "http://localhost:8080/api/payments/cancel";
}