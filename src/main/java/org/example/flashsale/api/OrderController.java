package org.example.flashsale.api;

import org.example.flashsale.api.dto.OrderDto;
import org.example.flashsale.api.dto.PaymentIntentResponse;
import org.example.flashsale.model.Order;
import org.example.flashsale.security.UserPrincipal;
import org.example.flashsale.service.FlashSaleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final FlashSaleService flashSaleService;

    public OrderController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @PostMapping("/{orderId}/confirm")
    public OrderDto confirm(@PathVariable long orderId,
                            @AuthenticationPrincipal UserPrincipal principal) {
        Order confirmed = flashSaleService.confirmOrder(principal.getUserId(), orderId);
        return toDto(confirmed);
    }

    @PostMapping("/{orderId}/pay")
    public PaymentIntentResponse pay(@PathVariable long orderId,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        String clientSecret = flashSaleService.initiatePayment(principal.getUserId(), orderId);
        return new PaymentIntentResponse(orderId, clientSecret);
    }

    private OrderDto toDto(Order order) {
        return new OrderDto(
                order.getId(),
                order.getUserId(),
                order.getFlashSaleId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt()
        );
    }
}