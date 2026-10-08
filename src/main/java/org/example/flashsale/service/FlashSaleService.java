package org.example.flashsale.service;

import org.example.flashsale.exception.SaleNotActiveException;
import org.example.flashsale.exception.SoldOutException;
import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.Order;
import org.example.flashsale.repository.FlashSaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FlashSaleService {

    private final FlashSaleRepository saleRepo;
    private final AtomicLong orderIdGenerator = new AtomicLong(1);

    public FlashSaleService(FlashSaleRepository saleRepo) {
        this.saleRepo = saleRepo;
    }

    public FlashSale getSale(long id) {
        return saleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
    }

    public List<FlashSale> getAllSales() {
        return saleRepo.findAll();
    }

    /**
     * Reserve stock for a user.
     * Order persistence is a placeholder (in-memory ID) until Phase 5.
     */
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

        long orderId = orderIdGenerator.getAndIncrement();
        return new Order(orderId, userId, flashSaleId, quantity,
                quantity * sale.getSalePrice(), now);
    }
}