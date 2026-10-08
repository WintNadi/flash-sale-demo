package org.example.flashsale.api;

import jakarta.validation.Valid;
import org.example.flashsale.api.dto.*;
import org.example.flashsale.model.FlashSale;
import org.example.flashsale.model.Order;
import org.example.flashsale.service.FlashSaleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/flash-sales")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @GetMapping
    public List<FlashSaleDto> getAll() {
        return flashSaleService.getAllSales().stream()
                .map(DtoMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public FlashSaleDto getById(@PathVariable long id) {
        return DtoMapper.toDto(flashSaleService.getSale(id));
    }

    @PostMapping("/{id}/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public ReserveResponse reserve(@PathVariable long id,
                                   @Valid @RequestBody ReserveRequest request) {
        Order order = flashSaleService.reserve(request.userId(), id, request.quantity());
        FlashSale after = flashSaleService.getSale(id);

        return new ReserveResponse(
                order.getId(),
                order.getUserId(),
                order.getFlashSaleId(),
                order.getQuantity(),
                order.getTotalPrice(),
                after.getRemainingStock(),
                order.getCreatedAt()
        );
    }
}