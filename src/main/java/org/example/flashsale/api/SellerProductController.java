package org.example.flashsale.api;

import jakarta.validation.Valid;
import org.example.flashsale.api.dto.CreateProductRequest;
import org.example.flashsale.api.dto.DtoMapper;
import org.example.flashsale.api.dto.ProductDto;
import org.example.flashsale.api.dto.UpdateProductRequest;
import org.example.flashsale.model.Product;
import org.example.flashsale.security.UserPrincipal;
import org.example.flashsale.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductDto> myProducts(@AuthenticationPrincipal UserPrincipal principal) {
        return productService.getProductsForSeller(principal.getUserId())
                .stream()
                .map(DtoMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductDto getOne(@PathVariable long id,
                             @AuthenticationPrincipal UserPrincipal principal) {
        Product product = productService.getManageableProduct(
                id, principal.getUserId(), principal.getRole());
        return DtoMapper.toDto(product);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody CreateProductRequest request,
                             @AuthenticationPrincipal UserPrincipal principal) {
        Product product = productService.create(
                principal.getUserId(),
                request.name(),
                request.regularPrice(),
                request.imageUrl()
        );
        return DtoMapper.toDto(product);
    }

    @PutMapping("/{id}")
    public ProductDto update(@PathVariable long id,
                             @Valid @RequestBody UpdateProductRequest request,
                             @AuthenticationPrincipal UserPrincipal principal) {
        Product product = productService.update(
                id, principal.getUserId(), principal.getRole(),
                request.name(), request.regularPrice(), request.imageUrl()
        );
        return DtoMapper.toDto(product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id,
                       @AuthenticationPrincipal UserPrincipal principal) {
        productService.delete(id, principal.getUserId(), principal.getRole());
    }
}