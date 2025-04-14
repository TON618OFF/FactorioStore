package com.example.factorio;

import java.io.Serializable;

/**
 * CartItem - класс, представляющий элемент корзины покупок.
 *
 * Основные функции:
 * - Хранение информации о товаре в корзине, включая ID продукта, название, цену, количество и URL изображения.
 * - Предоставление методов для получения и изменения данных о товаре.
 * - Вычисление общей стоимости товара в корзине (цена за единицу * количество).
 *
 * Поля:
 * - String productId: Уникальный идентификатор товара.
 * - String name: Название товара.
 * - int price: Цена за единицу товара.
 * - int quantity: Количество товара в корзине.
 * - String imageUrl: URL изображения товара.
 *
 * Конструкторы:
 * - Пустой конструктор: Необходим для корректной работы с Firestore.
 * - Конструктор с аргументами: Для создания экземпляра CartItem с заданными значениями.
 *
 * Методы:
 * - Геттеры и сеттеры для всех полей.
 * - getTotalPrice(): Вычисляет и возвращает общую стоимость товара в корзине.
 */

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