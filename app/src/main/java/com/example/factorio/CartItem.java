package com.example.factorio;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String productId;
    private String name;
    private int price; // Цена за единицу
    private int quantity; // Количество
    private String imageUrl;

    public CartItem(String productId, String name, int price, int quantity, String imageUrl) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    // Геттеры и сеттеры
    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getTotalPrice() {
        return price * quantity;
    }
}