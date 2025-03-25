package com.example.factorio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
    private SeekBar ratingSeekBar;
    private RecyclerView reviewsRecyclerView;
    private LinearLayout addReviewContainer;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Review> reviewsList;
    private ReviewAdapter reviewAdapter;
    private String productId;

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
        ratingSeekBar = findViewById(R.id.rating_seekbar);
        ratingValue = findViewById(R.id.rating_value);
        submitReviewButton = findViewById(R.id.submit_review_button);
        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view);
        addReviewContainer = findViewById(R.id.add_review_container);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        reviewsList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewsList);
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

        // Обработка рейтинга
        ratingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ratingValue.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Добавление в корзину
        addToCartButton.setOnClickListener(v -> {
            Toast.makeText(this, "Товар добавлен в корзину", Toast.LENGTH_SHORT).show();
        });

        // Отправка отзыва
        submitReviewButton.setOnClickListener(v -> {
            if (user != null) {
                submitReview(user.getEmail());
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

                        productName.setText(name);
                        productDescription.setText(description);
                        productPrice.setText(String.format("%d руб.", priceLong != null ? priceLong.intValue() : 0));
                        Glide.with(this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_placeholder)
                                .error(R.drawable.ic_placeholder)
                                .into(productImage);

                        // Загрузка названия категории
                        if (categoryId != null) {
                            db.collection("categories").document(categoryId)
                                    .get()
                                    .addOnSuccessListener(categoryDoc -> {
                                        if (categoryDoc.exists()) {
                                            String categoryName = categoryDoc.getString("name");
                                            productCategory.setText("Категория: " + (categoryName != null ? categoryName : "Без категории"));
                                        } else {
                                            productCategory.setText("Категория: Не найдена");
                                        }
                                    })
                                    .addOnFailureListener(e -> productCategory.setText("Категория: Ошибка загрузки"));
                        } else {
                            productCategory.setText("Категория: Не указана");
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки товара: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadReviews() {
        db.collection("products").document(productId).collection("reviews")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String email = document.getString("userEmail");
                            String text = document.getString("text");
                            Long ratingLong = document.getLong("rating");
                            int rating = ratingLong != null ? ratingLong.intValue() : 0;
                            reviewsList.add(new Review(email, text, rating));
                        }
                        reviewAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Ошибка загрузки отзывов: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void submitReview(String userEmail) {
        String reviewText = reviewInput.getText().toString().trim();
        int rating = ratingSeekBar.getProgress();

        if (reviewText.isEmpty()) {
            Toast.makeText(this, "Введите текст отзыва", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> review = new HashMap<>();
        review.put("userEmail", userEmail);
        review.put("text", reviewText);
        review.put("rating", rating);

        db.collection("products").document(productId).collection("reviews")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Отзыв добавлен", Toast.LENGTH_SHORT).show();
                    reviewInput.setText("");
                    ratingSeekBar.setProgress(5);
                    loadReviews();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка добавления отзыва: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Модель для отзыва
    private static class Review {
        String userEmail;
        String text;
        int rating;

        Review(String userEmail, String text, int rating) {
            this.userEmail = userEmail;
            this.text = text;
            this.rating = rating;
        }
    }

    // Адаптер для отзывов
    private static class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private List<Review> reviews;

        ReviewAdapter(List<Review> reviews) {
            this.reviews = reviews;
        }

        @NonNull
        @Override
        public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
            return new ReviewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
            Review review = reviews.get(position);
            holder.userEmail.setText(review.userEmail);
            holder.reviewText.setText(review.text);
            holder.reviewRating.setText(String.valueOf(review.rating));
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        static class ReviewViewHolder extends RecyclerView.ViewHolder {
            TextView userEmail, reviewText, reviewRating;

            ReviewViewHolder(@NonNull View itemView) {
                super(itemView);
                userEmail = itemView.findViewById(R.id.review_user_email);
                reviewText = itemView.findViewById(R.id.review_text);
                reviewRating = itemView.findViewById(R.id.review_rating);
            }
        }
    }
}