package org.example.flashsale.api.dto;

import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.Product;
import org.example.flashsale.model.User;

public class DtoMapper {

    public static ProductDto toDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getSellerId() == null ? 0L : p.getSellerId(),
                p.getName(),
                p.getRegularPrice(),
                p.getImageUrl()
        );
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
                Math.round(discount * 100.0) / 100.0,
                fs.getStartsAt(),
                fs.getEndsAt(),
                fs.getStatus().name()
        );
    }

    public static UserDto toDto(User u) {
        return new UserDto(
                u.getId(),
                u.getEmail(),
                u.getRole().name(),
                u.getCreatedAt()
        );
    }
}