package com.example.factorio;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FavoritesActivity - активность для отображения списка избранных товаров.
 *
 * Основные функции:
 * - Загрузка избранных товаров пользователя из Firestore.
 * - Отображение товаров в RecyclerView с дополнительной информацией о категории и состоянии корзины.
 * - Автоматическое обновление состояния избранных товаров при изменении корзины.
 *
 * Поля:
 * - RecyclerView favoritesRecyclerView: Список избранных товаров.
 * - TextView emptyFavoritesText: Текст для отображения при отсутствии избранных товаров.
 * - ProductAdapter productAdapter: Адаптер для отображения товаров.
 * - List<Product> favoritesList: Список избранных товаров.
 * - FirebaseFirestore db: Ссылка на Firestore для работы с данными.
 * - FirebaseAuth auth: Ссылка на FirebaseAuth для проверки текущего пользователя.
 * - Map<String, String> categoryNames: Карта для хранения названий категорий по их ID.
 *
 * Методы:
 * - onCreate(Bundle): Инициализация активности, настройка интерфейса и загрузка данных.
 * - onDestroy(): Удаляет слушатели изменений корзины при завершении активности.
 * - onCartChanged(List<CartItem>): Обновляет состояние товаров в избранном при изменении корзины.
 * - loadCategories(): Загружает названия категорий из Firestore.
 * - loadFavorites(): Загружает список избранных товаров из Firestore для текущего пользователя.
 * - updateUI(): Обновляет интерфейс в зависимости от состояния списка избранных товаров.
 *
 * Логика:
 * - При загрузке активности сначала загружаются категории, а затем избранные товары.
 * - Если пользователь не авторизован, активность завершается с уведомлением.
 * - Для каждого избранного товара загружается информация из коллекции "products".
 * - Если список избранного пуст, отображается соответствующее сообщение.
 * - Состояние корзины синхронизируется с отображением избранных товаров через CartManager.
 */

public class FavoritesActivity extends AppCompatActivity implements CartManager.OnCartChangedListener {
    private static final String TAG = "FavoritesActivity";
    private RecyclerView favoritesRecyclerView;
    private TextView emptyFavoritesText;
    private ProductAdapter productAdapter;
    private List<Product> favoritesList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Map<String, String> categoryNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view);
        emptyFavoritesText = findViewById(R.id.empty_favorites_text);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        favoritesList = new ArrayList<>();
        categoryNames = new HashMap<>();
        productAdapter = new ProductAdapter(this, favoritesList);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecyclerView.setAdapter(productAdapter);

        CartManager.getInstance().addOnCartChangedListener(this);
        loadCategories();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CartManager.getInstance().removeOnCartChangedListener(this);
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        Log.d(TAG, "Корзина изменилась, элементов: " + cartItems.size());
        for (CartItem item : cartItems) {
            for (int i = 0; i < favoritesList.size(); i++) {
                if (favoritesList.get(i).getId().equals(item.getProductId())) {
                    productAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private void loadCategories() {
        db.collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categoryNames.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            if (name != null) {
                                categoryNames.put(document.getId(), name);
                                Log.d(TAG, "Категория загружена: " + name + ", ID: " + document.getId());
                            }
                        }
                        loadFavorites();
                    } else {
                        Log.e(TAG, "Ошибка загрузки категорий: ", task.getException());
                        Toast.makeText(this, "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
                        loadFavorites();
                    }
                });
    }

    private void loadFavorites() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Войдите, чтобы увидеть избранное", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnSuccessListener(snapshot -> {
                    favoritesList.clear();
                    if (!snapshot.isEmpty()) {
                        Log.d(TAG, "Получено избранных: " + snapshot.size());
                        int totalFavorites = snapshot.size();
                        int[] loadedCount = {0};

                        for (QueryDocumentSnapshot document : snapshot) {
                            String productId = document.getString("productId");
                            if (productId != null) {
                                db.collection("products").document(productId)
                                        .get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                Product product = productDoc.toObject(Product.class);
                                                if (product != null) {
                                                    product.setId(productId);
                                                    product.setFavorite(true);
                                                    String categoryId = product.getCategory();
                                                    String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                                    product.setCategoryName(categoryName);
                                                    Long quantity = productDoc.getLong("quantity");
                                                    product.setQuantity(quantity != null ? quantity.intValue() : 0);
                                                    favoritesList.add(product);
                                                    Log.d(TAG, "Добавлен товар: " + product.getName() + ", ID: " + productId);
                                                }
                                            }
                                            loadedCount[0]++;
                                            if (loadedCount[0] == totalFavorites) {
                                                updateUI();
                                            }
                                        })
                                        .addOnFailureListener(ex -> {
                                            Log.e(TAG, "Ошибка загрузки товара " + productId + ": ", ex);
                                            loadedCount[0]++;
                                            if (loadedCount[0] == totalFavorites) {
                                                updateUI();
                                            }
                                        });
                            } else {
                                loadedCount[0]++;
                                if (loadedCount[0] == totalFavorites) {
                                    updateUI();
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "Список избранного пуст");
                        updateUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка загрузки избранного: ", e);
                    Toast.makeText(this, "Ошибка загрузки избранного", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        productAdapter.notifyDataSetChanged();
        if (favoritesList.isEmpty()) {
            favoritesRecyclerView.setVisibility(View.GONE);
            emptyFavoritesText.setVisibility(View.VISIBLE);
        } else {
            favoritesRecyclerView.setVisibility(View.VISIBLE);
            emptyFavoritesText.setVisibility(View.GONE);
        }
        Log.d(TAG, "UI обновлён, товаров в списке: " + favoritesList.size());
    }
}