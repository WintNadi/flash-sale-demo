package org.example.flashsale.model;

import java.time.LocalDateTime;

public class FlashSale {
    private final Long id;
    private final Product product;
    private final int totalStock;
    private int remainingStock;              // ⚠️ mutated by multiple threads
    private final double salePrice;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private FlashSaleStatus status;

    public FlashSale(Long id, Product product, int totalStock, double salePrice,
                     LocalDateTime startsAt, LocalDateTime endsAt) {
        this.id = id;
        this.product = product;
        this.totalStock = totalStock;
        this.remainingStock = totalStock;
        this.salePrice = salePrice;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = FlashSaleStatus.SCHEDULED;
    }

    // Getters
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public int getTotalStock() { return totalStock; }
    public int getRemainingStock() { return remainingStock; }
    public double getSalePrice() { return salePrice; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public FlashSaleStatus getStatus() { return status; }

    // Setters for status only (stock changes go through the service)
    public void setStatus(FlashSaleStatus status) { this.status = status; }

    // ⚠️ IMPORTANT: this method is called from multiple threads
    // It must be thread-safe!
    /*public boolean tryReserve(int quantity) {
        if (remainingStock >= quantity) {
            remainingStock -= quantity;
            if (remainingStock == 0) {
                status = FlashSaleStatus.SOLD_OUT;
            }
            return true;
        }
        return false;
    }*/

    public void release(int quantity) {
        remainingStock += quantity;
        if (remainingStock > 0 && status == FlashSaleStatus.SOLD_OUT) {
            status = FlashSaleStatus.ACTIVE;
        }
    }

    @Override
    public String toString() {
        return "FlashSale#" + id + " [" + product.getName() + "] stock="
                + remainingStock + "/" + totalStock + " $" + salePrice + " " + status;
    }

    public void setRemainingStockFromDb(int stock) {
        this.remainingStock = stock;
    }
}