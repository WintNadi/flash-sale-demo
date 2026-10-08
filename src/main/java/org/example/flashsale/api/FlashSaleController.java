package org.example.flashsale.api;

import org.example.flashsale.api.dto.DtoMapper;
import org.example.flashsale.api.dto.FlashSaleDto;
import org.example.flashsale.model.FlashSale;
import org.example.flashsale.service.FlashSaleService;
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
        FlashSale sale = flashSaleService.getSale(id);
        return DtoMapper.toDto(sale);
    }
}
