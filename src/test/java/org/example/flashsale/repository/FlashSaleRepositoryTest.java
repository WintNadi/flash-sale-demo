package org.example.flashsale.repository;

import org.example.flashsale.model.FlashSale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FlashSaleRepositoryTest {

    @Autowired FlashSaleRepository saleRepo;
    @Autowired ProductRepository productRepo;

    @Test
    void onlyOneUserCanBuyTheLastItem_onRealDatabase() throws InterruptedException {
        long productId = productRepo.insert("Pixel 9 Test", 799.0);
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
                    if (saleRepo.tryReserve(saleId, 1)) successes.incrementAndGet();
                    else failures.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();
        doneGate.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        FlashSale after = saleRepo.findById(saleId).orElseThrow();

        assertEquals(1, successes.get());
        assertEquals(99, failures.get());
        assertEquals(0, after.getRemainingStock());

        saleRepo.deleteById(saleId);
    }

    @Test
    void exactly100UsersGet100Items_onRealDatabase() throws InterruptedException {
        long productId = productRepo.insert("Pixel 9 Test 2", 799.0);
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

        assertEquals(100, successes.get());
        assertEquals(0, after.getRemainingStock());

        saleRepo.deleteById(saleId);
    }
}