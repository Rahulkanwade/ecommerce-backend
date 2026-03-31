package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.request.PaymentRequest;
import com.ecommerce.backend.dto.response.PaymentResponse;
import com.ecommerce.backend.entity.Order;
import com.ecommerce.backend.enums.OrderStatus;
import com.ecommerce.backend.repository.OrderRepository;
import com.paypal.api.payments.*;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final APIContext apiContext;
    private final OrderRepository orderRepository;

    // ── Create PayPal Payment ─────────────────────────────────────────────
    @Transactional
    public PaymentResponse createPayment(
            String email,
            PaymentRequest request) throws PayPalRESTException {

        // 1. Get and validate order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException(
                        "Order not found with id: " + request.getOrderId()));

        if (!order.getUser().getEmail().equals(email)) {
            throw new RuntimeException(
                    "You are not authorized to pay for this order");
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new RuntimeException(
                    "Order is already " + order.getStatus().name());
        }

        // 2. Set payment amount
        Amount amount = new Amount();
        amount.setCurrency(request.getCurrency());
        amount.setTotal(String.format("%.2f",
                order.getTotalPrice().doubleValue()));

        // 3. Set transaction details
        Transaction transaction = new Transaction();
        transaction.setDescription("Order #" + order.getId());
        transaction.setAmount(amount);

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction);

        // 4. Set payer (PayPal handles this)
        Payer payer = new Payer();
        payer.setPaymentMethod("paypal");

        // 5. Set redirect URLs
        RedirectUrls redirectUrls = new RedirectUrls();
        redirectUrls.setReturnUrl(request.getSuccessUrl()
                + "?orderId=" + order.getId());
        redirectUrls.setCancelUrl(request.getCancelUrl()
                + "?orderId=" + order.getId());

        // 6. Create payment object
        Payment payment = new Payment();
        payment.setIntent("sale");
        payment.setPayer(payer);
        payment.setTransactions(transactions);
        payment.setRedirectUrls(redirectUrls);

        // 7. Execute payment creation with PayPal
        Payment createdPayment = payment.create(apiContext);

        // 8. Extract approval URL
        String approvalUrl = createdPayment.getLinks().stream()
                .filter(link -> "approval_url".equals(link.getRel()))
                .findFirst()
                .map(Links::getHref)
                .orElseThrow(() -> new RuntimeException(
                        "No approval URL returned from PayPal"));

        // 9. Save PayPal payment ID to order
        order.setStripePaymentIntentId(createdPayment.getId());
        orderRepository.save(order);

        return PaymentResponse.builder()
                .paymentId(createdPayment.getId())
                .approvalUrl(approvalUrl)
                .status(createdPayment.getState())
                .orderId(order.getId())
                .currency(request.getCurrency())
                .amount(order.getTotalPrice().doubleValue())
                .build();
    }

    // ── Execute Payment After User Approves ───────────────────────────────
    @Transactional
    public PaymentResponse executePayment(
            String paymentId,
            String payerId,
            Long orderId) throws PayPalRESTException {

        // 1. Execute payment on PayPal
        Payment payment = new Payment();
        payment.setId(paymentId);

        PaymentExecution execution = new PaymentExecution();
        execution.setPayerId(payerId);

        Payment executedPayment = payment.execute(apiContext, execution);

        // 2. Find and update order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found with id: " + orderId));

        // 3. Update order status
        if ("approved".equals(executedPayment.getState())) {
            order.setStatus(OrderStatus.PAID);
        } else {
            order.setStatus(OrderStatus.FAILED);
        }

        orderRepository.save(order);

        return PaymentResponse.builder()
                .paymentId(executedPayment.getId())
                .status(executedPayment.getState())
                .orderId(order.getId())
                .build();
    }

    // ── Cancel Payment ────────────────────────────────────────────────────
    @Transactional
    public PaymentResponse cancelPayment(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Order not found with id: " + orderId));

        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        return PaymentResponse.builder()
                .status("CANCELLED")
                .orderId(order.getId())
                .build();
    }
}