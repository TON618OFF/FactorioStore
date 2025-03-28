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

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainPage extends Fragment {

    private static final int FILTER_SORT_REQUEST = 2;

    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private List<Product> productsList;
    private List<Product> filteredList;
    private Map<String, String> categoryNames;
    private String currentQuery = "";
    private boolean inStockFilter = false; // Фильтр "В наличии"
    private String priceSort = "none"; // "asc", "desc", "none"
    private String quantitySort = "none"; // "asc", "desc", "none"

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
        filteredList = new ArrayList<>();
        productAdapter = new ProductAdapter(getContext(), filteredList);
        productsRecyclerView.setAdapter(productAdapter);

        MaterialButton filterButton = view.findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> openFilterSortActivity());

        loadCategories();

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
            applyFiltersAndSort();
        } else if (requestCode == FILTER_SORT_REQUEST && resultCode == RESULT_OK && data != null) {
            inStockFilter = data.getBooleanExtra("inStock", false);
            priceSort = data.getStringExtra("priceSort");
            quantitySort = data.getStringExtra("quantitySort");
            applyFiltersAndSort();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProductsFromFirestore();
    }

    private void openFilterSortActivity() {
        Intent intent = new Intent(getActivity(), FilterSortActivity.class);
        intent.putExtra("inStock", inStockFilter);
        intent.putExtra("priceSort", priceSort);
        intent.putExtra("quantitySort", quantitySort);
        startActivityForResult(intent, FILTER_SORT_REQUEST);
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
            fetchProducts(new ArrayList<>());
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites")
                .addSnapshotListener((favoritesSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Ошибка загрузки избранного: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> favoriteIds = new ArrayList<>();
                    if (favoritesSnapshot != null) {
                        for (QueryDocumentSnapshot doc : favoritesSnapshot) {
                            String productId = doc.getString("productId");
                            if (productId != null) {
                                favoriteIds.add(productId);
                            }
                        }
                    }
                    fetchProducts(favoriteIds);
                });
    }

    private void fetchProducts(List<String> favoriteIds) {
        db.collection("products")
                .addSnapshotListener((productsSnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Ошибка загрузки товаров: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (productsSnapshot != null) {
                        productsList.clear();
                        filteredList.clear();
                        for (QueryDocumentSnapshot document : productsSnapshot) {
                            String name = document.getString("name");
                            String description = document.getString("description");
                            Long priceLong = document.getLong("price");
                            String imageUrl = document.getString("imageUrl");
                            String categoryId = document.getString("category");
                            Integer quantity = document.getLong("quantity").intValue();
                            if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                int price = priceLong.intValue();
                                String categoryName = categoryNames.getOrDefault(categoryId, "Без категории");
                                boolean isFavorite = favoriteIds.contains(document.getId());
                                Product product = new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName, quantity);
                                product.setFavorite(isFavorite);
                                productsList.add(product);
                            }
                        }
                        applyFiltersAndSort();
                    }
                });
    }

    public void searchProducts(String query) {
        currentQuery = query;
        applyFiltersAndSort();
    }

    private void applyFiltersAndSort() {
        filteredList.clear();
        String lowerQuery = currentQuery.trim().toLowerCase();

        // Фильтрация
        for (Product product : productsList) {
            boolean matchesQuery = lowerQuery.isEmpty();
            if (!lowerQuery.isEmpty()) {
                String lowerName = product.getName().toLowerCase();
                String[] words = lowerName.split("\\s+");
                for (String word : words) {
                    if (word.startsWith(lowerQuery)) {
                        matchesQuery = true;
                        break;
                    }
                }
            }

            boolean matchesStock = !inStockFilter || product.getQuantity() > 0;

            if (matchesQuery && matchesStock) {
                filteredList.add(product);
            }
        }

        // Сортировка по цене (если выбрана)
        if ("asc".equals(priceSort)) {
            Collections.sort(filteredList, Comparator.comparingInt(Product::getPrice));
        } else if ("desc".equals(priceSort)) {
            Collections.sort(filteredList, (p1, p2) -> Integer.compare(p2.getPrice(), p1.getPrice()));
        }
        // Сортировка по количеству (если выбрана и нет сортировки по цене)
        else if ("asc".equals(quantitySort) && !"asc".equals(priceSort) && !"desc".equals(priceSort)) {
            Collections.sort(filteredList, Comparator.comparingInt(Product::getQuantity));
        } else if ("desc".equals(quantitySort) && !"asc".equals(priceSort) && !"desc".equals(priceSort)) {
            Collections.sort(filteredList, (p1, p2) -> Integer.compare(p2.getQuantity(), p1.getQuantity()));
        }
        // Если нет сортировки, рандомизируем список
        else {
            Collections.shuffle(filteredList);
        }

        productAdapter.notifyDataSetChanged();
    }
}