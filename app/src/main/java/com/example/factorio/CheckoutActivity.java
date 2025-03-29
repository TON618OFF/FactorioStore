package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private RecyclerView checkoutRecyclerView;
    private TextView itemsCountText, totalPriceText, commissionText, commissionAmountText, totalAmountText;
    private RadioGroup paymentMethodGroup;
    private RadioButton paymentCard, paymentCash;
    private Button checkoutButton;
    private CheckBox termsCheckbox;
    private List<CartItem> cartItems;
    private CartAdapter cartAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Инициализация элементов
        checkoutRecyclerView = findViewById(R.id.checkout_recycler_view);
        itemsCountText = findViewById(R.id.items_count_text);
        totalPriceText = findViewById(R.id.total_price_text);
        commissionText = findViewById(R.id.commission_text);
        commissionAmountText = findViewById(R.id.commission_amount_text);
        totalAmountText = findViewById(R.id.total_amount_text);
        paymentMethodGroup = findViewById(R.id.payment_method_group);
        paymentCard = findViewById(R.id.payment_card);
        paymentCash = findViewById(R.id.payment_cash);
        checkoutButton = findViewById(R.id.checkout_button);
        termsCheckbox = findViewById(R.id.terms_checkbox);

        // Получение данных из Intent
        cartItems = (List<CartItem>) getIntent().getSerializableExtra("cart_items");
        if (cartItems == null) cartItems = new ArrayList<>();

        // Настройка RecyclerView
        cartAdapter = new CartAdapter(cartItems, null);
        checkoutRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checkoutRecyclerView.setAdapter(cartAdapter);

        // Установка значений
        int totalItems = calculateTotalItems(cartItems);
        int totalPrice = calculateTotalPrice(cartItems);
        itemsCountText.setText("Товары (" + totalItems + ")");
        totalPriceText.setText(totalPrice + " ₽");
        totalAmountText.setText(totalPrice + " ₽");

        // Активация кнопки "Оформить заказ" при установке галочки
        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkoutButton.setEnabled(isChecked);
        });

        // Обработка выбора способа оплаты
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateCommissionAndTotal(totalPrice, checkedId);
        });

        // Инициализация комиссии при старте (для выбранного по умолчанию метода)
        int initialCheckedId = paymentMethodGroup.getCheckedRadioButtonId();
        updateCommissionAndTotal(totalPrice, initialCheckedId);

        // Обработка нажатия на кнопку "Оформить заказ"
        checkoutButton.setOnClickListener(v -> checkoutOrder());
    }

    // Метод для обновления комиссии и итоговой суммы
    private void updateCommissionAndTotal(int totalPrice, int checkedId) {
        int commission = 0;
        if (checkedId == R.id.payment_card) {
            commission = (int) (totalPrice * 0.05); // 5% комиссия
            findViewById(R.id.commission_layout).setVisibility(View.VISIBLE);
            commissionAmountText.setText(commission + " ₽");
            totalAmountText.setText((totalPrice + commission) + " ₽");
        } else if (checkedId == R.id.payment_cash) {
            findViewById(R.id.commission_layout).setVisibility(View.GONE);
            totalAmountText.setText(totalPrice + " ₽");
        }
    }

    private void checkoutOrder() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Войдите, чтобы оформить заказ", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(this, "Email не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        int totalPrice = calculateTotalPrice(cartItems);
        int commission = paymentCard.isChecked() ? (int) (totalPrice * 0.05) : 0;
        int finalPrice = totalPrice + commission;

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", user.getUid());
        orderData.put("email", email);
        orderData.put("timestamp", FieldValue.serverTimestamp());
        orderData.put("items", cartItems);
        orderData.put("totalPrice", totalPrice);
        orderData.put("commission", commission);
        orderData.put("finalPrice", finalPrice);
        orderData.put("paymentMethod", paymentCard.isChecked() ? "card" : "cash");

        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();
                    Toast.makeText(this, "Заказ оформлен! ID: " + orderId, Toast.LENGTH_SHORT).show();

                    // Возвращаем результат в CartPageFragment
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("order_id", orderId);
                    resultIntent.putExtra("email", email);
                    resultIntent.putExtra("cart_items", new ArrayList<>(cartItems));
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        // Получаем товары из корзины
        List<CartItem> cartItems = CartManager.getInstance().getCartItems();

        // Обновляем количество для каждого товара
        for (CartItem item : cartItems) {
            String productId = item.getProductId(); // ID товара
            int orderedQuantity = item.getQuantity(); // Количество, заказанное пользователем

            // Получаем текущее количество товара из Firestore
            db.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            int currentQuantity = document.getLong("quantity").intValue();
                            int newQuantity = currentQuantity - orderedQuantity;

                            // Не допускаем отрицательного количества
                            if (newQuantity < 0) {
                                newQuantity = 0;
                            }

                            // Обновляем количество в Firestore
                            db.collection("products").document(productId)
                                    .update("quantity", newQuantity)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.i("CheckoutActivity", "Количество обновлено для товара: " + productId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("CheckoutActivity", "Ошибка при обновлении количества: " + e.getMessage());
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CheckoutActivity", "Ошибка при получении данных товара: " + e.getMessage());
                    });
        }

        // Очищаем корзину после успешного оформления
        CartManager.getInstance().removeAllFromCart();
    }

    private int calculateTotalItems(List<CartItem> items) {
        int totalItems = 0;
        for (CartItem item : items) {
            totalItems += item.getQuantity();
        }
        return totalItems;
    }

    private int calculateTotalPrice(List<CartItem> items) {
        int totalPrice = 0;
        for (CartItem item : items) {
            totalPrice += item.getTotalPrice();
        }
        return totalPrice;
    }
}