package com.example.factorio;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String productId;
    private String name;
    private int price; // Цена за единицу
    private int quantity; // Количество
    private String imageUrl;

    // Пустой конструктор (обязателен для Firestore)
    public CartItem() {}

    // Конструктор с аргументами
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

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
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

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getTotalPrice() {
        return price * quantity;
    }
}