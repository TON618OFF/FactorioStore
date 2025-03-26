package com.example.factorio;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    private static CartManager instance;
    private List<CartItem> cartItems;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private CartManager() {
        cartItems = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addToCart(CartItem item) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        // Проверяем, есть ли товар уже в корзине
        for (CartItem cartItem : cartItems) {
            if (cartItem.getProductId().equals(item.getProductId())) {
                cartItem.setQuantity(cartItem.getQuantity() + item.getQuantity());
                updateFirestore(userId, cartItem);
                return;
            }
        }
        cartItems.add(item);
        updateFirestore(userId, item);
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
                    });
        }
    }

    public void updateQuantity(String productId, int newQuantity) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) return;

        for (CartItem item : cartItems) {
            if (item.getProductId().equals(productId)) {
                if (newQuantity <= 0) {
                    cartItems.remove(item);
                    db.collection("users").document(userId).collection("cart").document(productId).delete();
                } else {
                    item.setQuantity(newQuantity);
                    updateFirestore(userId, item);
                }
                break;
            }
        }
    }

    private void updateFirestore(String userId, CartItem item) {
        Map<String, Object> cartData = new HashMap<>();
        cartData.put("productId", item.getProductId());
        cartData.put("name", item.getName());
        cartData.put("price", item.getPrice());
        cartData.put("quantity", item.getQuantity());
        cartData.put("imageUrl", item.getImageUrl());

        db.collection("users").document(userId).collection("cart").document(item.getProductId())
                .set(cartData);
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void loadCartFromFirestore(OnCartLoadedListener listener) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            listener.onCartLoaded(new ArrayList<>());
            return;
        }

        db.collection("users").document(userId).collection("cart")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;
                    cartItems.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String productId = doc.getString("productId");
                            String name = doc.getString("name");
                            Long priceLong = doc.getLong("price");
                            Long quantityLong = doc.getLong("quantity");
                            String imageUrl = doc.getString("imageUrl");
                            if (productId != null && name != null && priceLong != null && quantityLong != null && imageUrl != null) {
                                cartItems.add(new CartItem(productId, name, priceLong.intValue(), quantityLong.intValue(), imageUrl));
                            }
                        }
                    }
                    listener.onCartLoaded(cartItems);
                });
    }

    public interface OnCartLoadedListener {
        void onCartLoaded(List<CartItem> items);
    }
}