package com.example.factorio;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName, productCategory, productDescription, productPrice, ratingValue, detailQuantity;
    private Button addToCartButton, submitReviewButton;
    private EditText reviewInput;
    private RadioGroup ratingRadioGroup;
    private RecyclerView reviewsRecyclerView;
    private LinearLayout addReviewContainer;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Review> reviewsList;
    private ReviewAdapter reviewAdapter;
    private String productId;
    private ImageView favoriteIcon;
    private Product product;
    private String currentUserReviewId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        productImage = findViewById(R.id.product_image);
        productName = findViewById(R.id.product_name);
        productCategory = findViewById(R.id.product_category);
        productDescription = findViewById(R.id.product_description);
        productPrice = findViewById(R.id.product_price);
        detailQuantity = findViewById(R.id.detail_quantity);
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
        reviewAdapter = new ReviewAdapter(reviewsList, this::showEditReviewDialog, this::deleteReview);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewAdapter);

        productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            loadProductDetails();
            loadReviews();
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            checkUserOrderAndReview(user.getUid());
        } else {
            addReviewContainer.setVisibility(View.GONE);
        }

        ratingRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int rating = getRatingFromRadioGroup(checkedId);
            ratingValue.setText(String.valueOf(rating));
        });

        addToCartButton.setOnClickListener(v -> {
            if (product != null && product.getQuantity() > 0) {
                CartItem cartItem = new CartItem(product.getId(), product.getName(), product.getPrice(), 1, product.getImageUrl());
                CartManager.getInstance().addToCart(cartItem);
                Toast.makeText(this, "Товар добавлен в корзину", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Товара нет в наличии", Toast.LENGTH_SHORT).show();
            }
        });

        favoriteIcon.setOnClickListener(v -> toggleFavorite(user));

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
                        product = document.toObject(Product.class);
                        if (product != null) {
                            product.setId(productId);
                            productName.setText(product.getName() != null ? product.getName() : "Без названия");
                            productDescription.setText(product.getDescription() != null ? product.getDescription() : "Без описания");
                            productPrice.setText(String.format("%d руб.", product.getPrice()));
                            detailQuantity.setText("В наличии: " + product.getQuantity());
                            Glide.with(this).load(product.getImageUrl()).placeholder(R.drawable.ic_placeholder).error(R.drawable.ic_placeholder).into(productImage);

                            String category = product.getCategory();
                            Log.d("ProductDetails", "Category from product: " + category);
                            if (category != null && !category.isEmpty()) {
                                db.collection("categories").document(category)
                                        .get()
                                        .addOnSuccessListener(categoryDoc -> {
                                            if (categoryDoc.exists()) {
                                                String categoryName = categoryDoc.getString("name");
                                                Log.d("ProductDetails", "CategoryName from Firestore: " + categoryName);
                                                if (categoryName != null && !categoryName.isEmpty()) {
                                                    product.setCategoryName(categoryName);
                                                } else {
                                                    product.setCategoryName("Без категории");
                                                }
                                            } else {
                                                product.setCategoryName("Категория не найдена");
                                            }
                                            productCategory.setText("Категория: " + product.getCategoryName());
                                        })
                                        .addOnFailureListener(e -> {
                                            product.setCategoryName("Ошибка загрузки");
                                            productCategory.setText("Категория: " + product.getCategoryName());
                                            Log.e("ProductDetails", "Error loading category: " + e.getMessage());
                                        });
                            } else {
                                product.setCategoryName("Не указана");
                                productCategory.setText("Категория: " + product.getCategoryName());
                            }

                            updateAddToCartButton();
                            checkFavoriteStatus();
                        } else {
                            Toast.makeText(this, "Ошибка: продукт не загружен", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки продукта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    productCategory.setText("Категория: Ошибка загрузки");
                });
    }

    private void loadReviews() {
        db.collection("products").document(productId).collection("reviews")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        reviewsList.clear();
                        currentUserReviewId = null;
                        FirebaseUser user = auth.getCurrentUser();
                        String userId = user != null ? user.getUid() : null;

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String nickname = document.getString("nickname");
                            String text = document.getString("text");
                            Long ratingLong = document.getLong("rating");
                            int rating = ratingLong != null ? ratingLong.intValue() : 0;
                            String reviewUserId = document.getString("userId");
                            Review review = new Review(nickname, text, rating, reviewUserId);
                            reviewsList.add(review);

                            if (userId != null && userId.equals(reviewUserId)) {
                                currentUserReviewId = document.getId();
                            }
                        }
                        reviewAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void checkUserOrderAndReview(String userId) {
        db.collection("users").document(userId).collection("orders_history")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    boolean hasOrdered = false;
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) document.get("items");
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                String itemProductId = (String) item.get("productId");
                                if (productId.equals(itemProductId)) {
                                    hasOrdered = true;
                                    break;
                                }
                            }
                        }
                        if (hasOrdered) break;
                    }

                    if (hasOrdered) {
                        Log.d("ProductDetails", "Заказ найден для productId: " + productId);
                        db.collection("products").document(productId).collection("reviews")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener(reviewSnapshot -> {
                                    if (reviewSnapshot.isEmpty()) {
                                        Log.d("ProductDetails", "Отзывов нет, показываем форму");
                                        addReviewContainer.setVisibility(View.VISIBLE);
                                    } else {
                                        Log.d("ProductDetails", "Отзыв уже существует, скрываем форму");
                                        addReviewContainer.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("ProductDetails", "Ошибка проверки отзывов: " + e.getMessage());
                                    addReviewContainer.setVisibility(View.GONE);
                                });
                    } else {
                        Log.d("ProductDetails", "Заказ не найден для productId: " + productId);
                        addReviewContainer.setVisibility(View.GONE);
                        Toast.makeText(this, "Вы можете оставить отзыв только после покупки товара", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProductDetails", "Ошибка проверки заказов: " + e.getMessage());
                    addReviewContainer.setVisibility(View.GONE);
                    Toast.makeText(this, "Ошибка проверки покупки", Toast.LENGTH_SHORT).show();
                });
    }

    private void submitReview(String nickname) {
        String reviewText = reviewInput.getText().toString().trim();
        int rating = getRatingFromRadioGroup(ratingRadioGroup.getCheckedRadioButtonId());

        if (reviewText.isEmpty()) {
            Toast.makeText(this, "Введите текст отзыва", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> review = new HashMap<>();
        review.put("nickname", nickname);
        review.put("text", reviewText);
        review.put("rating", rating);
        review.put("userId", auth.getCurrentUser().getUid());

        db.collection("products").document(productId).collection("reviews")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Отзыв добавлен", Toast.LENGTH_SHORT).show();
                    reviewInput.setText("");
                    ratingRadioGroup.check(R.id.rating_5);
                    ratingValue.setText("5");
                    addReviewContainer.setVisibility(View.GONE);
                    loadReviews();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEditReviewDialog(Review review) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_review, null);
        builder.setView(dialogView);

        EditText editReviewText = dialogView.findViewById(R.id.edit_review_text);
        RadioGroup editRatingGroup = dialogView.findViewById(R.id.edit_rating_radio_group);
        Button saveButton = dialogView.findViewById(R.id.save_review_button);

        editReviewText.setText(review.getText());
        setRatingInRadioGroup(editRatingGroup, review.getRating());

        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            String newText = editReviewText.getText().toString().trim();
            int newRating = getRatingFromRadioGroup(editRatingGroup.getCheckedRadioButtonId());

            if (newText.isEmpty()) {
                Toast.makeText(this, "Введите текст отзыва", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updatedReview = new HashMap<>();
            updatedReview.put("text", newText);
            updatedReview.put("rating", newRating);

            db.collection("products").document(productId).collection("reviews").document(currentUserReviewId)
                    .update(updatedReview)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Отзыв обновлён", Toast.LENGTH_SHORT).show();
                        loadReviews();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    private void deleteReview() {
        if (currentUserReviewId != null) {
            db.collection("products").document(productId).collection("reviews").document(currentUserReviewId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Отзыв удалён", Toast.LENGTH_SHORT).show();
                        addReviewContainer.setVisibility(View.VISIBLE);
                        loadReviews();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private int getRatingFromRadioGroup(int checkedId) {
        if (checkedId == R.id.rating_1 || checkedId == R.id.edit_rating_1) return 1;
        if (checkedId == R.id.rating_2 || checkedId == R.id.edit_rating_2) return 2;
        if (checkedId == R.id.rating_3 || checkedId == R.id.edit_rating_3) return 3;
        if (checkedId == R.id.rating_4 || checkedId == R.id.edit_rating_4) return 4;
        return 5;
    }

    private void setRatingInRadioGroup(RadioGroup group, int rating) {
        int radioButtonId;
        switch (rating) {
            case 1: radioButtonId = R.id.edit_rating_1; break;
            case 2: radioButtonId = R.id.edit_rating_2; break;
            case 3: radioButtonId = R.id.edit_rating_3; break;
            case 4: radioButtonId = R.id.edit_rating_4; break;
            default: radioButtonId = R.id.edit_rating_5;
        }
        group.check(radioButtonId);
    }

    private void updateAddToCartButton() {
        if (product != null && product.getQuantity() > 0) {
            addToCartButton.setText("Добавить в корзину");
            addToCartButton.setEnabled(true);
            addToCartButton.setBackgroundTintList(getResources().getColorStateList(R.color.circuit_green));
        } else {
            addToCartButton.setText("Нет в наличии");
            addToCartButton.setEnabled(false);
            addToCartButton.setBackgroundTintList(getResources().getColorStateList(R.color.coal_gray));
        }
    }

    private void checkFavoriteStatus() {
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

    private void toggleFavorite(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Войдите, чтобы добавить в избранное", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isFavorite = favoriteIcon.getTag() != null && (boolean) favoriteIcon.getTag();
        isFavorite = !isFavorite;
        favoriteIcon.setImageResource(isFavorite ? R.drawable.favorite_on : R.drawable.favorite);
        favoriteIcon.setTag(isFavorite);
        if (product != null) product.setFavorite(isFavorite);

        String userId = user.getUid();
        if (isFavorite) {
            Map<String, Object> favoriteData = new HashMap<>();
            favoriteData.put("productId", productId);
            favoriteData.put("addedAt", System.currentTimeMillis());
            db.collection("users").document(userId).collection("favorites").document(productId)
                    .set(favoriteData);
        } else {
            db.collection("users").document(userId).collection("favorites").document(productId)
                    .delete();
        }
    }
}