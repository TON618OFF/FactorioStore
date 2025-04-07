package com.example.factorio;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoritesActivity extends AppCompatActivity {

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

        loadCategories();
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
                        Log.e(TAG, "Ошибка загрузки категорий: " + task.getException());
                        Toast.makeText(this, "Ошибка загрузки категорий: " + task.getException(), Toast.LENGTH_SHORT).show();
                        loadFavorites(); // Продолжаем без категорий
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
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки избранного: " + e.getMessage());
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    favoritesList.clear();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        Log.d(TAG, "Получено избранных: " + snapshot.size());
                        int totalFavorites = snapshot.size();
                        int[] loadedCount = {0}; // Для отслеживания завершённых загрузок

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
                                                    favoritesList.add(product);
                                                    Log.d(TAG, "Добавлен товар: " + product.getName() + ", ID: " + productId);
                                                }
                                            } else {
                                                Log.w(TAG, "Товар не найден: " + productId);
                                            }
                                            loadedCount[0]++;
                                            if (loadedCount[0] == totalFavorites) {
                                                updateUI();
                                            }
                                        })
                                        .addOnFailureListener(ex -> {
                                            Log.e(TAG, "Ошибка загрузки товара " + productId + ": " + ex.getMessage());
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