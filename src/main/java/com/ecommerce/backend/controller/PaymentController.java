package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.service.PaymentService;
import com.paypal.base.rest.PayPalRESTException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── POST /api/payments/create ─────────────────────────────────────────
    // Creates PayPal payment & returns approvalUrl
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentRequest request)
            throws PayPalRESTException {

        return ResponseEntity.ok(
                paymentService.createPayment(
                        userDetails.getUsername(), request));
    }

    // ── GET /api/payments/success ─────────────────────────────────────────
    // PayPal redirects here after user approves
    @GetMapping("/success")
    public ResponseEntity<PaymentResponse> paymentSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            @RequestParam("orderId") Long orderId)
            throws PayPalRESTException {

        return ResponseEntity.ok(
                paymentService.executePayment(paymentId, payerId, orderId));
    }

    // ── GET /api/payments/cancel ──────────────────────────────────────────
    // PayPal redirects here if user cancels
    @GetMapping("/cancel")
    public ResponseEntity<PaymentResponse> paymentCancel(
            @RequestParam("orderId") Long orderId) {

        return ResponseEntity.ok(
                paymentService.cancelPayment(orderId));
    }
}