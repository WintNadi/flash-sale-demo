package org.example.flashsale.service;

import org.example.flashsale.exception.SaleNotActiveException;
import org.example.flashsale.exception.SoldOutException;
import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.Order;
import org.example.flashsale.repository.FlashSaleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlashSaleService {

    private final FlashSaleRepository saleRepo;

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
     * Reserves stock using the atomic SQL UPDATE.
     * Thread-safe — no synchronized block needed.
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

        // Placeholder — order persistence comes in a later session
        return new Order(0L, userId, flashSaleId, quantity,
                quantity * sale.getSalePrice(), now);
    }
}