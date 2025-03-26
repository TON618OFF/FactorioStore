package com.example.factorio;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainPage extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private RecyclerView productsRecyclerView;
    private CategoryAdapter categoryAdapter;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private List<Category> categoriesList;
    private List<Product> productsList;
    private Map<String, String> categoryNames;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        db = FirebaseFirestore.getInstance();
        categoryNames = new HashMap<>();

        // Настройка RecyclerView для категорий
        categoriesRecyclerView = view.findViewById(R.id.categories_recycler_view);
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        categoriesList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(categoriesList);
        categoriesRecyclerView.setAdapter(categoryAdapter);

        // Настройка RecyclerView для товаров
        productsRecyclerView = view.findViewById(R.id.products_recycler_view);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        productsList = new ArrayList<>();
        productAdapter = new ProductAdapter(getContext(), productsList);
        productsRecyclerView.setAdapter(productAdapter);

        // Загрузка данных из Firestore
        loadCategoriesAndProducts();

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

    private void loadCategoriesAndProducts() {
        db.collection("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        categoriesList.clear();
                        categoryNames.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String name = document.getString("name");
                            String imageUrl = document.getString("imageUrl");
                            if (name != null && imageUrl != null) {
                                categoriesList.add(new Category(name, imageUrl));
                                categoryNames.put(document.getId(), name);
                            }
                        }
                        categoriesList.sort((c1, c2) -> {
                            int id1 = Integer.parseInt(task.getResult().getDocuments().get(categoriesList.indexOf(c1)).getId());
                            int id2 = Integer.parseInt(task.getResult().getDocuments().get(categoriesList.indexOf(c2)).getId());
                            return Integer.compare(id1, id2);
                        });
                        categoryAdapter.notifyDataSetChanged();
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
                                if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                    int price = priceLong.intValue();
                                    String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                    productsList.add(new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName));
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
                                        if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                            int price = priceLong.intValue();
                                            String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                            boolean isFavorite = favoriteIds.contains(document.getId());
                                            Product product = new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName);
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
                            if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                int price = priceLong.intValue();
                                String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                productsList.add(new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName));
                            }
                        }
                        productAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Ошибка поиска: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}