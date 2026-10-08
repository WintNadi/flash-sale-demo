package org.example.flashsale.api;

import org.example.flashsale.api.dto.ReserveRequest;
import org.example.flashsale.model.FlashSale;
import org.example.flashsale.repository.FlashSaleRepository;
import org.example.flashsale.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlashSaleConcurrencyHttpTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate http;

    @Autowired
    FlashSaleRepository saleRepo;

    @Autowired
    ProductRepository productRepo;

    @Test
    void onlyOneOf500HttpRequestsWinsWhenStockIsOne() throws InterruptedException {
        // Setup: fresh product + sale with stock = 1
        long productId = productRepo.insert("HTTP Test Product", 999.0);
        long saleId = saleRepo.insert(productId, 1, 499.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1));

        int threads = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threads);

        AtomicInteger created = new AtomicInteger();   // 201
        AtomicInteger conflict = new AtomicInteger();  // 409
        AtomicInteger other = new AtomicInteger();     // anything else

        for (int i = 0; i < threads; i++) {
            final long userId = i + 1;
            pool.submit(() -> {
                try {
                    startGate.await();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    ReserveRequest body = new ReserveRequest(userId, 1);
                    HttpEntity<ReserveRequest> entity = new HttpEntity<>(body, headers);

                    ResponseEntity<String> resp = http.exchange(
                            "http://localhost:" + port + "/api/flash-sales/" + saleId + "/reserve",
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

                    if (resp.getStatusCode() == HttpStatus.CREATED) created.incrementAndGet();
                    else if (resp.getStatusCode() == HttpStatus.CONFLICT) conflict.incrementAndGet();
                    else {
                        other.incrementAndGet();
                        System.out.println("UNEXPECTED: " + resp.getStatusCode() + " → " + resp.getBody());
                    }

                } catch (Exception e) {
                    other.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown();   // 🔫 fire all 500 at the same instant
        boolean finished = doneGate.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertTrue(finished, "Test timed out");

        FlashSale after = saleRepo.findById(saleId).orElseThrow();

        System.out.println();
        System.out.println("════════════════════════════════════════");
        System.out.println("  HTTP CONCURRENCY TEST RESULT");
        System.out.println("════════════════════════════════════════");
        System.out.println("  201 Created (won):        " + created.get());
        System.out.println("  409 Conflict (sold out):  " + conflict.get());
        System.out.println("  Other (unexpected):       " + other.get());
        System.out.println("  Remaining stock in DB:    " + after.getRemainingStock());
        System.out.println("════════════════════════════════════════");

        assertEquals(1, created.get(), "Exactly 1 HTTP request should win");
        assertEquals(499, conflict.get(), "499 should get 409 Conflict");
        assertEquals(0, other.get(), "No unexpected responses");
        assertEquals(0, after.getRemainingStock(), "Stock must be exactly 0");

        saleRepo.deleteById(saleId);
    }
}
