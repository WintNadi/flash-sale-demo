package org.example.flashsale.service;

import org.example.flashsale.exception.SoldOutException;
import org.example.flashsale.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FlashSaleServiceTest {

    private FlashSale createActiveSale(int stock) {
        Product p = new Product(1L, "Pixel 9", 799.0);
        FlashSale sale = new FlashSale(1L, p, stock, 499.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1));
        sale.setStatus(FlashSaleStatus.ACTIVE);
        return sale;
    }

    @Test
    void onlyOneUserCanBuyTheLastItem() throws InterruptedException {
        FlashSaleService service = new FlashSaleService();
        FlashSale sale = createActiveSale(1);
        service.addSale(sale);

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final long userId = i + 1;
            pool.submit(() -> {
                try {
                    startGate.await();
                    service.reserve(userId, 1L, 1);
                    successes.incrementAndGet();
                } catch (SoldOutException e) {
                    failures.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, successes.get(), "Exactly one user should win");
        assertEquals(99, failures.get(), "99 should be sold out");
        assertEquals(0, sale.getRemainingStock(), "Stock should be 0, never negative");
    }

    @Test
    void exactly100UsersGet100Items() throws InterruptedException {
        FlashSaleService service = new FlashSaleService();
        FlashSale sale = createActiveSale(100);
        service.addSale(sale);

        int threads = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final long userId = i + 1;
            pool.submit(() -> {
                try {
                    startGate.await();
                    service.reserve(userId, 1L, 1);
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(100, successes.get());
        assertEquals(0, sale.getRemainingStock());
    }
}
