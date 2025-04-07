package com.example.factorio;

public class Category {
    String id; // ID категории
    String name;
    String imageUrl;

    // Конструктор по умолчанию (требуется для Firestore)
    public Category() {
    }

    // Конструктор с параметрами
    public Category(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}