package com.example.factorio;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryProductsActivity extends AppCompatActivity {

    private static final String TAG = "CategoryProductsActivity";
    private RecyclerView categoryProductsRecyclerView;
    private TextView categoryTitle;
    private FloatingActionButton backFab;
    private ProductAdapter productAdapter;
    private List<Product> productsList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String categoryId;
    private String categoryName;
    private Set<String> favoriteIds;

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

        Log.d(TAG, "Открыта категория: " + categoryName + ", ID: " + categoryId);

        productsList = new ArrayList<>();
        favoriteIds = new HashSet<>();
        productAdapter = new ProductAdapter(this, productsList);

        categoryProductsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryProductsRecyclerView.setAdapter(productAdapter);

        categoryTitle.setText(categoryName);
        loadFavoritesAndProducts();

        backFab.setOnClickListener(v -> finish());
    }

    private void loadFavoritesAndProducts() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.d(TAG, "Пользователь не авторизован, загружаем товары без избранного");
            loadProductsByCategory(new HashSet<>());
            return;
        }

        String userId = user.getUid();
        db.collection("users").document(userId).collection("favorites")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Ошибка загрузки избранного: " + e.getMessage());
                        Toast.makeText(this, "Ошибка загрузки избранного: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        loadProductsByCategory(new HashSet<>());
                        return;
                    }
                    favoriteIds.clear();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            String productId = document.getString("productId");
                            if (productId != null) {
                                favoriteIds.add(productId);
                            }
                        }
                        Log.d(TAG, "Избранное загружено: " + favoriteIds.size() + " элементов");
                    }
                    loadProductsByCategory(favoriteIds);
                });
    }

    private void loadProductsByCategory(Set<String> favoriteIds) {
        if ("all".equals(categoryId)) {
            db.collection("products")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Ошибка загрузки всех товаров: " + e.getMessage());
                            Toast.makeText(this, "Ошибка загрузки товаров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        processProducts(snapshot, favoriteIds);
                    });
        } else {
            db.collection("products")
                    .whereEqualTo("category", categoryId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Ошибка загрузки товаров для категории " + categoryId + ": " + e.getMessage());
                            Toast.makeText(this, "Ошибка загрузки товаров: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        processProducts(snapshot, favoriteIds);
                    });
        }
    }

    private void processProducts(com.google.firebase.firestore.QuerySnapshot snapshot, Set<String> favoriteIds) {
        productsList.clear();
        if (snapshot != null) {
            Log.d(TAG, "Получено товаров: " + snapshot.size());
            for (QueryDocumentSnapshot document : snapshot) {
                Product product = document.toObject(Product.class);
                product.setId(document.getId());
                product.setFavorite(favoriteIds.contains(product.getId()));
                product.setCategoryName(categoryName);
                productsList.add(product);
                Log.d(TAG, "Товар добавлен: " + product.getName() + ", ID: " + product.getId());
            }
            productAdapter.notifyDataSetChanged();
            if (productsList.isEmpty()) {
                Log.w(TAG, "Список товаров пуст для категории: " + categoryId);
                Toast.makeText(this, "Товаров в этой категории нет", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "Snapshot равен null");
        }
    }
}