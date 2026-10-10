package org.example.flashsale.service;

import org.example.flashsale.exception.SaleNotActiveException;
import org.example.flashsale.exception.SoldOutException;
import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.Order;
import org.example.flashsale.repository.FlashSaleRepository;
import org.example.flashsale.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FlashSaleService {

    private final FlashSaleRepository saleRepo;
    private final OrderRepository orderRepo;

    public FlashSaleService(FlashSaleRepository saleRepo, OrderRepository orderRepo) {
        this.saleRepo = saleRepo;
        this.orderRepo = orderRepo;
    }

    public FlashSale getSale(long id) {
        return saleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sale not found: " + id));
    }

    public List<FlashSale> getAllSales() {
        return saleRepo.findAll();
    }

    public List<Order> getOrdersForUser(long userId) {
        return orderRepo.findByUserId(userId);
    }

    /**
     * Reserve stock AND persist the order.
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

        double totalPrice = quantity * sale.getSalePrice();
        long orderId = orderRepo.insert(userId, flashSaleId, quantity, totalPrice);

        return orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found after insert"));
    }
}