package org.example.flashsale;

import org.example.flashsale.model.*;
import org.example.flashsale.service.FlashSaleService;
import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {
        FlashSaleService service = new FlashSaleService();

        Product phone = new Product(1L, "Pixel 9", 799.0);

        FlashSale sale = new FlashSale(
                1L, phone, 5, 499.0,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(1)
        );
        sale.setStatus(FlashSaleStatus.ACTIVE);
        service.addSale(sale);

        System.out.println("=== Flash Sale Started ===");
        System.out.println(sale);

        // Simulate 10 buyers, all trying to buy at once
        Thread[] buyers = new Thread[10];
        for (int i = 0; i < 10; i++) {
            final long userId = i + 1;
            buyers[i] = new Thread(() -> {
                try {
                    Order o = service.reserve(userId, 1L, 1);
                    System.out.println("✅ User " + userId + " got an order: " + o);
                } catch (Exception e) {
                    System.out.println("❌ User " + userId + " failed: " + e.getMessage());
                }
            });
        }

        for (Thread t : buyers) t.start();
        for (Thread t : buyers) { try { t.join(); } catch (InterruptedException ignored) {} }

        System.out.println("\n=== Sale Ended ===");
        System.out.println("Final: " + sale);
        System.out.println("Total orders: " + service.getAllOrders().size());
        System.out.println("Expected: 5 orders (stock was 5)");
    }
}