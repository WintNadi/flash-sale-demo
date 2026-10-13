package org.example.flashsale.api;

import org.example.flashsale.api.dto.OrderDto;
import org.example.flashsale.api.dto.PaymentIntentResponse;
import org.example.flashsale.model.Order;
import org.example.flashsale.security.UserPrincipal;
import org.example.flashsale.service.FlashSaleService;
import org.example.flashsale.service.ReservationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final FlashSaleService flashSaleService;
    private final ReservationService reservationService;

    public OrderController(FlashSaleService flashSaleService,
                           ReservationService reservationService) {
        this.flashSaleService = flashSaleService;
        this.reservationService = reservationService;
    }

    @GetMapping("/me")
    public List<OrderDto> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return flashSaleService.getOrdersWithProductForUser(principal.getUserId())
                .stream()
                .map(o -> new OrderDto(
                        o.id(), o.userId(), o.flashSaleId(), o.productName(),
                        o.quantity(), o.totalPrice(), o.status(),
                        "RESERVED".equals(o.status()) ? reservationService.ttlSeconds(o.id()) : -1L,
                        o.createdAt()
                ))
                .toList();
    }

    @PostMapping("/{orderId}/confirm")
    public OrderDto confirm(@PathVariable long orderId,
                            @AuthenticationPrincipal UserPrincipal principal) {
        Order confirmed = flashSaleService.confirmOrder(principal.getUserId(), orderId);
        return fromOrder(confirmed);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderDto cancel(@PathVariable long orderId,
                           @AuthenticationPrincipal UserPrincipal principal) {
        Order cancelled = flashSaleService.cancelOrder(principal.getUserId(), orderId);
        return fromOrder(cancelled);
    }

    @PostMapping("/{orderId}/pay")
    public PaymentIntentResponse pay(@PathVariable long orderId,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        String clientSecret = flashSaleService.initiatePayment(principal.getUserId(), orderId);
        return new PaymentIntentResponse(orderId, clientSecret);
    }

    private OrderDto fromOrder(Order o) {
        return new OrderDto(
                o.getId(),
                o.getUserId(),
                o.getFlashSaleId(),
                "Product #" + o.getFlashSaleId(),
                o.getQuantity(),
                o.getTotalPrice(),
                o.getStatus().name(),
                "RESERVED".equals(o.getStatus().name())
                        ? reservationService.ttlSeconds(o.getId())
                        : -1L,
                o.getCreatedAt()
        );
    }
}