package com.example.factorio;

import java.util.List;

/**
 * Order - класс, представляющий заказ пользователя.
 *
 * Основные функции:
 * - Хранение информации о заказе, включая идентификатор, email клиента, дату, список товаров, и финансовую информацию.
 * - Предоставление методов для получения и изменения данных заказа.
 *
 * Поля:
 * - String orderId: Уникальный идентификатор заказа.
 * - String email: Email клиента, оформившего заказ.
 * - com.google.firebase.Timestamp timestamp: Временная метка оформления заказа.
 * - List<CartItem> items: Список товаров в заказе.
 * - int subtotal: Сумма стоимости товаров без комиссии.
 * - int commission: Комиссия за обработку заказа.
 * - int totalWithCommission: Итоговая сумма заказа с учетом комиссии.
 * - String paymentMethod: Способ оплаты заказа (например, "card" или "cash").
 *
 * Конструкторы:
 * - Order(): Пустой конструктор, необходимый для работы с Firestore.
 *
 * Методы:
 * - getOrderId(), setOrderId(String): Получение и установка уникального идентификатора заказа.
 * - getEmail(), setEmail(String): Получение и установка email клиента.
 * - getTimestamp(), setTimestamp(com.google.firebase.Timestamp): Получение и установка временной метки заказа.
 * - getItems(), setItems(List<CartItem>): Получение и установка списка товаров в заказе.
 * - getSubtotal(), setSubtotal(int): Получение и установка суммы товаров без комиссии.
 * - getCommission(), setCommission(int): Получение и установка комиссии за заказ.
 * - getTotalWithCommission(), setTotalWithCommission(int): Получение и установка итоговой суммы заказа с комиссией.
 * - getPaymentMethod(), setPaymentMethod(String): Получение и установка способа оплаты.
 */

class Order {
    private String orderId;
    private String email;
    private com.google.firebase.Timestamp timestamp;
    private List<CartItem> items;
    private int subtotal;
    private int commission;
    private int totalWithCommission;
    private String paymentMethod;

    public Order() {} // Пустой конструктор для Firestore

    // Геттеры и сеттеры
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public int getSubtotal() { return subtotal; }
    public void setSubtotal(int subtotal) { this.subtotal = subtotal; }
    public int getCommission() { return commission; }
    public void setCommission(int commission) { this.commission = commission; }
    public int getTotalWithCommission() { return totalWithCommission; }
    public void setTotalWithCommission(int totalWithCommission) { this.totalWithCommission = totalWithCommission; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}

