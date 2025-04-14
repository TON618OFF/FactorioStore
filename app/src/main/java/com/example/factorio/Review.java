package com.example.factorio;

/**
 * Review - класс, представляющий отзыв о продукте.
 *
 * Основные функции:
 * - Хранение информации об отзыве: никнейм автора, текст отзыва, рейтинг и идентификатор пользователя.
 *
 * Поля:
 * - String nickname: Никнейм автора отзыва.
 * - String text: Текст отзыва.
 * - int rating: Рейтинг, выставленный автором (например, от 1 до 5).
 * - String userId: Уникальный идентификатор пользователя, оставившего отзыв.
 *
 * Конструкторы:
 * - Review(String, String, int, String): Инициализация экземпляра отзыва с никнеймом, текстом, рейтингом и userId.
 *
 * Методы:
 * - getNickname(): Возвращает никнейм автора отзыва.
 * - getText(): Возвращает текст отзыва.
 * - getRating(): Возвращает рейтинг отзыва.
 * - getUserId(): Возвращает идентификатор пользователя, оставившего отзыв.
 */
public class Review {
    String nickname;
    String text;
    int rating;
    String userId; // Добавляем userId для идентификации автора

    public Review(String nickname, String text, int rating, String userId) {
        this.nickname = nickname;
        this.text = text;
        this.rating = rating;
        this.userId = userId;
    }

    public String getNickname() { return nickname; }
    public String getText() { return text; }
    public int getRating() { return rating; }
    public String getUserId() { return userId; }
}