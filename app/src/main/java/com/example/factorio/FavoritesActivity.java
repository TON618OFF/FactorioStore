package com.example.factorio;

import android.os.Bundle;
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

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView favoritesRecyclerView;
    private TextView emptyFavoritesText;
    private ProductAdapter productAdapter;
    private List<Product> favoritesList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Map<String, String> categoryNames; // Для хранения имён категорий

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Инициализация компонентов
        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view);
        emptyFavoritesText = findViewById(R.id.empty_favorites_text);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        favoritesList = new ArrayList<>();
        categoryNames = new HashMap<>(); // Инициализация мапы категорий
        productAdapter = new ProductAdapter(this, favoritesList);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecyclerView.setAdapter(productAdapter);

        // Загружаем категории перед загрузкой избранного
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
                            }
                        }
                        // После загрузки категорий загружаем избранное
                        loadFavorites();
                    } else {
                        Toast.makeText(this, "Ошибка загрузки категорий: " + task.getException(), Toast.LENGTH_SHORT).show();
                        // Загружаем избранное даже при ошибке, но с "Без категории"
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
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    favoritesList.clear();
                    if (snapshot != null && !snapshot.isEmpty()) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            String productId = document.getString("productId");
                            if (productId != null) {
                                db.collection("products").document(productId)
                                        .get()
                                        .addOnSuccessListener(productDoc -> {
                                            if (productDoc.exists()) {
                                                String name = productDoc.getString("name");
                                                String description = productDoc.getString("description");
                                                Long priceLong = productDoc.getLong("price");
                                                String imageUrl = productDoc.getString("imageUrl");
                                                String categoryId = productDoc.getString("category");
                                                int quantity = productDoc.getLong("quantity").intValue();
                                                if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                                    // Получаем имя категории из мапы или "Без категории", если не найдено
                                                    String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                                    Product product = new Product(
                                                            name,
                                                            description,
                                                            priceLong.intValue(),
                                                            imageUrl,
                                                            productId,
                                                            categoryId,
                                                            categoryName,
                                                            quantity
                                                    );
                                                    product.setFavorite(true);
                                                    favoritesList.add(product);
                                                    updateUI();
                                                }
                                            }
                                        });
                            }
                        }
                    } else {
                        updateUI(); // Обновляем UI, если список пуст
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
    }
}