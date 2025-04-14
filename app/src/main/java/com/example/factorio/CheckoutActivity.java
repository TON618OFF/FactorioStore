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
import androidx.core.content.ContextCompat;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CheckoutActivity - активность для оформления заказа.
 *
 * Основные функции:
 * - Проверка доступности товаров для заказа.
 * - Расчет итоговой стоимости с учетом комиссии.
 * - Создание заказа и обновление количества товаров в базе данных.
 * - Очистка корзины после успешного оформления.
 *
 * Поля:
 * - RecyclerView checkoutRecyclerView: Список товаров для оформления.
 * - TextView itemsCountText, totalPriceText, commissionText, commissionAmountText, totalAmountText: Отображение информации о заказе.
 * - RadioGroup paymentMethodGroup: Группа переключателей для выбора способа оплаты.
 * - RadioButton paymentCard, paymentCash: Способы оплаты (карта или наличные).
 * - Button checkoutButton: Кнопка для оформления заказа.
 * - CheckBox termsCheckbox: Чекбокс для подтверждения условий использования.
 * - List<CartItem> cartItems: Список товаров в корзине.
 * - CheckoutAdapter checkoutAdapter: Адаптер для отображения товаров в списке.
 * - FirebaseFirestore db: Ссылка на Firestore для работы с данными.
 * - FirebaseAuth auth: Ссылка на FirebaseAuth для проверки текущего пользователя.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация активности, настройка интерфейса и загрузка данных.
 * - initViews(): Инициализирует элементы интерфейса.
 * - setupListeners(): Настраивает обработчики событий для интерфейса.
 * - updateCheckoutButtonState(boolean): Обновляет состояние кнопки оформления заказа.
 * - updateUI(int): Обновляет отображение информации о заказе.
 * - updateCommissionAndTotal(int, int): Рассчитывает комиссию и итоговую сумму в зависимости от способа оплаты.
 * - checkStockBeforeCheckout(): Проверяет наличие товаров перед оформлением заказа.
 * - checkoutOrder(): Создает заказ и обновляет количество товаров в базе данных.
 * - completeCheckout(String, String, int): Завершает процесс оформления заказа, очищает корзину и возвращает данные результата.
 * - calculateTotalItems(List<CartItem>): Вычисляет общее количество товаров.
 * - calculateTotalPrice(List<CartItem>): Вычисляет общую стоимость товаров.
 *
 * Логика:
 * - Пользователь может выбрать способ оплаты (карта или наличные).
 * - Комиссия добавляется только при оплате картой (5% от общей стоимости).
 * - Перед оформлением заказа проверяется наличие товаров на складе.
 * - Заказ сохраняется в Firestore с обновлением количества товаров.
 * - После успешного оформления корзина очищается, и пользователь получает уведомление.
 */

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";

    private RecyclerView checkoutRecyclerView;
    private TextView itemsCountText, totalPriceText, commissionText, commissionAmountText, totalAmountText;
    private RadioGroup paymentMethodGroup;
    private RadioButton paymentCard, paymentCash;
    private Button checkoutButton;
    private CheckBox termsCheckbox;
    private List<CartItem> cartItems;
    private CheckoutAdapter checkoutAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        updateCheckoutButtonState(false);

        cartItems = (List<CartItem>) getIntent().getSerializableExtra("cart_items");
        if (cartItems == null) cartItems = new ArrayList<>();

        checkoutAdapter = new CheckoutAdapter(cartItems);
        checkoutRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checkoutRecyclerView.setAdapter(checkoutAdapter);

        updateUI(calculateTotalPrice(cartItems));

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
        termsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateCheckoutButtonState(isChecked);
        });

        checkoutButton.setOnClickListener(v -> {
            if (!termsCheckbox.isChecked()) {
                Toast.makeText(this, "Пожалуйста, согласитесь с условиями использования", Toast.LENGTH_SHORT).show();
                return;
            }
            checkStockBeforeCheckout();
        });

        paymentMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int totalPrice = calculateTotalPrice(cartItems);
            updateCommissionAndTotal(totalPrice, checkedId);
        });

        int initialCheckedId = paymentMethodGroup.getCheckedRadioButtonId();
        updateCommissionAndTotal(calculateTotalPrice(cartItems), initialCheckedId);
    }

    private void updateCheckoutButtonState(boolean isChecked) {
        if (isChecked) {
            checkoutButton.setEnabled(true);
            checkoutButton.setText("Оформить заказ");
            checkoutButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.circuit_green));
        } else {
            checkoutButton.setEnabled(false);
            checkoutButton.setText("Согласитесь с условиями");
            checkoutButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.coal_gray));
        }
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
            commission = (int) (totalPrice * 0.05);
            findViewById(R.id.commission_layout).setVisibility(View.VISIBLE);
            commissionAmountText.setText(commission + " ₽");
            totalAmountText.setText((totalPrice + commission) + " ₽");
        } else if (checkedId == R.id.payment_cash) {
            findViewById(R.id.commission_layout).setVisibility(View.GONE);
            commissionAmountText.setText("0 ₽");
            totalAmountText.setText(totalPrice + " ₽");
        }
    }

    private void checkStockBeforeCheckout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Корзина пуста", Toast.LENGTH_SHORT).show();
            return;
        }

        final int[] processedItems = {0};
        AtomicBoolean stockAvailable = new AtomicBoolean(true);

        for (CartItem item : cartItems) {
            String productId = item.getProductId();
            int requestedQuantity = item.getQuantity();

            db.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        processedItems[0]++;
                        if (documentSnapshot.exists()) {
                            Long currentQuantity = documentSnapshot.getLong("quantity");
                            if (currentQuantity == null || currentQuantity < requestedQuantity) {
                                stockAvailable.set(false);
                                Toast.makeText(this, "Товара '" + item.getName() + "' нет в наличии", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            stockAvailable.set(false);
                            Toast.makeText(this, "Товар '" + item.getName() + "' не найден", Toast.LENGTH_LONG).show();
                        }

                        if (processedItems[0] == cartItems.size()) {
                            if (stockAvailable.get()) {
                                checkoutOrder();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        processedItems[0]++;
                        Toast.makeText(this, "Ошибка проверки наличия", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Ошибка Firestore: ", e);
                    });
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

        Map<String, Object> orderData = new HashMap<>();
        orderData.put("userId", user.getUid());
        orderData.put("email", email);
        orderData.put("timestamp", FieldValue.serverTimestamp());
        orderData.put("items", new ArrayList<>(cartItems));
        orderData.put("totalPrice", totalPrice);
        orderData.put("commission", commission);
        orderData.put("finalPrice", finalPrice);
        orderData.put("paymentMethod", paymentCard.isChecked() ? "card" : "cash");

        WriteBatch batch = db.batch();

        for (CartItem item : cartItems) {
            String productId = item.getProductId();
            int orderedQuantity = item.getQuantity();
            batch.update(db.collection("products").document(productId),
                    "quantity", FieldValue.increment(-orderedQuantity));
        }

        db.collection("orders")
                .add(orderData)
                .addOnSuccessListener(documentReference -> {
                    String orderId = documentReference.getId();
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Количество товаров обновлено");
                                completeCheckout(orderId, email, commission);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Ошибка обновления товаров: ", e);
                                Toast.makeText(this, "Ошибка при обновлении товаров", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка создания заказа: ", e);
                    Toast.makeText(this, "Ошибка создания заказа", Toast.LENGTH_SHORT).show();
                });
    }

    private void completeCheckout(String orderId, String email, int commission) {
        CartManager.getInstance().removeAllFromCart();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("order_id", orderId);
        resultIntent.putExtra("email", email);
        resultIntent.putExtra("cart_items", new ArrayList<>(cartItems));
        resultIntent.putExtra("commission", commission);
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