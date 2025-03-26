package com.example.factorio;

public class Review {
    String nickname;
    String text;
    int rating;

    public Review(String nickname, String text, int rating) {
        this.nickname = nickname;
        this.text = text;
        this.rating = rating;
    }

    // Геттеры (для доступа к полям)
    public String getNickname() {
        return nickname;
    }

    public String getText() {
        return text;
    }

    public int getRating() {
        return rating;
    }
}