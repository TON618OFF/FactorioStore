package com.example.factorio;

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