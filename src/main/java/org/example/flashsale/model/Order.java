package org.example.flashsale.model;

import java.time.LocalDateTime;

public class Order {
    private final Long id;
    private final Long userId;
    private final Long flashSaleId;
    private final int quantity;
    private final double totalPrice;
    private final LocalDateTime createdAt;
    private OrderStatus status;

    public Order(Long id, Long userId, Long flashSaleId, int quantity,
                 double totalPrice, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.flashSaleId = flashSaleId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
        this.status = OrderStatus.RESERVED;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getFlashSaleId() { return flashSaleId; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Order#" + id + " user=" + userId + " sale=" + flashSaleId
                + " qty=" + quantity + " $" + totalPrice + " " + status;
    }
}