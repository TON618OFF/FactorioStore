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
import androidx.core.content.ContextCompat;
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

public class ProductDetailsActivity extends AppCompatActivity implements CartManager.OnCartChangedListener {
    private static final String TAG = "ProductDetailsActivity";

    private ImageView productImage;
    private TextView productName, productCategory, productDescription, productPrice, ratingValue, detailQuantity, averageRatingValue;
    private Button addToCartButton, submitReviewButton;
    private EditText reviewInput;
    private RadioGroup ratingRadioGroup;
    private RecyclerView reviewsRecyclerView;
    private LinearLayout addReviewContainer, quantityLayout;
    private ImageView favoriteIcon, increaseButton, decreaseButton;
    private TextView quantityText;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Review> reviewsList;
    private ReviewAdapter reviewAdapter;
    private String productId;
    private Product product;
    private String currentUserReviewId;
    private CartManager cartManager;
    private int cartQuantity; // Локальное состояние корзины

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
        averageRatingValue = findViewById(R.id.average_rating_value);
        quantityLayout = findViewById(R.id.quantity_layout);
        increaseButton = findViewById(R.id.increase_quantity_button);
        decreaseButton = findViewById(R.id.decrease_quantity_button);
        quantityText = findViewById(R.id.quantity_text);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        cartManager = CartManager.getInstance();
        reviewsList = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(reviewsList, this::showEditReviewDialog, this::deleteReview);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setAdapter(reviewAdapter);

        productId = getIntent().getStringExtra("productId");
        if (productId == null) {
            Toast.makeText(this, "Ошибка: productId не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cartManager.loadCartFromFirestore(items -> {
            Log.d(TAG, "Корзина загружена, элементов: " + items.size());
            cartQuantity = cartManager.getItemQuantity(productId);
            loadProductDetails();
        });

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
            if (user == null) {
                Toast.makeText(this, "Войдите, чтобы добавить в корзину", Toast.LENGTH_SHORT).show();
                return;
            }
            if (product == null || product.getQuantity() <= 0) {
                Toast.makeText(this, "Товара нет в наличии", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Long quantity = document.getLong("quantity");
                            int availableQuantity = quantity != null ? quantity.intValue() : 0;
                            if (availableQuantity > 0) {
                                CartItem cartItem = new CartItem(productId, product.getName(),
                                        product.getPrice(), 1, product.getImageUrl());
                                cartManager.addToCart(cartItem);
                                cartQuantity = 1;
                                updateAddToCartButton();
                                Toast.makeText(this, product.getName() + " добавлен в корзину", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Товара нет в наличии", Toast.LENGTH_SHORT).show();
                                product.setQuantity(0);
                                updateAddToCartButton();
                            }
                        } else {
                            Toast.makeText(this, "Товар не найден", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка проверки наличия", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Ошибка Firestore: " + e.getMessage());
                    });
        });

        increaseButton.setOnClickListener(v -> {
            if (user == null) {
                Toast.makeText(this, "Войдите, чтобы изменить количество", Toast.LENGTH_SHORT).show();
                return;
            }
            if (product == null) return;
            db.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Long quantity = document.getLong("quantity");
                            int availableQuantity = quantity != null ? quantity.intValue() : 0;
                            if (cartQuantity < availableQuantity) {
                                cartManager.updateQuantity(productId, cartQuantity + 1);
                                cartQuantity++;
                                updateAddToCartButton();
                            } else {
                                Toast.makeText(this, "Нельзя добавить больше, чем есть в наличии", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка проверки наличия", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Ошибка Firestore: " + e.getMessage());
                    });
        });

        decreaseButton.setOnClickListener(v -> {
            if (user == null) {
                Toast.makeText(this, "Войдите, чтобы изменить количество", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cartQuantity > 0) {
                cartManager.updateQuantity(productId, cartQuantity - 1);
                cartQuantity--;
                updateAddToCartButton();
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
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Ошибка загрузки данных пользователя", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Войдите, чтобы оставить отзыв", Toast.LENGTH_SHORT).show();
            }
        });

        cartManager.addOnCartChangedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cartManager.removeOnCartChangedListener(this);
    }

    @Override
    public void onCartChanged(List<CartItem> cartItems) {
        Log.d(TAG, "Корзина изменилась, элементов: " + cartItems.size());
        cartQuantity = cartManager.getItemQuantity(productId);
        updateAddToCartButton();
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

                            String imageUrl = product.getImageUrl();
                            if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("/1")) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_placeholder)
                                        .error(R.drawable.ic_placeholder)
                                        .into(productImage);
                            } else {
                                productImage.setImageResource(R.drawable.ic_placeholder);
                            }

                            double avgRating = product.getAverageRating();
                            averageRatingValue.setText(String.format("%.1f", avgRating));

                            String category = product.getCategory();
                            if (category != null && !category.isEmpty()) {
                                db.collection("categories").document(category)
                                        .get()
                                        .addOnSuccessListener(categoryDoc -> {
                                            String categoryName = categoryDoc.exists() ? categoryDoc.getString("name") : "Без категории";
                                            product.setCategoryName(categoryName != null ? categoryName : "Без категории");
                                            productCategory.setText("Категория: " + product.getCategoryName());
                                        })
                                        .addOnFailureListener(e -> {
                                            product.setCategoryName("Ошибка загрузки");
                                            productCategory.setText("Категория: Ошибка");
                                        });
                            } else {
                                product.setCategoryName("Не указана");
                                productCategory.setText("Категория: Не указана");
                            }

                            updateAddToCartButton();
                            checkFavoriteStatus();
                            loadReviews();
                        } else {
                            Toast.makeText(this, "Ошибка: продукт не удалось загрузить", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Продукт не найден", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка загрузки продукта", Toast.LENGTH_SHORT).show();
                    productCategory.setText("Категория: Ошибка");
                });
    }

    private void updateAddToCartButton() {
        if (product == null) return;

        Log.d(TAG, "Обновление кнопки корзины, товар: " + product.getName() + ", количество: " + cartQuantity);
        if (cartQuantity > 0) {
            addToCartButton.setVisibility(View.GONE);
            quantityLayout.setVisibility(View.VISIBLE);
            quantityText.setText(String.valueOf(cartQuantity));
            decreaseButton.setEnabled(true);
            increaseButton.setEnabled(cartQuantity < product.getQuantity());
        } else {
            addToCartButton.setVisibility(View.VISIBLE);
            quantityLayout.setVisibility(View.GONE);
            addToCartButton.setText(product.getQuantity() > 0 ? "Добавить в корзину" : "Нет в наличии");
            addToCartButton.setEnabled(product.getQuantity() > 0);
            addToCartButton.setBackgroundTintList(ContextCompat.getColorStateList(
                    this, product.getQuantity() > 0 ? R.color.circuit_green : R.color.coal_gray));
        }
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
                        updateAverageRating();
                    } else {
                        Log.e(TAG, "Ошибка загрузки отзывов: ", task.getException());
                        Toast.makeText(this, "Ошибка загрузки отзывов", Toast.LENGTH_SHORT).show();
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
                        db.collection("products").document(productId).collection("reviews")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener(reviewSnapshot -> {
                                    if (reviewSnapshot.isEmpty()) {
                                        addReviewContainer.setVisibility(View.VISIBLE);
                                    } else {
                                        addReviewContainer.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    addReviewContainer.setVisibility(View.GONE);
                                });
                    } else {
                        addReviewContainer.setVisibility(View.GONE);
                        Toast.makeText(this, "Отзывы доступны только после покупки", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
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
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка добавления отзыва", Toast.LENGTH_SHORT).show());
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
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка обновления отзыва", Toast.LENGTH_SHORT).show());
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
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка удаления отзыва", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateAverageRating() {
        if (product == null) {
            averageRatingValue.setText("0.0");
            return;
        }

        if (reviewsList.isEmpty()) {
            product.setAverageRating(0.0);
            averageRatingValue.setText("0.0");
            updateFirestoreRating(0.0);
            return;
        }

        double totalRating = 0;
        for (Review review : reviewsList) {
            totalRating += review.getRating();
        }
        double avgRating = totalRating / reviewsList.size();
        product.setAverageRating(avgRating);
        averageRatingValue.setText(String.format("%.1f", avgRating));
        updateFirestoreRating(avgRating);
    }

    private void updateFirestoreRating(double avgRating) {
        db.collection("products").document(productId)
                .update("averageRating", avgRating)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Средний рейтинг обновлён: " + avgRating))
                .addOnFailureListener(e -> Log.e(TAG, "Ошибка обновления рейтинга"));
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

    private void checkFavoriteStatus() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).collection("favorites").document(productId)
                    .get()
                    .addOnSuccessListener(favDoc -> {
                        boolean isFavorite = favDoc.exists();
                        if (product != null) {
                            product.setFavorite(isFavorite);
                            favoriteIcon.setImageResource(isFavorite ? R.drawable.favorite_on : R.drawable.favorite);
                            favoriteIcon.setTag(isFavorite);
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Ошибка проверки избранного"));
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
                    .set(favoriteData)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка добавления в избранное", Toast.LENGTH_SHORT).show();
                        favoriteIcon.setImageResource(R.drawable.favorite);
                        favoriteIcon.setTag(false);
                        if (product != null) product.setFavorite(false);
                    });
        } else {
            db.collection("users").document(userId).collection("favorites").document(productId)
                    .delete()
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Ошибка удаления из избранного", Toast.LENGTH_SHORT).show();
                        favoriteIcon.setImageResource(R.drawable.favorite_on);
                        favoriteIcon.setTag(true);
                        if (product != null) product.setFavorite(true);
                    });
        }
    }
}