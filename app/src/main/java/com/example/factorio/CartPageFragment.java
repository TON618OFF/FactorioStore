package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartPageFragment extends Fragment {

    private RecyclerView cartRecyclerView;
    private TextView cartItemsCount, cartTotalPrice;
    private ImageButton clearCartButton;
    private Button checkoutButton;
    private CartAdapter cartAdapter;
    private CartManager cartManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart_page, container, false);

        cartRecyclerView = view.findViewById(R.id.cart_recycler_view);
        cartItemsCount = view.findViewById(R.id.cart_items_count);
        cartTotalPrice = view.findViewById(R.id.cart_total_price);
        clearCartButton = view.findViewById(R.id.clear_cart_button);
        checkoutButton = view.findViewById(R.id.checkout_button);

        cartManager = CartManager.getInstance();
        cartAdapter = new CartAdapter(cartManager.getCartItems(), this::updateTotalPrice);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cartRecyclerView.setAdapter(cartAdapter);

        loadCart();

        clearCartButton.setOnClickListener(v -> {
            cartManager.removeAllFromCart();
            updateUI();
        });

        checkoutButton.setOnClickListener(v -> checkout());

        return view;
    }

    private void loadCart() {
        cartManager.loadCartFromFirestore(items -> {
            cartAdapter.notifyDataSetChanged();
            updateUI();
        });
    }

    private void updateUI() {
        List<CartItem> items = cartManager.getCartItems();
        int totalItems = 0;
        int totalPrice = 0;
        for (CartItem item : items) {
            totalItems += item.getQuantity();
            totalPrice += item.getTotalPrice();
        }
        cartItemsCount.setText("Товаров в корзине: " + totalItems);
        cartTotalPrice.setText("Общая цена: " + totalPrice + " руб.");
        cartAdapter.notifyDataSetChanged();
    }

    private void updateTotalPrice() {
        updateUI();
    }

    private void checkout() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Войдите, чтобы оформить заказ", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(getContext(), "Email не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<CartItem> items = cartManager.getCartItems();
        if (items.isEmpty()) {
            Toast.makeText(getContext(), "Корзина пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        // Подготовка данных заказа
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", user.getUid());
        orderData.put("email", email);
        orderData.put("timestamp", FieldValue.serverTimestamp());
        orderData.put("items", items); // Список объектов CartItem
        orderData.put("totalPrice", calculateTotalPrice(items));

        // Сохранение заказа в Firestore
        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();
                    Toast.makeText(getContext(), "Заказ оформлен! ID: " + orderId, Toast.LENGTH_SHORT).show();

                    // Формирование чека для отправки (пока текст)
                    StringBuilder receipt = new StringBuilder();
                    receipt.append("Чек заказа #").append(orderId).append("\n");
                    receipt.append("Email: ").append(email).append("\n");
                    receipt.append("Дата: ").append(new java.util.Date().toString()).append("\n\n");
                    receipt.append("Товары:\n");

                    int totalPrice = 0;
                    for (CartItem item : items) {
                        receipt.append(item.getName())
                                .append(" - ")
                                .append(item.getQuantity())
                                .append(" шт. x ")
                                .append(item.getPrice())
                                .append(" руб. = ")
                                .append(item.getTotalPrice())
                                .append(" руб.\n");
                        totalPrice += item.getTotalPrice();
                    }
                    receipt.append("\nИтого: ").append(totalPrice).append(" руб.");

                    // Отправка чека на почту
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Чек заказа Factorio #" + orderId);
                    emailIntent.putExtra(Intent.EXTRA_TEXT, receipt.toString());

                    try {
                        startActivity(Intent.createChooser(emailIntent, "Отправить чек"));
                        cartManager.removeAllFromCart(); // Очистка корзины
                        updateUI();
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getContext(), "Нет приложений для отправки email", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при оформлении заказа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private int calculateTotalPrice(List<CartItem> items) {
        int totalPrice = 0;
        for (CartItem item : items) {
            totalPrice += item.getTotalPrice();
        }
        return totalPrice;
    }
}