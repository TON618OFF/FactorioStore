package com.example.factorio;

import java.util.List;

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

