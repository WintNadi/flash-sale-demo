package org.example.flashsale.service;

import org.example.flashsale.model.Order;
import org.example.flashsale.repository.FlashSaleRepository;
import org.example.flashsale.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class ReservationSweeper {

    private static final Logger log = LoggerFactory.getLogger(ReservationSweeper.class);

    private final ReservationService reservationService;
    private final OrderRepository orderRepo;
    private final FlashSaleRepository saleRepo;

    public ReservationSweeper(ReservationService reservationService,
                              OrderRepository orderRepo,
                              FlashSaleRepository saleRepo) {
        this.reservationService = reservationService;
        this.orderRepo = orderRepo;
        this.saleRepo = saleRepo;
    }

    /**
     * Runs every 30 seconds.
     * Finds reservations whose TTL has just expired (or is about to),
     * marks them EXPIRED, and returns the stock.
     */
    @Scheduled(fixedDelay = 30_000)
    public void sweepExpiredReservations() {
        log.debug("Sweeper: checking for expired reservations...");

        Set<String> activeKeys = reservationService.findAllActiveKeys();
        if (activeKeys.isEmpty()) {
            log.debug("Sweeper: no active reservations");
            return;
        }

        log.debug("Sweeper: {} active reservation(s)", activeKeys.size());

        for (String key : activeKeys) {
            long orderId = parseOrderId(key);
            long ttl = reservationService.ttlSeconds(orderId);

            // If TTL is about to expire (< 5 seconds left), release it now
            if (ttl >= 0 && ttl <= 5) {
                releaseOrder(orderId);
            }
        }
    }

    /**
     * Called by the sweeper when a reservation's TTL is up.
     * - Marks the order EXPIRED
     * - Returns stock to the flash sale
     * - Deletes the Redis reservation
     */
    private void releaseOrder(long orderId) {
        Optional<Order> orderOpt = orderRepo.findById(orderId);
        if (orderOpt.isEmpty()) {
            // Order missing — just clean up the key
            reservationService.release(orderId);
            return;
        }

        Order order = orderOpt.get();

        // Only release if still RESERVED (not CONFIRMED)
        if (!"RESERVED".equals(order.getStatus().name())) {
            reservationService.release(orderId);
            return;
        }

        log.info("Sweeper: releasing expired reservation for order {} (sale {}, qty {})",
                orderId, order.getFlashSaleId(), order.getQuantity());

        // 1. Mark order EXPIRED
        orderRepo.updateStatus(orderId, "EXPIRED");

        // 2. Return stock to the sale
        saleRepo.release(order.getFlashSaleId(), order.getQuantity());

        // 3. Remove Redis key
        reservationService.release(orderId);
    }

    private long parseOrderId(String key) {
        // key format: "reservation:123"
        return Long.parseLong(key.substring("reservation:".length()));
    }
}