package org.example.flashsale.model;

public class Product {
    private final Long id;
    private final String name;
    private final double regularPrice;
    private final String imageUrl;

    public Product(Long id, String name, double regularPrice, String imageUrl) {
        this.id = id;
        this.name = name;
        this.regularPrice = regularPrice;
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getRegularPrice() { return regularPrice; }
    public String getImageUrl() { return imageUrl; }

    @Override
    public String toString() {
        return "Product{" + name + ", $" + regularPrice + "}";
    }
}