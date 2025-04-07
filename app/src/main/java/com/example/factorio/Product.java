package com.example.factorio;

import java.io.Serializable;

public class Product implements Serializable {
    private String name;
    private String description;
    private int price;
    private String imageUrl;
    private String id;
    private String category; // Изменено с categoryId на category
    private String categoryName;
    private int quantity;
    private boolean isFavorite;

    public Product() {}

    public Product(String name, String description, int price, String imageUrl, String id, String category, String categoryName, int quantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.id = id;
        this.category = category; // Используем category
        this.categoryName = categoryName;
        this.quantity = quantity;
        this.isFavorite = false;
    }

    // Геттеры и сеттеры
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCategory() { return category; } // Изменено с getCategoryId
    public void setCategory(String category) { this.category = category; } // Изменено с setCategoryId
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}