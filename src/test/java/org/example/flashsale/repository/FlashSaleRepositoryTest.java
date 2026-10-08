package org.example.flashsale.repository;

import org.example.flashsale.model.FlashSale;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FlashSaleRepositoryTest {

    static FlashSaleRepository saleRepo;
    static ProductRepository productRepo;
    static long productId;

    @BeforeAll
    static void setup() {
        saleRepo = new FlashSaleRepository();
        productRepo = new ProductRepository();
        productId = productRepo.insert("Pixel 9 Test", 799.0);
    }

    @AfterAll
    static void cleanup() {
        Database.shutdown();
    }

    @Test
    void onlyOneUserCanBuyTheLastItem_onRealDatabase() throws InterruptedException {
        // Setup: sale with stock = 1
        long saleId = saleRepo.insert(productId, 1, 499.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1));

        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    boolean reserved = saleRepo.tryReserve(saleId, 1);
                    if (reserved) successes.incrementAndGet();
                    else failures.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();   // 🔫 fire all 100 at once
        boolean finished = doneGate.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(finished, "Test timed out");

        FlashSale after = saleRepo.findById(saleId).orElseThrow();

        assertEquals(1, successes.get(), "Exactly ONE user should win");
        assertEquals(99, failures.get(), "99 should be sold out");
        assertEquals(0, after.getRemainingStock(), "Stock must be 0, never negative");

        // Cleanup
        saleRepo.deleteById(saleId);
    }

    @Test
    void exactly100UsersGet100Items_onRealDatabase() throws InterruptedException {
        long saleId = saleRepo.insert(productId, 100, 499.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1));

        int threads = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    if (saleRepo.tryReserve(saleId, 1)) successes.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        FlashSale after = saleRepo.findById(saleId).orElseThrow();

        assertEquals(100, successes.get(), "Exactly 100 wins");
        assertEquals(0, after.getRemainingStock(), "Stock must be exactly 0");

        saleRepo.deleteById(saleId);
    }
}