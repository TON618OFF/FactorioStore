package com.example.factorio;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class MainPage extends Fragment implements CartManager.OnCartChangedListener {
    private static final String TAG = "MainPage";
    private static final int FILTER_SORT_REQUEST = 2;
    private static final int FAVORITE_UPDATE_REQUEST = 1;

    private RecyclerView productsRecyclerView;
    private ProductAdapter productAdapter;
    private FirebaseFirestore db;
    private List<Product> productsList = new ArrayList<>();
    private List<Product> filteredList = new ArrayList<>();
    private Map<String, String> categoryNames;
    private String currentQuery = "";
    private boolean inStockFilter = false;
    private String priceSort = "none";
    private String quantitySort = "none";
    private double ratingFilter = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_page, container, false);

        db = FirebaseFirestore.getInstance();
        categoryNames = new HashMap<>();

        productsRecyclerView = view.findViewById(R.id.products_recycler_view);
        productsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        productAdapter = new ProductAdapter(getContext(), filteredList);
        productsRecyclerView.setAdapter(productAdapter);

        MaterialButton filterButton = view.findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> openFilterSortActivity());

        CartManager.getInstance().addOnCartChangedListener(this);

        loadCategories();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FAVORITE_UPDATE_REQUEST && resultCode == RESULT_OK && data != null) {
            String productId = data.getStringExtra("productId");
            boolean isFavorite = data.getBooleanExtra("isFavorite", false);
            updateFavoriteStatus(productId, isFavorite);
        } else if (requestCode == FILTER_SORT_REQUEST && resultCode == RESULT_OK && data != null) {
            inStockFilter = data.getBooleanExtra("inStock", false);
            priceSort = data.getStringExtra("priceSort");
            quantitySort = data.getStringExtra("quantitySort");
            ratingFilter = data.getDoubleExtra("ratingFilter", 0.0);
            applyFiltersAndSort();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProductsFromFirestore();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        CartManager.getInstance().removeOnCartChangedListener(this);
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        Log.d(TAG, "Корзина изменилась в MainPage, элементов: " + cartItems.size());
        for (CartItem item : cartItems) {
            int position = getProductPositionById(item.getProductId());
            if (position != -1) {
                productAdapter.notifyItemChanged(position);
            }
        }
        applyFiltersAndSort(); // Пересчитываем фильтры для актуального состояния
    }

    private void openFilterSortActivity() {
        Intent intent = new Intent(getActivity(), FilterSortActivity.class);
        intent.putExtra("inStock", inStockFilter);
        intent.putExtra("priceSort", priceSort);
        intent.putExtra("quantitySort", quantitySort);
        intent.putExtra("ratingFilter", ratingFilter);
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
                                Log.d(TAG, "Категория загружена: " + name + ", ID: " + document.getId());
                            }
                        }
                        loadProductsFromFirestore();
                    } else {
                        Log.e(TAG, "Ошибка загрузки категорий: ", task.getException());
                        Toast.makeText(getContext(), "Ошибка загрузки категорий", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadProductsFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        List<String> favoriteIds = new ArrayList<>();
        if (user != null) {
            String userId = user.getUid();
            db.collection("users").document(userId).collection("favorites")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                String productId = doc.getString("productId");
                                if (productId != null) {
                                    favoriteIds.add(productId);
                                }
                            }
                            Log.d(TAG, "Избранное загружено: " + favoriteIds.size() + " элементов");
                        } else {
                            Log.e(TAG, "Ошибка загрузки избранного: ", task.getException());
                        }
                        fetchProducts(favoriteIds);
                    });
        } else {
            fetchProducts(favoriteIds);
        }
    }

    private void fetchProducts(List<String> favoriteIds) {
        db.collection("products")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки товаров: ", e);
                        Toast.makeText(getContext(), "Ошибка загрузки товаров", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot != null) {
                        productsList.clear();
                        filteredList.clear();
                        Log.d(TAG, "Получено товаров: " + snapshot.size());
                        for (QueryDocumentSnapshot document : snapshot) {
                            Product product = document.toObject(Product.class);
                            if (product != null) {
                                product.setId(document.getId());
                                product.setFavorite(favoriteIds.contains(document.getId()));
                                String categoryName = categoryNames.getOrDefault(product.getCategory(), "Без категории");
                                product.setCategoryName(categoryName);
                                productsList.add(product);
                                Log.d(TAG, "Товар добавлен: " + product.getName() + ", ID: " + product.getId() + ", Rating: " + product.getAverageRating());
                            }
                        }
                        applyFiltersAndSort();
                    } else {
                        Log.w(TAG, "Snapshot равен null");
                    }
                });
    }

    public void searchProducts(String query) {
        currentQuery = query;
        applyFiltersAndSort();
    }

    private void updateFavoriteStatus(String productId, boolean isFavorite) {
        for (Product product : productsList) {
            if (product.getId().equals(productId)) {
                product.setFavorite(isFavorite);
                break;
            }
        }
        int position = getProductPositionById(productId);
        if (position != -1) {
            filteredList.get(position).setFavorite(isFavorite);
            productAdapter.notifyItemChanged(position);
        }
    }

    private void addToCart(Product product) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Войдите, чтобы добавить в корзину", Toast.LENGTH_SHORT).show();
            return;
        }
        if (product.getQuantity() <= 0) {
            Toast.makeText(getContext(), "Товара нет в наличии", Toast.LENGTH_SHORT).show();
            return;
        }
        CartItem cartItem = new CartItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                1,
                product.getImageUrl()
        );
        CartManager.getInstance().addToCart(cartItem);
        Toast.makeText(getContext(), product.getName() + " добавлен в корзину", Toast.LENGTH_SHORT).show();
        int position = getProductPositionById(product.getId());
        if (position != -1) {
            productAdapter.notifyItemChanged(position);
        }
    }

    private int getProductPositionById(String productId) {
        for (int i = 0; i < filteredList.size(); i++) {
            if (filteredList.get(i).getId().equals(productId)) {
                return i;
            }
        }
        return -1;
    }

    private void applyFiltersAndSort() {
        filteredList.clear();
        String lowerQuery = currentQuery.trim().toLowerCase();

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
            boolean matchesRating = ratingFilter == 0.0 || product.getAverageRating() >= ratingFilter;

            if (matchesQuery && matchesStock && matchesRating) {
                filteredList.add(product);
                Log.d(TAG, "Товар соответствует фильтру: " + product.getName() + ", Rating: " + product.getAverageRating());
            }
        }

        // Сортировка
        if ("asc".equals(priceSort)) {
            Collections.sort(filteredList, Comparator.comparingInt(Product::getPrice));
        } else if ("desc".equals(priceSort)) {
            Collections.sort(filteredList, (p1, p2) -> Integer.compare(p2.getPrice(), p1.getPrice()));
        } else if ("asc".equals(quantitySort)) {
            Collections.sort(filteredList, Comparator.comparingInt(Product::getQuantity));
        } else if ("desc".equals(quantitySort)) {
            Collections.sort(filteredList, (p1, p2) -> Integer.compare(p2.getQuantity(), p1.getQuantity()));
        } else {
            Collections.sort(filteredList, (p1, p2) -> {
                if (p1.getTimestamp() == null || p2.getTimestamp() == null) return 0;
                return p2.getTimestamp().compareTo(p1.getTimestamp());
            });
        }

        productAdapter.notifyDataSetChanged();
        Log.d(TAG, "Фильтрация и сортировка завершены, товаров в filteredList: " + filteredList.size());
    }
}