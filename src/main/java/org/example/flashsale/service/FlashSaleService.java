package org.example.flashsale.service;

import org.example.flashsale.exception.SaleNotActiveException;
import org.example.flashsale.exception.SoldOutException;
import org.example.flashsale.model.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FlashSaleService {

    private final Map<Long, FlashSale> sales = new ConcurrentHashMap<>();
    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong orderIdGen = new AtomicLong(1);

    public void addSale(FlashSale sale) {
        sales.put(sale.getId(), sale);
    }

    public FlashSale getSale(Long id) {
        return sales.get(id);
    }

    public Collection<FlashSale> getAllSales() {
        return sales.values();
    }

    public Collection<Order> getAllOrders() {
        return orders.values();
    }

    /**
     * Try to reserve `quantity` items from a flash sale for a user.
     * Throws SoldOutException if not enough stock.
     */
    public Order reserve(Long userId, Long flashSaleId, int quantity) {
        FlashSale sale = sales.get(flashSaleId);
        if (sale == null) {
            throw new IllegalArgumentException("Sale not found: " + flashSaleId);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(sale.getStartsAt())) {
            throw new SaleNotActiveException("Sale hasn't started yet");
        }
        if (now.isAfter(sale.getEndsAt())) {
            throw new SaleNotActiveException("Sale has ended");
        }

        // ⚠️ The critical section — must be thread-safe!
        synchronized (sale) {
            if (!sale.tryReserve(quantity)) {
                throw new SoldOutException(
                        "Not enough stock. Requested: " + quantity
                                + ", available: " + sale.getRemainingStock());
            }
        }

        Order order = new Order(
                orderIdGen.getAndIncrement(),
                userId,
                flashSaleId,
                quantity,
                quantity * sale.getSalePrice(),
                now
        );
        orders.put(order.getId(), order);
        return order;
    }
}