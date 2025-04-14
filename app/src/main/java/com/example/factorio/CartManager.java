package com.example.factorio;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Set<OnCartChangedListener> listeners;
    private Set<OnErrorListener> errorListeners;

    private CartManager() {
        cartItems = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        listeners = new HashSet<>();
        errorListeners = new HashSet<>();
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addOnCartChangedListener(OnCartChangedListener listener) {
        listeners.add(listener);
    }

    public void removeOnCartChangedListener(OnCartChangedListener listener) {
        listeners.remove(listener);
    }

    public void addOnErrorListener(OnErrorListener listener) {
        errorListeners.add(listener);
    }

    public void removeOnErrorListener(OnErrorListener listener) {
        errorListeners.remove(listener);
    }

    private void notifyCartChanged() {
        Log.d("CartManager", "Уведомление об изменении корзины, элементов: " + cartItems.size());
        List<CartItem> itemsCopy = new ArrayList<>(cartItems);
        for (OnCartChangedListener listener : listeners) {
            listener.onCartChanged(itemsCopy);
        }
    }

    private void notifyError(String message) {
        for (OnErrorListener listener : errorListeners) {
            listener.onError(message);
        }
    }

    public void addToCart(CartItem item) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.w("CartManager", "Пользователь не авторизован");
            notifyError("Войдите, чтобы добавить товар в корзину");
            return;
        }

        // Проверка на дубликат в локальном списке
        for (CartItem existingItem : cartItems) {
            if (existingItem.getProductId().equals(item.getProductId())) {
                int newQuantity = existingItem.getQuantity() + item.getQuantity();
                updateQuantity(item.getProductId(), newQuantity);
                return;
            }
        }

        db.collection("products").document(item.getProductId())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        int availableQuantity = document.getLong("quantity").intValue();
                        if (availableQuantity >= item.getQuantity()) {
                            cartItems.add(item);
                            updateFirestore(userId, item);
                            Log.d("CartManager", "Товар добавлен: " + item.getProductId() + ", количество: " + item.getQuantity());
                            notifyCartChanged();
                        } else {
                            Log.w("CartManager", "Недостаточно товара: " + item.getProductId());
                            notifyError("Недостаточно товара: " + item.getName());
                        }
                    } else {
                        Log.e("CartManager", "Товар не найден: " + item.getProductId());
                        notifyError("Товар не найден");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CartManager", "Ошибка проверки товара: ", e);
                    notifyError("Ошибка добавления товара");
                });
    }

    public void removeAllFromCart() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(userId).collection("cart").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            doc.getReference().delete();
                        }
                        cartItems.clear();
                        Log.d("CartManager", "Корзина очищена");
                        notifyCartChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CartManager", "Ошибка очистки корзины: ", e);
                        notifyError("Ошибка очистки корзины");
                    });
        }
    }

    public void updateQuantity(String productId, int newQuantity) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.w("CartManager", "Пользователь не авторизован");
            notifyError("Войдите, чтобы изменить корзину");
            return;
        }

        if (newQuantity <= 0) {
            for (int i = 0; i < cartItems.size(); i++) {
                if (cartItems.get(i).getProductId().equals(productId)) {
                    cartItems.remove(i);
                    db.collection("users").document(userId).collection("cart").document(productId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d("CartManager", "Товар удалён: " + productId);
                                notifyCartChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("CartManager", "Ошибка удаления товара: ", e);
                                notifyError("Ошибка удаления товара");
                            });
                    return;
                }
            }
            return;
        }

        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        int availableQuantity = document.getLong("quantity").intValue();
                        if (newQuantity <= availableQuantity) {
                            boolean found = false;
                            for (CartItem item : cartItems) {
                                if (item.getProductId().equals(productId)) {
                                    item.setQuantity(newQuantity);
                                    updateFirestore(userId, item);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                String name = document.getString("name");
                                Long price = document.getLong("price");
                                String imageUrl = document.getString("imageUrl");
                                if (name != null && price != null && imageUrl != null) {
                                    CartItem newItem = new CartItem(productId, name, price.intValue(), newQuantity, imageUrl);
                                    cartItems.add(newItem);
                                    updateFirestore(userId, newItem);
                                }
                            }
                            Log.d("CartManager", "Обновлено количество: " + productId + ", новое: " + newQuantity);
                            notifyCartChanged();
                        } else {
                            Log.w("CartManager", "Запрошено больше, чем в наличии: " + newQuantity + " > " + availableQuantity);
                            notifyError("Нельзя добавить больше, чем есть в наличии");
                        }
                    } else {
                        Log.e("CartManager", "Товар не найден: " + productId);
                        notifyError("Товар не найден");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CartManager", "Ошибка обновления количества: ", e);
                    notifyError("Ошибка изменения количества");
                });
    }

    private void updateFirestore(String userId, CartItem item) {
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("productId", item.getProductId());
        cartData.put("name", item.getName());
        cartData.put("price", item.getPrice());
        cartData.put("quantity", item.getQuantity());
        cartData.put("imageUrl", item.getImageUrl());

        db.collection("users").document(userId).collection("cart").document(item.getProductId())
                .set(cartData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("CartManager", "Корзина обновлена в Firestore: " + item.getProductId());
                })
                .addOnFailureListener(e -> {
                    Log.e("CartManager", "Ошибка обновления Firestore: ", e);
                    notifyError("Ошибка сохранения корзины");
                });
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }

    public int getItemQuantity(String productId) {
        for (CartItem item : cartItems) {
            if (item.getProductId().equals(productId)) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    public void loadCartFromFirestore(OnCartLoadedListener listener) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            cartItems.clear();
            Log.d("CartManager", "Пользователь не авторизован, корзина очищена");
            listener.onCartLoaded(new ArrayList<>());
            notifyCartChanged();
            return;
        }

        db.collection("users").document(userId).collection("cart")
                .get()
                .addOnSuccessListener(snapshot -> {
                    cartItems.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        String productId = doc.getString("productId");
                        String name = doc.getString("name");
                        Long priceLong = doc.getLong("price");
                        Long quantityLong = doc.getLong("quantity");
                        String imageUrl = doc.getString("imageUrl");
                        if (productId != null && name != null && priceLong != null && quantityLong != null && imageUrl != null) {
                            CartItem item = new CartItem(productId, name, priceLong.intValue(), quantityLong.intValue(), imageUrl);
                            cartItems.add(item);
                            Log.d("CartManager", "Добавлен в корзину: " + item.getName() + ", количество: " + item.getQuantity());
                        }
                    }
                    Log.d("CartManager", "Корзина загружена, элементов: " + cartItems.size());
                    listener.onCartLoaded(new ArrayList<>(cartItems));
                    notifyCartChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("CartManager", "Ошибка загрузки корзины: ", e);
                    listener.onCartLoaded(new ArrayList<>());
                    notifyError("Ошибка загрузки корзины");
                });
    }

    public interface OnCartChangedListener {
        void onCartChanged(List<CartItem> cartItems);
    }

    public interface OnCartLoadedListener {
        void onCartLoaded(List<CartItem> items);
    }

    public interface OnErrorListener {
        void onError(String message);
    }
}