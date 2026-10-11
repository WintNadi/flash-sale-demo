package org.example.flashsale.service;

import org.example.flashsale.api.dto.ReservationDto;
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
    private final ReservationService reservationService;

    public FlashSaleService(FlashSaleRepository saleRepo,
                            OrderRepository orderRepo,
                            ReservationService reservationService) {
        this.saleRepo = saleRepo;
        this.orderRepo = orderRepo;
        this.reservationService = reservationService;
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

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found after insert"));

        // Store the reservation in Redis with a TTL
        reservationService.hold(new ReservationDto(
                order.getId(),
                order.getUserId(),
                order.getFlashSaleId(),
                order.getQuantity(),
                order.getCreatedAt()
        ));

        return order;
    }
}