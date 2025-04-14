package com.example.factorio;

/**
 * User - класс, представляющий пользователя в системе.
 *
 * Основные функции:
 * - Хранение информации о пользователе, включая идентификатор, email, никнейм, дату рождения, адрес и статус администратора.
 * - Предоставление методов для получения и изменения данных пользователя.
 *
 * Поля:
 * - String id: Уникальный идентификатор пользователя.
 * - String email: Email пользователя.
 * - String nickname: Никнейм пользователя.
 * - String birthday: Дата рождения пользователя.
 * - String address: Адрес пользователя.
 * - boolean isAdmin: Флаг, указывающий, является ли пользователь администратором.
 *
 * Конструкторы:
 * - User(): Пустой конструктор, необходимый для работы с Firestore.
 * - User(String, String, String, String, String, boolean): Конструктор для создания объекта с полной информацией.
 *
 * Методы:
 * - getId(), setId(String): Получение и установка идентификатора пользователя.
 * - getEmail(), setEmail(String): Получение и установка email пользователя.
 * - getNickname(), setNickname(String): Получение и установка никнейма пользователя.
 * - getBirthday(), setBirthday(String): Получение и установка даты рождения пользователя.
 * - getAddress(), setAddress(String): Получение и установка адреса пользователя.
 * - isAdmin(), setAdmin(boolean): Получение и установка статуса администратора.
 */
public class User {
    private String id;
    private String email;
    private String nickname;
    private String birthday;
    private String address;
    private boolean isAdmin;

    // Пустой конструктор для Firestore
    public User() {}

    public User(String id, String email, String nickname, String birthday, String address, boolean isAdmin) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.birthday = birthday;
        this.address = address;
        this.isAdmin = isAdmin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}