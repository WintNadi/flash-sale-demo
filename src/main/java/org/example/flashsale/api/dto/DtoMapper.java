package org.example.flashsale.api.dto;

import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.Product;

public class DtoMapper {

    public static ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getRegularPrice());
    }

    public static FlashSaleDto toDto(FlashSale fs) {
        double regular = fs.getProduct().getRegularPrice();
        double sale = fs.getSalePrice();
        double discount = regular > 0 ? ((regular - sale) / regular) * 100.0 : 0;

        return new FlashSaleDto(
                fs.getId(),
                toDto(fs.getProduct()),
                fs.getTotalStock(),
                fs.getRemainingStock(),
                sale,
                Math.round(discount * 100.0) / 100.0,   // round to 2 decimals
                fs.getStartsAt(),
                fs.getEndsAt(),
                fs.getStatus().name()
        );
    }
}