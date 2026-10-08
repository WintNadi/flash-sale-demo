package org.example.flashsale.model;

public class Product {
    private final Long id;
    private final String name;
    private final double regularPrice;

    public Product(Long id, String name, double regularPrice) {
        this.id = id;
        this.name = name;
        this.regularPrice = regularPrice;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getRegularPrice() { return regularPrice; }

    @Override
    public String toString() {
        return "Product{" + name + ", $" + regularPrice + "}";
    }
}