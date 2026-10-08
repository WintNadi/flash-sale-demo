package org.example.flashsale.service;

import org.example.flashsale.exception.SaleNotActiveException;
import org.example.flashsale.exception.SoldOutException;
import org.example.flashsale.model.*;
import org.example.flashsale.repository.FlashSaleRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public class FlashSaleService {

    private final FlashSaleRepository saleRepo = new FlashSaleRepository();

    public FlashSale getSale(long id) {
        return saleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
    }

    public Collection<FlashSale> getAllSales() {
        return saleRepo.findAll();
    }

    /**
     * Reserves stock using an atomic SQL UPDATE.
     * No synchronized block needed — the DB handles concurrency.
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

        // ⭐ Atomic reservation — the DB does the locking
        boolean reserved = saleRepo.tryReserve(flashSaleId, quantity);
        if (!reserved) {
            throw new SoldOutException("Not enough stock");
        }

        // Placeholder order — we'll write to DB in the next step
        return new Order(0L, userId, flashSaleId, quantity,
                quantity * sale.getSalePrice(), now);
    }
}