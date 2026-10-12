package org.example.flashsale.service;

import org.example.flashsale.exception.ReservationExpiredException;
import org.example.flashsale.model.Order;
import org.example.flashsale.repository.FlashSaleRepository;
import org.example.flashsale.repository.OrderRepository;
import org.example.flashsale.repository.ProductRepository;
import org.example.flashsale.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReservationLifecycleTest {

    @Autowired FlashSaleService flashSaleService;
    @Autowired FlashSaleRepository saleRepo;
    @Autowired ProductRepository productRepo;
    @Autowired UserRepository userRepo;
    @Autowired ReservationService reservationService;

    private long testUserId;

    @BeforeEach
    void setUp() {
        String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
        testUserId = userRepo.insert(uniqueEmail,
                "$2a$10$fakehashfakehashfakehashfakehashfakehashfakehashfake");
    }

    @Test
    void fullReserveConfirmLifecycle() {
        long productId = productRepo.insert("Test Product", 100.0);
        long saleId = saleRepo.insert(productId, 10, 49.99,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusDays(1));

        Order order = flashSaleService.reserve(testUserId, saleId, 1);
        assertNotNull(order.getId());
        assertEquals("RESERVED", order.getStatus().name());

        assertTrue(reservationService.find(order.getId()).isPresent());
        assertTrue(reservationService.ttlSeconds(order.getId()) > 0);

        assertEquals(9, saleRepo.findById(saleId).orElseThrow().getRemainingStock());

        Order confirmed = flashSaleService.confirmOrder(testUserId, order.getId());
        assertEquals("CONFIRMED", confirmed.getStatus().name());

        assertTrue(reservationService.find(order.getId()).isEmpty());
    }

    @Test
    void cancelReleasesStock() {
        long productId = productRepo.insert("Test Product 2", 100.0);
        long saleId = saleRepo.insert(productId, 10, 49.99,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusDays(1));

        Order order = flashSaleService.reserve(testUserId, saleId, 2);
        assertEquals(8, saleRepo.findById(saleId).orElseThrow().getRemainingStock());

        flashSaleService.cancelOrder(testUserId, order.getId());
        assertEquals(10, saleRepo.findById(saleId).orElseThrow().getRemainingStock());
        assertTrue(reservationService.find(order.getId()).isEmpty());
    }

    @Test
    void cannotConfirmExpiredReservation() {
        long productId = productRepo.insert("Test Product 3", 100.0);
        long saleId = saleRepo.insert(productId, 10, 49.99,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusDays(1));

        Order order = flashSaleService.reserve(testUserId, saleId, 1);

        // Manually expire the Redis hold
        reservationService.release(order.getId());

        assertThrows(ReservationExpiredException.class, () ->
                flashSaleService.confirmOrder(testUserId, order.getId())
        );
    }
}