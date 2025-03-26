package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName, productCategory, productDescription, productPrice, ratingValue;
    private Button addToCartButton, submitReviewButton;
    private EditText reviewInput;
    private RadioGroup ratingRadioGroup;
    private RecyclerView reviewsRecyclerView;
    private LinearLayout addReviewContainer;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Review> reviewsList; // Если Review вынесен, используйте просто Review
    private ReviewAdapter reviewAdapter; // Теперь используем отдельный класс
    private String productId;
    private ImageView favoriteIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        // Инициализация компонентов
        productImage = findViewById(R.id.product_image);
        productName = findViewById(R.id.product_name);
        productCategory = findViewById(R.id.product_category);
        productDescription = findViewById(R.id.product_description);
        productPrice = findViewById(R.id.product_price);
        addToCartButton = findViewById(R.id.add_to_cart_button);
        reviewInput = findViewById(R.id.review_input);
        ratingRadioGroup = findViewById(R.id.rating_radio_group);
        ratingValue = findViewById(R.id.rating_value);
        submitReviewButton = findViewById(R.id.submit_review_button);
        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view);
        addReviewContainer = findViewById(R.id.add_review_container);
        favoriteIcon = findViewById(R.id.favorite_icon);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        reviewsList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewsList); // Используем новый ReviewAdapter
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewAdapter);

        // Получение productId из Intent
        productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            loadProductDetails();
            loadReviews();
        }

        // Проверка авторизации для отображения поля добавления отзыва
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            addReviewContainer.setVisibility(View.VISIBLE);
        } else {
            addReviewContainer.setVisibility(View.GONE);
        }

        // Обработка выбора рейтинга
        ratingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int rating = 5;
            if (checkedId == R.id.rating_1) rating = 1;
            else if (checkedId == R.id.rating_2) rating = 2;
            else if (checkedId == R.id.rating_3) rating = 3;
            else if (checkedId == R.id.rating_4) rating = 4;
            else if (checkedId == R.id.rating_5) rating = 5;
            ratingValue.setText(String.valueOf(rating));
        });

        // Добавление в корзину
        addToCartButton.setOnClickListener(v -> {
            db.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("name");
                            Long priceLong = document.getLong("price");
                            String imageUrl = document.getString("imageUrl");
                            CartItem cartItem = new CartItem(
                                    productId,
                                    name,
                                    priceLong != null ? priceLong.intValue() : 0,
                                    1,
                                    imageUrl
                            );
                            CartManager.getInstance().addToCart(cartItem);
                            Toast.makeText(ProductDetailsActivity.this, "Товар добавлен в корзину", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Обработка нажатия на favoriteIcon
        favoriteIcon.setOnClickListener(v -> {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isFavorite = favoriteIcon.getTag() != null && (boolean) favoriteIcon.getTag();
            isFavorite = !isFavorite;
            favoriteIcon.setImageResource(isFavorite ? R.drawable.favorite_on : R.drawable.favorite);
            favoriteIcon.setTag(isFavorite);

            String userId = currentUser.getUid();
            if (isFavorite) {
                Map<String, Object> favoriteData = new HashMap<>();
                favoriteData.put("productId", productId);
                favoriteData.put("addedAt", System.currentTimeMillis());
                db.collection("users").document(userId).collection("favorites").document(productId)
                        .set(favoriteData)
                        .addOnSuccessListener(aVoid -> {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("productId", productId);
                            resultIntent.putExtra("isFavorite", true);
                            setResult(RESULT_OK, resultIntent);
                        });
            } else {
                db.collection("users").document(userId).collection("favorites").document(productId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("productId", productId);
                            resultIntent.putExtra("isFavorite", false);
                            setResult(RESULT_OK, resultIntent);
                        });
            }
        });
        // Отправка отзыва
        submitReviewButton.setOnClickListener(v -> {
            if (user != null) {
                db.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(document -> {
                            if (document.exists()) {
                                String nickname = document.getString("nickname");
                                if (nickname != null && !nickname.isEmpty()) {
                                    submitReview(nickname);
                                } else {
                                    Toast.makeText(this, "Никнейм не найден", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                Toast.makeText(this, "Войдите, чтобы оставить отзыв", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProductDetails() {
        db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");
                        String description = document.getString("description");
                        Long priceLong = document.getLong("price");
                        String imageUrl = document.getString("imageUrl");
                        String categoryId = document.getString("category");

                        Product product = new Product(
                                name,
                                description,
                                priceLong != null ? priceLong.intValue() : 0,
                                imageUrl,
                                productId,
                                categoryId,
                                getIntent().getStringExtra("categoryName")
                        );

                        productName.setText(product.getName());
                        productDescription.setText(product.getDescription());
                        productPrice.setText(String.format("%d руб.", product.getPrice()));
                        productCategory.setText("Категория: " + product.getCategoryName()); // Добавьте эту строку
                        Glide.with(this).load(product.getImageUrl()).placeholder(R.drawable.ic_placeholder).error(R.drawable.ic_placeholder).into(productImage);

                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                                    .get()
                                    .addOnSuccessListener(favDoc -> {
                                        boolean isFavorite = favDoc.exists();
                                        product.setFavorite(isFavorite);
                                        favoriteIcon.setImageResource(isFavorite ? R.drawable.favorite_on : R.drawable.favorite);
                                        favoriteIcon.setTag(isFavorite);
                                    });
                        }
                    }
                });
    }

    private void loadReviews() {
        db.collection("products").document(productId).collection("reviews")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String nickname = document.getString("nickname");
                            String text = document.getString("text");
                            Long ratingLong = document.getLong("rating");
                            int rating = ratingLong != null ? ratingLong.intValue() : 0;
                            reviewsList.add(new Review(nickname, text, rating)); // Если Review вынесен
                        }
                        reviewAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void submitReview(String nickname) {
        String reviewText = reviewInput.getText().toString().trim();
        int rating = 5;
        int checkedRadioButtonId = ratingRadioGroup.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rating_1) rating = 1;
        else if (checkedRadioButtonId == R.id.rating_2) rating = 2;
        else if (checkedRadioButtonId == R.id.rating_3) rating = 3;
        else if (checkedRadioButtonId == R.id.rating_4) rating = 4;
        else if (checkedRadioButtonId == R.id.rating_5) rating = 5;

        if (reviewText.isEmpty()) {
            Toast.makeText(this, "Введите текст отзыва", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> review = new HashMap<>();
        review.put("nickname", nickname);
        review.put("text", reviewText);
        review.put("rating", rating);

        db.collection("products").document(productId).collection("reviews")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Отзыв добавлен", Toast.LENGTH_SHORT).show();
                    reviewInput.setText("");
                    ratingRadioGroup.check(R.id.rating_5);
                    ratingValue.setText("5");
                    loadReviews();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}