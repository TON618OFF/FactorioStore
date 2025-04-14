package com.example.factorio;

import com.google.firebase.Timestamp;

/**
 * Product - класс, представляющий продукт в системе.
 *
 * Основные функции:
 * - Хранение информации о продукте, включая идентификатор, название, изображение, описание, категорию, цену, количество, рейтинг и статус избранного.
 * - Предоставление методов для получения и изменения полей продукта.
 *
 * Поля:
 * - String id: Уникальный идентификатор продукта.
 * - String name: Название продукта.
 * - String imageUrl: URL изображения продукта.
 * - String description: Описание продукта.
 * - String category: Идентификатор категории продукта.
 * - String categoryName: Название категории продукта.
 * - int price: Цена продукта.
 * - int quantity: Количество доступного продукта.
 * - boolean isFavorite: Статус избранного для продукта.
 * - Timestamp timestamp: Временная метка добавления продукта.
 * - double averageRating: Средний рейтинг продукта.
 *
 * Конструкторы:
 * - Product(): Пустой конструктор, необходимый для работы с Firestore.
 *
 * Методы:
 * - getId(), setId(String): Получение и установка идентификатора продукта.
 * - getName(), setName(String): Получение и установка названия продукта.
 * - getImageUrl(), setImageUrl(String): Получение и установка URL изображения продукта.
 * - getDescription(), setDescription(String): Получение и установка описания продукта.
 * - getCategory(), setCategory(String): Получение и установка идентификатора категории продукта.
 * - getCategoryName(), setCategoryName(String): Получение и установка названия категории продукта.
 * - getPrice(), setPrice(int): Получение и установка цены продукта.
 * - getQuantity(), setQuantity(int): Получение и установка доступного количества продукта.
 * - isFavorite(), setFavorite(boolean): Получение и установка статуса избранного для продукта.
 * - getTimestamp(), setTimestamp(Timestamp): Получение и установка временной метки добавления продукта.
 * - getAverageRating(), setAverageRating(double): Получение и установка среднего рейтинга продукта.
 */

public class Product {
    private String id, name, imageUrl, description, category, categoryName;
    private int price, quantity;
    private boolean isFavorite;
    private Timestamp timestamp;
    private double averageRating; // Новое поле для среднего рейтинга

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }
}