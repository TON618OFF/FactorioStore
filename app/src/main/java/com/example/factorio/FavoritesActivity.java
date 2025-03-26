package com.example.factorio;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView favoritesRecyclerView;
    private ProductAdapter productAdapter;
    private List<Product> favoritesList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        favoritesList = new ArrayList<>();
        productAdapter = new ProductAdapter(this, favoritesList);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecyclerView.setAdapter(productAdapter);

        loadFavorites();
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
                .addOnSuccessListener(favoritesSnapshot -> {
                    List<String> favoriteIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : favoritesSnapshot) {
                        favoriteIds.add(doc.getString("productId"));
                    }

                    if (favoriteIds.isEmpty()) {
                        Toast.makeText(this, "Список избранного пуст", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("products")
                            .get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    favoritesList.clear();
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        if (favoriteIds.contains(document.getId())) {
                                            String name = document.getString("name");
                                            String description = document.getString("description");
                                            Long priceLong = document.getLong("price");
                                            String imageUrl = document.getString("imageUrl");
                                            String categoryId = document.getString("category");
                                            if (name != null && description != null && priceLong != null && imageUrl != null && categoryId != null) {
                                                int price = priceLong.intValue();
                                                String categoryName = ""; // Загрузите категорию, если нужно
                                                Product product = new Product(name, description, price, imageUrl, document.getId(), categoryId, categoryName);
                                                product.setFavorite(true); // Все товары в избранном
                                                favoritesList.add(product);
                                            }
                                        }
                                    }
                                    productAdapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(this, "Ошибка загрузки: " + task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}