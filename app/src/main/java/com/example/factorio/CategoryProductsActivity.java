package com.example.factorio;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryProductsActivity extends AppCompatActivity {

    private RecyclerView categoryProductsRecyclerView;
    private TextView categoryTitle;
    private FloatingActionButton backFab;
    private ProductAdapter productAdapter;
    private List<Product> productsList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String categoryId;
    private String categoryName;
    private Set<String> favoriteIds; // Для хранения ID избранных товаров

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_products);

        categoryProductsRecyclerView = findViewById(R.id.category_products_recycler_view);
        categoryTitle = findViewById(R.id.category_title);
        backFab = findViewById(R.id.back_fab);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        categoryId = getIntent().getStringExtra("categoryId");
        categoryName = getIntent().getStringExtra("categoryName");

        productsList = new ArrayList<>();
        favoriteIds = new HashSet<>();
        productAdapter = new ProductAdapter(this, productsList);

        categoryProductsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        categoryProductsRecyclerView.setAdapter(productAdapter);

        categoryTitle.setText(categoryName);
        loadFavoritesAndProducts(); // Загружаем избранное и товары

        backFab.setOnClickListener(v -> finish());
    }

    private void loadFavoritesAndProducts() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Если пользователь не авторизован, просто загружаем товары без избранного
            loadProductsByCategory(new HashSet<>());
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        favoriteIds.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String productId = document.getString("productId");
                            if (productId != null) {
                                favoriteIds.add(productId);
                            }
                        }
                        loadProductsByCategory(favoriteIds); // Передаём список избранных ID
                    } else {
                        Toast.makeText(this, "Ошибка загрузки избранного: " + task.getException(), Toast.LENGTH_SHORT).show();
                        loadProductsByCategory(new HashSet<>()); // Загружаем без избранного при ошибке
                    }
                });
    }

    private void loadProductsByCategory(Set<String> favoriteIds) {
        if ("all".equals(categoryId)) {
            db.collection("products")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            productsList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                String description = document.getString("description");
                                Long priceLong = document.getLong("price");
                                String imageUrl = document.getString("imageUrl");
                                String categoryId = document.getString("category");
                                int quantity = document.getLong("quantity").intValue();
                                if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                    String catName = categoryName.equals("Все категории") ? "Без категории" : categoryName;
                                    Product product = new Product(name, description, priceLong.intValue(), imageUrl, document.getId(), categoryId, catName, quantity);
                                    product.setFavorite(favoriteIds.contains(document.getId())); // Устанавливаем статус избранного
                                    productsList.add(product);
                                }
                            }
                            productAdapter.notifyDataSetChanged();
                        }
                    });
        } else {
            db.collection("products")
                    .whereEqualTo("category", categoryId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            productsList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String name = document.getString("name");
                                String description = document.getString("description");
                                Long priceLong = document.getLong("price");
                                String imageUrl = document.getString("imageUrl");
                                String categoryId = document.getString("category");
                                int quantity = document.getLong("quantity").intValue();
                                if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                    Product product = new Product(name, description, priceLong.intValue(), imageUrl, document.getId(), categoryId, categoryName, quantity);
                                    product.setFavorite(favoriteIds.contains(document.getId())); // Устанавливаем статус избранного
                                    productsList.add(product);
                                }
                            }
                            productAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }
}