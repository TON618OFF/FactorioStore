package com.example.factorio;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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

public class MainPage extends Fragment {

    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private List<Product> productsList;
    private Map<String, String> categoryNames;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        db = FirebaseFirestore.getInstance();
        categoryNames = new HashMap<>();

        productsRecyclerView = view.findViewById(R.id.products_recycler_view);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        productsList = new ArrayList<>();
        productAdapter = new ProductAdapter(getContext(), productsList);
        productsRecyclerView.setAdapter(productAdapter);

        loadCategories();
        loadProductsFromFirestore();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String productId = data.getStringExtra("productId");
            boolean isFavorite = data.getBooleanExtra("isFavorite", false);
            for (Product product : productsList) {
                if (product.getId().equals(productId)) {
                    product.setFavorite(isFavorite);
                    break;
                }
            }
            productAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProductsFromFirestore();
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
                        loadProductsFromFirestore();
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки категорий: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProductsFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
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
                                int quantity = document.getLong("quantity").intValue(); // Загружаем quantity
                                if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                    int price = priceLong.intValue();
                                    String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                    productsList.add(new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName, quantity));
                                }
                            }
                            productAdapter.notifyDataSetChanged();
                        }
                    });
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites")
                .addSnapshotListener((favoritesSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> favoriteIds = new ArrayList<>();
                    if (favoritesSnapshot != null) {
                        for (QueryDocumentSnapshot doc : favoritesSnapshot) {
                            favoriteIds.add(doc.getString("productId"));
                        }
                    }

                    db.collection("products")
                            .addSnapshotListener((productsSnapshot, error) -> {
                                if (error != null) {
                                    Toast.makeText(getContext(), "Ошибка: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                if (productsSnapshot != null) {
                                    productsList.clear();
                                    for (QueryDocumentSnapshot document : productsSnapshot) {
                                        String name = document.getString("name");
                                        String description = document.getString("description");
                                        Long priceLong = document.getLong("price");
                                        String imageUrl = document.getString("imageUrl");
                                        String categoryId = document.getString("category");
                                        int quantity = document.getLong("quantity").intValue(); // Загружаем quantity
                                        if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                            int price = priceLong.intValue();
                                            String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                            boolean isFavorite = favoriteIds.contains(document.getId());
                                            Product product = new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName, quantity);
                                            product.setFavorite(isFavorite);
                                            productsList.add(product);
                                        }
                                    }
                                    productAdapter.notifyDataSetChanged();
                                }
                            });
                });
    }

    public void searchProducts(String query) {
        if (query.isEmpty()) {
            loadProductsFromFirestore();
            return;
        }

        db.collection("products")
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
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
                            int quantity = document.getLong("quantity").intValue(); // Загружаем quantity
                            if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                int price = priceLong.intValue();
                                String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                productsList.add(new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName, quantity));
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Ошибка поиска: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}