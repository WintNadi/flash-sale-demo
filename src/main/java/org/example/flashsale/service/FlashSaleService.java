package org.example.flashsale.service;

import org.example.flashsale.api.dto.ReservationDto;
import org.example.flashsale.exception.ReservationExpiredException;
import org.example.flashsale.exception.SaleNotActiveException;
import org.example.flashsale.exception.SoldOutException;
import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.Order;
import org.example.flashsale.repository.FlashSaleRepository;
import org.example.flashsale.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlashSaleService {

    private final FlashSaleRepository saleRepo;
    private final OrderRepository orderRepo;
    private final ReservationService reservationService;
    private final StripeService stripeService;

    // Update the constructor signature:
    public FlashSaleService(FlashSaleRepository saleRepo,
                            OrderRepository orderRepo,
                            ReservationService reservationService,
                            StripeService stripeService) {
        this.saleRepo = saleRepo;
        this.orderRepo = orderRepo;
        this.reservationService = reservationService;
        this.stripeService = stripeService;
    }

    public FlashSale getSale(long id) {
        return saleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
    }

    public List<FlashSale> getAllSales() {
        return saleRepo.findAll();
    }

    public List<Order> getOrdersForUser(long userId) {
        return orderRepo.findByUserId(userId);
    }

    public Order reserve(long userId, long flashSaleId, int quantity) {
        FlashSale sale = getSale(flashSaleId);

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(sale.getStartsAt())) {
            throw new SaleNotActiveException("Sale hasn't started");
        }
        if (now.isAfter(sale.getEndsAt())) {
            throw new SaleNotActiveException("Sale has ended");
        }

        boolean reserved = saleRepo.tryReserve(flashSaleId, quantity);
        if (!reserved) {
            throw new SoldOutException("Not enough stock");
        }

        double totalPrice = quantity * sale.getSalePrice();
        long orderId = orderRepo.insert(userId, flashSaleId, quantity, totalPrice);

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found after insert"));

        // Store the reservation in Redis with a TTL
        reservationService.hold(new ReservationDto(
                order.getId(),
                order.getUserId(),
                order.getFlashSaleId(),
                order.getQuantity(),
                order.getCreatedAt()
        ));

        return order;
    }

    /**
     * Confirm a reservation.
     * Only the owner of the order can confirm it.
     * The order must currently be RESERVED and the Redis reservation must still exist.
     */
    public Order confirmOrder(long userId, long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getUserId() != userId) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (!"RESERVED".equals(order.getStatus().name())) {
            throw new IllegalArgumentException("Order is not in RESERVED state");
        }

        // Reservation must still be active in Redis
        if (reservationService.find(orderId).isEmpty()) {
            throw new ReservationExpiredException("Reservation has expired");
        }

        // Move to CONFIRMED and release the Redis hold
        orderRepo.updateStatus(orderId, "CONFIRMED");
        reservationService.release(orderId);

        return orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found after confirm"));
    }

    /**
     * Initiate payment for an order.
     * Returns the Stripe client_secret.
     * Only the owner can pay, and only if the order is RESERVED and not expired.
     */
    public String initiatePayment(long userId, long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getUserId() != userId) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (!"RESERVED".equals(order.getStatus().name())) {
            throw new IllegalArgumentException("Order is not in RESERVED state");
        }

        if (reservationService.find(orderId).isEmpty()) {
            throw new ReservationExpiredException("Reservation has expired");
        }

        try {
            return stripeService.createPaymentIntent(order);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage(), e);
        }
    }

    /**
     * Confirm an order after successful payment.
     * Called by the webhook handler.
     * Idempotent: if already CONFIRMED, does nothing.
     */
    public void markOrderAsPaid(long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if ("CONFIRMED".equals(order.getStatus().name())) {
            return; // already confirmed — safe to ignore
        }

        if (!"RESERVED".equals(order.getStatus().name())) {
            // Order was expired or cancelled. In production, trigger a refund here.
            throw new IllegalStateException("Cannot confirm order in state: " + order.getStatus());
        }

        orderRepo.updateStatus(orderId, "CONFIRMED");
        reservationService.release(orderId);
    }

    /**
     * Cancel a reservation. Only the owner can cancel.
     * Returns the stock to the pool and deletes the Redis hold.
     */
    public Order cancelOrder(long userId, long orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getUserId() != userId) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (!"RESERVED".equals(order.getStatus().name())) {
            throw new IllegalArgumentException("Only RESERVED orders can be cancelled");
        }

        // Release stock, remove Redis hold, mark order CANCELLED
        saleRepo.release(order.getFlashSaleId(), order.getQuantity());
        reservationService.release(orderId);
        orderRepo.updateStatus(orderId, "CANCELLED");

        return orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found after cancel"));
    }

    public List<OrderRepository.OrderWithProduct> getOrdersWithProductForUser(long userId) {
        return orderRepo.findByUserIdWithProduct(userId);
    }
}