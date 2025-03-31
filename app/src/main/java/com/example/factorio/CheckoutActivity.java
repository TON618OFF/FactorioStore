package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";

    private RecyclerView checkoutRecyclerView;
    private TextView itemsCountText, totalPriceText, commissionText, commissionAmountText, totalAmountText;
    private RadioGroup paymentMethodGroup;
    private RadioButton paymentCard, paymentCash;
    private Button checkoutButton;
    private CheckBox termsCheckbox;
    private List<CartItem> cartItems;
    private CartAdapter cartAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Инициализация UI
        initViews();

        // Получение данных из Intent
        cartItems = (List<CartItem>) getIntent().getSerializableExtra("cart_items");
        if (cartItems == null) cartItems = new ArrayList<>();

        // Настройка RecyclerView
        cartAdapter = new CartAdapter(cartItems, null);
        checkoutRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checkoutRecyclerView.setAdapter(cartAdapter);

        // Установка начальных значений
        updateUI(calculateTotalPrice(cartItems));

        // Слушатели
        setupListeners();
    }

    private void initViews() {
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
    }

    private void setupListeners() {
        // Активация кнопки "Оформить заказ" при согласии с условиями
        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> checkoutButton.setEnabled(isChecked));

        // Обновление комиссии при выборе способа оплаты
        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int totalPrice = calculateTotalPrice(cartItems);
            updateCommissionAndTotal(totalPrice, checkedId);
        });

        // Инициализация комиссии для выбранного по умолчанию метода
        int initialCheckedId = paymentMethodGroup.getCheckedRadioButtonId();
        updateCommissionAndTotal(calculateTotalPrice(cartItems), initialCheckedId);

        // Оформление заказа
        checkoutButton.setOnClickListener(v -> checkoutOrder());
    }

    private void updateUI(int totalPrice) {
        int totalItems = calculateTotalItems(cartItems);
        itemsCountText.setText("Товары (" + totalItems + ")");
        totalPriceText.setText(totalPrice + " ₽");
        updateCommissionAndTotal(totalPrice, paymentMethodGroup.getCheckedRadioButtonId());
    }

    private void updateCommissionAndTotal(int totalPrice, int checkedId) {
        int commission = 0;
        if (checkedId == R.id.payment_card) {
            commission = (int) (totalPrice * 0.05); // 5% комиссия для карты
            findViewById(R.id.commission_layout).setVisibility(View.VISIBLE);
            commissionAmountText.setText(commission + " ₽");
            totalAmountText.setText((totalPrice + commission) + " ₽");
        } else if (checkedId == R.id.payment_cash) {
            findViewById(R.id.commission_layout).setVisibility(View.GONE);
            commissionAmountText.setText("0 ₽");
            totalAmountText.setText(totalPrice + " ₽");
        }
    }

    private void checkoutOrder() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Войдите, чтобы оформить заказ", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = user.getEmail();
        if (email == null) {
            Toast.makeText(this, "Email не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        int totalPrice = calculateTotalPrice(cartItems);
        int commission = paymentCard.isChecked() ? (int) (totalPrice * 0.05) : 0;
        int finalPrice = totalPrice + commission;

        // Подготовка данных заказа
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", user.getUid());
        orderData.put("email", email);
        orderData.put("timestamp", FieldValue.serverTimestamp());
        orderData.put("items", new ArrayList<>(cartItems));
        orderData.put("totalPrice", totalPrice);
        orderData.put("commission", commission);
        orderData.put("finalPrice", finalPrice);
        orderData.put("paymentMethod", paymentCard.isChecked() ? "card" : "cash");

        // Создание батча для атомарного обновления
        WriteBatch batch = db.batch();

        // Обновление количества товаров
        for (CartItem item : cartItems) {
            String productId = item.getProductId();
            int orderedQuantity = item.getQuantity();
            batch.update(db.collection("products").document(productId),
                    "quantity", FieldValue.increment(-orderedQuantity));
        }

        // Добавление заказа в коллекцию
        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();

                    // Выполняем батч для обновления товаров
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.i(TAG, "Количество товаров обновлено успешно");
                                completeCheckout(orderId, email, commission);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Ошибка обновления количества товаров: " + e.getMessage());
                                Toast.makeText(this, "Ошибка при обновлении товаров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка создания заказа: " + e.getMessage());
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void completeCheckout(String orderId, String email, int commission) {
        // Очищаем корзину
        CartManager.getInstance().removeAllFromCart();

        // Возвращаем результат в CartPageFragment
        Intent resultIntent = new Intent();
        resultIntent.putExtra("order_id", orderId);
        resultIntent.putExtra("email", email);
        resultIntent.putExtra("cart_items", new ArrayList<>(cartItems));
        resultIntent.putExtra("commission", commission); // Передаём комиссию
        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "Заказ оформлен! ID: " + orderId, Toast.LENGTH_SHORT).show();
        finish();
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