package org.example.flashsale.service;

import org.example.flashsale.exception.ForbiddenException;
import org.example.flashsale.model.Product;
import org.example.flashsale.model.Role;
import org.example.flashsale.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepo;

    public ProductService(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    /**
     * Get a product the caller is allowed to manage.
     * Admins can manage any; sellers can only manage their own.
     */
    public Product getManageableProduct(long productId, long userId, Role role) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (role == Role.ADMIN) return product;

        if (product.getSellerId() == null || product.getSellerId() != userId) {
            throw new ForbiddenException("You don't own this product");
        }
        return product;
    }

    public List<Product> getProductsForSeller(long sellerId) {
        return productRepo.findBySellerId(sellerId);
    }

    public Product create(long sellerId, String name, double price, String imageUrl) {
        long id = productRepo.insert(name, price, imageUrl, sellerId);
        return productRepo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Product not found after insert"));
    }

    public Product update(long productId, long userId, Role role,
                          String name, double price, String imageUrl) {
        Product product = getManageableProduct(productId, userId, role);

        if (productRepo.hasFlashSales(productId)) {
            throw new IllegalStateException(
                    "Cannot edit a product that has active or scheduled flash sales");
        }

        productRepo.update(productId, name, price, imageUrl);

        return productRepo.findById(productId)
                .orElseThrow(() -> new IllegalStateException("Product not found after update"));
    }

    public void delete(long productId, long userId, Role role) {
        getManageableProduct(productId, userId, role);   // ownership check

        if (productRepo.hasFlashSales(productId)) {
            throw new IllegalStateException(
                    "Cannot delete a product that has active or scheduled flash sales");
        }

        productRepo.deleteById(productId);
    }
}