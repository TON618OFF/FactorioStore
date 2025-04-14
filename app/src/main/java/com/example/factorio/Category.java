package com.example.factorio;

/**
 * Category - класс, представляющий категорию товаров.
 *
 * Основные функции:
 * - Хранение информации о категории, включая ID, имя и URL изображения.
 * - Предоставление методов для получения и изменения данных категории.
 *
 * Поля:
 * - String id: Уникальный идентификатор категории.
 * - String name: Название категории.
 * - String imageUrl: URL изображения категории.
 *
 * Конструкторы:
 * - Пустой конструктор: Необходим для корректной работы с Firestore.
 * - Конструктор с параметрами: Для создания экземпляра Category с заданными данными.
 *
 * Методы:
 * - getId(): Возвращает уникальный идентификатор категории.
 * - setId(String): Устанавливает уникальный идентификатор категории.
 * - getName(): Возвращает название категории.
 * - setName(String): Устанавливает название категории.
 * - getImageUrl(): Возвращает URL изображения категории.
 * - setImageUrl(String): Устанавливает URL изображения категории.
 */
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